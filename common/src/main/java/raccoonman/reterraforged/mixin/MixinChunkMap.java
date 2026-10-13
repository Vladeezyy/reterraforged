package raccoonman.reterraforged.mixin;

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

	// only the callback: the constructor's parameters change between Minecraft versions
	@Inject(
		at = @At("TAIL"),
		method = "<init>"
	)
	public void ChunkMap(CallbackInfo callback) {
		if((Object) this.randomState instanceof RTFRandomState rtfRandomState) {
			rtfRandomState.initialize(this.level);
		}
	}
}
