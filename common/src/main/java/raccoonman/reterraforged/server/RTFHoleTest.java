package raccoonman.reterraforged.server;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;

/**
 * "rtf holes [chunks]" (with -Drtf.selftest=true): pits in the ground, as players reported ("holes in the ground with
 * water at the bottom"). Builds 16 squares of chunks x chunks chunks spread over 9 x 9 km and looks for closed pits:
 * places whose ground (under water too) lies at least DEPTH blocks below the ground in all eight directions RING
 * blocks away, the ground there dry. Rivers, lakes and valleys are open on two sides and do not count, nor the sea
 * floor. A pit is a "trench" (a ravine) when it is more than three times as long as wide, else a "funnel". Writes the
 * pits to rtf-holes-<rtf|vanilla>.txt and prints
 * "[RTF holes] <rtf|vanilla> pits=N per_km2=D funnels=F high_funnels=H area=A" (high: the rim 40 blocks above the sea).
 */
public final class RTFHoleTest {
	private static final int DEPTH = 12, RING = 16, SQUARES = 4, SPACING = 3000;

	private record Column(int x, int z, int depth, int ground, int water, int rim) {}

	private RTFHoleTest() {
	}

	public static String run(MinecraftServer server, int chunks) {
		try {
			return test(server, chunks);
		} catch (Throwable t) {
			RTFCommon.LOGGER.error("[RTF holes] exception", t);
			return "[RTF holes] FAIL exception: " + t;
		}
	}

	private static String test(MinecraftServer server, int chunks) throws IOException {
		ServerLevel level = server.overworld();
		boolean rtf = (Object) level.getChunkSource().randomState() instanceof RTFRandomState s && s.generatorContext() != null;
		String kind = rtf ? "rtf" : "vanilla";
		BlockPos spawn = level.getRespawnData().pos();
		List<String> lines = new ArrayList<>();
		lines.add("# " + kind + " world, seed " + level.getSeed() + ", " + SQUARES * SQUARES + " squares of " + chunks + " x " + chunks
			+ " chunks: x z ground water-top rim depth shape(w x l) biome block");
		int pits = 0, funnels = 0, highFunnels = 0;
		int[] highWater = new int[2];
		long t0 = System.nanoTime();
		for (int i = 0; i < SQUARES; i++) {
			for (int j = 0; j < SQUARES; j++) {
				int cx0 = ((spawn.getX() + (int) ((i - (SQUARES - 1) / 2.0) * SPACING)) >> 4) - chunks / 2;
				int cz0 = ((spawn.getZ() + (int) ((j - (SQUARES - 1) / 2.0) * SPACING)) >> 4) - chunks / 2;
				for (String line : square(level, cx0, cz0, chunks, highWater, lines)) {
					lines.add(line);
					pits++;
					if (line.contains(" funnel ")) {
						funnels++;
						if (Integer.parseInt(line.split(" ")[4]) > level.getSeaLevel() + 40) highFunnels++;
					}
				}
			}
		}
		RTFCommon.LOGGER.info("[RTF holes] {} chunks built in {} ms", SQUARES * SQUARES * chunks * chunks, (System.nanoTime() - t0) / 1_000_000);
		Files.write(server.getServerDirectory().resolve("rtf-holes-" + kind + ".txt"), lines);
		double km2 = SQUARES * SQUARES * (chunks * 16.0) * (chunks * 16.0) / 1_000_000.0;
		String r = String.format(Locale.ROOT, "[RTF holes] %s pits=%d per_km2=%.2f funnels=%d high_funnels=%d high_water=%d high_water_spots=%d area=%.2f", kind, pits, pits / km2, funnels, highFunnels, highWater[0], highWater[1], km2);
		RTFCommon.LOGGER.info(r);
		return r;
	}

