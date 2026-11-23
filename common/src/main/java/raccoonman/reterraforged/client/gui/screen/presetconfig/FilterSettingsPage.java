package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.util.Optional;

import net.minecraft.client.gui.components.CycleButton;

import net.minecraft.network.chat.Component;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.client.gui.screen.page.LinkedPageScreen.Page;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetListPage.PresetEntry;
import raccoonman.reterraforged.client.gui.widget.Slider;
import raccoonman.reterraforged.data.worldgen.preset.settings.FilterSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.FilterSettings.Erosion;
import raccoonman.reterraforged.data.worldgen.preset.settings.FilterSettings.RidgeStyle;
import raccoonman.reterraforged.data.worldgen.preset.settings.FilterSettings.Ridges;
import raccoonman.reterraforged.data.worldgen.preset.settings.FilterSettings.Smoothing;

class FilterSettingsPage extends PresetEditorPage {
	private Slider erosionDropletsPerChunk;
	private Slider erosionDropletLifetime;
	private Slider erosionDropletVolume;
	private Slider erosionDropletVelocity;
	private Slider erosionRate;
	private Slider depositeRate;
	
	private Slider smoothingIterations;
	private Slider smoothingRadius;
	private Slider smoothingRate;
	
	private Slider ridgesStrength;
	private CycleButton<RidgeStyle> ridgesStyle;

	public FilterSettingsPage(PresetConfigScreen screen, PresetEntry preset) {
		super(screen, preset);
	}

	@Override
	public void init() {
		super.init();
		
		Preset preset = this.preset.getPreset();
		FilterSettings filters = preset.filters();
		
		Erosion erosion = filters.erosion;
		this.erosionDropletsPerChunk = PresetWidgets.createIntSlider(erosion.dropletsPerChunk, 10, 250, RTFTranslationKeys.GUI_SLIDER_EROSION_DROPLETS_PER_CHUNK, (slider, value) -> {
			erosion.dropletsPerChunk = (int) slider.scaleValue(value);
			return value;
		});
		this.erosionDropletLifetime = PresetWidgets.createIntSlider(erosion.dropletLifetime, 1, 32, RTFTranslationKeys.GUI_SLIDER_EROSION_DROPLET_LIFETIME, (slider, value) -> {
			erosion.dropletLifetime = (int) slider.scaleValue(value);
			return value;
		});
		this.erosionDropletVolume = PresetWidgets.createFloatSlider(erosion.dropletVolume, 0.0F, 1.0F, RTFTranslationKeys.GUI_SLIDER_EROSION_DROPLET_VOLUME, (slider, value) -> {
			erosion.dropletVolume = (float) slider.scaleValue(value);
			return value;
		});
		this.erosionDropletVelocity = PresetWidgets.createFloatSlider(erosion.dropletVelocity, 0.1F, 1.0F, RTFTranslationKeys.GUI_SLIDER_EROSION_DROPLET_VELOCITY, (slider, value) -> {
			erosion.dropletVelocity = (float) slider.scaleValue(value);
			return value;
		});
		this.erosionRate = PresetWidgets.createFloatSlider(erosion.erosionRate, 0.0F, 1.0F, RTFTranslationKeys.GUI_SLIDER_EROSION_RATE, (slider, value) -> {
			erosion.erosionRate =  (float) slider.scaleValue(value);
			return value;
		});
		this.depositeRate = PresetWidgets.createFloatSlider(erosion.depositeRate, 0.0F, 1.0F, RTFTranslationKeys.GUI_SLIDER_DEPOSITE_RATE, (slider, value) -> {
			erosion.depositeRate = (float) slider.scaleValue(value);
			return value;
		});
		
		Smoothing smoothing = filters.smoothing;
		this.smoothingIterations = PresetWidgets.createIntSlider(smoothing.iterations, 0, 5, RTFTranslationKeys.GUI_SLIDER_SMOOTHING_ITERATIONS, (slider, value) -> {
			smoothing.iterations = (int) slider.scaleValue(value);
			return value;
		});
		this.smoothingRadius = PresetWidgets.createFloatSlider(smoothing.smoothingRadius, 0, 5, RTFTranslationKeys.GUI_SLIDER_SMOOTHING_RADIUS, (slider, value) -> {
			smoothing.smoothingRadius = (float) slider.scaleValue(value);
			return value;
		});
		this.smoothingRate = PresetWidgets.createFloatSlider(smoothing.smoothingRate, 0.0F, 1.0F, RTFTranslationKeys.GUI_SLIDER_SMOOTHING_RATE, (slider, value) -> {
			smoothing.smoothingRate = (float) slider.scaleValue(value);
			return value;
		});
		
		Ridges ridges = filters.ridges;
		// steps of 0.05 (0 = off): stepTowards in twentieths, rounded first so 0.45 * 20 is 9 and not 9.000000000000002
		this.ridgesStrength = PresetWidgets.createFloatSlider(ridges.strength, 0.0F, 1.0F, RTFTranslationKeys.GUI_SLIDER_RIDGES_STRENGTH, (slider, value) -> {
			int twentieths = SliderLimits.stepTowards(Math.round(value * 20.0E6) / 1.0E6, Math.round(slider.getPrevious() * 20.0E6) / 1.0E6, 1);
			twentieths = Math.max(0, Math.min(20, twentieths));
			ridges.strength = twentieths / 20.0F;
			return twentieths / 20.0;
		});
		this.ridgesStyle = PresetWidgets.createCycle(RidgeStyle.values(), ridges.style, RTFTranslationKeys.GUI_SLIDER_RIDGES_STYLE, (button, value) -> {
			ridges.style = value;
		});
		
		this.left.addWidget(PresetWidgets.createLabel(RTFTranslationKeys.GUI_LABEL_EROSION));
		this.left.addWidget(this.erosionDropletsPerChunk);
		this.left.addWidget(this.erosionDropletLifetime);
		this.left.addWidget(this.erosionDropletVolume);
		this.left.addWidget(this.erosionDropletVelocity);
		this.left.addWidget(this.erosionRate);
		this.left.addWidget(this.depositeRate);
		
		this.left.addWidget(PresetWidgets.createLabel(RTFTranslationKeys.GUI_LABEL_SMOOTHING));
		this.left.addWidget(this.smoothingIterations);
		this.left.addWidget(this.smoothingRadius);
		this.left.addWidget(this.smoothingRate);
		
		this.left.addWidget(PresetWidgets.createLabel(RTFTranslationKeys.GUI_LABEL_RIDGES));
		this.left.addWidget(this.ridgesStrength);
		this.left.addWidget(this.ridgesStyle);
	}
		
	@Override
	public Component title() {
		return Component.translatable(RTFTranslationKeys.GUI_FILTER_SETTINGS_TITLE);
	}

	@Override
	public Optional<Page> previous() {
		return Optional.of(new RiverSettingsPage(this.screen, this.preset));
	}

	@Override
	public Optional<Page> next() {
		return Optional.of(new StructureSettingsPage(this.screen, this.preset));
	}

}
