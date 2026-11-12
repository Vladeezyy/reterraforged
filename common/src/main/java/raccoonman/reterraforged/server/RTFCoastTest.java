package raccoonman.reterraforged.server;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.cell.Cell;

/**
 * "rtf coasts" (with -Drtf.selftest=true, tools/servertest.sh COASTS=1, #38, #40): what the shores of this world are. On a
 * 6 x 6 km grid every 8 blocks, the points the biome source takes for a coast (the climate sampler's continentalness
 * from -0.19 to -0.11) and their biomes at the surface. For those above the sea, how far the ground rises within 8 blocks
 * (the height of the 4 points 8 blocks away, less the point's own): a steep shore rises STEEP blocks or more. The heights:
 * in this world ReTerraForged's before the tile's erosion (what its base height queries give, a thousand times faster),
 * in a vanilla world the base height. A beach
 * (minecraft:is_beach) on a steep shore carries its sand up the slope (#38); stony shores (c:is_stony_shores) are what
 * Regions Unexplored's Chalk Cliffs replace (#40). Then 3 x 3 real chunks at a few of the steepest shores, chosen by the
 * ground alone (the same places when only the biomes change): sand on top of columns above the sea that stand on a slope,
 * by biome and by height above the sea. There too the chunk's biome against the biome source's (the cell without the tile:
 * what the erosion of older worlds reads, SurfaceBiomes): the land columns where they differ, and the sand on a slope the
 * erosion of rock slopes takes (a beach or not c:is_sandy) by one of them and leaves by the other.
 * The vanilla world is the reference. Writes rtf-coasts-<rtf|vanilla>.txt in the server folder and prints "[RTF coasts] ...".
 */
public final class RTFCoastTest {
	private static final int RADIUS = 3072, STEP = 8, NEAR = 8, STEEP = 6;
	// the coast band of the vanilla biome tables (OverworldBiomeBuilder.coastContinentalness)
	private static final float COAST_MIN = -0.19F, COAST_MAX = -0.11F;
	// the spots for real chunks: the ground rises SPOT_RISE blocks within SPOT_NEAR (an island's cliffs stand behind its beach)
	private static final int SPOTS = 8, SPOT_DISTANCE = 384, SPOT_NEAR = 24, SPOT_RISE = 2 * STEEP;
	// a column on a slope: a neighbour column this many blocks higher or lower
	private static final int SLOPE = 2;
	private static final TagKey<Biome> SANDY = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c", "is_sandy"));
	private static final TagKey<Biome> STONY_SHORES = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c", "is_stony_shores"));

	private RTFCoastTest() {
	}

	public static String run(MinecraftServer server) {
		try {
			return test(server);
		} catch (Throwable t) {
			RTFCommon.LOGGER.error("[RTF coasts] exception", t);
			return "[RTF coasts] FAIL exception: " + t;
		}
	}

	private static String name(Holder<Biome> biome) {
		return biome.unwrapKey().map((key) -> key.location().toString()).orElse("?");
	}

	private static boolean stony(Holder<Biome> biome) {
		return biome.is(STONY_SHORES) || biome.is(Biomes.STONY_SHORE);
	}

