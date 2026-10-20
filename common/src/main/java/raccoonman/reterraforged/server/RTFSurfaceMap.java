package raccoonman.reterraforged.server;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.cell.Cell;

/**
 * "rtf surfacemap <biome> [chunks]" (with -Drtf.selftest=true): the ground seen from above where the biome is, as a map
 * mod draws it (issue #2: stone and blackstone in chunk-sized squares on the mountains of William Wythers' Overhauled
 * Overworld). Builds chunks x chunks chunks around the nearest place of the biome and writes
 * rtf-surfacemap-<rtf|vanilla>-<biome>.png (map colours shaded by height). The ground block changes between two
 * neighbouring columns about as often across a chunk border as inside a chunk, unless something works chunk by chunk;
 * prints "[RTF surfacemap] <rtf|vanilla> <biome> seam=S border=B inner=I at x z top=<blocks>" where B and I are the
 * share of neighbour pairs with different ground blocks across borders and inside chunks, S = B / I.
 */
public final class RTFSurfaceMap {

	private RTFSurfaceMap() {
	}

	public static String run(MinecraftServer server, String biome, int chunks) {
		try {
			return map(server, biome, chunks);
		} catch (Throwable t) {
			RTFCommon.LOGGER.error("[RTF surfacemap] exception", t);
			return "[RTF surfacemap] FAIL exception: " + t;
		}
	}

