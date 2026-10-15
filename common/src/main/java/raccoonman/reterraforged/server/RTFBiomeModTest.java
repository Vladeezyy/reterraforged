package raccoonman.reterraforged.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;

/**
 * "rtf biomemods" (with -Drtf.selftest=true, tools/servertest.sh with MODS=...): how the overworld biomes of other
 * mods (Biomes O' Plenty and others through TerraBlender) turn out in this world. Samples the biome at the surface on
 * a 16 x 16 km grid from the generator, then builds a chunk in every other mod's biome found and reads its top blocks
 * (the mod's surface rules: BoP's white sand, mud, volcanic rock...). The same command in a vanilla world with the
 * same mods is the reference. Writes the table to rtf-biomemods-<rtf|vanilla>.txt in the server folder and prints
 * "[RTF biomemods] <rtf|vanilla> found=N/M share=S underwater=U surface=K/N".
 */
public final class RTFBiomeModTest {
	private static final int RADIUS = 8192, STEP = 256;

	private static final class Stat {
		int samples, underwater;
		long height;
		BlockPos first;
		final Map<String, Integer> tops = new HashMap<>();
		int columns;
		final int[][] bands = new int[5][8];
	}

	private RTFBiomeModTest() {
	}

	private static String namespace(Holder<Biome> biome) {
		return biome.unwrapKey().map((key) -> key.location().getNamespace()).orElse("minecraft");
	}

	private static boolean modded(Holder<Biome> biome) {
		String ns = namespace(biome);
		return !ns.equals("minecraft") && !ns.equals(RTFCommon.MOD_ID);
	}

	private static String name(Holder<Biome> biome) {
		return biome.unwrapKey().map((key) -> key.location().toString()).orElse("?");
	}

	// the vanilla biome table bands of a climate point: temperature 0-4, humidity 0-4, continentalness 0-6 (mushroom
	// fields, deep ocean, ocean, coast, near, mid, far inland), erosion 0-6, weirdness 0-7 (valley 4 and 5 around 0)
	private static int[] bands(Climate.TargetPoint point) {
		return new int[] {
			band(point.temperature(), -0.45F, -0.15F, 0.2F, 0.55F),
			band(point.humidity(), -0.35F, -0.1F, 0.1F, 0.3F),
			band(point.continentalness(), -1.05F, -0.455F, -0.19F, -0.11F, 0.03F, 0.3F),
			band(point.erosion(), -0.78F, -0.375F, -0.2225F, 0.05F, 0.45F, 0.55F),
			band(point.weirdness(), -0.7666667F, -0.56666666F, -0.4F, -0.26666668F, -0.05F, 0.05F, 0.26666668F)
		};
	}

	// index of the band a quantized climate value falls in, by the bands' lower bounds after the first
	private static int band(long value, float... bounds) {
		float v = Climate.unquantizeCoord(value);
		int i = 0;
		while (i < bounds.length && v >= bounds[i]) i++;
		return i;
	}

	public static String run(MinecraftServer server) {
		try {
			return test(server);
		} catch (Throwable t) {
			RTFCommon.LOGGER.error("[RTF biomemods] exception", t);
			return "[RTF biomemods] FAIL exception: " + t;
		}
	}

