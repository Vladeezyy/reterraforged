package raccoonman.reterraforged.world.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

public final class NoiseFunction {

	private NoiseFunction() {
	}

	// a 2D noise module of the preset, sampled with the world seed
	public record Marker(Holder<Noise> noise) implements DensityFunction {
		public static final MapCodec<Marker> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Noise.CODEC.fieldOf("noise").forGetter(NoiseFunction.Marker::noise)
		).apply(instance, NoiseFunction.Marker::new));

		@Override
		public DensitySampler compileSampler(CompileContext context) {
			int seed = context instanceof RTFCompileContext rtfContext ? (int) rtfContext.randomState().seed() : 0;
			return new Sampler(this.noise.value(), seed);
		}

		@Override
		public DensityFunction rewriteChildren(DfRewriteRule rule) {
			return this;
		}

		@Override
		public Interval range() {
			Noise noise = this.noise.value();
			return Interval.encapsulating(noise.minValue(), noise.maxValue());
		}

		@Override
		public int domainAxes() {
			return AXIS_X | AXIS_Z;
		}

		@Override
		public MapCodec<Marker> codec() {
			return CODEC;
		}
	}

	public record Sampler(Noise noise, int seed) implements DensitySampler {

		@Override
		public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
			DensitySampler.sampleVolumeNaive(context, outputBuffer, volume, this);
		}

		@Override
		public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
			return this.noise.compute(blockX, blockZ, this.seed);
		}
	}
}
