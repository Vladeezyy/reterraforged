package raccoonman.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import raccoonman.reterraforged.world.worldgen.AltitudeCooling;

@Mixin(Biome.class)
class MixinBiome {

	// rain or snow, frozen water and snow layers, on both sides: the height above 80 scaled to the vanilla world height
	// in a dimension whose preset asks for it (AltitudeCooling). Before the per thread cache of temperatures, not in
	// getHeightAdjustedTemperature (its only caller): the cache is keyed by the position alone, and with the scaled
	// position as the key an entry stays right for every dimension
	@ModifyVariable(method = "getTemperature", at = @At("HEAD"), argsOnly = true)
	private BlockPos scaleCoolingHeight(BlockPos pos) {
		return AltitudeCooling.adjust(pos);
	}
}
