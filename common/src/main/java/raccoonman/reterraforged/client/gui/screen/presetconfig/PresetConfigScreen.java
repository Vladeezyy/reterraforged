package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiFunction;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import raccoonman.reterraforged.client.gui.screen.createworld.RTFCreateWorld;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.client.gui.screen.page.LinkedPageScreen;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetListPage.PresetEntry;
import raccoonman.reterraforged.client.gui.Toasts;
import raccoonman.reterraforged.data.worldgen.PresetExport;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;

public class PresetConfigScreen extends LinkedPageScreen {
	// built-in presets as Done left them (they are not saved to files) for each create world screen, by name
	private static final Map<CreateWorldScreen, Map<String, Preset>> EDITED_BUILTINS = new WeakHashMap<>();
	private final CreateWorldScreen parent;
	// what this screen edits, by name: copies, until Done keeps them and Cancel throws them away. The pages wrote straight
	// into the presets and saved the files when they closed, so Cancel changed the player's preset all the same
	private final Map<String, PresetEntry> working = new LinkedHashMap<>();
	private final String seedBefore;
	private boolean seedChanged;
	private boolean done;
	// where the preview map is looking: the same on every page, and after the window changes size
	public int previewOffsetX, previewOffsetZ;
	public int previewZoom = 68;
	
	public PresetConfigScreen(CreateWorldScreen parent) {
		this.parent = parent;
		this.seedBefore = parent.getUiState().getSeed();
		this.currentPage = new PresetListPage(this);
	}
	
	@Override
	public void onClose() {
		super.onClose();

		if(!this.done && this.seedChanged) {
			this.parent.getUiState().setSeed(this.seedBefore);
		}
		// Done started a reload of the datapacks, and the game brings the create world screen back when it is over. Back at
		// once, that screen still had the old preset, and a quick Create New World made the world with it
		if(this.minecraft.gui.screen() == this) {
			this.minecraft.gui.setScreen(this.parent);
		}
	}
	
	@Override
	public void onDone() {
		this.done = true;
		for(PresetEntry entry : this.working.values()) {
			if(entry.isBuiltin()) {
				EDITED_BUILTINS.computeIfAbsent(this.parent, (screen) -> new HashMap<>()).put(entry.getName().getString(), entry.getPreset());
			} else {
				Toasts.tryOrToast(RTFTranslationKeys.GUI_CREATE_WORLD_APPLY_FAILED, entry::save);
			}
		}
		super.onDone();
	}
	
	// the copy this screen edits: a built-in preset's has the changes of the last Done on this create world screen
	public PresetEntry editable(PresetEntry entry) {
		return this.working.computeIfAbsent(entry.getName().getString(), (name) -> {
			Map<String, Preset> edited = entry.isBuiltin() ? EDITED_BUILTINS.get(this.parent) : null;
			Preset preset = edited != null ? edited.get(name) : null;
			return entry.withPreset((preset != null ? preset : entry.getPreset()).copy());
		});
	}
	
	// the preset was deleted: nothing of it to keep on Done
	public void forget(PresetEntry entry) {
		this.working.remove(entry.getName().getString());
	}

	// through the seed field's own state: the field on the World tab shows it when the screen comes back
	public void setSeed(long seed) {
		this.seedChanged = true;
		this.parent.getUiState().setSeed(Long.toString(seed));
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

	// the settings pages as tabs on the left (before, only "<<" and ">>" went from page to page, one at a time)
	private record Tab(String key, Class<?> type, BiFunction<PresetConfigScreen, PresetEntry, Page> page) {
	}

	private static final List<Tab> TABS = List.of(
		new Tab(RTFTranslationKeys.GUI_TAB_PRESETS, PresetListPage.class, (screen, preset) -> new PresetListPage(screen, preset.getName().getString())),
		new Tab(RTFTranslationKeys.GUI_TAB_WORLD, WorldSettingsPage.class, WorldSettingsPage::new),
		new Tab(RTFTranslationKeys.GUI_TAB_SURFACE, SurfaceSettingsPage.class, SurfaceSettingsPage::new),
		new Tab(RTFTranslationKeys.GUI_TAB_CAVES, CaveSettingsPage.class, CaveSettingsPage::new),
		new Tab(RTFTranslationKeys.GUI_TAB_CLIMATE, ClimateSettingsPage.class, ClimateSettingsPage::new),
		new Tab(RTFTranslationKeys.GUI_TAB_TERRAIN, TerrainSettingsPage.class, TerrainSettingsPage::new),
		new Tab(RTFTranslationKeys.GUI_TAB_WATER_BIOMES, FakeWaterBiomeSettingsPage.class, FakeWaterBiomeSettingsPage::new),
		new Tab(RTFTranslationKeys.GUI_TAB_ISLANDS, IslandSettingsPage.class, IslandSettingsPage::new),
		new Tab(RTFTranslationKeys.GUI_TAB_RIVERS, RiverSettingsPage.class, RiverSettingsPage::new),
		new Tab(RTFTranslationKeys.GUI_TAB_FILTERS, FilterSettingsPage.class, FilterSettingsPage::new),
		new Tab(RTFTranslationKeys.GUI_TAB_STRUCTURES, StructureSettingsPage.class, StructureSettingsPage::new),
		new Tab(RTFTranslationKeys.GUI_TAB_MISC, MiscellaneousPage.class, MiscellaneousPage::new)
	);
	private static final int TAB_TOP = 30;

	private int tabWidth() {
		if (!(this.currentPage instanceof PresetEditorPage)) {
			return 0;
		}
		int widest = 0;
		for (Tab tab : TABS) {
			widest = Math.max(widest, this.font.width(Component.translatable(tab.key())));
		}
		return Math.min(Math.max(widest + 16, 70), Math.max(70, this.width / 6));
	}

	@Override
	public int leftInset() {
		int width = this.tabWidth();
		return width > 0 ? width + 4 : 0;
	}

	@Override
	public void init() {
		super.init();
		if (!(this.currentPage instanceof PresetEditorPage editor)) {
			return;
		}
		int width = this.tabWidth();
		// all tabs between the title and the bottom buttons, smaller on short windows
		int height = Math.max(12, Math.min(20, (this.height - TAB_TOP - 35) / TABS.size() - 2));
		int y = TAB_TOP;
		for (Tab tab : TABS) {
			boolean current = tab.type() == this.currentPage.getClass();
			Component text = Component.translatable(tab.key());
			Button button = Button.builder(current ? Component.literal("> ").append(text).withStyle(ChatFormatting.YELLOW) : text, (b) -> {
				this.setPage(tab.page().apply(this, editor.getPreset()));
			}).bounds(10, y, width, height).build();
			button.active = !current;
			this.addRenderableWidget(button);
			y += height + 2;
		}
	}
}
