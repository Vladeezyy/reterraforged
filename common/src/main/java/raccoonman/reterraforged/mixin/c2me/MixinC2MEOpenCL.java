package raccoonman.reterraforged.mixin.c2me;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.levelgen.RandomState;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;

// C2ME's OpenCL module (c2me-opts-accel-opencl) compiles each world's density functions for the GPU and only knows the
// vanilla ones. On ReTerraForged's it stops the server unless its allowIncompatibilityFallback is set (off by default,
// a constant read at start). For a world with ReTerraForged functions it falls back as with that setting on: the
// world is generated on the CPU, still multi-threaded by C2ME; other worlds keep the GPU (#19).
// Applied only when the module is loaded (MixinPlugin); C2ME bundles and starts MixinSquared.
@Mixin(value = ChunkMap.class, priority = 1500)
public class MixinC2MEOpenCL {
	@Shadow
	private RandomState randomState;

	@TargetHandler(
		mixin = "com.ishland.c2me.opts.accel.opencl.mixin.MixinThreadedAnvilChunkStorage",
		name = "postInit"
	)
	@ModifyExpressionValue(
		method = "@MixinSquared:Handler",
		at = @At(
			value = "FIELD",
			target = "Lcom/ishland/c2me/opts/accel/opencl/common/Config;allowIncompatibilityFallback:Z",
			remap = false
		),
		require = 0
	)
	private boolean reterraforged$fallBackForReTerraForged(boolean allowFallback) {
		return allowFallback || ((Object) this.randomState instanceof RTFRandomState rtfRandomState && rtfRandomState.hasReTerraForgedFunctions());
	}
}
