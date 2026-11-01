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

		// the altitude cooling scale: set from the overworld's preset before its spawn chunks, sent to joining players
		// (raccoonman.reterraforged.world.worldgen.AltitudeCooling)
		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(raccoonman.reterraforged.network.AltitudeCoolingPayload.TYPE, raccoonman.reterraforged.network.AltitudeCoolingPayload.STREAM_CODEC);
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents.LOAD.register((server, level) -> {
			if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
				raccoonman.reterraforged.world.worldgen.AltitudeCooling.set(raccoonman.reterraforged.world.worldgen.AltitudeCooling.of(level));
			}
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register((server) -> raccoonman.reterraforged.world.worldgen.AltitudeCooling.reset());
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(handler, raccoonman.reterraforged.network.AltitudeCoolingPayload.TYPE)) {
				sender.sendPacket(new raccoonman.reterraforged.network.AltitudeCoolingPayload(raccoonman.reterraforged.world.worldgen.AltitudeCooling.of(server.overworld())));
			}
		});
	}

	//TODO merge this with forge's datagen since they're the same now
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		Pack pack = fabricDataGenerator.createPack();

		pack.addProvider((FabricDataOutput output) -> new RTFLanguageProvider.EnglishUS(output));
		pack.addProvider((FabricDataOutput output) -> PackMetadataGenerator.forFeaturePack(output, Component.translatable(RTFTranslationKeys.METADATA_DESCRIPTION)));
	}
}