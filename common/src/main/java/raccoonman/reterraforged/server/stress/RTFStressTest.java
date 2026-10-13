package raccoonman.reterraforged.server.stress;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.util.RandomSource;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.concurrent.cache.CacheManager;
import raccoonman.reterraforged.config.PerformanceConfig;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.Presets;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

/**
 * "rtf stress [random count]" — with -Drtf.selftest=true (tools/stresstest.sh). Every setting of the preset screen at
 * both ends of its slider (every toggle state, every cycle value), every built-in preset, everything at the minimum,
 * everything at the maximum and random combinations. Each variant goes the way a player's preset goes: through the
 * preset codec, into the datapack registries world creation builds (noise router, noise settings, dimension type,
 * features...), into the preview of the preset screen and into world tiles with every filter (erosion), near the
 * spawn and far out. A variant fails on an exception or a cell value that is not finite, and hangs past the time
 * limit. Prints "[RTF stress] ..." lines and a summary.
 */
public final class RTFStressTest {
	private static final int SEED = 20261013;
	private static final long TIME_LIMIT_SECONDS = 180;
	// chunk positions of the world tiles: the spawn area, far out, near the world border
	private static final int[][] CHUNKS = { { 0, 0 }, { 6000, -9000 }, { -1_870_000, 1_870_000 } };

	private record Change(Setting<?> setting, Object value) {

		@Override
		public String toString() {
			return this.setting.name() + " = " + this.value;
		}
	}

	// a base preset with settings changed on it
	private record Variant(String name, Supplier<Preset> base, List<Change> changes) {

		Preset build() {
			Preset preset = this.base.get();
			this.changes.forEach((change) -> change.setting().set(preset, change.value()));
			return preset;
		}

		Variant without(Change change) {
			List<Change> changes = new ArrayList<>(this.changes);
			changes.remove(change);
			return new Variant(this.name, this.base, changes);
		}
	}

	private RTFStressTest() {
	}

	public static String run(MinecraftServer server, int randomCount) {
		return run(server, variants(randomCount), TIME_LIMIT_SECONDS);
	}

	// "rtf stress abuse": every number setting at values no slider gives (NaN, infinities, negative, huge), as a
	// broken or edited preset file can hold them; they must not crash, hang or leave NaN in the terrain
	public static String runAbuse(MinecraftServer server) {
		List<Variant> variants = new ArrayList<>();
		for (Setting<?> setting : PresetStressTable.SETTINGS) {
			for (Object value : setting.abuse()) {
				Change change = new Change(setting, value);
				variants.add(new Variant(change.toString(), Presets::makeDefault, List.of(change)));
			}
		}
		return run(server, variants, 30);
	}

	private static String run(MinecraftServer server, List<Variant> variants, long timeLimitSeconds) {
		RegistryAccess registries = server.registries().getAccessForLoading(RegistryLayer.DIMENSIONS);
		RTFCommon.LOGGER.info("[RTF stress] {} variants, {} settings, time limit {} s each", variants.size(), PresetStressTable.SETTINGS.size(), timeLimitSeconds);
		Map<String, Integer> failures = new LinkedHashMap<>();
		List<String> hangs = new ArrayList<>();
		String slowest = "";
		long slowestMs = 0;
		long start = System.nanoTime();
		Runner runner = new Runner(registries);
		for (int i = 0; i < variants.size(); i++) {
			Variant variant = variants.get(i);
			long t0 = System.nanoTime();
			Outcome outcome = runner.run(variant, timeLimitSeconds);
			if (outcome.hung()) {
				hangs.add(variant.name());
				RTFCommon.LOGGER.error("[RTF stress] HANG {} (over {} s)", variant.name(), timeLimitSeconds);
			} else if (outcome.problem() != null) {
				fail(failures, variant, outcome.problem(), outcome.cause());
				culprits(runner, variant);
			}
			long ms = (System.nanoTime() - t0) / 1_000_000;
			if (ms > slowestMs) {
				slowestMs = ms;
				slowest = variant.name();
			}
			if ((i + 1) % 25 == 0) {
				// the heap after a collection: it must stay flat (a growing one is a leak, see Cache.Poller)
				System.gc();
				Runtime runtime = Runtime.getRuntime();
				RTFCommon.LOGGER.info("[RTF stress] {}/{} done, heap {} MB", i + 1, variants.size(), (runtime.totalMemory() - runtime.freeMemory()) >> 20);
			}
		}
		runner.close();
		int failed = failures.values().stream().mapToInt(Integer::intValue).sum();
		String summary = String.format("[RTF stress] %d variants in %d s: %d ok, %d failed, %d hung; slowest %s (%d ms)",
			variants.size(), (System.nanoTime() - start) / 1_000_000_000L, variants.size() - failed - hangs.size(), failed, hangs.size(), slowest, slowestMs);
		RTFCommon.LOGGER.info(summary);
		failures.forEach((problem, count) -> RTFCommon.LOGGER.info("[RTF stress] {}x {}", count, problem));
		hangs.forEach((name) -> RTFCommon.LOGGER.info("[RTF stress] HANG {}", name));
		return summary;
	}

