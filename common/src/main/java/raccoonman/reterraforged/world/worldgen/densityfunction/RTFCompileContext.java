package raccoonman.reterraforged.world.worldgen.densityfunction;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;

/**
 * Since 26.3 density functions are compiled into samplers with the {@link DensityFunction.CompileContext} of their
 * {@link RandomState}. MixinRandomState wraps that context in this one, so the mod's functions reach the world seed
 * and the world's GeneratorContext (both used to be handed out by visiting the noise router in RandomState).
 */
public record RTFCompileContext(DensityFunction.CompileContext delegate, RandomState randomState) implements DensityFunction.CompileContext {

	@Override
	public Noise createNoiseSampler(Holder<NormalNoise> parameters) {
		return this.delegate.createNoiseSampler(parameters);
	}

	@Override
	public RandomSource createRandom(Identifier seed) {
		return this.delegate.createRandom(seed);
	}

	@Deprecated
	@Override
	public RandomSource createEndIslandRandom() {
		return this.delegate.createEndIslandRandom();
	}

	@Nullable
	public static RTFRandomState randomState(DensityFunction.CompileContext context) {
		return context instanceof RTFCompileContext rtfContext ? (RTFRandomState) (Object) rtfContext.randomState() : null;
	}
}
