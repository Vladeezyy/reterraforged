package raccoonman.reterraforged.server;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.cell.Cell;

/**
 * "rtf vegetation [biome [chunks]]" (with -Drtf.selftest=true, tools/servertest.sh VEGETATION=1, #37): how much grows
 * where a biome is. Builds chunks x chunks chunks around the middle of the nearest place of each biome and looks through
 * every column down to the ground, under the leaves too. A line per biome, to rtf-vegetation.txt, numbers per 16 x 16
 * columns of dry land of the biome: trees; how many 16 x 16 and 32 x 32 windows have no trunk; how uneven the trees are
 * (the deviation of the trees per 32 x 32 window over their mean); how far the land is from the nearest trunk (95th
 * percentile and furthest); short grass, tall grass, ferns, large ferns, flowers, other plants; the time the chunks took.
 * Writes rtf-vegetation-<biome>.png: trunks from above. The places only depend on the seed and the terrain, so two runs
 * on one seed compare the same chunks.
 */
public final class RTFVegetationTest {
	private static final String[] BIOMES = {
		"minecraft:taiga", "minecraft:old_growth_spruce_taiga", "minecraft:old_growth_pine_taiga", "minecraft:snowy_taiga",
		"minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
		"minecraft:dark_forest", "minecraft:jungle", "minecraft:sparse_jungle", "minecraft:wooded_badlands",
		"minecraft:grove", "minecraft:windswept_forest", "minecraft:plains", "minecraft:savanna", "minecraft:swamp", "minecraft:cherry_grove",
		"minecraft:meadow", "minecraft:windswept_hills", "minecraft:snowy_plains", "minecraft:bamboo_jungle", "minecraft:savanna_plateau"
	};

	private RTFVegetationTest() {
	}

	public static String run(MinecraftServer server, String biome, int chunks) {
		String[] biomes = biome != null ? new String[] { biome } : BIOMES;
		try {
			List<String> lines = new ArrayList<>();
			long columns = 0, nanos = 0;
			for (String name : biomes) {
				Measured measured = measure(server, name, chunks);
				RTFCommon.LOGGER.info("[RTF vegetation] {}", measured.line());
				lines.add(measured.line());
				// the first biome warms the JIT up
				if (name != biomes[0] || biomes.length == 1) {
					columns += measured.chunks();
					nanos += measured.nanos();
				}
			}
			Files.write(server.getServerDirectory().resolve("rtf-vegetation.txt"), lines);
			String result = String.format(Locale.ROOT, "[RTF vegetation] done %d biomes, %.1f chunks/s", biomes.length, columns * 1.0E9 / Math.max(1, nanos));
			RTFCommon.LOGGER.info(result);
			return result;
		} catch (Throwable t) {
			RTFCommon.LOGGER.error("[RTF vegetation] exception", t);
			return "[RTF vegetation] done FAIL exception: " + t;
		}
	}

	private record Measured(String line, int chunks, long nanos) {
	}

