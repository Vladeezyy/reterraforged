package raccoonman.reterraforged.world.worldgen.noise.module;

import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.function.Interpolation;

final class Blend implements Noise {
	public static final MapCodec<Blend> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Noise.HOLDER_HELPER_CODEC.fieldOf("alpha").forGetter(Blend::alpha),
		Noise.HOLDER_HELPER_CODEC.fieldOf("lower").forGetter(Blend::lower),
		Noise.HOLDER_HELPER_CODEC.fieldOf("upper").forGetter(Blend::upper),
		Codec.FLOAT.fieldOf("mid").forGetter(Blend::mid),
		Codec.FLOAT.fieldOf("range").forGetter(Blend::range),
		Interpolation.CODEC.fieldOf("interpolation").forGetter(Blend::interpolation)
	).apply(instance, Blend::new));

	private final Noise alpha;
	private final Noise lower;
	private final Noise upper;
	private final float mid;
	private final float range;
	private final Interpolation interpolation;
	// where the blend starts and ends in the alpha's range: worked out on every sample before (five walks down the
	// alpha's tree); taken on the first sample, when the noises it refers to are bound
	private Bounds bounds;

	private record Bounds(float lower, float upper, float range) {
	}

	Blend(Noise alpha, Noise lower, Noise upper, float mid, float range, Interpolation interpolation) {
		this.alpha = alpha;
		this.lower = lower;
		this.upper = upper;
		this.mid = mid;
		this.range = range;
		this.interpolation = interpolation;
	}

	public Noise alpha() {
		return this.alpha;
	}

	public Noise lower() {
		return this.lower;
	}

	public Noise upper() {
		return this.upper;
	}

	public float mid() {
		return this.mid;
	}

	public float range() {
		return this.range;
	}

	public Interpolation interpolation() {
		return this.interpolation;
	}
	
	@Override
	public float minValue() {
		return Math.min(this.lower.minValue(), this.upper.minValue());
	}

	@Override
	public float maxValue() {
		return Math.max(this.lower.maxValue(), this.upper.maxValue());
	}
	
	@Override
	public float compute(float x, float z, int seed) {
        Bounds bounds = this.bounds;
        if (bounds == null) {
            float mid = this.alpha.minValue() + (this.alpha.maxValue() - this.alpha.minValue()) * this.mid;
            float blendLower = Math.max(this.alpha.minValue(), mid - this.range / 2.0F);
            float blendUpper = Math.min(this.alpha.maxValue(), mid + this.range / 2.0F);
            this.bounds = bounds = new Bounds(blendLower, blendUpper, blendUpper - blendLower);
        }
        float blendLower = bounds.lower;
        float blendUpper = bounds.upper;
        float blendRange = bounds.range;
		float alpha = this.alpha.compute(x, z, seed);
        if (alpha < blendLower) {
            return this.lower.compute(x, z, seed);
        }
        if (alpha > blendUpper) {
            return this.upper.compute(x, z, seed);
        }
        return NoiseUtil.lerp(this.lower.compute(x, z, seed), this.upper.compute(x, z, seed), this.interpolation.apply((alpha - blendLower) / blendRange));
	}

	@Override
	public Noise mapAll(Visitor visitor) {
		return visitor.apply(new Blend(this.alpha.mapAll(visitor), this.lower.mapAll(visitor), this.upper.mapAll(visitor), this.mid, this.range, this.interpolation));
	}

	@Override
	public MapCodec<Blend> codec() {
		return CODEC;
	}

	// as the record it was: equal when its inputs and settings are
	@Override
	public boolean equals(Object o) {
		return o instanceof Blend other && this.alpha.equals(other.alpha) && this.lower.equals(other.lower) && this.upper.equals(other.upper)
			&& Float.compare(this.mid, other.mid) == 0 && Float.compare(this.range, other.range) == 0 && this.interpolation.equals(other.interpolation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.alpha, this.lower, this.upper, this.mid, this.range, this.interpolation);
	}

	@Override
	public String toString() {
		return "Blend[alpha=" + this.alpha + ", lower=" + this.lower + ", upper=" + this.upper + ", mid=" + this.mid + ", range=" + this.range + ", interpolation=" + this.interpolation + "]";
	}
}
