package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.world.level.levelgen.WorldOptions;
import raccoonman.reterraforged.client.gui.screen.createworld.RTFCreateWorld;
import raccoonman.reterraforged.client.gui.screen.page.LinkedPageScreen;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetListPage.PresetEntry;
import raccoonman.reterraforged.data.worldgen.PresetExport;

//FIXME pressing the create world screen before the pack is copied will fuck the game up (surprisingly noone seems to have run into this?)
public class PresetConfigScreen extends LinkedPageScreen {
	// edited copies of the built-in presets (they are not saved to files) for each create world screen, by name
	private static final Map<CreateWorldScreen, Map<String, PresetEntry>> EDITED_BUILTINS = new WeakHashMap<>();
	private CreateWorldScreen parent;
	
	public PresetConfigScreen(CreateWorldScreen parent) {
		this.parent = parent;
		this.currentPage = new PresetListPage(this);
	}
	
	@Override
	public void onClose() {
		super.onClose();

		this.minecraft.gui.setScreen(this.parent);
	}
	
	// a built-in preset is edited as a copy: the same copy until the create world screen is closed, so the changes
	// stay when going back to the preset list or opening Customize again (they used to be thrown away)
	public PresetEntry editable(PresetEntry entry) {
		if(!entry.isBuiltin()) {
			return entry;
		}
		return EDITED_BUILTINS.computeIfAbsent(this.parent, (screen) -> new HashMap<>()).computeIfAbsent(entry.getName().getString(), (name) -> {
			return new PresetEntry(entry.getName(), entry.getPreset().copy(), true, (b) -> {});
		});
	}

	public void setSeed(long seed) {
		//TODO update the seed edit box
		this.parent.getUiState().setSettings(this.getSettings().withOptions((options) -> {
			return new WorldOptions(seed, options.generateStructures(), options.generateBonusChest());
		}));
	}
	
	public WorldCreationContext getSettings() {
		return this.parent.getUiState().getSettings();
	}

	public void applyPreset(PresetEntry preset) throws IOException {
		RTFCreateWorld.apply(this.parent, preset.getName(), preset.getPreset());
	}
	
	public void exportAsDatapack(Path outputPath, PresetEntry presetEntry) throws IOException {
		PresetExport.toZip(presetEntry.getPreset(), presetEntry.getName().getString(), this.getSettings().worldgenLoadContext(), outputPath);
	}
}
