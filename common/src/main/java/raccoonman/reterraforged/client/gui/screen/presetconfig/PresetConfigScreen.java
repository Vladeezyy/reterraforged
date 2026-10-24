package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiFunction;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.world.level.levelgen.WorldOptions;
import raccoonman.reterraforged.client.gui.screen.createworld.RTFCreateWorld;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
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
