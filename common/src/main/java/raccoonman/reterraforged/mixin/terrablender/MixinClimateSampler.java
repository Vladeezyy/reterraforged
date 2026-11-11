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
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import raccoonman.reterraforged.world.worldgen.terrablender.TBClimateSampler;
import raccoonman.reterraforged.world.worldgen.terrablender.TBTargetPoint;

// the preset's biome region goes with every climate sample, for TerraBlender's region choice (MixinParameterList)
@Mixin(Climate.Sampler.class)
@Implements(@Interface(iface = TBClimateSampler.class, prefix = "reterraforged$TBClimateSampler$"))
class MixinClimateSampler {
	@Unique
	@Nullable
	private DensitySampler.Bound uniqueness;

	// a @ModifyReturnValue, not an @Inject that reads the callback: applied after the general MixinClimateSampler (listed
	// after it, same order), so the point it gets is the one Highest Swamps may have replaced (SwampHeights.offset)
	@ModifyReturnValue(
		at = @At("RETURN"),
		method = "sample"
	)
	private Climate.TargetPoint sampleUniqueness(Climate.TargetPoint point, @Local(argsOnly = true, ordinal = 0) int quartX, @Local(argsOnly = true, ordinal = 1) int quartY, @Local(argsOnly = true, ordinal = 2) int quartZ) {
		if(this.uniqueness != null && (Object) point instanceof TBTargetPoint tbTargetPoint) {
			tbTargetPoint.setUniqueness(this.uniqueness.sampleValue(QuartPos.toBlock(quartX), QuartPos.toBlock(quartY), QuartPos.toBlock(quartZ)));
		}
		return point;
	}
	
	public void reterraforged$TBClimateSampler$setUniqueness(DensitySampler.Bound uniqueness) {
		this.uniqueness = uniqueness;
	}
	
	public DensitySampler.Bound reterraforged$TBClimateSampler$getUniqueness() {
		return this.uniqueness;
	}
}