	// the pits of one square of chunks, one line each
	// highWater: columns whose top is water 30 blocks above the sea level or more, and such places (32 blocks apart),
	// which are listed as "water x z top"
	private static List<String> square(ServerLevel level, int cx0, int cz0, int chunks, int[] highWater, List<String> out) {
		int size = chunks * 16;
		int[] ground = new int[size * size];
		int[] water = new int[size * size];
		for (int cx = 0; cx < chunks; cx++) {
			for (int cz = 0; cz < chunks; cz++) {
				ChunkAccess chunk = level.getChunk(cx0 + cx, cz0 + cz, ChunkStatus.FULL, true);
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						int i = (cx * 16 + x) * size + cz * 16 + z;
						ground[i] = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
						water[i] = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
					}
				}
			}
		}
		List<int[]> spots = new ArrayList<>();
		for (int x = 0; x < size; x++) {
			for (int z = 0; z < size; z++) {
				int i = x * size + z;
				if (water[i] > ground[i] && water[i] >= level.getSeaLevel() + 30) {
					highWater[0]++;
					final int fx = x, fz = z;
					if (spots.stream().noneMatch((sp) -> Math.abs(sp[0] - fx) < 32 && Math.abs(sp[1] - fz) < 32)) {
						spots.add(new int[] { x, z });
						out.add("water " + ((cx0 << 4) + x) + " " + ((cz0 << 4) + z) + " " + water[i] + " ground " + ground[i]);
					}
				}
			}
		}
		highWater[1] += spots.size();
		List<Column> columns = new ArrayList<>();
		int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }, { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };
		for (int x = RING; x < size - RING; x += 2) {
			for (int z = RING; z < size - RING; z += 2) {
				int h = ground[x * size + z];
				int rim = Integer.MAX_VALUE;
				boolean dryRim = true;
				for (int[] d : dirs) {
					int r = d[0] != 0 && d[1] != 0 ? RING * 7 / 10 : RING;
					int j = (x + d[0] * r) * size + z + d[1] * r;
					rim = Math.min(rim, ground[j]);
					dryRim &= water[j] == ground[j];
				}
				// on land: the ground all around is dry (the sea floor is uneven everywhere)
				if (dryRim && rim - h >= DEPTH) columns.add(new Column(x, z, rim - h, h, water[x * size + z], rim));
			}
		}
		// the deepest column within 32 blocks stands for its pit; its shape from the pit columns around it
		columns.sort(Comparator.comparingInt(Column::depth).reversed());
		List<Column> pits = new ArrayList<>();
		for (Column c : columns) {
			if (pits.stream().noneMatch((p) -> Math.abs(p.x() - c.x()) < 32 && Math.abs(p.z() - c.z()) < 32)) pits.add(c);
		}
		List<String> lines = new ArrayList<>();
		for (Column p : pits) {
			// the pit: the columns connected to its deepest one whose ground is at least DEPTH / 2 below its rim
			int minX = p.x(), maxX = p.x(), minZ = p.z(), maxZ = p.z();
			java.util.ArrayDeque<int[]> open = new java.util.ArrayDeque<>();
			java.util.BitSet seen = new java.util.BitSet(size * size);
			open.add(new int[] { p.x(), p.z() });
			seen.set(p.x() * size + p.z());
			while (!open.isEmpty()) {
				int[] c = open.poll();
				minX = Math.min(minX, c[0]); maxX = Math.max(maxX, c[0]);
				minZ = Math.min(minZ, c[1]); maxZ = Math.max(maxZ, c[1]);
				for (int[] d : dirs) {
					int nx = c[0] + d[0], nz = c[1] + d[1];
					if (nx < 0 || nz < 0 || nx >= size || nz >= size || Math.abs(nx - p.x()) > 64 || Math.abs(nz - p.z()) > 64) continue;
					int j = nx * size + nz;
					if (seen.get(j) || p.rim() - ground[j] < DEPTH / 2) continue;
					seen.set(j);
					open.add(new int[] { nx, nz });
				}
			}
			int w = Math.min(maxX - minX, maxZ - minZ) + 1, l = Math.max(maxX - minX, maxZ - minZ) + 1;
			String shape = l > 3 * w ? "trench" : "funnel";
			int x = (cx0 << 4) + p.x(), z = (cz0 << 4) + p.z();
			String biome = level.getBiome(new BlockPos(x, p.ground(), z)).unwrapKey().map((k) -> k.location().toString()).orElse("?");
			String block = BuiltInRegistries.BLOCK.getKey(level.getBlockState(new BlockPos(x, p.ground(), z)).getBlock()).toString();
			lines.add(x + " " + z + " " + p.ground() + " " + p.water() + " " + p.rim() + " " + p.depth() + " " + shape + " " + w + "x" + l + " " + biome + " " + block);
		}
		return lines;
	}
}
