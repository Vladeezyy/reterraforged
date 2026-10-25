package raccoonman.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import raccoonman.reterraforged.client.gui.screen.createworld.RTFCreateWorld;

// a new world gets ReTerraForged with the Default preset (RTFCreateWorld)
@Mixin(CreateWorldScreen.class)
class MixinCreateWorldScreen {

	// every openFresh: its parameters differ between versions (Screen up to 1.21.8, Runnable after) and some have two
	@Inject(method = "openFresh*", at = @At("HEAD"))
	private static void openFresh(CallbackInfo callback) {
		RTFCreateWorld.markFresh();
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void init(CallbackInfo callback) {
		RTFCreateWorld.onInit((CreateWorldScreen) (Object) this);
	}
}
