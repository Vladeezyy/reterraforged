package raccoonman.reterraforged.server;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.biome.Continentalness;
import raccoonman.reterraforged.world.worldgen.biome.Erosion;
import raccoonman.reterraforged.world.worldgen.biome.Humidity;
import raccoonman.reterraforged.world.worldgen.biome.Temperature;
import raccoonman.reterraforged.world.worldgen.biome.Weirdness;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.densityfunction.CellSampler;

/**
 * "rtf islands" (with -Drtf.selftest=true, tools/servertest.sh ISLANDS=1, #38): what the land of the archipelago's
 * islands is made of in this world. On a 16 x 16 km grid every 32 blocks, for each point of island land: the terrain,
 * the height, the continent edge and the climate point, the biome at the surface and at y 0. Then real chunks (to the
 * surface: noise, aquifers, surface rules) around the middle of a few islands and on their rims: their hash, the blocks
 * and biomes on top. All of it goes to rtf-islands.txt, which tools/compareislands.py compares between two worlds of the
 * same seed. rtf-islands.png: the largest island from above, a colour per biome, lighter higher up. The line also says
 * how much the biome table itself depends on how far inland a point is, at the erosion levels of the islands.
 */
public final class RTFIslandTest {
	private static final int RADIUS = 8192, STEP = 32;
	// the middle of an island: this many grid points all around are island land too
	private static final int MARGIN = 3;
	private static final int PLACES = 4, CHUNKS_MIDDLE = 9, CHUNKS_RIM = 5;
	private static final int PLACE_DISTANCE = 1024;
	// the largest island: the most land within this many grid points
	private static final int LARGEST = 15;
	private static final int MAP_STEP = 4, MAP_SIZE = 256, MAP_SCALE = 3;
	private static final int LARGE_ISLAND = 16;

	private RTFIslandTest() {
	}

	public static String run(MinecraftServer server) {
		try {
			return test(server);
		} catch (Throwable t) {
			RTFCommon.LOGGER.error("[RTF islands] exception", t);
			return "[RTF islands] FAIL exception: " + t;
		}
	}