	private static String test(MinecraftServer server) throws IOException {
		ServerLevel level = server.overworld();
		var gen = level.getChunkSource().getGenerator();
		var rs = level.getChunkSource().randomState();
		var source = gen.getBiomeSource();
		boolean rtf = (Object) rs instanceof RTFRandomState s && s.generatorContext() != null;
		String kind = rtf ? "rtf" : "vanilla";
		Set<Holder<Biome>> possible = source.possibleBiomes().stream().filter(RTFBiomeModTest::modded).collect(Collectors.toSet());

		// biomes at the surface over the grid
		Map<String, Stat> stats = new TreeMap<>();
		int samples = 0, moddedSamples = 0, moddedUnderwater = 0;
		// the climate the biome source gets, in the bands of the vanilla biome tables (OverworldBiomeBuilder), which
		// TerraBlender regions like Biomes O' Plenty's use too
		int[][] bands = new int[5][8];
		long t0 = System.nanoTime();
		for (int x = -RADIUS; x <= RADIUS; x += STEP) {
			for (int z = -RADIUS; z <= RADIUS; z += STEP) {
				int ground = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
				int top = gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, rs);
				Holder<Biome> biome = source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(top), QuartPos.fromBlock(z), rs.sampler());
				Climate.TargetPoint point = rs.sampler().sample(QuartPos.fromBlock(x), QuartPos.fromBlock(top), QuartPos.fromBlock(z));
				int[] b = bands(point);
				for (int i = 0; i < b.length; i++) bands[i][b[i]]++;
				samples++;
				if (!modded(biome)) continue;
				Stat s = stats.computeIfAbsent(name(biome), (k) -> new Stat());
				s.samples++;
				s.height += ground;
				if (top > ground) s.underwater++;
				if (s.first == null) s.first = new BlockPos(x, top, z);
				for (int i = 0; i < b.length; i++) s.bands[i][b[i]]++;
				moddedSamples++;
				if (top > ground) moddedUnderwater++;
			}
		}
		RTFCommon.LOGGER.info("[RTF biomemods] {} samples in {} ms", samples, (System.nanoTime() - t0) / 1_000_000);

		// top blocks of the columns of each mod biome in a 3 x 3 chunk area at its first sample
		for (Map.Entry<String, Stat> e : stats.entrySet()) {
			Stat s = e.getValue();
			int cx0 = s.first.getX() >> 4, cz0 = s.first.getZ() >> 4;
			for (int cx = cx0 - 1; cx <= cx0 + 1; cx++) {
				for (int cz = cz0 - 1; cz <= cz0 + 1; cz++) {
					ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, true);
					for (int x = 0; x < 16; x++) {
						for (int z = 0; z < 16; z++) {
							// the top block itself: ChunkAccess.getHeight is the heightmap's height less one
							int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
							BlockPos pos = new BlockPos((cx << 4) + x, y, (cz << 4) + z);
							if (!name(level.getBiome(pos)).equals(e.getKey())) continue;
							String block = BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(pos).getBlock()).toString();
							s.tops.merge(block, 1, Integer::sum);
							s.columns++;
						}
					}
				}
			}
		}

		// biomes whose top blocks include their own mod's blocks
		int withModSurface = 0;
		List<String> lines = new ArrayList<>();
		lines.add("# " + kind + " world, seed " + level.getSeed() + ": biome, samples, underwater %, mean ground height, usual climate bands (t h c e w), top blocks in a 3 x 3 chunk area");
		for (Map.Entry<String, Stat> e : stats.entrySet()) {
			Stat s = e.getValue();
			String ns = e.getKey().substring(0, e.getKey().indexOf(':'));
			if (s.tops.keySet().stream().anyMatch((b) -> b.startsWith(ns + ":"))) withModSurface++;
			Map<String, Integer> top = s.tops.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(4)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
			// the climate bands most of the biome's samples have: temperature humidity continentalness erosion weirdness
			StringBuilder mode = new StringBuilder();
			for (int[] b : s.bands) {
				int best = 0;
				for (int i = 1; i < b.length; i++) if (b[i] > b[best]) best = i;
				mode.append(best);
			}
			lines.add(String.format("%-40s %5d %5.1f %6.1f %s  %s", e.getKey(), s.samples, 100.0 * s.underwater / s.samples, (double) s.height / s.samples, mode,
				top.entrySet().stream().map((t) -> t.getKey() + " " + Math.round(100.0 * t.getValue() / Math.max(1, s.columns)) + "%").collect(Collectors.joining(", "))));
		}
		List<String> missing = possible.stream().map(RTFBiomeModTest::name).filter((n) -> !stats.containsKey(n)).sorted(Comparator.naturalOrder()).toList();
		lines.add("# not found: " + missing);
		String[] bandNames = { "temperature", "humidity", "continentalness", "erosion", "weirdness" };
		for (int i = 0; i < bands.length; i++) {
			final int[] b = bands[i];
			final int total = samples;
			lines.add("# " + bandNames[i] + " bands %: " + java.util.Arrays.stream(b).mapToObj((n) -> String.format("%.1f", 100.0 * n / total)).collect(Collectors.joining(" ")));
		}
		Files.write(server.getServerDirectory().resolve("rtf-biomemods-" + kind + ".txt"), lines);

		String r = String.format("[RTF biomemods] %s found=%d/%d share=%.1f underwater=%.1f surface=%d/%d", kind, stats.size(), possible.size(),
			100.0 * moddedSamples / samples, moddedSamples == 0 ? 0.0 : 100.0 * moddedUnderwater / moddedSamples, withModSurface, stats.size());
		RTFCommon.LOGGER.info(r);
		return r;
	}
}
