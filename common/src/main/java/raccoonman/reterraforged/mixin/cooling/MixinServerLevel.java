package raccoonman.reterraforged.mixin.cooling;

import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.server.level.ServerLevel;
import raccoonman.reterraforged.world.worldgen.AltitudeCooling;

@Mixin(ServerLevel.class)
class MixinServerLevel {

	// all of a level's tick on the server thread (rain and snow, ice, cauldrons, entities, random ticks) cools with
	// its own dimension's scale (AltitudeCooling); no event: none wraps the whole tick on both loaders
	@WrapMethod(method = "tick(Ljava/util/function/BooleanSupplier;)V")
	private void reterraforged$coolingTick(BooleanSupplier hasTimeLeft, Operation<Void> original) {
		float previous = AltitudeCooling.enter(AltitudeCooling.of((ServerLevel) (Object) this));
		try {
			original.call(hasTimeLeft);
		} finally {
			AltitudeCooling.exit(previous);
		}
	}
}
