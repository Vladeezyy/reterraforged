package raccoonman.reterraforged.neoforge;

import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.client.event.RegisterPresetEditorsEvent;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetConfigScreen;

class RTFNeoForgeClient {

	// another server must not keep this world's altitude cooling scale
	static void registerEvents() {
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) -> raccoonman.reterraforged.world.worldgen.AltitudeCooling.reset());
	}

	public static void registerPresetEditors(RegisterPresetEditorsEvent event) {
		// TODO we probably shouldn't register this for the default preset
		event.register(WorldPresets.NORMAL, (screen, ctx) -> new PresetConfigScreen(screen));
	}
}