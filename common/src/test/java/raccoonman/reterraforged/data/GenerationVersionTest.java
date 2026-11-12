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
import raccoonman.reterraforged.data.worldgen.preset.settings.SpawnType;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings.Properties;

/** The fixes after 0.0.31 are only in worlds whose preset says so: the datapack of an older world has no version */
class GenerationVersionTest {
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
	void aNewPresetHasTheCurrentVersion() {
		Properties properties = properties();
		assertEquals(Properties.CURRENT_GENERATION_VERSION, properties.generationVersion);
		assertTrue(properties.fixes(Properties.CURRENT_GENERATION_VERSION));
		assertEquals(Properties.CURRENT_GENERATION_VERSION, decode(encode(properties)).generationVersion);
		assertEquals(Properties.CURRENT_GENERATION_VERSION, properties.copy().generationVersion);
	}

	@Test
	void aPresetSavedBeforeItKeepsItsFixes() {
		JsonObject saved = encode(properties());
		assertNotNull(saved.remove("generationVersion"));
		Properties old = decode(saved);
		assertEquals(0, old.generationVersion);
		// the 0.0.30 fixes stay on, the later ones are off
		assertTrue(old.generationFixes);
		assertTrue(old.fixes(0));
		assertFalse(old.fixes(1));
		assertEquals(0, old.copy().generationVersion);
	}

	// such a preset saved again (rtf exportpreset of an old world's preset.json, PRESET=file:) writes the same file
	@Test
	void versionZeroIsNotWritten() {
		JsonObject saved = encode(properties());
		saved.remove("generationVersion");
		JsonObject again = encode(decode(saved));
		assertFalse(again.has("generationVersion"));
		assertEquals(saved, again);
	}

	@Test
	void theFixesNeedTheToggle() {
		Properties properties = properties();
		properties.generationFixes = false;
		assertFalse(properties.fixes(0));
		assertFalse(properties.fixes(1));
		assertFalse(decode(encode(properties)).fixes(1));
	}

	@Test
	void aPresetForANewWorldIsUpgraded() {
		JsonObject saved = encode(properties());
		saved.remove("generationVersion");
		Properties library = decode(saved);
		library.upgradeGeneration();
		assertTrue(library.fixes(Properties.CURRENT_GENERATION_VERSION));
		assertEquals(Properties.CURRENT_GENERATION_VERSION, decode(encode(library)).generationVersion);
	}
}
