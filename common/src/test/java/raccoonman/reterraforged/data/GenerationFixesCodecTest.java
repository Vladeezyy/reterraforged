package raccoonman.reterraforged.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import raccoonman.reterraforged.data.worldgen.preset.settings.ClimateSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.ClimateSettings.BiomeNoise;
import raccoonman.reterraforged.data.worldgen.preset.settings.ClimateSettings.BiomeShape;
import raccoonman.reterraforged.data.worldgen.preset.settings.ClimateSettings.RangeValue;
import raccoonman.reterraforged.data.worldgen.preset.settings.SpawnType;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings.Properties;

/** Existing worlds keep their terrain: what changes generation is on for new presets, off for the ones saved before it */
class GenerationFixesCodecTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private static Properties properties() {
		return new Properties(SpawnType.CONTINENT_CENTER, 320, 64, 63, -54);
	}

	private static JsonObject encode(Properties properties) {
		return Properties.CODEC.encodeStart(JsonOps.INSTANCE, properties).getOrThrow().getAsJsonObject();
	}

	private static Properties decode(JsonObject json) {
		return Properties.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
	}

	@Test
	void onInANewPreset() {
		assertTrue(properties().generationFixes);
		assertTrue(decode(encode(properties())).generationFixes);
		assertTrue(properties().copy().generationFixes);
	}

	@Test
	void offInAPresetSavedBeforeIt() {
		JsonObject saved = encode(properties());
		assertNotNull(saved.remove("generationFixes"));
		assertFalse(decode(saved).generationFixes);
	}

	@Test
	void theToggleIsSaved() {
		Properties properties = properties();
		properties.generationFixes = false;
		assertFalse(decode(encode(properties)).generationFixes);
		assertFalse(properties.copy().generationFixes);
	}

	// Codec.intRange refused it before PresetLimits could cut it, and the server did not start
	@Test
	void aSwampHeightOutOfRangeIsNotRefused() {
		ClimateSettings climate = new ClimateSettings(new RangeValue(0, 6, 2, 0.0F, 0.98F, 0.05F), new RangeValue(0, 6, 1, 0.0F, 1.0F, 0.0F), new BiomeShape(225, 8, 150, 80), new BiomeNoise(BiomeNoise.EdgeType.SIMPLEX, 24, 2, 0.5F, 2.65F, 14));
		JsonObject saved = ClimateSettings.CODEC.encodeStart(JsonOps.INSTANCE, climate).getOrThrow().getAsJsonObject();
		saved.addProperty("lowlandSwamps", 99);
		saved.addProperty("swampMaxHeight", -5);
		ClimateSettings read = ClimateSettings.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
		assertEquals(99, read.lowlandSwamps);
		assertEquals(-5, read.swampMaxHeight);
	}
}
