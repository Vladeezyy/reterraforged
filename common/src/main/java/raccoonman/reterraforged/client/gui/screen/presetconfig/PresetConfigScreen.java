package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.levelgen.WorldOptions;
import raccoonman.reterraforged.client.gui.screen.page.LinkedPageScreen;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetListPage.PresetEntry;
import raccoonman.reterraforged.data.worldgen.PresetExport;

//FIXME pressing the create world screen before the pack is copied will fuck the game up (surprisingly noone seems to have run into this?)
public class PresetConfigScreen extends LinkedPageScreen {
	private static final String PACK_NAME = "reterraforged-preset";
	private static final AtomicInteger APPLIED = new AtomicInteger();
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
		Pair<Path, PackRepository> path = this.parent.getDataPackSelectionSettings(this.parent.getUiState().getSettings().dataConfiguration());
		PackRepository repository = path.getSecond();
		// the preset goes in under a new file name each time: the create world screen reloads the datapacks only when
		// the list of them changes, so a preset edited and applied again (Customize, Done, Customize, Done) used to
		// keep the first one, and the new world was made with the old settings
		List<Path> previous;
		try(Stream<Path> files = Files.list(path.getFirst())) {
			previous = files.filter((file) -> file.getFileName().toString().startsWith(PACK_NAME)).toList();
		}
		for(Path file : previous) {
			repository.removePack("file/" + file.getFileName());
			try {
				Files.deleteIfExists(file);
			} catch(IOException e) {
				// still open: it stays in the temporary folder, not selected
			}
		}
		Path exportPath = path.getFirst().resolve(PACK_NAME + "-" + APPLIED.incrementAndGet() + ".zip");
		this.exportAsDatapack(exportPath, preset);
		repository.reload();
		if(repository.addPack("file/" + exportPath.getFileName())) {
			this.parent.tryApplyNewDataPacks(repository, false, (data) -> {
			});
		}
	}
	
	public void exportAsDatapack(Path outputPath, PresetEntry presetEntry) throws IOException {
		PresetExport.toZip(presetEntry.getPreset(), presetEntry.getName().getString(), this.getSettings().worldgenLoadContext(), outputPath);
	}
}