	private static String map(MinecraftServer server, String biome, int chunks) throws IOException {
		ServerLevel level = server.overworld();
		boolean rtf = (Object) level.getChunkSource().randomState() instanceof RTFRandomState s && s.generatorContext() != null;
		String kind = rtf ? "rtf" : "vanilla";
		ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, ResourceLocation.parse(biome));
		long t0 = System.nanoTime();
		BlockPos centre = rtf ? find(level, key, 8000) : null;
		if (centre == null) {
			var found = RTFWorldChecks.surfaceBiome(level, key, rtf ? 0 : 8000);
			if (found != null) centre = found.getFirst();
		}
		if (centre == null) return "[RTF surfacemap] " + kind + " " + biome + " FAIL not found";
		RTFCommon.LOGGER.info("[RTF surfacemap] {} at {} {} found in {} ms", biome, centre.getX(), centre.getZ(), (System.nanoTime() - t0) / 1_000_000);
		int size = chunks * 16;
		int cx0 = (centre.getX() >> 4) - chunks / 2, cz0 = (centre.getZ() >> 4) - chunks / 2;
		Block[][] top = new Block[size][size];
		int[][] height = new int[size][size];
		Map<Block, Integer> counts = new HashMap<>();
		for (int cx = 0; cx < chunks; cx++) for (int cz = 0; cz < chunks; cz++) {
			ChunkAccess c = level.getChunk(cx0 + cx, cz0 + cz, ChunkStatus.FULL, true);
			for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
				// the highest solid block: under snow layers, grass and flowers
				int y = c.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
				BlockState state = c.getBlockState(new BlockPos(x, y, z));
				height[cx * 16 + x][cz * 16 + z] = y;
				top[cx * 16 + x][cz * 16 + z] = state.getBlock();
				counts.merge(state.getBlock(), 1, Integer::sum);
			}
		}
		// per chunk: the biome under the ground block (what the "biome" placement filter sees) and the ground blocks,
		// to rtf-surfacemap-<kind>-<biome>.txt
		List<String> grid = new ArrayList<>();
		grid.add("# chunk x z: share of columns whose biome at the ground is " + biome + ", most common ground block");
		for (int cz = 0; cz < chunks; cz++) {
			StringBuilder line = new StringBuilder();
			for (int cx = 0; cx < chunks; cx++) {
				int same = 0;
				Map<Block, Integer> chunkCounts = new HashMap<>();
				for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
					int wx = (cx0 + cx) * 16 + x, wz = (cz0 + cz) * 16 + z;
					if (level.getBiome(new BlockPos(wx, height[cx * 16 + x][cz * 16 + z], wz)).is(key)) same++;
					chunkCounts.merge(top[cx * 16 + x][cz * 16 + z], 1, Integer::sum);
				}
				Block main = chunkCounts.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
				String n = BuiltInRegistries.BLOCK.getKey(main).getPath();
				line.append(String.format(Locale.ROOT, "%3d%%%-8.8s ", same * 100 / 256, n));
			}
			grid.add(line.toString());
		}
		java.nio.file.Files.write(server.getServerDirectory().resolve("rtf-surfacemap-" + kind + "-" + biome.replace(':', '_') + ".txt"), grid);
		// every column's ground block and height, to compare two worlds (another build, other mods) column by column
		List<String> columns = new ArrayList<>();
		for (int z = 0; z < size; z++) for (int x = 0; x < size; x++) {
			columns.add(((cx0 << 4) + x) + " " + ((cz0 << 4) + z) + " " + height[x][z] + " " + BuiltInRegistries.BLOCK.getKey(top[x][z]).getPath());
		}
		java.nio.file.Files.write(server.getServerDirectory().resolve("rtf-surfacemap-" + kind + "-" + biome.replace(':', '_') + ".columns"), columns);
		// neighbour pairs (x, x + 1) and (z, z + 1): across a chunk border or inside a chunk
		long border = 0, borderDiff = 0, inner = 0, innerDiff = 0;
		for (int a = 0; a < size; a++) for (int b = 0; b + 1 < size; b++) {
			boolean seam = (b & 15) == 15;
			int d = (top[b][a] != top[b + 1][a] ? 1 : 0) + (top[a][b] != top[a][b + 1] ? 1 : 0);
			if (seam) { border += 2; borderDiff += d; } else { inner += 2; innerDiff += d; }
		}
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < size; x++) for (int z = 0; z < size; z++) {
			int col = top[x][z].defaultMapColor().col;
			if (col == 0) col = MapColor.STONE.col;
			// lighter towards the north-west light, darker away from it, as the map mods shade slopes
			int dh = z > 0 ? height[x][z] - height[x][z - 1] : 0;
			float f = dh > 0 ? 1.12F : dh < 0 ? 0.82F : 1.0F;
			int r = Math.min(255, (int) (((col >> 16) & 255) * f)), g = Math.min(255, (int) (((col >> 8) & 255) * f)), bl = Math.min(255, (int) ((col & 255) * f));
			image.setRGB(x, z, r << 16 | g << 8 | bl);
		}
		String name = biome.replace(':', '_');
		Path out = server.getServerDirectory().resolve("rtf-surfacemap-" + kind + "-" + name + ".png");
		ImageIO.write(image, "png", out.toFile());
		List<Map.Entry<Block, Integer>> sorted = new ArrayList<>(counts.entrySet());
		sorted.sort((p, q) -> q.getValue() - p.getValue());
		StringBuilder blocks = new StringBuilder();
		for (int i = 0; i < Math.min(6, sorted.size()); i++) {
			blocks.append(i > 0 ? "," : "").append(BuiltInRegistries.BLOCK.getKey(sorted.get(i).getKey()).getPath())
				.append(String.format(Locale.ROOT, ":%.0f%%", 100.0 * sorted.get(i).getValue() / (size * size)));
		}
		double b = (double) borderDiff / border, in = (double) innerDiff / inner;
		String r = String.format(Locale.ROOT, "[RTF surfacemap] %s %s seam=%.2f border=%.3f inner=%.3f at %d %d top=%s", kind, biome,
			in > 0 ? b / in : 0, b, in, centre.getX(), centre.getZ(), blocks);
		RTFCommon.LOGGER.info(r);
		return r;
	}

	// the nearest dry place of the biome, nearest ring first, heights from the generator's cells: getBaseHeight builds a
	// whole noise chunk for every point (minutes for a biome a few kilometres away)
	private static BlockPos find(ServerLevel level, ResourceKey<Biome> key, int radius) {
		GeneratorContext ctx = ((RTFRandomState) (Object) level.getChunkSource().randomState()).generatorContext();
		Cell cell = new Cell();
		BlockPos spawn = level.getRespawnData().pos();
		for (int r = 0; r <= radius; r += 64) {
			for (int x = -r; x <= r; x += 64) for (int z = -r; z <= r; z += 64) {
				if (Math.max(Math.abs(x), Math.abs(z)) != r) continue;
				int inside = 0, h = 0;
				for (int i = 0; i < 9; i++) {
					int px = spawn.getX() + x + (i % 3 - 1) * 24, pz = spawn.getZ() + z + (i / 3 - 1) * 24;
					ctx.lookup.applyCell(cell, px, pz, false);
					int ph = ctx.levels.scale(cell.height);
					boolean in = cell.height > ctx.levels.water && level.getUncachedNoiseBiome(px >> 2, ph >> 2, pz >> 2).is(key);
					if (i == 4) {
						if (!in) break;
						h = ph;
					}
					if (in) inside++;
				}
				if (inside >= 6) return new BlockPos(spawn.getX() + x, h, spawn.getZ() + z);
			}
		}
		return null;
	}
}
