package raccoonman.reterraforged.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import raccoonman.reterraforged.network.AltitudeCoolingPayload;
import raccoonman.reterraforged.world.worldgen.AltitudeCooling;

public class RTFFabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		// the altitude cooling scale of the player's dimension, on the client's thread only (in single player the
		// integrated server keeps its own); another server must not keep it
		ClientPlayNetworking.registerGlobalReceiver(AltitudeCoolingPayload.TYPE, (payload, context) -> context.client().execute(() -> AltitudeCooling.setClient(payload.scale())));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(AltitudeCooling::clearClient));
	}
}
