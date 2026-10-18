package raccoonman.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import raccoonman.reterraforged.compat.SeedGenCompat;

// The default implementation only logs a warning (proto chunks override it); see SeedGenCompat
@Mixin(ChunkAccess.class)
public class MixinChunkAccess {

	// markPosForPostprocessing up to 26.1, markPosForPostProcessing from 26.2
	@Inject(method = { "markPosForPostprocessing", "markPosForPostProcessing" }, at = @At("HEAD"), cancellable = true, require = 1)
	private void reterraforged$seedGenWarning(BlockPos pos, CallbackInfo callback) {
		if (SeedGenCompat.dropPostProcessingWarning()) {
			callback.cancel();
		}
	}
}
