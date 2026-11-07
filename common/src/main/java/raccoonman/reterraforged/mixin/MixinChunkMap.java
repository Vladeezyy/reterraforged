package raccoonman.reterraforged.mixin;

import net.minecraft.world.level.chunk.ChunkGenerator;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFBiomeSource;
import raccoonman.reterraforged.world.worldgen.biome.SwampHeights;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.RandomState;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;

@Mixin(ChunkMap.class)
public class MixinChunkMap {
	@Shadow
    private RandomState randomState;
	@Shadow
	@Final
	ServerLevel level;
	@Shadow
	protected ChunkGenerator generator() {
		throw new AssertionError();
	}

	// only the callback: the constructor's parameters change between Minecraft versions
	@Inject(
		at = @At("TAIL"),
		method = "<init>"
	)
	public void ChunkMap(CallbackInfo callback) {
		if((Object) this.randomState instanceof RTFRandomState rtfRandomState) {
			rtfRandomState.initialize(this.level);
			// Highest Swamps (#28), off for presets saved before 0.0.29
			if((Object) this.generator().getBiomeSource() instanceof RTFBiomeSource biomeSource) {
				Preset preset = rtfRandomState.preset();
				GeneratorContext context = rtfRandomState.generatorContext();
				int maxHeight = preset != null ? preset.climate().swampMaxHeight : 0;
				boolean lowlands = preset != null && preset.climate().lowlandSwamps > 0 && preset.world().properties.generationFixes;
				boolean basins = preset != null && preset.climate().swampBasins > 0.0F;
				biomeSource.setSwampHeights((maxHeight > 0 || lowlands || basins) && context != null ? new SwampHeights(context, maxHeight, lowlands || basins, basins && !lowlands) : null);
			}
		}
	}
}
