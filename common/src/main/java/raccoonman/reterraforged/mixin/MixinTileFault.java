package raccoonman.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import raccoonman.reterraforged.server.RTFFaultTest;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;

// tools/servertest.sh FAULT=1 (RTFFaultTest): a tile's generation thread fails on purpose. Applied only with -Drtf.faulttest
// (MixinPlugin): the generation of a normal game does not have this call at all
@Mixin(value = Tile.class, remap = false)
class MixinTileFault {

	@Inject(method = "getChunkWriter", at = @At("HEAD"))
	private void fail(int chunkX, int chunkZ, CallbackInfoReturnable<Tile.Chunk> callback) {
		RTFFaultTest.onTileWork((Tile) (Object) this);
	}
}
