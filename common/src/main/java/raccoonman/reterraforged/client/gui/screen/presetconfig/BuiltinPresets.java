package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.util.List;
import java.util.function.Supplier;

import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.Presets;

/**
 * The presets that come with the mod, in the order of the list. The id is the preset's own, whatever language its name
 * is shown in: the screen keeps its edits under it (a player's file of the same shown name is another preset).
 */
public final class BuiltinPresets {
	// the four presets of the original TerraForged share one description
	public static final String LEGACY_DESCRIPTION = RTFCommon.MOD_ID + ".gui.preset.legacy.description";

	public static final List<Builtin> ALL = List.of(
		modern("default", RTFTranslationKeys.GUI_DEFAULT_PRESET_NAME, Presets::makeDefault),
		modern("archipelago", RTFTranslationKeys.GUI_ARCHIPELAGO_PRESET_NAME, Presets::makeArchipelago),
		modern("wetlands", RTFTranslationKeys.GUI_WETLANDS_PRESET_NAME, Presets::makeWetlands),
		modern("toweringPeaks", RTFTranslationKeys.GUI_TOWERING_PEAKS_PRESET_NAME, Presets::makeToweringPeaks),
		modern("endlessContinent", RTFTranslationKeys.GUI_ENDLESS_CONTINENT_PRESET_NAME, Presets::makeEndlessContinent),
		modern("riverValleys", RTFTranslationKeys.GUI_RIVER_VALLEYS_PRESET_NAME, Presets::makeRiverValleys),
		modern("lushWilds", RTFTranslationKeys.GUI_LUSH_WILDS_PRESET_NAME, Presets::makeLushWilds),
		modern("rollingPlains", RTFTranslationKeys.GUI_ROLLING_PLAINS_PRESET_NAME, Presets::makeRollingPlains),
		modern("ruggedHighlands", RTFTranslationKeys.GUI_RUGGED_HIGHLANDS_PRESET_NAME, Presets::makeRuggedHighlands),
		legacy("beautiful", RTFTranslationKeys.GUI_BEAUTIFUL_PRESET_NAME, Presets::makeLegacyBeautiful),
		legacy("hugeBiomes", RTFTranslationKeys.GUI_HUGE_BIOMES_PRESET_NAME, Presets::makeLegacyHugeBiomes),
		legacy("lite", RTFTranslationKeys.GUI_LITE_PRESET_NAME, Presets::makeLegacyLite),
		legacy("vanillaish", RTFTranslationKeys.GUI_VANILLAISH_PRESET_NAME, Presets::makeLegacyVanillaish)
	);

	private BuiltinPresets() {
	}

	/** nameKey and descriptionKey are translation keys */
	public record Builtin(String id, String nameKey, String descriptionKey, Supplier<Preset> factory) {
	}

	// reterraforged.gui.preset.<id>.description, next to the name's reterraforged.gui.preset.<id>.name
	public static String descriptionKey(String id) {
		return RTFCommon.MOD_ID + ".gui.preset." + id + ".description";
	}

	private static Builtin modern(String id, String nameKey, Supplier<Preset> factory) {
		return new Builtin(id, nameKey, descriptionKey(id), factory);
	}

	private static Builtin legacy(String id, String nameKey, Supplier<Preset> factory) {
		return new Builtin(id, nameKey, LEGACY_DESCRIPTION, factory);
	}
}
