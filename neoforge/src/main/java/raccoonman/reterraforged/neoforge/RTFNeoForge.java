package raccoonman.reterraforged.neoforge;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.client.data.RTFLanguageProvider;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.platform.neoforge.RegistryUtilImpl;

@Mod("reterraforged")
public class RTFNeoForge {

    public RTFNeoForge(IEventBus modEventBus, ModContainer container, Dist dist) {
    	RTFCommon.bootstrap();

    	if (dist == Dist.CLIENT) {
    		modEventBus.addListener(RTFNeoForgeClient::registerPresetEditors);
    		RTFNeoForgeClient.registerEvents();
    	}
    	RTFNeoForgeNetwork.register(modEventBus);
    	modEventBus.addListener(RTFNeoForge::gatherData);
    	net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent event) -> raccoonman.reterraforged.server.RTFSelfTest.register(event.getDispatcher()));
    	net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent event) -> RTFCommon.registerCommands(event.getDispatcher()));
    	// NeoForge reads only neoforge/biome_modifier: a preset datapack with forge/biome_modifier alone gets none of its modifiers
    	raccoonman.reterraforged.server.WorldInfo.setPlatform(new raccoonman.reterraforged.server.WorldInfo.Platform("NeoForge", version("neoforge"), version("minecraft"), container.getModInfo().getVersion().toString(), "neoforge/biome_modifier", "forge/biome_modifier", false));
    	RegistryUtilImpl.register(modEventBus);
    	// its queued tiles are not generated ahead of the next world's (the generation threads are shared)
    	net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event) -> raccoonman.reterraforged.world.worldgen.densityfunction.tile.TileCache.closeAll());
    }
    
    private static String version(String modId) {
    	// only for the log: a mod list not there yet must not stop the mod from loading
    	try {
    		return net.neoforged.fml.ModList.get().getModContainerById(modId).map((mod) -> mod.getModInfo().getVersion().toString()).orElse("?");
    	} catch (RuntimeException e) {
    		return "?";
    	}
    }

    // the language file is client data (NeoForge 21.4 splits GatherDataEvent into Client and Server)
    private static void gatherData(GatherDataEvent.Client event) {
    	boolean includeClient = true; //event.includeClient();
    	DataGenerator generator = event.getGenerator();
    	PackOutput output = generator.getPackOutput();

    	generator.addProvider(includeClient, new RTFLanguageProvider.EnglishUS(output));
    	generator.addProvider(includeClient, PackMetadataGenerator.forFeaturePack(output, Component.translatable(RTFTranslationKeys.METADATA_DESCRIPTION)));
    }
}