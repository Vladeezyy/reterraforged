package raccoonman.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import raccoonman.reterraforged.client.gui.screen.createworld.RTFCreateWorld;

// a new world gets ReTerraForged with the Default preset (RTFCreateWorld)
@Mixin(CreateWorldScreen.class)
class MixinCreateWorldScreen {

	@Inject(method = "openFresh", at = @At("HEAD"))
	private static void openFresh(Minecraft minecraft, Screen lastScreen, CallbackInfo callback) {
		RTFCreateWorld.markFresh();
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void init(CallbackInfo callback) {
		RTFCreateWorld.onInit((CreateWorldScreen) (Object) this);
	}
}
