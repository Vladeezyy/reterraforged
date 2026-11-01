package raccoonman.reterraforged.world.worldgen.noise.module;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;

final class Invert implements Noise {
	public static final MapCodec<Invert> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Noise.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(Invert::input)
	).apply(instance, Invert::new));

	private final Noise input;
	// the input's range: asked on every sample before; taken on the first sample, when the noises it refers to are bound
	private Range inputRange;

	private record Range(float min, float max) {
	}

	Invert(Noise input) {
		this.input = input;
	}

	public Noise input() {
		return this.input;
	}
	
	@Override
	public float compute(float x, float z, int seed) {
		Range range = this.inputRange;
		if (range == null) {
			this.inputRange = range = new Range(this.input.minValue(), this.input.maxValue());
		}
		float min = range.min;
		float max = range.max;
		return max - NoiseUtil.clamp(this.input.compute(x, z, seed), min, max);
	}

	@Override
	public float minValue() {
		return this.input.minValue();
	}

	@Override
	public float maxValue() {
		return this.input.maxValue();
	}

	@Override
	public Noise mapAll(Visitor visitor) {
		return visitor.apply(new Invert(this.input.mapAll(visitor)));
	}

	@Override
	public MapCodec<Invert> codec() {
		return CODEC;
	}

	// as the record it was: equal when its input is
	@Override
	public boolean equals(Object o) {
		return o instanceof Invert other && this.input.equals(other.input);
	}

	@Override
	public int hashCode() {
		return this.input.hashCode();
	}

	@Override
	public String toString() {
		return "Invert[input=" + this.input + "]";
	}
}
