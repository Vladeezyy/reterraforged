package raccoonman.reterraforged.server;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.densityfunction.CellSampler;
import raccoonman.reterraforged.world.worldgen.structure.rule.StructureRule;
import raccoonman.reterraforged.world.worldgen.structure.rule.StructureRules;

/**
 * "rtf tilecache" (with -Drtf.selftest=true, tools/servertest.sh TILECACHE=1): whether the answers that do not come
 * from the terrain of a chunk being generated depend on the tile cache. Those lookups: the biome at a place (what a
 * structure checks, /locate biome, the strongholds), the surface height a structure starts at, the presets' structure
 * rule. For points in places no chunk was generated in, each is asked before the place's tile is in the cache and
 * again once it is; any difference means the world depends on the order its chunks are generated in.
 */
public final class RTFTileCacheTest {
	// areas of 32 x 32 points 37 blocks apart (block coordinates of the first point), far from the spawn and from the
	// other tests' chunks: plains, mountains, coasts and oceans of the test seed
	private static final int[][] AREAS = { { 20000, 20000 }, { -24000, 16000 }, { 30000, -28000 }, { -36000, -32000 }, { 44000, 8000 }, { -8000, 52000 } };
	private static final int POINTS = 32;
	private static final int STEP = 37;
	// the presets' structure rule (PresetStructureRuleData)
	private static final StructureRule CELL_TEST = StructureRules.cellTest(0.225F, TerrainType.MOUNTAIN_CHAIN, TerrainType.MOUNTAINS_1, TerrainType.MOUNTAINS_2, TerrainType.MOUNTAINS_3);

	private RTFTileCacheTest() {
	}

	private record Answers(String biome, int baseHeight, boolean structureAllowed, String terrain) {
	}

	/** The differences between the answers of two ways of asking */
	private static final class Comparison {
		private int points, biome, base, baseMax, rule, ruleFirstOnly, ruleSecondOnly, terrain;
		private long baseSum;
		private final Map<String, Integer> biomeSwaps = new HashMap<>();
		private final Map<String, Integer> terrainSwaps = new HashMap<>();

		private void add(Answers a, Answers b) {
			this.points++;
			if (!a.biome.equals(b.biome)) {
				this.biome++;
				this.biomeSwaps.merge(a.biome + " -> " + b.biome, 1, Integer::sum);
			}
			int d = Math.abs(a.baseHeight - b.baseHeight);
			if (d != 0) {
				this.base++;
				this.baseSum += d;
				this.baseMax = Math.max(this.baseMax, d);
			}
			if (a.structureAllowed != b.structureAllowed) {
				this.rule++;
				if (a.structureAllowed) {
					this.ruleFirstOnly++;
				} else {
					this.ruleSecondOnly++;
				}
			}
			if (!a.terrain.equals(b.terrain)) {
				this.terrain++;
				this.terrainSwaps.merge(a.terrain + " -> " + b.terrain, 1, Integer::sum);
			}
		}

		private boolean same() {
			return this.biome == 0 && this.base == 0 && this.rule == 0 && this.terrain == 0;
		}

		private String describe(String first, String second) {
			return String.format(Locale.ROOT, "biome differs %d (%.1f%%; %s); surface height differs %d (%.1f%%, mean %.1f, max %d blocks); "
				+ "structure rule differs %d (%.1f%%: allowed only %s %d, only %s %d); terrain differs %d (%s)",
				this.biome, 100.0 * this.biome / this.points, top(this.biomeSwaps), this.base, 100.0 * this.base / this.points,
				this.base == 0 ? 0.0 : (double) this.baseSum / this.base, this.baseMax, this.rule, 100.0 * this.rule / this.points,
				first, this.ruleFirstOnly, second, this.ruleSecondOnly, this.terrain, top(this.terrainSwaps));
		}
	}

	public static String start(MinecraftServer server) {
		ServerLevel level = server.overworld();
		RandomState randomState = level.getChunkSource().randomState();
		if (!((Object) randomState instanceof RTFRandomState state) || state.generatorContext() == null) {
			return "[RTF tilecache] FAIL not a ReTerraForged world";
		}
		GeneratorContext context = state.generatorContext();
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		Thread thread = new Thread(() -> {
			Comparison cache = new Comparison();
			for (int[] area : AREAS) {
				// all of an area before any of its tiles is cached (its points share tiles), then again
				Answers[] uncached = new Answers[POINTS * POINTS];
				for (int n = 0; n < uncached.length; n++) {
					uncached[n] = answers(level, generator, randomState, context, area[0] + n / POINTS * STEP, area[1] + n % POINTS * STEP);
				}
				for (int n = 0; n < uncached.length; n++) {
					int x = area[0] + n / POINTS * STEP, z = area[1] + n % POINTS * STEP;
					context.cache.provideAtChunk(x >> 4, z >> 4);
					cache.add(uncached[n], answers(level, generator, randomState, context, x, z));
				}
			}
			String r = String.format(Locale.ROOT, "[RTF tilecache] %d points, %s: before the tile is cached vs after: %s",
				cache.points, cache.same() ? "PASS" : "FAIL", cache.describe("before", "after"));
			RTFCommon.LOGGER.info(r);
		}, "RTF tilecache");
		thread.setDaemon(true);
		thread.start();
		return "[RTF tilecache] started";
	}

	private static String top(Map<String, Integer> swaps) {
		StringBuilder top = new StringBuilder();
		swaps.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey(Comparator.naturalOrder()))).limit(6)
			.forEach((e) -> top.append(top.length() == 0 ? "" : ", ").append(e.getValue()).append(" ").append(e.getKey().replace("minecraft:", "")));
		return top.toString();
	}

	private static Answers answers(ServerLevel level, ChunkGenerator generator, RandomState randomState, GeneratorContext context, int x, int z) {
		// the climate sampler keeps the last place's cell on this thread
		CellSampler.clearThreadLocalCache();
		Cell cell = new Cell();
		context.lookup.applyCell(cell, x, z, false);
		int baseHeight = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, randomState);
		String biome = level.getUncachedNoiseBiome(x >> 2, 80 >> 2, z >> 2).getRegisteredName();
		boolean allowed = CELL_TEST.test(randomState, new BlockPos(x, 80, z));
		return new Answers(biome, baseHeight, allowed, String.valueOf(cell.terrain));
	}
}
