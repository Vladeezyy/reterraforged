package raccoonman.reterraforged.data.worldgen.preset.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import raccoonman.reterraforged.RTFCommon;

/**
 * The ranges of the preset screen's sliders (PresetLimitTable, generated from the pages by tools/stresstable.py),
 * which every preset read from a file is held to: a world's or a server's datapack, an imported preset. A broken or
 * edited file can hold values no slider gives, and those hung world generation (a climate falloff of 2^31 is a loop
 * of 2^31 steps for every point) or made the terrain NaN. Out of range numbers are clamped to the slider, NaN is the
 * Default preset's value, sizes snap to their step; every changed value is logged.
 */
public final class PresetLimits {

	public record Limit<T extends Number>(String name, T min, T max, int step, Function<Preset, T> getter, BiConsumer<Preset, T> setter) {

		static Limit<Float> ofFloat(String name, float min, float max, Function<Preset, Float> getter, BiConsumer<Preset, Float> setter) {
			return new Limit<>(name, min, max, 0, getter, setter);
		}

		static Limit<Integer> ofInt(String name, int min, int max, int step, Function<Preset, Integer> getter, BiConsumer<Preset, Integer> setter) {
			return new Limit<>(name, min, max, step, getter, setter);
		}

		// the value in range, or null when it already is
		private T fixed(T value, Preset defaults) {
			if (value instanceof Float f) {
				float fixed = Float.isNaN(f) ? (Float) this.getter.apply(defaults) : Math.max((Float) this.min, Math.min((Float) this.max, f));
				return fixed == f ? null : this.cast(fixed);
			}
			int i = (Integer) value;
			int min = (Integer) this.min, max = (Integer) this.max;
			int fixed = Math.max(min, Math.min(max, i));
			if (this.step > 1) {
				fixed = Math.max(min, Math.min(max, Math.round(fixed / (float) this.step) * this.step));
			}
			return fixed == i ? null : this.cast(fixed);
		}

		@SuppressWarnings("unchecked")
		private T cast(Number value) {
			return (T) value;
		}
	}

	private PresetLimits() {
	}

	public static Preset clamp(Preset preset) {
		Preset defaults = Presets.makeRTFDefault();
		List<String> changed = new ArrayList<>();
		for (Limit<?> limit : PresetLimitTable.LIMITS) {
			fix(limit, preset, defaults, changed);
		}
		// the structure page's sliders (spacing 0 .. 1000, separation below the spacing): a spacing of 0 divides by
		// zero in the structure placement, a separation not below the spacing is refused by Minecraft
		preset.structures().entries.forEach((key, entry) -> {
			int spacing = Math.max(1, Math.min(1000, entry.spacing));
			int separation = Math.max(0, Math.min(spacing - 1, entry.separation));
			if (spacing != entry.spacing || separation != entry.separation) {
				changed.add("structure " + key + " spacing " + entry.spacing + " separation " + entry.separation + " -> " + spacing + " " + separation);
				entry.spacing = spacing;
				entry.separation = separation;
			}
		});
		if (!changed.isEmpty()) {
			RTFCommon.LOGGER.warn("[ReTerraForged] preset values outside the preset screen's ranges were changed: {}", changed);
		}
		return preset;
	}

	private static <T extends Number> void fix(Limit<T> limit, Preset preset, Preset defaults, List<String> changed) {
		T value = limit.getter().apply(preset);
		T fixed = limit.fixed(value, defaults);
		if (fixed != null) {
			limit.setter().accept(preset, fixed);
			changed.add(limit.name() + " " + value + " -> " + fixed);
		}
	}
}
