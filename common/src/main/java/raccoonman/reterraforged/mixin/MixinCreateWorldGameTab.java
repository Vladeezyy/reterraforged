package raccoonman.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import raccoonman.reterraforged.client.gui.screen.createworld.RTFCreateWorld;

// the Game tab of the create world screen: ReTerraForged or vanilla generation, and the settings button
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$GameTab")
abstract class MixinCreateWorldGameTab extends GridLayoutTab {

	private MixinCreateWorldGameTab(Component title) {
		super(title);
	}

	// the screen comes from the constructor's argument: javac leaves out the this$0 field of an inner class that only
	// uses its outer instance in the constructor (26.3, Java 25)
	@Inject(method = "<init>", at = @At("RETURN"))
	private void init(CreateWorldScreen screen, CallbackInfo callback) {
		RTFCreateWorld.addGameTabWidgets(screen, this.layout);
	}
}
