package raccoonman.reterraforged.client.gui.screen.presetconfig;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SettingSearchTest {

	@Test
	void anEmptyQueryFindsNothingAndIsEmpty() {
		assertTrue(SettingSearch.of("").isEmpty());
		assertTrue(SettingSearch.of("   ").isEmpty());
		assertFalse(SettingSearch.of("").matches("Continent Scale"));
	}

	@Test
	void partOfANameInAnyCase() {
		assertTrue(SettingSearch.of("scal").matches("Continent Scale"));
		assertTrue(SettingSearch.of("CONTINENT").matches("Continent Scale"));
		assertTrue(SettingSearch.of("масшта").matches("Масштаб материков"));
		assertFalse(SettingSearch.of("river").matches("Continent Scale"));
	}

	@Test
	void everyWordSomewhere() {
		assertTrue(SettingSearch.of("scale cont").matches("Continent Scale"));
		assertFalse(SettingSearch.of("scale river").matches("Continent Scale"));
		assertTrue(SettingSearch.of("  continent   scale ").matches("Continent  Scale"));
	}

	@Test
	void accentsAndYo() {
		assertTrue(SettingSearch.of("hohe").matches("Höhe der Schneegrenze"));
		assertTrue(SettingSearch.of("еще").matches("Ещё"));
		assertTrue(SettingSearch.of("ещё").matches("Еще"));
	}

	// the voicing marks of kana are not accents: が is not か
	@Test
	void kanaKeepTheirMarks() {
		assertTrue(SettingSearch.of("大陸").matches("大陸の規模"));
		assertFalse(SettingSearch.of("が").matches("か"));
	}

	@Test
	void formattingCodesAreNotText() {
		assertTrue(SettingSearch.of("scale").matches("§eScale"));
	}
}
