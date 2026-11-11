package raccoonman.reterraforged.mixin.terrablender;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import raccoonman.reterraforged.world.worldgen.terrablender.TBClimateSampler;
import raccoonman.reterraforged.world.worldgen.terrablender.TBTargetPoint;

@Mixin(Climate.Sampler.class)
@Implements(@Interface(iface = TBClimateSampler.class, prefix = "reterraforged$TBClimateSampler$"))
class MixinClimateSampler {
	@Unique
	@Nullable
	private DensityFunction uniqueness;

	// the context is made again from the arguments, as sample makes it: a capture of sample's locals failed the class
	// load when another mod changed them. Applied after the general MixinClimateSampler (listed after it, same order),
	// so the point it gets is the one Highest Swamps may have replaced (SwampHeights.offset), as with the @Inject before
	@ModifyReturnValue(
		at = @At("RETURN"),
		method = "sample"
	)
	private Climate.TargetPoint sampleUniqueness(Climate.TargetPoint point, @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 1) int y, @Local(argsOnly = true, ordinal = 2) int z) {
		if(this.uniqueness != null && (Object) point instanceof TBTargetPoint tbTargetPoint) {
			tbTargetPoint.setUniqueness(this.uniqueness.compute(new DensityFunction.SinglePointContext(QuartPos.toBlock(x), QuartPos.toBlock(y), QuartPos.toBlock(z))));
		}
		return point;
	}
	
	public void reterraforged$TBClimateSampler$setUniqueness(DensityFunction uniqueness) {
		this.uniqueness = uniqueness;
	}
	
	public DensityFunction reterraforged$TBClimateSampler$getUniqueness() {
		return this.uniqueness;
	}
}
