package raccoonman.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.level.biome.Climate;
import raccoonman.reterraforged.world.worldgen.biome.SwampHeights;

// 26.3: Climate.Sampler is a record; only the swamps' erosion offset is added to it
@Mixin(Climate.Sampler.class)
class MixinClimateSampler {

	// Highest Swamps: the erosion offset while a swamp is picked again (SwampHeights), the point as it is otherwise
	@ModifyReturnValue(method = "sample", at = @At("RETURN"))
	private Climate.TargetPoint sample(Climate.TargetPoint point) {
		return SwampHeights.offset(point);
	}
}