	// what a variant did: nothing wrong (problem null), failed, or hung past the time limit
	private record Outcome(@Nullable String problem, @Nullable Throwable cause, boolean hung) {
	}

	// runs variants on a thread of its own with a time limit; a hung thread is left behind (daemon) with the stacks
	// of the threads stuck in the mod's code logged, once per place
	private static final class Runner {
		private final RegistryAccess registries;
		private final Set<String> loggedHangs = new HashSet<>();
		private ExecutorService executor = newExecutor();

		Runner(RegistryAccess registries) {
			this.registries = registries;
		}

		Outcome run(Variant variant, long timeLimitSeconds) {
			Future<String> result = this.executor.submit(() -> test(variant.build(), this.registries));
			try {
				String problem = result.get(timeLimitSeconds, TimeUnit.SECONDS);
				return new Outcome(problem, null, false);
			} catch (TimeoutException e) {
				this.logStuckThreads();
				result.cancel(true);
				this.executor.shutdownNow();
				this.executor = newExecutor();
				return new Outcome("hung", null, true);
			} catch (Throwable t) {
				Throwable cause = t.getCause() != null ? t.getCause() : t;
				return new Outcome(cause.toString(), cause, false);
			} finally {
				clearCaches();
			}
		}

		private void logStuckThreads() {
			Thread.getAllStackTraces().forEach((thread, stack) -> {
				int first = -1;
				for (int i = 0; i < stack.length; i++) {
					if (stack[i].getClassName().startsWith("raccoonman.reterraforged.") && !stack[i].getClassName().startsWith("raccoonman.reterraforged.server.stress.")) {
						first = i;
						break;
					}
				}
				if (first >= 0 && this.loggedHangs.add(stack[first].toString())) {
					StringBuilder trace = new StringBuilder();
					for (int i = 0; i < Math.min(stack.length, first + 25); i++) {
						trace.append("\n\tat ").append(stack[i]);
					}
					RTFCommon.LOGGER.error("[RTF stress] stuck thread {}:{}", thread.getName(), trace);
				}
			});
		}

		void close() {
			this.executor.shutdownNow();
		}
	}

	private static void fail(Map<String, Integer> failures, Variant variant, String problem, Throwable t) {
		boolean first = !failures.containsKey(problem);
		failures.merge(problem, 1, Integer::sum);
		RTFCommon.LOGGER.error("[RTF stress] FAIL {}: {}", variant.name(), problem);
		if (first && t != null) {
			RTFCommon.LOGGER.error("[RTF stress] first time:", t);
		}
	}

	private static ExecutorService newExecutor() {
		return Executors.newSingleThreadExecutor((r) -> {
			Thread thread = new Thread(r, "RTF stress test");
			thread.setDaemon(true);
			return thread;
		});
	}

	private static void clearCaches() {
		try {
			CacheManager.clear();
		} catch (Exception e) {
			RTFCommon.LOGGER.warn("[RTF stress] cache clear failed", e);
		}
	}

