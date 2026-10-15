package raccoonman.reterraforged.server;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
 * water at the bottom"). Builds a square of chunks east of the spawn and looks for closed pits: places whose ground
 * (under water too) lies at least DEPTH blocks below the ground in all eight directions RING blocks away. Rivers, lakes
 * and valleys are open on two sides and do not count, nor the sea floor (the rim must be dry). Writes the pits to rtf-holes-<rtf|vanilla>.txt and prints
 * "[RTF holes] <rtf|vanilla> pits=N per_km2=D area=A".
 */
public final class RTFHoleTest {
	private static final int DEPTH = 12, RING = 16;

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
		BlockPos spawn = level.getSharedSpawnPos();
		int cx0 = (spawn.getX() >> 4) + 8, cz0 = (spawn.getZ() >> 4) - chunks / 2;
		int size = chunks * 16;
		int[] ground = new int[size * size];
		int[] water = new int[size * size];
		long t0 = System.nanoTime();
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
		RTFCommon.LOGGER.info("[RTF holes] {} chunks built in {} ms", chunks * chunks, (System.nanoTime() - t0) / 1_000_000);

		// pit columns, then pits: the deepest column within 32 blocks stands for its pit
		record Pit(int x, int z, int depth, int ground, int water) {}
		List<Pit> columns = new ArrayList<>();
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
				if (dryRim && rim - h >= DEPTH) columns.add(new Pit(x, z, rim - h, h, water[x * size + z]));
			}
		}
		columns.sort(Comparator.comparingInt(Pit::depth).reversed());
		List<Pit> pits = new ArrayList<>();
		for (Pit c : columns) {
			if (pits.stream().noneMatch((p) -> Math.abs(p.x() - c.x()) < 32 && Math.abs(p.z() - c.z()) < 32)) pits.add(c);
		}
		double km2 = size * (double) size / 1_000_000.0;
		List<String> lines = new ArrayList<>();
		lines.add("# " + kind + " world, seed " + level.getSeed() + ", " + chunks + " x " + chunks + " chunks from chunk " + cx0 + " " + cz0 + ": x z ground water-top depth biome");
		for (Pit p : pits) {
			int x = (cx0 << 4) + p.x(), z = (cz0 << 4) + p.z();
			String biome = level.getBiome(new BlockPos(x, p.ground(), z)).unwrapKey().map((k) -> k.location().toString()).orElse("?");
			String block = BuiltInRegistries.BLOCK.getKey(level.getBlockState(new BlockPos(x, p.ground(), z)).getBlock()).toString();
			lines.add(x + " " + z + " " + p.ground() + " " + p.water() + " " + p.depth() + " " + biome + " " + block);
		}
		Files.write(server.getServerDirectory().resolve("rtf-holes-" + kind + ".txt"), lines);
		String r = String.format(java.util.Locale.ROOT, "[RTF holes] %s pits=%d per_km2=%.2f area=%.2f", kind, pits.size(), pits.size() / km2, km2);
		RTFCommon.LOGGER.info(r);
		return r;
	}
}