	private static String test(MinecraftServer server) throws IOException {
		ServerLevel level = server.overworld();
		RandomState randomState = level.getChunkSource().randomState();
		if (!((Object) randomState instanceof RTFRandomState state) || state.generatorContext() == null) {
			return "[RTF islands] FAIL not a ReTerraForged world";
		}
		GeneratorContext ctx = state.generatorContext();
		Heightmap heightmap = ctx.lookup.getHeightmap();
		Climate.Sampler climate = randomState.createClimateSampler(SamplerContext.EMPTY_UNCACHED);
		int n = RADIUS * 2 / STEP + 1;
		boolean[][] land = new boolean[n][n], rim = new boolean[n][n], continent = new boolean[n][n];
		List<String> lines = new ArrayList<>();
		lines.add("# seed " + level.getSeed());
		lines.add("# point x z terrain y continentEdge continentalness erosion weirdness surfaceBiome biomeAtY0");
		Map<String, Integer> surface = new TreeMap<>(), below = new TreeMap<>(), bands = new TreeMap<>();
		List<Integer> heights = new ArrayList<>();
		int points = 0;
		for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
			int x = -RADIUS + i * STEP, z = -RADIUS + j * STEP;
			Cell raw = new Cell();
			heightmap.applyTerrain(raw, x, z);
			if (raw.terrain != TerrainType.ISLAND && raw.terrain != TerrainType.ISLAND_BEACH && raw.terrain != TerrainType.ISLAND_MOUNTAINS) {
				continent[i][j] = raw.height > ctx.levels.water;
				continue;
			}
			String terrain = raw.terrain.getName();
			Cell cell = new Cell();
			ctx.lookup.applyCell(cell, x, z, false);
			if (cell.height <= ctx.levels.water) continue;
			land[i][j] = raw.terrain != TerrainType.ISLAND_BEACH;
			rim[i][j] = !land[i][j];
			points++;
			int y = ctx.levels.scale(cell.height);
			if (land[i][j]) heights.add(y);
			CellSampler.clearThreadLocalCache();
			String top = name(level.getUncachedNoiseBiome(x >> 2, y >> 2, z >> 2));
			CellSampler.clearThreadLocalCache();
			String deep = name(level.getUncachedNoiseBiome(x >> 2, 0, z >> 2));
			CellSampler.clearThreadLocalCache();
			Climate.TargetPoint point = climate.sample(x >> 2, y >> 2, z >> 2);
			int[] band = RTFBiomeModTest.bands(point);
			surface.merge(top, 1, Integer::sum);
			below.merge(deep, 1, Integer::sum);
			bands.merge("c" + band[2] + " e" + band[3], 1, Integer::sum);
			lines.add(String.format(Locale.ROOT, "point %d %d %s %d %.5f %d %d %d %s %s", x, z, terrain, y, cell.continentEdge, point.continentalness(), point.erosion(), point.weirdness(), top, deep));
		}
		if (points == 0) {
			return "[RTF islands] FAIL no island land within " + RADIUS + " blocks (is the archipelago on?)";
		}
		// real chunks around the middle of a few islands and on the rims of a few
		List<int[]> middles = new ArrayList<>(), rims = new ArrayList<>();
		int[] largest = null;
		int largestLand = 0;
		for (int i = MARGIN; i < n - MARGIN; i++) for (int j = MARGIN; j < n - MARGIN; j++) {
			int x = -RADIUS + i * STEP, z = -RADIUS + j * STEP;
			if (rim[i][j] && rims.size() < PLACES && farFrom(rims, x, z)) rims.add(new int[] { x, z });
			if (!land[i][j] || !allLand(land, i, j)) continue;
			if (middles.size() < PLACES && farFrom(middles, x, z)) middles.add(new int[] { x, z });
			int around = 0;
			for (int di = Math.max(-LARGEST, -i); di <= LARGEST && i + di < n; di++) for (int dj = Math.max(-LARGEST, -j); dj <= LARGEST && j + dj < n; dj++) {
				if (land[i + di][j + dj]) around++;
			}
			if (around > largestLand) {
				largestLand = around;
				largest = new int[] { x, z };
			}
		}
		Map<String, Integer> tops = new TreeMap<>(), topBiomes = new TreeMap<>();
		int chunks = 0;
		for (int[] place : middles) chunks += chunks(level, place, CHUNKS_MIDDLE, "chunk", lines, tops, topBiomes);
		Map<String, Integer> rimTops = new TreeMap<>(), rimBiomes = new TreeMap<>();
		int rimChunks = 0;
		for (int[] place : rims) rimChunks += chunks(level, place, CHUNKS_RIM, "rimchunk", lines, rimTops, rimBiomes);
		if (largest != null) {
			map(server, level, ctx, largest, lines);
		}
		String table = shape(land, rim, continent) + "; " + table(level);
		lines.add("# " + table);
		java.nio.file.Files.write(server.getServerDirectory().resolve("rtf-islands.txt"), lines);
		heights.sort(null);
		int size = Math.max(1, heights.size());
		String r = String.format(Locale.ROOT, "[RTF islands] %d points of island land, height of the land mean %.1f, median %d, 90%% %d, highest %d; climate bands %s; surface %s; at y 0 %s; %d chunks around the middle, on top %s, biomes %s; %d chunks on rims, on top %s, biomes %s; %s",
			points, heights.stream().mapToInt(Integer::intValue).average().orElse(0), heights.isEmpty() ? 0 : heights.get(size / 2), heights.isEmpty() ? 0 : heights.get((int) (size * 0.9F)), heights.isEmpty() ? 0 : heights.get(size - 1),
			top(bands, points), top(surface, points), top(below, points), chunks, top(tops, chunks * 256), top(topBiomes, chunks * 256), rimChunks, top(rimTops, rimChunks * 256), top(rimBiomes, rimChunks * 256), table);
		RTFCommon.LOGGER.info(r);
		return r;
	}

	// how the islands lie: how many of them, the length of their coast against a square of the same area (more with a rougher
	// coast), how far their land is from a continent's
	private static String shape(boolean[][] land, boolean[][] rim, boolean[][] continent) {
		int n = land.length;
		int[][] distance = new int[n][n];
		ArrayDeque<int[]> queue = new ArrayDeque<>();
		for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
			distance[i][j] = continent[i][j] ? 0 : -1;
			if (continent[i][j]) queue.add(new int[] { i, j });
		}
		while (!queue.isEmpty()) {
			int[] p = queue.poll();
			for (int di = -1; di <= 1; di++) for (int dj = -1; dj <= 1; dj++) {
				int a = p[0] + di, b = p[1] + dj;
				if (a < 0 || b < 0 || a >= n || b >= n || distance[a][b] >= 0) continue;
				distance[a][b] = distance[p[0]][p[1]] + 1;
				queue.add(new int[] { a, b });
			}
		}
		boolean[][] seen = new boolean[n][n];
		List<Integer> distances = new ArrayList<>();
		int islands = 0, large = 0;
		long area = 0;
		double coast = 0.0;
		for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
			if (seen[i][j] || !(land[i][j] || rim[i][j])) continue;
			islands++;
			int points = 0, edges = 0;
			seen[i][j] = true;
			queue.add(new int[] { i, j });
			while (!queue.isEmpty()) {
				int[] p = queue.poll();
				points++;
				if (distance[p[0]][p[1]] >= 0) distances.add(distance[p[0]][p[1]] * STEP);
				for (int[] d : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
					int a = p[0] + d[0], b = p[1] + d[1];
					if (a < 0 || b < 0 || a >= n || b >= n) continue;
					if (!(land[a][b] || rim[a][b])) {
						edges++;
					} else if (!seen[a][b]) {
						seen[a][b] = true;
						queue.add(new int[] { a, b });
					}
				}
			}
			if (points >= LARGE_ISLAND) {
				large++;
				area += points;
				coast += points * (edges / (4.0 * Math.sqrt(points)));
			}
		}
		distances.sort(null);
		return String.format(Locale.ROOT, "%d islands, %d of at least %d points; coast %.2f; distance from a continent nearest %d, median %d blocks", islands, large, LARGE_ISLAND, area == 0 ? 0.0 : coast / area,
			distances.isEmpty() ? -1 : distances.get(0), distances.isEmpty() ? -1 : distances.get(distances.size() / 2));
	}

	private static boolean farFrom(List<int[]> places, int x, int z) {
		return places.stream().allMatch((p) -> Math.abs(p[0] - x) + Math.abs(p[1] - z) >= PLACE_DISTANCE);
	}

	private static boolean allLand(boolean[][] land, int i, int j) {
		for (int di = -MARGIN; di <= MARGIN; di++) for (int dj = -MARGIN; dj <= MARGIN; dj++) {
			if (!land[i + di][j + dj]) return false;
		}
		return true;
	}

	private static int chunks(ServerLevel level, int[] place, int size, String kind, List<String> lines, Map<String, Integer> tops, Map<String, Integer> biomes) {
		int centerX = place[0] >> 4, centerZ = place[1] >> 4, count = 0;
		for (int cx = centerX - size / 2; cx <= centerX + size / 2; cx++) for (int cz = centerZ - size / 2; cz <= centerZ + size / 2; cz++) {
			ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.TERRAIN, true);
			Map<String, Integer> chunkTops = new TreeMap<>(), chunkBiomes = new TreeMap<>();
			for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
				int y = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, x, z);
				String block = BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(new BlockPos((cx << 4) + x, y, (cz << 4) + z)).getBlock()).getPath();
				chunkTops.merge(block, 1, Integer::sum);
				tops.merge(block, 1, Integer::sum);
				String biome = name(chunk.getNoiseBiome(((cx << 4) + x) >> 2, y >> 2, ((cz << 4) + z) >> 2));
				chunkBiomes.merge(biome, 1, Integer::sum);
				biomes.merge(biome, 1, Integer::sum);
			}
			lines.add(kind + " " + cx + " " + cz + " " + Long.toHexString(RTFFingerprint.hash(level, chunk)) + " " + chunkTops.toString().replace(" ", "") + " " + chunkBiomes.toString().replace(" ", ""));
			count++;
		}
		return count;
	}

	// the largest island from above, one pixel every MAP_STEP blocks; "map" line: the share of its land by steepness and height
	private static void map(MinecraftServer server, ServerLevel level, GeneratorContext ctx, int[] center, List<String> lines) throws IOException {
		int[][] heights = new int[MAP_SIZE][MAP_SIZE];
		BufferedImage image = new BufferedImage(MAP_SIZE * MAP_SCALE, MAP_SIZE * MAP_SCALE, BufferedImage.TYPE_INT_RGB);
		Map<String, Integer> biomes = new TreeMap<>();
		int water = ctx.levels.waterLevel, land = 0, high = 0, steep = 0;
		for (int i = 0; i < MAP_SIZE; i++) for (int j = 0; j < MAP_SIZE; j++) {
			int x = center[0] + (i - MAP_SIZE / 2) * MAP_STEP, z = center[1] + (j - MAP_SIZE / 2) * MAP_STEP;
			Cell cell = new Cell();
			ctx.lookup.applyCell(cell, x, z, false);
			int y = ctx.levels.scale(cell.height);
			heights[i][j] = y;
			int rgb = 0x3d6fb8;
			if (cell.height > ctx.levels.water) {
				CellSampler.clearThreadLocalCache();
				String biome = name(level.getUncachedNoiseBiome(x >> 2, y >> 2, z >> 2));
				biomes.merge(biome, 1, Integer::sum);
				land++;
				if (y >= water + 10) high++;
				float light = Math.min(1.3F, 0.75F + (y - water) / 60.0F);
				rgb = shade(colour(biome), light);
			}
			for (int a = 0; a < MAP_SCALE; a++) for (int b = 0; b < MAP_SCALE; b++) {
				image.setRGB(i * MAP_SCALE + a, j * MAP_SCALE + b, rgb);
			}
		}
		for (int i = 1; i < MAP_SIZE - 1; i++) for (int j = 1; j < MAP_SIZE - 1; j++) {
			if (heights[i][j] <= water) continue;
			int rise = Math.max(Math.abs(heights[i + 1][j] - heights[i - 1][j]), Math.abs(heights[i][j + 1] - heights[i][j - 1]));
			// 45 degrees or more
			if (rise >= 2 * MAP_STEP) steep++;
		}
		ImageIO.write(image, "png", server.getServerDirectory().resolve("rtf-islands.png").toFile());
		lines.add(String.format(Locale.ROOT, "map center %d %d, land %d, at least 10 above the sea %.1f%%, 45 degrees or steeper %.2f%%, biomes %s", center[0], center[1], land, 100.0 * high / Math.max(1, land), 100.0 * steep / Math.max(1, land), top(biomes, Math.max(1, land))));
	}

	private static int shade(int rgb, float light) {
		int r = Math.min(255, (int) ((rgb >> 16 & 255) * light)), g = Math.min(255, (int) ((rgb >> 8 & 255) * light)), b = Math.min(255, (int) ((rgb & 255) * light));
		return r << 16 | g << 8 | b;
	}

	private static int colour(String biome) {
		if (biome.contains("ocean")) return 0x2f5fa8;
		if (biome.equals("snowy_beach")) return 0xd8d4c0;
		if (biome.contains("beach")) return 0xe6d88a;
		if (biome.contains("stony")) return 0x9a9a9a;
		if (biome.contains("badlands")) return 0xb8622c;
		if (biome.equals("desert")) return 0xd2c070;
		if (biome.equals("meadow")) return 0x9ad45a;
		if (biome.contains("cherry")) return 0xe39ac0;
		if (biome.contains("savanna")) return 0xb0a84a;
		if (biome.contains("jungle")) return 0x2e9a30;
		if (biome.contains("dark_forest")) return 0x2c5a2a;
		if (biome.contains("birch")) return 0x6fb060;
		if (biome.contains("forest")) return 0x4a8a3c;
		if (biome.contains("snowy") || biome.contains("ice") || biome.contains("frozen") || biome.contains("grove")) return 0xc8d8e4;
		if (biome.contains("taiga")) return 0x4e7a64;
		if (biome.contains("plains")) return 0x88b85a;
		return 0xa070a0;
	}

	// the biome table with the weirdness of the islands: in how many of the 25 climates the biome at far inland is another
	// one than at near inland, for each erosion level, and at a coast against near inland
	private static String table(ServerLevel level) {
		BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
		if (!(source instanceof MultiNoiseBiomeSource multiNoise)) {
			return "biome source " + source.getClass().getSimpleName();
		}
		StringBuilder far = new StringBuilder(), coast = new StringBuilder();
		float weirdness = Weirdness.MID_SLICE_NORMAL_DESCENDING.mid();
		for (Erosion erosion : Arrays.asList(Erosion.LEVEL_2, Erosion.LEVEL_3, Erosion.LEVEL_4)) {
			int fars = 0, coasts = 0;
			for (Temperature temperature : Temperature.values()) for (Humidity humidity : Humidity.values()) {
				Holder<Biome> near = biome(multiNoise, temperature, humidity, Continentalness.NEAR_INLAND, erosion, weirdness);
				if (!near.equals(biome(multiNoise, temperature, humidity, Continentalness.FAR_INLAND, erosion, weirdness))) fars++;
				if (!near.equals(biome(multiNoise, temperature, humidity, Continentalness.COAST, erosion, weirdness))) coasts++;
			}
			far.append(far.length() > 0 ? ", " : "").append(erosion.ordinal()).append(": ").append(fars);
			coast.append(coast.length() > 0 ? ", " : "").append(erosion.ordinal()).append(": ").append(coasts);
		}
		return "climates of 25 with another biome far inland than near inland by erosion " + far + "; at a coast " + coast;
	}

	private static Holder<Biome> biome(MultiNoiseBiomeSource source, Temperature temperature, Humidity humidity, Continentalness continentalness, Erosion erosion, float weirdness) {
		return source.getNoiseBiome(Climate.target(temperature.mid(), humidity.mid(), continentalness.mid(), erosion.mid(), 0.0F, weirdness));
	}

	private static String name(Holder<Biome> biome) {
		return biome.unwrapKey().map((key) -> key.identifier().getPath()).orElse("?");
	}

	// the most common first, the first few, as shares
	private static String top(Map<String, Integer> counts, int total) {
		List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
		sorted.sort((a, b) -> b.getValue() - a.getValue());
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < Math.min(6, sorted.size()); i++) {
			b.append(i > 0 ? ", " : "").append(sorted.get(i).getKey()).append(String.format(Locale.ROOT, " %.1f%%", 100.0 * sorted.get(i).getValue() / Math.max(1, total)));
		}
		return b + (sorted.size() > 6 ? " and " + (sorted.size() - 6) + " more" : "");
	}
}
