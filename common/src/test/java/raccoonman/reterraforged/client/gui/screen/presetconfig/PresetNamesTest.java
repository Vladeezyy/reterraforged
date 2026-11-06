package raccoonman.reterraforged.client.gui.screen.presetconfig;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
