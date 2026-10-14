package raccoonman.reterraforged.server.stress;

import java.util.List;
import java.util.function.BiConsumer;

import net.minecraft.util.RandomSource;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;

/**
 * One setting of the preset screen: its name (the field path on the preset), the values the stress test tries (the
 * ends of the slider, both toggle states, every cycle value) and how to set it on a preset.
 */
record Setting<T>(String name, List<T> extremes, List<T> abuse, Random<T> random, BiConsumer<Preset, T> setter) {
	// what no slider gives, but a broken or edited preset file (a datapack, an imported preset) can hold
	private static final List<Float> FLOAT_ABUSE = List.of(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1.0E9F, -1.0F, 0.0F, 1.0E9F);
	private static final List<Integer> INT_ABUSE = List.of(Integer.MIN_VALUE, -1_000_000, -1, 0, 1_000_000, Integer.MAX_VALUE);

	interface Random<T> {
		T next(RandomSource random);
	}

	static Setting<Float> ofFloat(String name, float min, float max, BiConsumer<Preset, Float> setter) {
		return new Setting<>(name, List.of(min, max), FLOAT_ABUSE, (r) -> min + r.nextFloat() * (max - min), setter);
	}

	static Setting<Integer> ofInt(String name, int min, int max, BiConsumer<Preset, Integer> setter) {
		return new Setting<>(name, List.of(min, max), INT_ABUSE, (r) -> min + r.nextInt(max - min + 1), setter);
	}

	static Setting<Boolean> ofBoolean(String name, BiConsumer<Preset, Boolean> setter) {
		return new Setting<>(name, List.of(false, true), List.of(), RandomSource::nextBoolean, setter);
	}

	// the random seed buttons take any int
	static Setting<Integer> ofSeed(String name, BiConsumer<Preset, Integer> setter) {
		return new Setting<>(name, List.of(Integer.MIN_VALUE, -1, 0, Integer.MAX_VALUE), List.of(), RandomSource::nextInt, setter);
	}

	static <E> Setting<E> ofEnum(String name, E[] values, BiConsumer<Preset, E> setter) {
		return ofEnum(name, List.of(values), setter);
	}

	static <E> Setting<E> ofEnum(String name, List<E> values, BiConsumer<Preset, E> setter) {
		return new Setting<>(name, List.copyOf(values), List.of(), (r) -> values.get(r.nextInt(values.size())), setter);
	}

	// the slider snaps to multiples of the step (world height and depth: 16)
	Setting<T> step(int step) {
		Random<T> random = this.random;
		return new Setting<>(this.name, this.extremes, this.abuse, (r) -> {
			T value = random.next(r);
			@SuppressWarnings("unchecked")
			T snapped = value instanceof Integer i ? (T) Integer.valueOf(Math.round(i / (float) step) * step) : value;
			return snapped;
		}, this.setter);
	}

	void set(Preset preset, Object value) {
		@SuppressWarnings("unchecked")
		T typed = (T) value;
		this.setter.accept(preset, typed);
	}

	T min() {
		return this.extremes.get(0);
	}

	T max() {
		return this.extremes.get(this.extremes.size() - 1);
	}
}
