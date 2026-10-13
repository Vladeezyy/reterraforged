package raccoonman.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import raccoonman.reterraforged.client.RTFClientTest;

@Mixin(Minecraft.class)
class MixinMinecraft {

	@Inject(method = "tick", at = @At("TAIL"))
	private void tick(CallbackInfo callback) {
		RTFClientTest.tick((Minecraft) (Object) this);
	}
}
