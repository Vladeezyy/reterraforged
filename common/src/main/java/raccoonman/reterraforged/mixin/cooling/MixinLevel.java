package raccoonman.reterraforged.mixin.cooling;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import raccoonman.reterraforged.world.worldgen.AltitudeCooling;

@Mixin(Level.class)
class MixinLevel {

	// rain on an entity outside its level's tick (a player's movement, commands): its own dimension's scale
	// (AltitudeCooling). A client level keeps the scale of the client's thread
	@WrapMethod(method = "isRainingAt(Lnet/minecraft/core/BlockPos;)Z")
	private boolean reterraforged$coolingRain(BlockPos pos, Operation<Boolean> original) {
		if (!((Object) this instanceof ServerLevel level)) {
			return original.call(pos);
		}
		float previous = AltitudeCooling.enter(AltitudeCooling.of(level));
		try {
			return original.call(pos);
		} finally {
			AltitudeCooling.exit(previous);
		}
	}
}
