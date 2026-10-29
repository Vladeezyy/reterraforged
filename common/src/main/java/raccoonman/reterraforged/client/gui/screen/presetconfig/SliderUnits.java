package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;

// the unit of a slider's value by its text key: "64 blocks", "Y 63", "x1.50", "25 %" instead of the bare number
final class SliderUnits {
	private static final Map<String, Unit> UNITS = new HashMap<>();

	static {
		for (String key : new String[] { RTFTranslationKeys.GUI_SLIDER_CONTINENT_SCALE, RTFTranslationKeys.GUI_SLIDER_OCEAN_DEPTH, RTFTranslationKeys.GUI_SLIDER_WORLD_DEPTH,
				RTFTranslationKeys.GUI_SLIDER_BIOME_SIZE, RTFTranslationKeys.GUI_SLIDER_LAKE_DEPTH, RTFTranslationKeys.GUI_SLIDER_LAKE_SIZE_MIN, RTFTranslationKeys.GUI_SLIDER_LAKE_SIZE_MAX,
				RTFTranslationKeys.GUI_SLIDER_LAKE_MIN_BANK_HEIGHT, RTFTranslationKeys.GUI_SLIDER_LAKE_MAX_BANK_HEIGHT, RTFTranslationKeys.GUI_SLIDER_RIVER_BANK_WIDTH,
				RTFTranslationKeys.GUI_SLIDER_RIVER_BED_DEPTH, RTFTranslationKeys.GUI_SLIDER_RIVER_BED_WIDTH, RTFTranslationKeys.GUI_SLIDER_RIVER_MIN_BANK_HEIGHT,
				RTFTranslationKeys.GUI_SLIDER_RIVER_MAX_BANK_HEIGHT, RTFTranslationKeys.GUI_SLIDER_ISLAND_SIZE, RTFTranslationKeys.GUI_SLIDER_DIRT_VARIANCE,
				RTFTranslationKeys.GUI_SLIDER_ROCK_VARIANCE, RTFTranslationKeys.GUI_SLIDER_SNOW_LINE_VARIATION, RTFTranslationKeys.GUI_SLIDER_SNOW_LINE_DEPTH_RANGE }) {
			UNITS.put(key, Unit.BLOCKS);
		}
		for (String key : new String[] { RTFTranslationKeys.GUI_SLIDER_SEA_LEVEL, RTFTranslationKeys.GUI_SLIDER_LAVA_LEVEL, RTFTranslationKeys.GUI_SLIDER_WORLD_HEIGHT,
				RTFTranslationKeys.GUI_SLIDER_DIRT_MIN, RTFTranslationKeys.GUI_SLIDER_ROCK_MIN, RTFTranslationKeys.GUI_SLIDER_SNOW_LINE_HEIGHT }) {
			UNITS.put(key, Unit.Y);
		}
		for (String key : new String[] { RTFTranslationKeys.GUI_SLIDER_GLOBAL_HORIZONTAL_SCALE, RTFTranslationKeys.GUI_SLIDER_GLOBAL_VERTICAL_SCALE,
				RTFTranslationKeys.GUI_SLIDER_TERRAIN_BASE_SCALE, RTFTranslationKeys.GUI_SLIDER_TERRAIN_VERTICAL_SCALE, RTFTranslationKeys.GUI_SLIDER_TERRAIN_HORIZONTAL_SCALE,
				RTFTranslationKeys.GUI_SLIDER_ISLAND_HORIZONTAL_SCALE, RTFTranslationKeys.GUI_SLIDER_ISLAND_VERTICAL_SCALE, RTFTranslationKeys.GUI_SLIDER_ISLAND_BASE_SCALE }) {
			UNITS.put(key, Unit.SCALE);
		}
		for (String key : new String[] { RTFTranslationKeys.GUI_SLIDER_CAVE_CARVER_PROBABILITY, RTFTranslationKeys.GUI_SLIDER_DEEP_CAVE_CARVER_PROBABILITY,
				RTFTranslationKeys.GUI_SLIDER_RAVINE_CARVER_PROBABILITY, RTFTranslationKeys.GUI_SLIDER_CHEESE_CAVE_PROBABILITY, RTFTranslationKeys.GUI_SLIDER_SPAGHETTI_CAVE_PROBABILITY,
				RTFTranslationKeys.GUI_SLIDER_NOODLE_CAVE_PROBABILITY, RTFTranslationKeys.GUI_SLIDER_ENTRANCE_CAVE_PROBABILITY, RTFTranslationKeys.GUI_SLIDER_LAKE_CHANCE,
				RTFTranslationKeys.GUI_SLIDER_ISLAND_MOUNTAIN_CHANCE, RTFTranslationKeys.GUI_SLIDER_ISLAND_VOLCANO_CHANCE, RTFTranslationKeys.GUI_SLIDER_ISLAND_BEACH_COVERAGE }) {
			UNITS.put(key, Unit.PERCENT);
		}
		for (String key : new String[] { RTFTranslationKeys.GUI_SLIDER_SNOW_LINE_DEPTH, RTFTranslationKeys.GUI_SLIDER_SNOW_LINE_PEAK_DEPTH }) {
			UNITS.put(key, Unit.SNOW_LAYERS);
		}
	}

	private SliderUnits() {
	}

	@Nullable
	static Function<String, Component> valueText(String sliderKey) {
		Unit unit = UNITS.get(sliderKey);
		return unit != null ? unit::text : null;
	}

	private enum Unit {
		BLOCKS(RTFTranslationKeys.GUI_UNIT_BLOCKS),
		Y(RTFTranslationKeys.GUI_UNIT_Y),
		SCALE(RTFTranslationKeys.GUI_UNIT_SCALE),
		PERCENT(RTFTranslationKeys.GUI_UNIT_PERCENT) {
			@Override
			String number(String shown) {
				return String.valueOf(Math.round(Double.parseDouble(shown.replace(',', '.')) * 100.0));
			}
		},
		// snow layers of the 8 of a full block
		SNOW_LAYERS(RTFTranslationKeys.GUI_UNIT_SNOW_LAYERS);

		private final String key;

		Unit(String key) {
			this.key = key;
		}

		// the number as the slider shows it ("0.250": three decimals for the float sliders)
		String number(String shown) {
			if (shown.indexOf('.') < 0 && shown.indexOf(',') < 0) {
				return shown;
			}
			return String.format(Locale.ROOT, "%.2f", Double.parseDouble(shown.replace(',', '.')));
		}

		Component text(String shown) {
			return Component.translatable(this.key, this.number(shown));
		}
	}
}
