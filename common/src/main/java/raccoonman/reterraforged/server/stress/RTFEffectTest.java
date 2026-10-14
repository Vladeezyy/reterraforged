package raccoonman.reterraforged.server.stress;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.concurrent.cache.CacheManager;
import raccoonman.reterraforged.config.PerformanceConfig;
import raccoonman.reterraforged.data.worldgen.preset.settings.ContinentType;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.Presets;
import raccoonman.reterraforged.platform.RegistryUtil;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

/**
 * "rtf stress effects" — with -Drtf.selftest=true (tools/stresstest.sh &lt;loader&gt; effects). Whether every setting of
 * the preset screen reaches the world: each at both ends of its slider (every toggle state, every cycle value), on
 * the Default preset, against Default itself. Compared are the cells of the preview and of a world tile (height,
 * terrain, climate, rivers, biome types...: what the terrain is made from) and every worldgen entry of the preset's
 * datapack apart from the preset (noise settings, density functions, features, biomes, biome modifiers...). A setting
 * that changes neither is listed as without effect: it is either read straight from the preset while chunks generate
 * (lava level, spawn) or does nothing. The table goes to rtf-effects.txt in the server folder.
 */
public final class RTFEffectTest {
	private static final int SEED = 20261013;
	private static final List<Field> CELL_FIELDS = new ArrayList<>();

	static {
		for (Field field : Cell.class.getFields()) {
			if (!Modifier.isStatic(field.getModifiers())) CELL_FIELDS.add(field);
		}
	}

	// what a preset makes: the cells of the preview and of world tiles, the datapack entries
	private record Print(Map<String, Object[]> cells, Map<String, String> data) {
	}

	// settings that only work with another one on: tried again with it when they change nothing on Default
	private static final Map<String, Consumer<Preset>> CONTEXTS = new LinkedHashMap<>();

	static {
		CONTEXTS.put("island().", (p) -> p.island().enableArchipelago = true);
		CONTEXTS.put("fakeWaterBiomes().", (p) -> p.fakeWaterBiomes().enableFakeWaterBiomes = true);
		CONTEXTS.put("world().continent.continentShape", (p) -> p.world().continent.continentType = ContinentType.MULTI);
	}

	// world tiles where the terrain is: the spawn, then from the Default preview the highest land, a river, a coast
	private static int[][] anchors = { { 0, 0 } };

	private RTFEffectTest() {
	}

	public static String run(MinecraftServer server) {
		RegistryAccess registries = server.registries().getAccessForLoading(RegistryLayer.DIMENSIONS);
		long start = System.nanoTime();
		Preset defaults = Presets.makeDefault();
		String defaultJson = json(defaults);
		anchors = anchors(defaults, registries);
		Print base = print(defaults, registries);
		Map<String, Print> contextBases = new LinkedHashMap<>();
		RTFCommon.LOGGER.info("[RTF stress] effects: Default has {} cells and {} datapack entries", base.cells().values().iterator().next().length, base.data().size());
		List<String> report = new ArrayList<>();
		List<String> without = new ArrayList<>();
		int variants = 0;
		for (Setting<?> setting : PresetStressTable.SETTINGS) {
			boolean any = false;
			report.add(setting.name());
			for (Object value : setting.extremes()) {
				Preset preset = Presets.makeDefault();
				setting.set(preset, value);
				if (json(preset).equals(defaultJson)) {
					report.add("    = " + value + ": Default's value");
					continue;
				}
				variants++;
				String effect;
				try {
					effect = compare(base, print(preset, registries));
				} catch (Throwable t) {
					effect = "FAILED " + t;
					RTFCommon.LOGGER.error("[RTF stress] effects: {} = {} failed", setting.name(), value, t);
				}
				// with the setting it depends on switched on
				String context = CONTEXTS.keySet().stream().filter(setting.name()::startsWith).findFirst().orElse(null);
				if (effect.isEmpty() && context != null) {
					Print contextBase = contextBases.computeIfAbsent(context, (c) -> {
						Preset p = Presets.makeDefault();
						CONTEXTS.get(c).accept(p);
						return print(p, registries);
					});
					Preset withContext = Presets.makeDefault();
					CONTEXTS.get(context).accept(withContext);
					setting.set(withContext, value);
					effect = compare(contextBase, print(withContext, registries));
					if (!effect.isEmpty()) effect = "(with its switch on) " + effect;
				}
				any |= !effect.isEmpty();
				report.add("    = " + value + ": " + (effect.isEmpty() ? "no effect" : effect));
			}
			if (!any) without.add(setting.name());
		}
		report.add(0, "");
		report.add(0, without.size() + " settings without effect on the terrain cells or the datapack: " + without);
		try {
			Files.write(Path.of("rtf-effects.txt"), report);
		} catch (IOException e) {
			RTFCommon.LOGGER.error("[RTF stress] effects: could not write rtf-effects.txt", e);
		}
		without.forEach((name) -> RTFCommon.LOGGER.info("[RTF stress] without effect: {}", name));
		String summary = String.format("[RTF stress] %d variants in %d s: %d settings, %d without effect (rtf-effects.txt)", variants, (System.nanoTime() - start) / 1_000_000_000L, PresetStressTable.SETTINGS.size(), without.size());
		RTFCommon.LOGGER.info(summary);
		return summary;
	}

