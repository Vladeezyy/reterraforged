package raccoonman.reterraforged.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
	@Shadow
	@Final
	CreateWorldScreen this$0;

	private MixinCreateWorldGameTab(Component title) {
		super(title);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void init(CallbackInfo callback) {
		RTFCreateWorld.addGameTabWidgets(this.this$0, this.layout);
	}
}