	// for a variant with several changed settings: the settings it fails with, by putting each back to the default
	// while the variant still fails without it (each try with a short time limit; a hang ends the search)
	private static void culprits(Runner runner, Variant variant) {
		if (variant.changes().size() < 2) {
			return;
		}
		Variant reduced = variant;
		for (Change change : variant.changes()) {
			Variant candidate = reduced.without(change);
			// logged before the run: a candidate that takes all the memory kills the server before its outcome
			RTFCommon.LOGGER.info("[RTF stress]   trying {} with {}", variant.name(), candidate.changes());
			Outcome outcome = runner.run(candidate, 30);
			if (outcome.hung()) {
				RTFCommon.LOGGER.error("[RTF stress]   {} without {} hangs: {}", variant.name(), change, candidate.changes());
				return;
			}
			if (outcome.problem() != null) {
				reduced = candidate;
			}
		}
		RTFCommon.LOGGER.error("[RTF stress]   {} fails with: {}", variant.name(), reduced.changes());
	}

	/**
	 * A stress preset by name, for a real world (tools/stressworld.sh): "min" (every setting at its minimum), "max"
	 * (every setting at its maximum) or "random:N" (the stress test's random combination number N).
	 */
	public static Preset makePreset(String spec) {
		// the README screenshots (tools/showcase.sh): Default, with a taller world (higher mountains) and larger biomes
		if (spec.equals("showcase")) {
			Preset showcase = Presets.makeDefault();
			showcase.world().properties.worldHeight = 384;
			showcase.climate().biomeShape.biomeSize = 300;
			showcase.terrain().mountains.verticalScale = 1.15F;
			RTFCommon.LOGGER.info("[RTF] showcase preset: Default, world height 384, biome size 300, mountain vertical scale 1.15");
			return showcase;
		}
		List<Variant> variants = variants(spec.startsWith("random:") ? Integer.parseInt(spec.substring(7)) + 1 : 0);
		String name = switch (spec) {
			case "min" -> "every setting at its minimum";
			case "max" -> "every setting at its maximum";
			default -> spec.startsWith("random:") ? "random #" + spec.substring(7) + " " : null;
		};
		if (name == null) {
			throw new IllegalArgumentException("unknown stress preset " + spec + " (min, max, random:N)");
		}
		Variant variant = variants.stream().filter((v) -> v.name().startsWith(name) || v.name().equals(name.trim())).findFirst().orElseThrow();
		RTFCommon.LOGGER.info("[RTF] stress preset {}: {}", spec, variant.changes());
		return variant.build();
	}

	private static List<Variant> variants(int randomCount) {
		List<Variant> variants = new ArrayList<>();
		variants.add(new Variant("preset Default", Presets::makeDefault, List.of()));
		variants.add(new Variant("preset Legacy Vanillaish", Presets::makeLegacyVanillaish, List.of()));
		variants.add(new Variant("preset Legacy Beautiful", Presets::makeLegacyBeautiful, List.of()));
		variants.add(new Variant("preset Legacy Lite", Presets::makeLegacyLite, List.of()));
		variants.add(new Variant("preset Legacy Huge Biomes", Presets::makeLegacyHugeBiomes, List.of()));
		for (Setting<?> setting : PresetStressTable.SETTINGS) {
			for (Object value : setting.extremes()) {
				Change change = new Change(setting, value);
				variants.add(new Variant(change.toString(), Presets::makeDefault, List.of(change)));
			}
		}
		variants.add(new Variant("every setting at its minimum", Presets::makeDefault, PresetStressTable.SETTINGS.stream().map((s) -> new Change(s, s.min())).toList()));
		variants.add(new Variant("every setting at its maximum", Presets::makeDefault, PresetStressTable.SETTINGS.stream().map((s) -> new Change(s, s.max())).toList()));
		RandomSource random = RandomSource.create(SEED);
		for (int i = 0; i < randomCount; i++) {
			long seed = random.nextLong();
			RandomSource values = RandomSource.create(seed);
			List<Change> changes = PresetStressTable.SETTINGS.stream().map((s) -> new Change(s, s.random().next(values))).toList();
			variants.add(new Variant("random #" + i + " (seed " + seed + ")", Presets::makeDefault, changes));
		}
		return variants;
	}

