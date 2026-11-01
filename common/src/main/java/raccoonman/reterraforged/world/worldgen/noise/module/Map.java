package raccoonman.reterraforged.world.worldgen.noise.module;

import java.util.Objects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

final class Map implements Noise {
	public static final MapCodec<Map> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Noise.HOLDER_HELPER_CODEC.fieldOf("alpha").forGetter(Map::alpha),
		Noise.HOLDER_HELPER_CODEC.fieldOf("from").forGetter(Map::from),
		Noise.HOLDER_HELPER_CODEC.fieldOf("to").forGetter(Map::to)
	).apply(instance, Map::new));

	private final Noise alpha;
	private final Noise from;
	private final Noise to;
	// the alpha's range: asked on every sample before, a walk down its whole tree each time (Add, Mul... sum their
	// inputs' ranges). Taken on the first sample, when the noises it refers to are bound (not when it is decoded).
	private AlphaRange alphaRange;

	private record AlphaRange(float min, float max) {
	}

	Map(Noise alpha, Noise from, Noise to) {
		this.alpha = alpha;
		this.from = from;
		this.to = to;
	}

	public Noise alpha() {
		return this.alpha;
	}

	public Noise from() {
		return this.from;
	}

	public Noise to() {
		return this.to;
	}

	@Override
	public float compute(float x, float z, int seed) {
		AlphaRange range = this.alphaRange;
		if (range == null) {
			this.alphaRange = range = new AlphaRange(this.alpha.minValue(), this.alpha.maxValue());
		}
		float alphaMin = range.min;
		float alphaMax = range.max;
		
        float value = this.alpha.compute(x, z, seed);
		float alpha = (value - alphaMin) / (alphaMax - alphaMin);
        float min = this.from.compute(x, z, seed);
        float max = this.to.compute(x, z, seed);
        return min + alpha * (max - min);
	}

	@Override
	public float minValue() {
		return this.from.minValue();
	}

	@Override
	public float maxValue() {
		return this.to.maxValue();
	}

	@Override
	public Noise mapAll(Visitor visitor) {
		return visitor.apply(new Map(this.alpha.mapAll(visitor), this.from.mapAll(visitor), this.to.mapAll(visitor)));
	}

	@Override
	public MapCodec<Map> codec() {
		return CODEC;
	}

	// as the record it was: equal when its inputs are
	@Override
	public boolean equals(Object o) {
		return o instanceof Map other && this.alpha.equals(other.alpha) && this.from.equals(other.from) && this.to.equals(other.to);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.alpha, this.from, this.to);
	}

	@Override
	public String toString() {
		return "Map[alpha=" + this.alpha + ", from=" + this.from + ", to=" + this.to + "]";
	}
}
