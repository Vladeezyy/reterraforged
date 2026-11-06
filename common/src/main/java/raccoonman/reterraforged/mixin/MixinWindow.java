package raccoonman.reterraforged.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.Window;

// tools/clienttest.sh: the test client's window does not take the focus from what the person is doing when it opens. The
// test clicks by itself, and the game does not need the focus (pauseOnLostFocus is off in the test's options)
@Mixin(Window.class)
class MixinWindow {

	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwDefaultWindowHints()V", shift = At.Shift.AFTER, remap = false), require = 0)
	private void noFocus(CallbackInfo callback) {
		if (System.getProperty("rtf.clienttest") != null) {
			GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
			GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
		}
	}
}
