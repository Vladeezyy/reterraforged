package raccoonman.reterraforged.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import raccoonman.reterraforged.RTFCommon;

/**
 * "rtf fingerprint [name]" (with -Drtf.selftest=true, tools/fingerprint.sh): a hash of every block and biome of chunks
 * in several places of the world, to prove that a change of the code leaves the generation as it was.
 * Chunks generated to the surface (noise, aquifers, surface rules: no features, so the same whatever order the chunks
 * are generated in) and fully generated chunks (features too), plus the generator's own height and biome on a grid.
 * Writes rtf-fingerprint[-name].txt: one line per chunk, compared by tools/comparefingerprint.py.
 */
public final class RTFFingerprint {
	// 6 x 6 chunks to the surface and 4 x 4 fully generated chunks in each of these places (chunk coordinates), far
	// apart: plains, mountains, coasts and oceans of the test seed, away from the self test's chunks
	private static final int[][] SURFACE_AREAS = { { 100, 100 }, { -400, 250 }, { 600, -700 }, { -1200, -1200 }, { 1500, 900 } };
	// the last two: an old growth pine taiga and a windswept forest of the test seed (tools/servertest.sh SEED), whose
	// trees come from the mod's templates (a large spruce template that pasted nothing went unnoticed before)
	private static final int[][] FULL_AREAS = { { 150, -150 }, { -300, -500 }, { 800, 300 }, { -900, 900 }, { 267, -60 }, { 267, -36 } };

	private RTFFingerprint() {
	}

	public static String run(MinecraftServer server, String name) {
		ServerLevel level = server.overworld();
		List<String> lines = new ArrayList<>();
		lines.add("# seed " + level.getSeed());
		long t0 = System.nanoTime();
		long all = 1;
		for (int[] area : SURFACE_AREAS) {
			for (int cx = area[0]; cx < area[0] + 6; cx++) for (int cz = area[1]; cz < area[1] + 6; cz++) {
				long h = hash(level, level.getChunk(cx, cz, ChunkStatus.SURFACE, true));
				lines.add("surface " + cx + " " + cz + " " + Long.toHexString(h));
				all = all * 31 + h;
			}
		}
		long t1 = System.nanoTime();
		for (int[] area : FULL_AREAS) {
			for (int cx = area[0]; cx < area[0] + 4; cx++) for (int cz = area[1]; cz < area[1] + 4; cz++) {
				ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, true);
				long h = hash(level, chunk);
				lines.add("full " + cx + " " + cz + " " + Long.toHexString(h));
				lines.add("fullblocks " + cx + " " + cz + " " + counts(chunk));
				all = all * 31 + h;
			}
		}
		long t2 = System.nanoTime();
		var gen = level.getChunkSource().getGenerator();
		var rs = level.getChunkSource().randomState();
		{
			for (int x = -16384; x <= 16384; x += 512) {
				for (int z = -16384; z <= 16384; z += 512) {
					int h = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
					String biome = level.getUncachedNoiseBiome(x >> 2, h >> 2, z >> 2).getRegisteredName();
					lines.add("base " + x + " " + z + " " + h + " " + biome);
					all = all * 31 + h * 31L + biome.hashCode();
				}
			}
		}
		long t3 = System.nanoTime();
		Path out = server.getServerDirectory().resolve("rtf-fingerprint" + (name.isEmpty() ? "" : "-" + name) + ".txt").toAbsolutePath();
		try {
			Files.write(out, lines);
		} catch (IOException e) {
			return "[RTF fingerprint] FAIL writing " + out + ": " + e;
		}
		String r = "[RTF fingerprint] " + Long.toHexString(all) + " (" + (lines.size() - 1) + " lines; surface " + (t1 - t0) / 1_000_000 + " ms, full " + (t2 - t1) / 1_000_000
			+ " ms, base heights " + (t3 - t2) / 1_000_000 + " ms) in " + out;
		RTFCommon.LOGGER.info(r);
		return r;
	}

	/** How many of each block the chunk has, section by section where it is not stone, air or water: tells what differs */
	private static String counts(ChunkAccess chunk) {
		java.util.TreeMap<String, Integer> counts = new java.util.TreeMap<>();
		LevelChunkSection[] sections = chunk.getSections();
		for (int s = 0; s < sections.length; s++) {
			for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
				var state = sections[s].getBlockState(x, y, z);
				counts.merge(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath() + "@" + s, 1, Integer::sum);
			}
		}
		StringBuilder b = new StringBuilder();
		counts.forEach((k, v) -> b.append(k).append('=').append(v).append(' '));
		return b.toString().trim();
	}

	/** Every block state (by its id) and every biome of the chunk, section by section */
	private static long hash(ServerLevel level, ChunkAccess chunk) {
		long h = 1125899906842597L;
		LevelChunkSection[] sections = chunk.getSections();
		for (int s = 0; s < sections.length; s++) {
			LevelChunkSection section = sections[s];
			for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
				h = h * 31 + Block.getId(section.getBlockState(x, y, z));
			}
			for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) for (int x = 0; x < 4; x++) {
				Holder<Biome> biome = section.getNoiseBiome(x, y, z);
				h = h * 31 + biome.getRegisteredName().hashCode();
			}
		}
		return h;
	}
}
