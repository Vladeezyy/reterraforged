package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.util.Optional;

import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.client.gui.screen.page.LinkedPageScreen.Page;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetListPage.PresetEntry;
import raccoonman.reterraforged.client.gui.widget.Slider;
import raccoonman.reterraforged.data.worldgen.preset.settings.FakeWaterBiomeSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;

public class FakeWaterBiomeSettingsPage extends PresetEditorPage {
	private CycleButton<Boolean> enableFakeWaterBiomes;
	private CycleButton<Boolean> steppeBelowSeaLevelRiverBiomes;
	private CycleButton<Boolean> badlandsBelowSeaLevelOceanBiomes;
	private CycleButton<Boolean> plateauBelowSeaLevelRiverBiomes;
	private CycleButton<Boolean> hillsBelowSeaLevelRiverBiomes;
	private CycleButton<Boolean> mountainsBelowSeaLevelRiverBiomes;
	private CycleButton<Boolean> plainsBelowSeaLevelRiverBiomes;
	private Slider heightOffset;

	public FakeWaterBiomeSettingsPage(PresetConfigScreen screen, PresetEntry preset) {
		super(screen, preset);
	}

	@Override
	public Component title() {
		return Component.translatable(RTFTranslationKeys.GUI_FAKE_WATER_BIOME_SETTINGS_TITLE);
	}
	
	@Override
	public void init() {
		super.init();

		Preset preset = this.preset.getPreset();
		FakeWaterBiomeSettings fakeWaterBiomes = preset.fakeWaterBiomes();
		
		this.enableFakeWaterBiomes = PresetWidgets.createToggle(fakeWaterBiomes.enableFakeWaterBiomes, RTFTranslationKeys.GUI_BUTTON_ENABLE_FAKE_WATER_BIOMES, (button, value) -> {
			fakeWaterBiomes.enableFakeWaterBiomes = value;
			this.regenerate();
		});
		this.steppeBelowSeaLevelRiverBiomes = PresetWidgets.createToggle(fakeWaterBiomes.steppeBelowSeaLevelRiverBiomes, RTFTranslationKeys.GUI_BUTTON_STEPPE_FAKE_RIVERS, (button, value) -> {
			fakeWaterBiomes.steppeBelowSeaLevelRiverBiomes = value;
			this.regenerate();
		});
		this.badlandsBelowSeaLevelOceanBiomes = PresetWidgets.createToggle(fakeWaterBiomes.badlandsBelowSeaLevelOceanBiomes, RTFTranslationKeys.GUI_BUTTON_BADLANDS_FAKE_OCEANS, (button, value) -> {
			fakeWaterBiomes.badlandsBelowSeaLevelOceanBiomes = value;
			this.regenerate();
		});
		this.plateauBelowSeaLevelRiverBiomes = PresetWidgets.createToggle(fakeWaterBiomes.plateauBelowSeaLevelRiverBiomes, RTFTranslationKeys.GUI_BUTTON_PLATEAU_FAKE_RIVERS, (button, value) -> {
			fakeWaterBiomes.plateauBelowSeaLevelRiverBiomes = value;
			this.regenerate();
		});
		this.hillsBelowSeaLevelRiverBiomes = PresetWidgets.createToggle(fakeWaterBiomes.hillsBelowSeaLevelRiverBiomes, RTFTranslationKeys.GUI_BUTTON_HILLS_FAKE_RIVERS, (button, value) -> {
			fakeWaterBiomes.hillsBelowSeaLevelRiverBiomes = value;
			this.regenerate();
		});
		this.mountainsBelowSeaLevelRiverBiomes = PresetWidgets.createToggle(fakeWaterBiomes.mountainsBelowSeaLevelRiverBiomes, RTFTranslationKeys.GUI_BUTTON_MOUNTAINS_FAKE_RIVERS, (button, value) -> {
			fakeWaterBiomes.mountainsBelowSeaLevelRiverBiomes = value;
			this.regenerate();
		});
		this.plainsBelowSeaLevelRiverBiomes = PresetWidgets.createToggle(fakeWaterBiomes.plainsBelowSeaLevelRiverBiomes, RTFTranslationKeys.GUI_BUTTON_PLAINS_FAKE_RIVERS, (button, value) -> {
			fakeWaterBiomes.plainsBelowSeaLevelRiverBiomes = value;
			this.regenerate();
		});
		this.heightOffset = PresetWidgets.createFloatSlider(fakeWaterBiomes.heightOffset, 0.0F, 0.05F, RTFTranslationKeys.GUI_SLIDER_FAKE_WATER_HEIGHT_OFFSET, (slider, value) -> {
			fakeWaterBiomes.heightOffset = (float) slider.scaleValue(value);
			this.regenerate();
			return value;
		});

		this.left.addWidget(PresetWidgets.createLabel(RTFTranslationKeys.GUI_LABEL_FAKE_WATER_BIOMES));
		this.left.addWidget(this.enableFakeWaterBiomes);
		this.left.addWidget(this.steppeBelowSeaLevelRiverBiomes);
		this.left.addWidget(this.badlandsBelowSeaLevelOceanBiomes);
		this.left.addWidget(this.plateauBelowSeaLevelRiverBiomes);
		this.left.addWidget(this.hillsBelowSeaLevelRiverBiomes);
		this.left.addWidget(this.mountainsBelowSeaLevelRiverBiomes);
		this.left.addWidget(this.plainsBelowSeaLevelRiverBiomes);
		this.left.addWidget(this.heightOffset);
	}

	@Override
	public Optional<Page> previous() {
		return Optional.of(new TerrainSettingsPage(this.screen, this.preset));
	}

	@Override
	public Optional<Page> next() {
		return Optional.of(new IslandSettingsPage(this.screen, this.preset));
	}
}
