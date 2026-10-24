package raccoonman.reterraforged.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;

// 26.3: the preset's cells are read by CellSampler's compiled sampler (per chunk volume), only the fluids are left here
@Mixin(NoiseChunk.class)
class MixinNoiseChunk {

	// lava below the preset's lava level, the default fluid below sea level
	@ModifyVariable(
		method = "<init>",
		at = @At("HEAD"),
		ordinal = 0,
		argsOnly = true
	)
	private static Aquifer.FluidPicker modifyFluidPicker(Aquifer.FluidPicker fluidPicker, RandomState randomState, @Nullable Beardifier beardifier, NoiseGeneratorSettings noiseGeneratorSettings) {
		if((Object) randomState instanceof RTFRandomState rtfRandomState) {
			@Nullable
			Preset preset = rtfRandomState.preset();
			if(preset != null && rtfRandomState.generatorContext() != null) {
				WorldSettings.Properties props = preset.world().properties;
				if(noiseGeneratorSettings.seaLevel() != props.seaLevel) {
					return fluidPicker;
				}
				// the noise settings' height is the whole world, depth + height (PresetNoiseGeneratorSettings): compared
				// with the height alone it never matched, the lava level stayed vanilla's -54 whatever the slider (#13)
				if(noiseGeneratorSettings.noiseSettings().minY() != -props.worldDepth || noiseGeneratorSettings.noiseSettings().height() != props.worldDepth + props.worldHeight) {
					return fluidPicker;
				}
				int lavaLevel = props.lavaLevel;
		        Aquifer.FluidStatus lava = new Aquifer.FluidStatus(lavaLevel, Blocks.LAVA.defaultBlockState());
		        int seaLevel = noiseGeneratorSettings.seaLevel();
		        Aquifer.FluidStatus defaultFluid = new Aquifer.FluidStatus(seaLevel, noiseGeneratorSettings.defaultFluid());
		        return (x, y, z) -> {
		            if (y < Math.min(lavaLevel, seaLevel)) {
		                return lava;
		            }
		            return defaultFluid;
		        };
			}
		}
		return fluidPicker;
	}
}
