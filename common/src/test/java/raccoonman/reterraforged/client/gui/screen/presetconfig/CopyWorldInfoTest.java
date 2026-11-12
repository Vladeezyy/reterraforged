package raccoonman.reterraforged.client.gui.screen.presetconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class CopyWorldInfoTest {

	@Test
	void listsWhatABugReportNeeds() {
		String text = CopyWorldInfo.text("0.0.33", "Minecraft 1.21.1, Fabric", -42L, "My preset", "My preset.json", List.of("world.continent.continentScale: 3000 -> 2000", "rivers.riverCount: 8 -> 12"));
		assertEquals("""
			ReTerraForged 0.0.33 (Minecraft 1.21.1, Fabric)
			Seed: -42
			Preset: My preset (My preset.json)
			Settings that differ from Default (2):
			  world.continent.continentScale: 3000 -> 2000
			  rivers.riverCount: 8 -> 12
			""", text);
	}

	@Test
	void saysWhenNothingDiffers() {
		String text = CopyWorldInfo.text("0.0.33", "Minecraft 1.21.1, NeoForge", 1L, "Default", "built-in default", List.of());
		assertEquals("""
			ReTerraForged 0.0.33 (Minecraft 1.21.1, NeoForge)
			Seed: 1
			Preset: Default (built-in default)
			Settings: same as Default
			""", text);
	}
}