	private static Measured measure(MinecraftServer server, String biome, int chunks) throws IOException {
		ServerLevel level = server.overworld();
		ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, Identifier.parse(biome));
		BlockPos centre = RTFSurfaceMap.find(level, key, 8000);
		if (centre == null) return new Measured(biome + " not found", 0, 0);
		int size = chunks * 16;
		centre = middle(level, key, centre, size);
		int cx0 = (centre.getX() >> 4) - chunks / 2, cz0 = (centre.getZ() >> 4) - chunks / 2;
		boolean[][] land = new boolean[size][size], water = new boolean[size][size], trunk = new boolean[size][size];
		long short_ = 0, tall = 0, fern = 0, largeFern = 0, flowers = 0, other = 0;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		long nanos = 0;
		for (int cx = 0; cx < chunks; cx++) for (int cz = 0; cz < chunks; cz++) {
			long t0 = System.nanoTime();
			ChunkAccess c = level.getChunk(cx0 + cx, cz0 + cz, ChunkStatus.FULL, true);
			nanos += System.nanoTime() - t0;
			for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
				int wx = (cx0 + cx) * 16 + x, wz = (cz0 + cz) * 16 + z;
				long s = 0, t = 0, f = 0, lf = 0, fl = 0, o = 0;
				boolean wet = false, isTrunk = false;
				int y = c.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
				for (; y > level.getMinY(); y--) {
					BlockState state = c.getBlockState(pos.set(wx, y, wz));
					if (state.isAir() || state.is(BlockTags.LEAVES) || state.is(Blocks.SNOW) || state.is(Blocks.VINE)) continue;
					if (state.is(BlockTags.LOGS)) {
						// a trunk stands on the ground and goes up: not a branch (air or leaves below), not a fallen log
						BlockState below = c.getBlockState(pos.set(wx, y - 1, wz));
						isTrunk = state.getOptionalValue(RotatedPillarBlock.AXIS).orElse(Direction.Axis.Y) == Direction.Axis.Y
							&& !below.is(BlockTags.LOGS) && !below.isAir() && !below.is(BlockTags.LEAVES)
							&& c.getBlockState(pos.set(wx, y + 1, wz)).is(BlockTags.LOGS);
						if (isTrunk) { y--; break; }
						continue;
					}
					if (!state.getFluidState().isEmpty()) { wet = true; break; }
					boolean lower = !state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
					if (state.is(Blocks.SHORT_GRASS)) s++;
					else if (state.is(Blocks.TALL_GRASS)) { if (lower) t++; }
					else if (state.is(Blocks.FERN)) f++;
					else if (state.is(Blocks.LARGE_FERN)) { if (lower) lf++; }
					else if (state.is(BlockTags.FLOWERS)) { if (lower) fl++; }
					else if (state.getCollisionShape(c, pos).isEmpty()) { if (lower) o++; }
					else break;
				}
				int ix = cx * 16 + x, iz = cz * 16 + z;
				trunk[ix][iz] = isTrunk;
				water[ix][iz] = wet;
				if (!wet && level.getBiome(pos.set(wx, y + 1, wz)).is(key)) {
					land[ix][iz] = true;
					short_ += s; tall += t; fern += f; largeFern += lf; flowers += fl; other += o;
				}
			}
		}
		// the trees: touching trunk columns are one tree (2 x 2 trunks)
		int[][] seen = new int[size][size];
		boolean[][] tree = new boolean[size][size];
		long trees = 0, landColumns = 0;
		int[] stack = new int[size * size];
		for (int x = 0; x < size; x++) for (int z = 0; z < size; z++) {
			if (land[x][z]) landColumns++;
			if (!trunk[x][z] || seen[x][z] != 0) continue;
			if (land[x][z]) trees++;
			tree[x][z] = true;
			int top = 0;
			stack[top++] = x << 16 | z;
			seen[x][z] = 1;
			while (top > 0) {
				int p = stack[--top], px = p >> 16, pz = p & 0xFFFF;
				for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
					int nx = px + dx, nz = pz + dz;
					if (nx >= 0 && nz >= 0 && nx < size && nz < size && trunk[nx][nz] && seen[nx][nz] == 0) {
						seen[nx][nz] = 1;
						stack[top++] = nx << 16 | nz;
					}
				}
			}
		}
		double perChunk = landColumns == 0 ? 0 : 256.0 / landColumns;
		// squared distance of each land column to the nearest trunk, not on the map's rim: its nearest may be outside
		int[] near = new int[size * size];
		int n = 0;
		List<int[]> trunks = new ArrayList<>();
		for (int x = 0; x < size; x++) for (int z = 0; z < size; z++) if (trunk[x][z]) trunks.add(new int[] { x, z });
		for (int x = 16; x < size - 16; x++) for (int z = 16; z < size - 16; z++) {
			if (!land[x][z]) continue;
			int best = Integer.MAX_VALUE;
			for (int[] p : trunks) best = Math.min(best, (p[0] - x) * (p[0] - x) + (p[1] - z) * (p[1] - z));
			near[n++] = best;
		}
		Arrays.sort(near, 0, n);
		double p95 = n == 0 ? 0 : Math.sqrt(near[(int) (n * 0.95)]), furthest = n == 0 ? 0 : Math.sqrt(near[n - 1]);
		String line = String.format(Locale.ROOT, "%s at %d %d: land=%d%% trees=%.2f empty16=%.0f%% empty32=%.0f%% uneven=%.2f nearest95=%.0f nearestMax=%.0f short=%.1f tall=%.1f fern=%.1f largeFern=%.1f flowers=%.1f other=%.1f gen=%.1fms",
			biome, centre.getX(), centre.getZ(), landColumns * 100 / (size * size), trees * perChunk, empty(land, trunk, 16) * 100, empty(land, trunk, 32) * 100, uneven(land, tree, 32), p95, furthest,
			short_ * perChunk, tall * perChunk, fern * perChunk, largeFern * perChunk, flowers * perChunk, other * perChunk, nanos / 1.0E6 / (chunks * chunks));
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < size; x++) for (int z = 0; z < size; z++) {
			image.setRGB(x, z, trunk[x][z] ? 0x103010 : water[x][z] ? 0x4060c0 : land[x][z] ? 0xb8d8a0 : 0xc0c0c0);
		}
		for (int x = 0; x < size; x++) for (int z = 0; z < size; z++) {
			if (!trunk[x][z]) continue;
			for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
				if (x + dx >= 0 && z + dz >= 0 && x + dx < size && z + dz < size) image.setRGB(x + dx, z + dz, 0x103010);
			}
		}
		Path out = server.getServerDirectory().resolve("rtf-vegetation-" + biome.replace(':', '_') + ".png");
		ImageIO.write(image, "png", out.toFile());
		return new Measured(line, chunks * chunks, nanos);
	}

	// from the biome's rim, where the search by rings finds it, towards its middle: where most of the map is the biome
	private static BlockPos middle(ServerLevel level, ResourceKey<Biome> key, BlockPos start, int size) {
		GeneratorContext ctx = ((RTFRandomState) (Object) level.getChunkSource().randomState()).generatorContext();
		Cell cell = new Cell();
		BlockPos best = start;
		int bestScore = score(level, ctx, cell, key, best, size);
		for (int step = 0; step < 32; step++) {
			BlockPos next = best;
			for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 && dz == 0) continue;
				BlockPos candidate = best.offset(dx * 48, 0, dz * 48);
				int candidateScore = score(level, ctx, cell, key, candidate, size);
				if (candidateScore > bestScore) {
					bestScore = candidateScore;
					next = candidate;
				}
			}
			if (next == best) break;
			best = next;
		}
		return best;
	}

	private static int score(ServerLevel level, GeneratorContext ctx, Cell cell, ResourceKey<Biome> key, BlockPos centre, int size) {
		int inside = 0;
		for (int i = -4; i <= 4; i++) for (int j = -4; j <= 4; j++) {
			int px = centre.getX() + i * size / 8, pz = centre.getZ() + j * size / 8;
			ctx.lookup.applyCell(cell, px, pz, false);
			if (cell.height > ctx.levels.water && level.getUncachedNoiseBiome(px >> 2, ctx.levels.scale(cell.height) >> 2, pz >> 2).is(key)) inside++;
		}
		return inside;
	}

	// the deviation of the trees per size x size window over their mean, on windows of land of the biome (half a window
	// apart): about 0.3 when the trees are evenly spread, more the more they stand in clumps and clearings
	private static double uneven(boolean[][] land, boolean[][] tree, int size) {
		int windows = 0;
		double sum = 0, squares = 0;
		for (int x0 = 0; x0 + size <= land.length; x0 += size / 2) for (int z0 = 0; z0 + size <= land.length; z0 += size / 2) {
			int columns = 0, trees = 0;
			for (int x = x0; x < x0 + size; x++) for (int z = z0; z < z0 + size; z++) {
				if (land[x][z]) columns++;
				if (tree[x][z]) trees++;
			}
			if (columns < size * size * 9 / 10) continue;
			windows++;
			sum += trees;
			squares += trees * trees;
		}
		if (windows == 0 || sum == 0) return 0;
		double mean = sum / windows;
		return Math.sqrt(Math.max(0, squares / windows - mean * mean)) / mean;
	}

	// the share of size x size windows (half a window apart) that are at least 90 % land of the biome and have no trunk
	private static double empty(boolean[][] land, boolean[][] trunk, int size) {
		int windows = 0, empty = 0;
		for (int x0 = 0; x0 + size <= land.length; x0 += size / 2) for (int z0 = 0; z0 + size <= land.length; z0 += size / 2) {
			int columns = 0;
			boolean any = false;
			for (int x = x0; x < x0 + size; x++) for (int z = z0; z < z0 + size; z++) {
				if (land[x][z]) columns++;
				any |= trunk[x][z];
			}
			if (columns < size * size * 9 / 10) continue;
			windows++;
			if (!any) empty++;
		}
		return windows == 0 ? 0 : (double) empty / windows;
	}
}