	// null when the variant works, else what went wrong
	private static String test(Preset preset, RegistryAccess registries) {
		// the preset codec (the preset is saved in the world's datapack and in exported preset files)
		JsonElement json = Preset.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, preset).getOrThrow();
		Preset decoded = Preset.DIRECT_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

		// the datapack registries of a new world with this preset
		HolderLookup.Provider patch = decoded.buildPatch(registries);
		Preset patched = patch.lookupOrThrow(RTFRegistries.PRESET).getOrThrow(Preset.KEY).value();
		HolderGetter<Noise> noises = patch.lookupOrThrow(RTFRegistries.NOISE);
		PerformanceConfig config = PerformanceConfig.makeDefault();

		// the preview of the preset screen
		GeneratorContext preview = GeneratorContext.makeUncached(patched, noises, SEED, 4, 0, config.batchCount());
		try (Tile tile = preview.generator.generateZoomed(0, 0, 50, false).join()) {
			String problem = checkCells(tile, "preview");
			if (problem != null) {
				return problem;
			}
		}

		// world tiles with every filter, as chunks generate
		GeneratorContext world = GeneratorContext.makeCached(patched, noises, SEED, config.tileSize(), config.batchCount(), false);
		for (int[] chunk : CHUNKS) {
			Tile tile = world.cache.provideAtChunk(chunk[0], chunk[1]);
			String problem = checkCells(tile, "world tile at chunk " + chunk[0] + ", " + chunk[1]);
			if (problem != null) {
				return problem;
			}
		}
		return null;
	}

	private static String checkCells(Tile tile, String where) {
		// every cell, the tile's border too (filters such as the steepness read the neighbours)
		Cell[] backing = tile.getBacking();
		int total = tile.getBlockSize().total();
		for (int i = 0; i < backing.length; i++) {
			if (!Float.isFinite(backing[i].height)) {
				return where + ": height is " + backing[i].height + " in cell " + i + " of " + backing.length + " (" + describe(backing[i]) + ")";
			}
		}
		for (int i = 0; i < backing.length; i++) {
			String bad = badValue(backing[i]);
			if (bad != null) {
				return where + ": " + bad + " in cell " + i + " of " + backing.length + " (" + describe(backing[i]) + "; " + neighbourHeights(backing, total, i % total, i / total) + ")";
			}
		}
		String[] problem = new String[1];
		tile.iterate((cell, x, z) -> {
			if (problem[0] == null) {
				String bad = badValue(cell);
				if (bad != null) {
					problem[0] = where + ": " + bad + " at " + x + ", " + z;
				}
			}
		});
		return problem[0];
	}

	// the steps a cell goes through, for a failure report: continent, terrain region, terrain, rivers
	private static String describe(Cell cell) {
		return "continent edge " + cell.continentEdge + ", continent distance " + cell.continentDistance + ", terrain " + cell.terrain
			+ ", terrain region edge " + cell.terrainRegionEdge + ", river mask " + cell.riverMask + ", erosion " + cell.erosion + ", biome region " + cell.biomeRegionId;
	}

	// the heights around a cell (the steepness filter reads them), for a failure report
	private static String neighbourHeights(Cell[] backing, int total, int x, int z) {
		StringBuilder heights = new StringBuilder("heights around:");
		for (int dz = -1; dz <= 2; dz++) {
			for (int dx = -1; dx <= 2; dx++) {
				int nx = x + dx, nz = z + dz;
				if (nx >= 0 && nz >= 0 && nx < total && nz * total + nx < backing.length) {
					heights.append(' ').append(backing[nz * total + nx].height);
				}
			}
		}
		return heights.toString();
	}

	private static String badValue(Cell cell) {
		float[] values = { cell.height, cell.erosion, cell.weirdness, cell.temperature, cell.moisture, cell.gradient, cell.sediment, cell.heightErosion, cell.continentEdge, cell.biomeRegionId };
		String[] names = { "height", "erosion", "weirdness", "temperature", "moisture", "gradient", "sediment", "height erosion", "continent edge", "biome region" };
		for (int i = 0; i < values.length; i++) {
			if (!Float.isFinite(values[i])) {
				return names[i] + " is " + values[i];
			}
		}
		if (cell.terrain == null) {
			return "no terrain type";
		}
		return null;
	}
}