	private static String test(MinecraftServer server) throws IOException {
		ServerLevel level = server.overworld();
		ChunkGenerator gen = level.getChunkSource().getGenerator();
		RandomState rs = level.getChunkSource().randomState();
		BiomeSource source = gen.getBiomeSource();
		Climate.Sampler sampler = rs.sampler();
		boolean rtf = (Object) rs instanceof RTFRandomState s && s.generatorContext() != null;
		String kind = rtf ? "rtf" : "vanilla";
		int sea = gen.getSeaLevel();
		int seaQuart = QuartPos.fromBlock(sea);
		long t0 = System.nanoTime();

		// the coast points of the grid
		List<int[]> coast = new ArrayList<>();
		int samples = 0;
		for (int x = -RADIUS; x <= RADIUS; x += STEP) {
			for (int z = -RADIUS; z <= RADIUS; z += STEP) {
				samples++;
				float c = Climate.unquantizeCoord(sampler.sample(QuartPos.fromBlock(x), seaQuart, QuartPos.fromBlock(z)).continentalness());
				if (c >= COAST_MIN && c < COAST_MAX) {
					coast.add(new int[] { x, z });
				}
			}
		}
		long t1 = System.nanoTime();

		// their biomes, and the rise of the ground around those above the sea
		Map<Long, Integer> heights = new HashMap<>();
		raccoonman.reterraforged.world.worldgen.cell.heightmap.Heightmap ground = rtf ? ((RTFRandomState) (Object) rs).generatorContext().lookup.getHeightmap() : null;
		Cell scratch = new Cell();
		java.util.function.BiFunction<Integer, Integer, Integer> height = (x, z) -> heights.computeIfAbsent(((long) x << 32) ^ (z & 0xFFFFFFFFL), (k) -> {
			if (ground == null) {
				return gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
			}
			// the first air block above the ground, as getBaseHeight
			ground.applyTerrain(scratch.reset(), x, z);
			ground.applyRivers(scratch, x, z, ground.continent().getRivermap(scratch));
			return ground.levels().scale(scratch.height) + 1;
		});
		Map<String, Integer> biomes = new HashMap<>(), steepBiomes = new HashMap<>();
		int land = 0, steep = 0, beachLand = 0, beachSteep = 0, stonyLand = 0, stonySteep = 0, stonyFlat = 0;
		// the shores in the climate of Regions Unexplored 0.6's Chalk Cliffs: temperature -0.1 to 0.2, erosion -0.6 or less
		int stonyWarm = 0, stonyWarmEroded = 0, chalk = 0;
		List<int[]> candidates = new ArrayList<>();
		for (int[] p : coast) {
			int x = p[0], z = p[1];
			int h = height.apply(x, z);
			int qy = QuartPos.fromBlock(Math.max(h, sea));
			Holder<Biome> biome = source.getNoiseBiome(QuartPos.fromBlock(x), qy, QuartPos.fromBlock(z), sampler);
			biomes.merge(name(biome), 1, Integer::sum);
			boolean chalky = name(biome).endsWith(":chalk_cliffs");
			if (chalky) chalk++;
			if (stony(biome) || chalky) {
				Climate.TargetPoint point = sampler.sample(QuartPos.fromBlock(x), qy, QuartPos.fromBlock(z));
				float temperature = Climate.unquantizeCoord(point.temperature());
				if (temperature >= -0.1F && temperature <= 0.2F) {
					stonyWarm++;
					if (Climate.unquantizeCoord(point.erosion()) <= -0.6F) stonyWarmEroded++;
				}
			}
			// getBaseHeight is the first air block: the top block is below the sea's surface up to the sea level
			if (h <= sea) continue;
			land++;
			int rise = Math.max(Math.max(height.apply(x + NEAR, z), height.apply(x - NEAR, z)), Math.max(height.apply(x, z + NEAR), height.apply(x, z - NEAR))) - h;
			boolean isSteep = rise >= STEEP;
			boolean beach = biome.is(BiomeTags.IS_BEACH), isStony = stony(biome);
			if (beach) beachLand++;
			if (isStony) stonyLand++;
			if (isSteep) {
				steep++;
				steepBiomes.merge(name(biome), 1, Integer::sum);
				if (beach) beachSteep++;
				if (isStony) stonySteep++;
			} else if (isStony) {
				stonyFlat++;
			}
			int far = rise;
			for (int d = 2 * NEAR; d <= SPOT_NEAR; d += NEAR) {
				far = Math.max(far, Math.max(Math.max(height.apply(x + d, z), height.apply(x - d, z)), Math.max(height.apply(x, z + d), height.apply(x, z - d))) - h);
			}
			if (far >= SPOT_RISE) candidates.add(new int[] { x, z, far });
		}
		long t2 = System.nanoTime();

		// the steepest shores far enough apart, by the ground alone
		candidates.sort((a, b) -> a[2] != b[2] ? Integer.compare(b[2], a[2]) : a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
		List<int[]> spots = new ArrayList<>();
		for (int[] c : candidates) {
			if (spots.size() >= SPOTS) break;
			boolean far = spots.stream().allMatch((s) -> Math.max(Math.abs(s[0] - c[0]), Math.abs(s[1] - c[1])) >= SPOT_DISTANCE);
			if (far) spots.add(c);
		}
		List<String> lines = new ArrayList<>();
		lines.add("# " + kind + " world, seed " + level.getSeed() + ": coast points on a grid every " + STEP + " blocks within " + RADIUS + ", steep: the ground rises " + STEEP + "+ blocks within " + NEAR);
		int landColumns = 0, sandColumns = 0, sandSlope = 0, sandHigh = 0, differs = 0, erodedByChunk = 0, erodedBySource = 0;
		Map<String, Integer> slopeBiomes = new HashMap<>();
		// the biome source's at the same blocks, with the zoom of the level's (and the regions') seed
		BiomeManager sourceBiomes = level.getBiomeManager().withDifferentSource((x, y, z) -> source.getNoiseBiome(x, y, z, sampler));
		for (int[] spot : spots) {
			Map<String, Integer> here = new HashMap<>();
			int[] r = chunks(level, sourceBiomes, spot[0] >> 4, spot[1] >> 4, sea, here);
			landColumns += r[0];
			sandColumns += r[1];
			sandSlope += r[2];
			sandHigh += r[3];
			differs += r[5];
			erodedByChunk += r[6];
			erodedBySource += r[7];
			here.forEach((k, v) -> slopeBiomes.merge(k, v, Integer::sum));
			lines.add(String.format(Locale.ROOT, "spot %d %d rise %d: land columns %d, sand %d, sand on a slope %d, sand %d+ above the sea %d, highest sand %d above the sea; sand on a slope by biome %s; "
				+ "chunk and source biome differ %d, sand on a slope erodible by the chunk's biome only %d, by the source's only %d",
				spot[0], spot[1], spot[2], r[0], r[1], r[2], STEEP, r[3], r[4], top(here, 6), r[5], r[6], r[7]));
		}
		long t3 = System.nanoTime();

		lines.add("# coast biomes: " + top(biomes, 40));
		lines.add("# steep coast biomes: " + top(steepBiomes, 40));
		Files.write(server.getServerDirectory().resolve("rtf-coasts-" + kind + ".txt"), lines);
		String result = String.format(Locale.ROOT,
			"[RTF coasts] %s coast %d of %d points, above the sea %d, steep %d (%.1f%%); beach %d, on steep shores %d (%.1f%% of steep); stony shore %d (%.1f%%), steep %d, flat %d; "
				+ "stony or chalk cliffs at temperature -0.1 to 0.2 %d, of them erosion -0.6 or less %d; chalk cliffs %d; "
				+ "%d spots: land columns %d, sand %d, sand on a slope %d (%s), sand %d+ above the sea %d; chunk and source biome differ %d, sand on a slope erodible by the chunk's biome only %d, by the source's only %d; biomes %s; %d + %d + %d ms",
			kind, coast.size(), samples, land, steep, pct(steep, land), beachLand, beachSteep, pct(beachSteep, steep), stonyLand, pct(stonyLand, land), stonySteep, stonyFlat, stonyWarm, stonyWarmEroded, chalk,
			spots.size(), landColumns, sandColumns, sandSlope, top(slopeBiomes, 3), STEEP, sandHigh, differs, erodedByChunk, erodedBySource, top(biomes, 8), (t1 - t0) / 1_000_000, (t2 - t1) / 1_000_000, (t3 - t2) / 1_000_000);
		RTFCommon.LOGGER.info(result);
		return result;
	}

	// 3 x 3 chunks around a chunk: land columns, sand on top, sand on a slope (and their biomes), sand high above the sea,
	// the highest sand above the sea; land columns whose chunk biome is not the source's, sand on a slope the rock slopes'
	// erosion takes by the chunk's biome and not by the source's, and the other way round
	private static int[] chunks(ServerLevel level, BiomeManager sourceBiomes, int cx, int cz, int sea, Map<String, Integer> slopeBiomes) {
		int size = 48;
		int[][] top = new int[size][size];
		boolean[][] sand = new boolean[size][size];
		String[][] biome = new String[size][size];
		boolean[][] differs = new boolean[size][size];
		// 1: the erosion takes sand on a rock slope by the chunk's biome only, 2: by the source's only
		byte[][] erodes = new byte[size][size];
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				ChunkAccess chunk = level.getChunk(cx + dx, cz + dz, ChunkStatus.FULL, true);
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						int bx = ((cx + dx) << 4) + x, bz = ((cz + dz) << 4) + z;
						// the top block of the ground: the heightmap's height less one, under trunks, cacti and the like
						int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
						BlockState state = chunk.getBlockState(pos.set(bx, y, bz));
						while (y > level.getMinBuildHeight() && notGround(state)) {
							state = chunk.getBlockState(pos.set(bx, --y, bz));
						}
						int i = (dx + 1) * 16 + x, j = (dz + 1) * 16 + z;
						// water on top: not land
						top[i][j] = state.getFluidState().isEmpty() ? y : Integer.MIN_VALUE;
						sand[i][j] = state.is(BlockTags.SAND);
						// the level's biome manager reads the chunk's biomes
						Holder<Biome> chunkBiome = level.getBiome(pos.set(bx, y, bz)), sourceBiome = sourceBiomes.getBiome(pos);
						differs[i][j] = !chunkBiome.equals(sourceBiome);
						if (sand[i][j]) {
							biome[i][j] = name(chunkBiome);
							boolean byChunk = erodesSand(chunkBiome), bySource = erodesSand(sourceBiome);
							erodes[i][j] = (byte) (byChunk == bySource ? 0 : byChunk ? 1 : 2);
						}
					}
				}
			}
		}
		int landColumns = 0, sandColumns = 0, sandSlope = 0, sandHigh = 0, highest = 0, differ = 0, byChunk = 0, bySource = 0;
		for (int i = 1; i < size - 1; i++) {
			for (int j = 1; j < size - 1; j++) {
				int y = top[i][j];
				if (y < sea) continue;
				landColumns++;
				if (differs[i][j]) differ++;
				if (!sand[i][j]) continue;
				sandColumns++;
				int slope = 0;
				for (int[] d : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
					int n = top[i + d[0]][j + d[1]];
					if (n != Integer.MIN_VALUE) slope = Math.max(slope, Math.abs(n - y));
				}
				if (slope >= SLOPE) {
					sandSlope++;
					slopeBiomes.merge(biome[i][j], 1, Integer::sum);
					if (erodes[i][j] == 1) byChunk++;
					if (erodes[i][j] == 2) bySource++;
				}
				if (y >= sea + STEEP) sandHigh++;
				highest = Math.max(highest, y - sea);
			}
		}
		return new int[] { landColumns, sandColumns, sandSlope, sandHigh, highest, differ, byChunk, bySource };
	}

	// the erosion turns sand on a rock slope to rock in these biomes (ErodeFeature.erodeColumn), not in the badlands at all
	private static boolean erodesSand(Holder<Biome> biome) {
		return !biome.is(Biomes.BADLANDS) && !biome.is(Biomes.WOODED_BADLANDS) && (!biome.is(SANDY) || biome.is(BiomeTags.IS_BEACH));
	}

	private static boolean notGround(BlockState state) {
		return state.isAir() || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(Blocks.CACTUS) || state.is(Blocks.BAMBOO)
			|| state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.RED_MUSHROOM_BLOCK) || state.is(Blocks.MUSHROOM_STEM) || state.is(Blocks.SNOW);
	}

	private static double pct(int a, int b) {
		return b == 0 ? 0.0 : 100.0 * a / b;
	}

	private static String top(Map<String, Integer> counts, int n) {
		int total = counts.values().stream().mapToInt(Integer::intValue).sum();
		return counts.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(n)
			.map((e) -> String.format(Locale.ROOT, "%s %.1f%%", e.getKey().replace("minecraft:", ""), pct(e.getValue(), total))).collect(Collectors.joining(", "));
	}
}
