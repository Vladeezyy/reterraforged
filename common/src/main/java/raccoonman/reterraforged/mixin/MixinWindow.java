package raccoonman.reterraforged.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.Window;

// tools/clienttest.sh: the test client's window does not take the focus from what the person is doing when it opens. The
// test clicks by itself, and the game does not need the focus (pauseOnLostFocus is off in the test's options)
@Mixin(Window.class)
class MixinWindow {

	@Inject(method = "createGlfwWindow", at = @At("HEAD"), require = 0)
	private static void noFocus(CallbackInfoReturnable<Long> callback) {
		if (System.getProperty("rtf.clienttest") != null) {
			GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
			GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
		}
	}
}
