package raccoonman.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.CrashReport;
import net.minecraft.util.thread.BlockableEventLoop;
import raccoonman.reterraforged.concurrent.ThreadPools;

// Since 26.1 Util.onThreadException hands a crash to this method and does not call System.exit, so no shutdown hook runs
@Mixin(BlockableEventLoop.class)
class MixinBlockableEventLoop {

	@Inject(method = "relayDelayCrash", at = @At("HEAD"))
	private static void relayDelayCrash(CrashReport report, CallbackInfo callback) {
		ThreadPools.onCrashRelayed();
	}
}
