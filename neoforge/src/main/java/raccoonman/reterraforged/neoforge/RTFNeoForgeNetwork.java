package raccoonman.reterraforged.neoforge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import raccoonman.reterraforged.network.AltitudeCoolingPayload;
import raccoonman.reterraforged.world.worldgen.AltitudeCooling;

/** The altitude cooling scale: set from the overworld's preset, sent to joining players (AltitudeCooling) */
final class RTFNeoForgeNetwork {

	private RTFNeoForgeNetwork() {
	}

	static void register(IEventBus modEventBus) {
		// optional: players without the mod (or an older one) can still join
		modEventBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").optional()
			.playToClient(AltitudeCoolingPayload.TYPE, AltitudeCoolingPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> AltitudeCooling.set(payload.scale()))));
		// before the spawn chunks are generated (their snow and ice depend on it)
		NeoForge.EVENT_BUS.addListener((LevelEvent.Load event) -> {
			if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
				AltitudeCooling.set(AltitudeCooling.of(level));
			}
		});
		NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> AltitudeCooling.reset());
		NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player && player.connection.hasChannel(AltitudeCoolingPayload.TYPE)) {
				PacketDistributor.sendToPlayer(player, new AltitudeCoolingPayload(AltitudeCooling.of(player.server.overworld())));
			}
		});
	}
}
