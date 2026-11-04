package raccoonman.reterraforged.world.worldgen.feature.placement.poisson;

import com.mojang.serialization.MapCodec;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

public class DensityNoise implements Noise {
    private final BiomeVariance biome;
    private final Noise variance;

    public DensityNoise(BiomeVariance biome, Noise variance) {
        this.biome = biome;
        this.variance = variance;
    }

    @Override
    public float compute(float x, float y, int seed) {
        float value1 = this.biome.compute(x, y, 0);
        if (value1 > 2F) {
            return value1;
        }

        float value2 = this.variance.compute(x, y, 0);
        if (value1 > 1F) {
            // value1 is 1-2 here: 0.0.6 blended by (value1 - 0.25) / 0.25, 3-7 instead of 0-1, and the spacing of the
            // trees near a climate region's edge grew up to 3.5 times (1.4 was meant), leaving bare strips in forests (#37)
            return NoiseUtil.lerp(value2, value1, value1 - 1.0F);
        }

        return value2;
    }

	@Override
	public float minValue() {
		return 0.0F;
	}

	@Override
	public float maxValue() {
		return 1.0F;
	}

	@Override
	public Noise mapAll(Visitor visitor) {
		return visitor.apply(this);
	}

	@Override
	public MapCodec<DensityNoise> codec() {
		throw new UnsupportedOperationException();
	}
}
