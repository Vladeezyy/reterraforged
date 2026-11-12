package raccoonman.reterraforged.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator.Pack;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.network.chat.Component;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.client.data.RTFLanguageProvider;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.platform.RegistryUtil;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.world.worldgen.biome.modifier.fabric.BiomeModifiersImpl;

public class RTFFabric implements ModInitializer, DataGeneratorEntrypoint {

	@Override
	public void onInitialize() {
		RTFCommon.bootstrap();

		RegistryUtil.createDataRegistry(RTFRegistries.BIOME_MODIFIER, BiomeModifiersImpl.DATA_CODEC, false);
		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> raccoonman.reterraforged.server.RTFSelfTest.register(dispatcher));

		// the altitude cooling scale: the default set from the overworld's preset before its spawn chunks, the player's
		// dimension's sent on join and on every change of dimension (raccoonman.reterraforged.world.worldgen.AltitudeCooling)
		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(raccoonman.reterraforged.network.AltitudeCoolingPayload.TYPE, raccoonman.reterraforged.network.AltitudeCoolingPayload.STREAM_CODEC);
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents.LOAD.register((server, level) -> {
			if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
				raccoonman.reterraforged.world.worldgen.AltitudeCooling.set(raccoonman.reterraforged.world.worldgen.AltitudeCooling.of(level));
			}
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register((server) -> raccoonman.reterraforged.RTFCommon.onServerStopped());
		// its queued tiles are not generated ahead of the next world's (the generation threads are shared)
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register((server) -> raccoonman.reterraforged.world.worldgen.densityfunction.tile.TileCache.closeAll());
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendAltitudeCooling(handler.player));
		net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> sendAltitudeCooling(player));
		net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> sendAltitudeCooling(newPlayer));
	}

	// the scale of the player's dimension: another mod's dimension cools as in vanilla
	private static void sendAltitudeCooling(net.minecraft.server.level.ServerPlayer player) {
		if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, raccoonman.reterraforged.network.AltitudeCoolingPayload.TYPE)) {
			net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new raccoonman.reterraforged.network.AltitudeCoolingPayload(raccoonman.reterraforged.world.worldgen.AltitudeCooling.of(player.level())));
		}
	}

	//TODO merge this with forge's datagen since they're the same now
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		Pack pack = fabricDataGenerator.createPack();

		pack.addProvider((FabricDataOutput output) -> new RTFLanguageProvider.EnglishUS(output));
		pack.addProvider((FabricDataOutput output) -> PackMetadataGenerator.forFeaturePack(output, Component.translatable(RTFTranslationKeys.METADATA_DESCRIPTION)));
	}
}