package raccoonman.reterraforged.client.gui.screen.presetconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PresetNamesTest {

	@Test
	void takesTheLettersOfAnyLanguage() {
		assertTrue(PresetNames.isValid("Мой пресет 2"));
		assertTrue(PresetNames.isValid("世界の設定"));
		assertTrue(PresetNames.isValid("Île-de_France"));
	}

	@Test
	void refusesWhatIsNoFileName() {
		assertFalse(PresetNames.isValid(""));
		assertFalse(PresetNames.isValid("   "));
		assertFalse(PresetNames.isValid(" name"));
		assertFalse(PresetNames.isValid("name "));
		assertFalse(PresetNames.isValid("a/b"));
		assertFalse(PresetNames.isValid("a\\b"));
		assertFalse(PresetNames.isValid(".."));
		assertFalse(PresetNames.isValid("a:b"));
		assertFalse(PresetNames.isValid("x".repeat(65)));
		assertTrue(PresetNames.isValid("x".repeat(64)));
	}

	@Test
	void refusesTheNamesWindowsKeeps() {
		assertFalse(PresetNames.isValid("CON"));
		assertFalse(PresetNames.isValid("nul"));
		assertFalse(PresetNames.isValid("Com1"));
		assertTrue(PresetNames.isValid("console"));
	}

	@Test
	void namesThatDifferInCaseAreOneFile() {
		assertTrue(PresetNames.sameFile("Default", "default"));
		assertFalse(PresetNames.sameFile("Default", "Default2"));
	}

	@Test
	void presetsShownAlikeAreEditedApart() {
		Path folder = Path.of("presets").toAbsolutePath();
		Path legacy = Path.of("terraforged", "presets").toAbsolutePath();
		// the built-in "Default" and a player's file named Default
		assertNotEquals(PresetNames.workingKey("default", folder.resolve("Default.json")), PresetNames.workingKey(null, folder.resolve("Default.json")));
		// two files shown with one name, in the mod's folder and TerraForged's
		assertNotEquals(PresetNames.workingKey(null, folder.resolve("Mine.json")), PresetNames.workingKey(null, legacy.resolve("Mine.json")));
		// a built-in preset by its id, whatever its name in the game's language
		assertEquals("builtin:toweringPeaks", PresetNames.workingKey("toweringPeaks", folder.resolve("Высокие пики.json")));
		// one file, however its path is written
		assertEquals(PresetNames.workingKey(null, folder.resolve("Mine.json")), PresetNames.workingKey(null, Path.of("presets", ".", "Mine.json")));
	}
}
