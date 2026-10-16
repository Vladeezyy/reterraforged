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
		return biome.unwrapKey().map((key) -> key.identifier().getNamespace()).orElse("minecraft");
	}

	private static boolean modded(Holder<Biome> biome) {
		String ns = namespace(biome);
		return !ns.equals("minecraft") && !ns.equals(RTFCommon.MOD_ID);
	}

	private static String name(Holder<Biome> biome) {
		return biome.unwrapKey().map((key) -> key.identifier().toString()).orElse("?");
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

	// "rtf biomemods probe": every other mod's region's biome tree searched over a grid of climate points (the middle of
	// each vanilla band), which band combinations give which biome: rtf-biomeprobe.txt
	public static String probe(MinecraftServer server) {
		try {
			ServerLevel level = server.overworld();
			var source = level.getChunkSource().getGenerator().getBiomeSource();
			int regions = terrablender.api.Regions.getCount(terrablender.api.RegionType.OVERWORLD);
			float[] t = { -0.6F, -0.3F, 0.0F, 0.37F, 0.8F }, h = { -0.6F, -0.22F, 0.0F, 0.2F, 0.6F };
			float[] c = { -0.15F, -0.04F, 0.16F, 0.65F }, e = { -0.9F, -0.55F, -0.3F, -0.08F, 0.25F, 0.5F, 0.8F };
			float[] w = { -0.9F, -0.65F, -0.48F, -0.33F, -0.15F, 0.0F, 0.15F, 0.5F };
			Map<String, List<String>> where = new TreeMap<>();
			raccoonman.reterraforged.world.worldgen.terrablender.TBCompat.probing = true;
			for (int r = 1; r < regions; r++) for (int ti = 0; ti < t.length; ti++) for (int hi = 0; hi < h.length; hi++) for (int ci = 0; ci < c.length; ci++) for (int ei = 0; ei < e.length; ei++) for (int wi = 0; wi < w.length; wi++) {
				// a sampler of constants; the region is chosen by the uniqueness ReTerraForged gives the points it samples
				// (MixinClimateSampler, MixinParameterList)
				Climate.Sampler sampler = new Climate.Sampler(constant(t[ti]), constant(h[hi]), constant(c[ci]), constant(e[ei]), constant(0.0F), constant(w[wi]), List.of());
				((raccoonman.reterraforged.world.worldgen.terrablender.TBClimateSampler) (Object) sampler).setUniqueness(constant(r / (float) (regions - 1)));
				Holder<Biome> biome = source.getNoiseBiome(0, QuartPos.fromBlock(level.getSeaLevel() + 8), 0, sampler);
				if (!modded(biome)) continue;
				where.computeIfAbsent(name(biome), (k) -> new ArrayList<>()).add("r" + r + " t" + ti + " h" + hi + " c" + (ci + 3) + " e" + ei + " w" + wi);
			}
			List<String> lines = new ArrayList<>();
			for (Map.Entry<String, List<String>> en : where.entrySet()) {
				// the bands each biome is found at, counted
				Map<String, Integer> ts = new TreeMap<>(), hs = new TreeMap<>(), cs = new TreeMap<>(), es = new TreeMap<>(), ws = new TreeMap<>(), rs = new TreeMap<>();
				for (String k : en.getValue()) {
					String[] f = k.split(" ");
					rs.merge(f[0], 1, Integer::sum); ts.merge(f[1], 1, Integer::sum); hs.merge(f[2], 1, Integer::sum);
					cs.merge(f[3], 1, Integer::sum); es.merge(f[4], 1, Integer::sum); ws.merge(f[5], 1, Integer::sum);
				}
				lines.add(String.format("%-40s %4d  %s %s %s %s %s %s", en.getKey(), en.getValue().size(), rs, ts, hs, cs, es, ws));
			}
			raccoonman.reterraforged.world.worldgen.terrablender.TBCompat.probing = false;
			Files.write(server.getServerDirectory().resolve("rtf-biomeprobe.txt"), lines);
			return "[RTF biomemods] rtf probe " + where.size() + " biomes";
		} catch (Throwable th) {
			RTFCommon.LOGGER.error("[RTF biomemods] probe exception", th);
			return "[RTF biomemods] FAIL probe " + th;
		}
	}

	// "rtf biomemods dump": the parameter points of every TerraBlender overworld region (rtf-regions.txt: region biome
	// then min max of temperature humidity continentalness erosion depth weirdness, and the offset) and the climate
	// points of this world on the biomemods grid (rtf-points-<rtf|vanilla>.txt: region t h c e d w, at the surface), to
	// try other climate mappings offline
	public static String dump(MinecraftServer server) {
		try {
			ServerLevel level = server.overworld();
			var registry = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
			List<String> regions = new ArrayList<>();
			List<terrablender.api.Region> list = terrablender.api.Regions.get(terrablender.api.RegionType.OVERWORLD);
			for (int r = 0; r < list.size(); r++) {
				final int index = r;
				list.get(r).addBiomes(registry, (pair) -> {
					Climate.ParameterPoint p = pair.getFirst();
					StringBuilder line = new StringBuilder(index + " " + pair.getSecond().identifier());
					for (Climate.Parameter q : List.of(p.temperature(), p.humidity(), p.continentalness(), p.erosion(), p.depth(), p.weirdness())) line.append(' ').append(q.min()).append(' ').append(q.max());
					regions.add(line.append(' ').append(p.offset()).toString());
				});
			}
			Files.write(server.getServerDirectory().resolve("rtf-regions.txt"), regions);
			var gen = level.getChunkSource().getGenerator();
			var rs = level.getChunkSource().randomState();
			boolean rtf = (Object) rs instanceof RTFRandomState s && s.generatorContext() != null;
			List<String> points = new ArrayList<>();
			for (int x = -RADIUS; x <= RADIUS; x += STEP) {
				for (int z = -RADIUS; z <= RADIUS; z += STEP) {
					int ground = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
					int top = gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, rs);
					Climate.TargetPoint p = rs.sampler().sample(QuartPos.fromBlock(x), QuartPos.fromBlock(top), QuartPos.fromBlock(z));
					double u = ((raccoonman.reterraforged.world.worldgen.terrablender.TBTargetPoint) (Object) p).getUniqueness();
					points.add((Double.isNaN(u) ? -1 : Math.round((list.size() - 1) * u)) + " " + p.temperature() + " " + p.humidity() + " " + p.continentalness() + " " + p.erosion() + " " + p.depth() + " " + p.weirdness() + " " + x + " " + z + " " + ground + " " + top);
				}
			}
			Files.write(server.getServerDirectory().resolve("rtf-points-" + (rtf ? "rtf" : "vanilla") + ".txt"), points);
			return "[RTF biomemods] " + (rtf ? "rtf" : "vanilla") + " dump " + regions.size() + " parameter points, " + points.size() + " climate points";
		} catch (Throwable th) {
			RTFCommon.LOGGER.error("[RTF biomemods] dump exception", th);
			return "[RTF biomemods] FAIL dump " + th;
		}
	}

	// "rtf biomemods match": the biome of 289 grid points from the biome source with the world's sampler and from the
	// chunk itself (built to the biomes status), how often they agree
	public static String match(MinecraftServer server) {
		ServerLevel level = server.overworld();
		var gen = level.getChunkSource().getGenerator();
		var rs = level.getChunkSource().randomState();
		var source = gen.getBiomeSource();
		int n = 0, ok = 0;
		List<String> diffs = new ArrayList<>();
		for (int x = -4096; x <= 4096; x += 512) {
			for (int z = -4096; z <= 4096; z += 512) {
				int top = gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, rs);
				int qx = QuartPos.fromBlock(x), qy = QuartPos.fromBlock(top), qz = QuartPos.fromBlock(z);
				String a = name(source.getNoiseBiome(qx, qy, qz, rs.sampler()));
				String c = name(level.getChunk(x >> 4, z >> 4, ChunkStatus.BIOMES, true).getNoiseBiome(qx, qy, qz));
				n++;
				if (a.equals(c)) ok++;
				else if (diffs.size() < 12) diffs.add(x + "," + z + ": source " + a + " chunk " + c);
			}
		}
		RTFCommon.LOGGER.info("[RTF biomemods] match differences: {}", diffs);
		return "[RTF biomemods] rtf match " + ok + "/" + n + " agree with the chunks";
	}

	private static net.minecraft.world.level.levelgen.DensityFunction constant(float value) {
		return net.minecraft.world.level.levelgen.DensityFunctions.constant(value);
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
		// temperature and humidity in steps of 0.05 from -1 to 1
		int[][] fine = new int[2][41];
		long t0 = System.nanoTime();
		for (int x = -RADIUS; x <= RADIUS; x += STEP) {
			for (int z = -RADIUS; z <= RADIUS; z += STEP) {
				int ground = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
				int top = gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, rs);
				Holder<Biome> biome = source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(top), QuartPos.fromBlock(z), rs.sampler());
				Climate.TargetPoint point = rs.sampler().sample(QuartPos.fromBlock(x), QuartPos.fromBlock(top), QuartPos.fromBlock(z));
				int[] b = bands(point);
				fine[0][Math.max(0, Math.min(40, Math.round((Climate.unquantizeCoord(point.temperature()) + 1.0F) * 20.0F)))]++;
				fine[1][Math.max(0, Math.min(40, Math.round((Climate.unquantizeCoord(point.humidity()) + 1.0F) * 20.0F)))]++;
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

		// top blocks of the columns of the chunks of every mod biome's first sample, by the biome each column has in the
		// built chunk: the biome of a place can change when the tile around it is built (WorldLookup.applyCell reads a
		// tile when there is one), so a chunk at a sample can hold other biomes. One chunk each: the server test world
		// holds the selftest's and structure test's chunks already, more ran a 3 GB server out of memory
		java.util.Set<Long> built = new java.util.HashSet<>();
		for (Stat first : List.copyOf(stats.values())) {
			int cx0 = first.first.getX() >> 4, cz0 = first.first.getZ() >> 4;
			if (!built.add(((long) cx0 << 32) ^ (cz0 & 0xFFFFFFFFL))) continue;
			ChunkAccess chunk = level.getChunk(cx0, cz0, ChunkStatus.FULL, true);
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					// the top block itself: ChunkAccess.getHeight is the heightmap's height less one
					int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
					BlockPos pos = new BlockPos((cx0 << 4) + x, y, (cz0 << 4) + z);
					Stat s = stats.get(name(level.getBiome(pos)));
					if (s == null) continue;
					String block = BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(pos).getBlock()).toString();
					s.tops.merge(block, 1, Integer::sum);
					s.columns++;
				}
			}
		}

		// biomes whose top blocks include their own mod's blocks
		int withModSurface = 0;
		List<String> lines = new ArrayList<>();
		lines.add("# " + kind + " world, seed " + level.getSeed() + ": biome, samples, underwater %, mean ground height, usual climate bands (t h c e w), top blocks in its first sample's chunk");
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
		for (int i = 0; i < 2; i++) {
			final int[] f = fine[i];
			lines.add("# " + (i == 0 ? "temperature" : "humidity") + " -1..1 by 0.05: " + java.util.Arrays.stream(f).mapToObj(Integer::toString).collect(Collectors.joining(" ")));
		}
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
