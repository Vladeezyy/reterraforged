package raccoonman.reterraforged.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import raccoonman.reterraforged.network.AltitudeCoolingPayload;
import raccoonman.reterraforged.world.worldgen.AltitudeCooling;

public class RTFFabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		// the server's altitude cooling scale; another server must not keep it
		ClientPlayNetworking.registerGlobalReceiver(AltitudeCoolingPayload.TYPE, (payload, context) -> context.client().execute(() -> AltitudeCooling.set(payload.scale())));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> AltitudeCooling.reset());
	}
}