	private static String json(Preset preset) {
		return Preset.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, preset).getOrThrow().toString();
	}

	private static Print print(Preset preset, RegistryAccess registries) {
		try {
			CacheManager.clear();
		} catch (Exception e) {
			RTFCommon.LOGGER.warn("[RTF stress] cache clear failed", e);
		}
		HolderLookup.Provider patch = preset.buildPatch(registries);
		Preset patched = patch.lookupOrThrow(RTFRegistries.PRESET).getOrThrow(Preset.KEY).value();
		HolderGetter<Noise> noises = patch.lookupOrThrow(RTFRegistries.NOISE);
		PerformanceConfig config = PerformanceConfig.makeDefault();
		Map<String, Object[]> cells = new LinkedHashMap<>();
		// the preview of the preset screen (12800 blocks across) and the world tile at the spawn (with the filters)
		GeneratorContext preview = GeneratorContext.makeUncached(patched, noises, SEED, 4, 0, config.batchCount());
		try (Tile tile = preview.generator.generateZoomed(0, 0, 50, false).join()) {
			cells(tile, "preview", cells);
		}
		GeneratorContext world = GeneratorContext.makeCached(patched, noises, SEED, config.tileSize(), config.batchCount(), false);
		for (int i = 0; i < anchors.length; i++) {
			cells(world.cache.provideAtChunk(anchors[i][0], anchors[i][1]), "world" + i, cells);
		}

		// the datapack entries, as the datapack is written (the preset itself left out)
		Map<String, String> data = new LinkedHashMap<>();
		DynamicOps<JsonElement> ops = patch.createSerializationContext(JsonOps.INSTANCE);
		for (RegistryDataLoader.RegistryData<?> registry : RegistryUtil.getDynamicRegistriesWithDimensions()) {
			if (!registry.key().equals(RTFRegistries.PRESET)) data(patch, ops, registry, data);
		}
		return new Print(cells, data);
	}

	private static int[][] anchors(Preset defaults, RegistryAccess registries) {
		HolderLookup.Provider patch = defaults.buildPatch(registries);
		Preset patched = patch.lookupOrThrow(RTFRegistries.PRESET).getOrThrow(Preset.KEY).value();
		GeneratorContext preview = GeneratorContext.makeUncached(patched, patch.lookupOrThrow(RTFRegistries.NOISE), SEED, 4, 0, PerformanceConfig.makeDefault().batchCount());
		Levels levels = new Levels(patched.world().properties.terrainScaler(), patched.world().properties.seaLevel);
		try (Tile tile = preview.generator.generateZoomed(0, 0, 50, false).join()) {
			int size = tile.getBlockSize().size();
			int[] found = { -1, -1, -1 };
			float[] highest = { Float.NEGATIVE_INFINITY };
			tile.iterate((cell, x, z) -> {
				int index = z * size + x;
				if (cell.height > highest[0]) {
					highest[0] = cell.height;
					found[0] = index;
				}
				if (found[1] < 0 && cell.riverMask < 0.5F && cell.height > levels.water) found[1] = index;
				if (found[2] < 0 && cell.terrain == TerrainType.COAST) found[2] = index;
			});
			List<int[]> result = new ArrayList<>();
			result.add(new int[] { 0, 0 });
			for (int index : found) {
				if (index < 0) continue;
				// the preview is 50 blocks a cell around 0, 0
				int blockX = (index % size - size / 2) * 50, blockZ = (index / size - size / 2) * 50;
				result.add(new int[] { blockX >> 4, blockZ >> 4 });
			}
			RTFCommon.LOGGER.info("[RTF stress] effects: world tiles at chunks {}", result.stream().map((a) -> a[0] + "," + a[1]).toList());
			return result.toArray(int[][]::new);
		}
	}

	private static <T> void data(HolderLookup.Provider patch, DynamicOps<JsonElement> ops, RegistryDataLoader.RegistryData<T> registry, Map<String, String> data) {
		patch.lookup(registry.key()).ifPresent((lookup) -> lookup.listElements().forEach((holder) -> {
			String json = registry.elementCodec().encodeStart(ops, holder.value()).mapOrElse(JsonElement::toString, (error) -> "error " + error.message());
			data.put(holder.key().toString(), json);
		}));
	}

	private static void cells(Tile tile, String where, Map<String, Object[]> cells) {
		Cell[] backing = tile.getBacking();
		for (Field field : CELL_FIELDS) {
			Object[] values = new Object[backing.length];
			for (int i = 0; i < backing.length; i++) {
				try {
					// numbers as they are, anything else by name (terrain types are made for each generator)
					Object value = field.get(backing[i]);
					values[i] = value instanceof Number || value instanceof Boolean || value == null ? value : String.valueOf(value);
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
			}
			cells.put(where + " " + field.getName(), values);
		}
	}

	// what differs: cell fields (share of cells, mean change of numbers) and datapack entries
	private static String compare(Print base, Print other) {
		List<String> parts = new ArrayList<>();
		for (Map.Entry<String, Object[]> entry : base.cells().entrySet()) {
			Object[] a = entry.getValue(), b = other.cells().get(entry.getKey());
			int differ = 0;
			double sum = 0;
			boolean numbers = true;
			for (int i = 0; i < a.length; i++) {
				if (!Objects.equals(a[i], b[i])) {
					differ++;
					if (a[i] instanceof Number x && b[i] instanceof Number y && Double.isFinite(x.doubleValue()) && Double.isFinite(y.doubleValue())) {
						sum += Math.abs(x.doubleValue() - y.doubleValue());
					} else {
						numbers = false;
					}
				}
			}
			if (differ > 0) {
				int share = (int) Math.ceil(100.0 * differ / a.length);
				parts.add(numbers ? String.format(Locale.ROOT, "%s %d%% (%.3g)", entry.getKey(), share, sum / differ) : String.format(Locale.ROOT, "%s %d%%", entry.getKey(), share));
			}
		}
		Set<String> changed = new TreeSet<>();
		for (String key : base.data().keySet()) {
			if (!Objects.equals(base.data().get(key), other.data().get(key))) changed.add(shortKey(key));
		}
		for (String key : other.data().keySet()) {
			if (!base.data().containsKey(key)) changed.add("+" + shortKey(key));
		}
		if (!changed.isEmpty()) parts.add("data " + changed);
		return String.join(", ", parts);
	}

	// "ResourceKey[minecraft:worldgen/noise_settings / minecraft:overworld]" -> "noise_settings minecraft:overworld"
	private static String shortKey(String key) {
		String text = key.replace("ResourceKey[", "").replace("]", "");
		int slash = text.indexOf(" / ");
		String registry = slash >= 0 ? text.substring(0, slash) : text;
		registry = registry.substring(registry.lastIndexOf('/') + 1);
		return registry + " " + (slash >= 0 ? text.substring(slash + 3) : "");
	}
}
