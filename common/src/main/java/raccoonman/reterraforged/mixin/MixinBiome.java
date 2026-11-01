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
	// when the preset asks for it (AltitudeCooling)
	@ModifyVariable(method = "getHeightAdjustedTemperature", at = @At("HEAD"), argsOnly = true)
	private BlockPos scaleCoolingHeight(BlockPos pos) {
		return AltitudeCooling.adjust(pos);
	}
}
