package raccoonman.reterraforged.mixin.terrablender;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Climate.TargetPoint;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import raccoonman.reterraforged.world.worldgen.terrablender.TBClimateSampler;
import raccoonman.reterraforged.world.worldgen.terrablender.TBTargetPoint;

// the preset's biome region goes with every climate sample, for TerraBlender's region choice (MixinParameterList)
@Mixin(Climate.Sampler.class)
@Implements(@Interface(iface = TBClimateSampler.class, prefix = "reterraforged$TBClimateSampler$"))
class MixinClimateSampler {
	@Nullable
	private DensitySampler.Bound uniqueness;
	
	@Inject(
		at = @At("RETURN"), 
		method = "sample"
	)
	public void sample(int quartX, int quartY, int quartZ, CallbackInfoReturnable<TargetPoint> callback) {
		if(this.uniqueness != null && (Object) callback.getReturnValue() instanceof TBTargetPoint tbTargetPoint) {
			tbTargetPoint.setUniqueness(this.uniqueness.sampleValue(QuartPos.toBlock(quartX), QuartPos.toBlock(quartY), QuartPos.toBlock(quartZ)));
		}
	}
	
	public void reterraforged$TBClimateSampler$setUniqueness(DensitySampler.Bound uniqueness) {
		this.uniqueness = uniqueness;
	}
	
	public DensitySampler.Bound reterraforged$TBClimateSampler$getUniqueness() {
		return this.uniqueness;
	}
}
