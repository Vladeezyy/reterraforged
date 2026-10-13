package raccoonman.reterraforged.world.worldgen.densityfunction;

import java.util.function.Function;

import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * C2ME wraps density functions with a shallow visitor that does not reach the CellSamplers inside some of ours; this
 * maps them explicitly. Lives outside MixinNoiseChunk: a mixin may only have private static methods, and the
 * non-private one there made every world generation crash ("contains non-private static method").
 */
public final class C2MECompat {
	private C2MECompat() {
	}

	public static DensityFunction repairShallowMapping(DensityFunction function, Function<CellSampler, DensityFunction> mapper) {
		if(!(function instanceof ClampToNearestUnit)
			&& !(function instanceof ConditionalFlatCache)
			&& !(function instanceof LinearSplineFunction)) {
			return function;
		}
		return function.mapAll((child) -> {
			return child instanceof CellSampler sampler ? mapper.apply(sampler) : child;
		});
	}
}
