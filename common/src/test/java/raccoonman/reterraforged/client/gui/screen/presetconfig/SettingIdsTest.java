package raccoonman.reterraforged.client.gui.screen.presetconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SettingIdsTest {

	// a page as its builds list it: a section (label) or a setting key with its value
	private record Line(boolean section, String text, String value) {
	}

	private static Line section(String name) {
		return new Line(true, name, null);
	}

	private static Line setting(String key, String value) {
		return new Line(false, key, value);
	}

	private static Map<String, String> ids(List<Line> page) {
		SettingIds ids = new SettingIds();
		Map<String, String> values = new HashMap<>();
		for (Line line : page) {
			if (line.section()) {
				ids.section(line.text());
			} else {
				values.put(ids.next(line.text()), line.value());
			}
		}
		return values;
	}

	@Test
	void theSameSettingHasTheSameIdInBothBuilds() {
		List<Line> edited = List.of(section("Continent"), setting("scale", "3000"), setting("jitter", "0.9"));
		List<Line> standard = List.of(section("Continent"), setting("scale", "3000"), setting("jitter", "0.7"));
		Map<String, String> a = ids(edited), b = ids(standard);
		assertEquals(a.keySet(), b.keySet());
		assertEquals(2, a.size());
	}

	// the structure page lists its sets in the order of the preset's map, all with the same keys: matched by section
	@Test
	void sectionsInAnotherOrderStillMatch() {
		List<Line> edited = List.of(section("Villages"), setting("spacing", "40"), section("Temples"), setting("spacing", "32"));
		List<Line> standard = List.of(section("Temples"), setting("spacing", "32"), section("Villages"), setting("spacing", "34"));
		Map<String, String> a = ids(edited), b = ids(standard);
		assertEquals(a.keySet(), b.keySet());
		SettingIds one = new SettingIds();
		one.section("Villages");
		String villages = one.next("spacing");
		assertEquals("40", a.get(villages));
		assertEquals("34", b.get(villages));
	}

	// the climate page has a seed offset in two sections; and a key twice in a section is told apart by its place
	@Test
	void aKeyTwiceGetsTwoIds() {
		SettingIds ids = new SettingIds();
		ids.section("Temperature");
		String temperature = ids.next("seedOffset");
		ids.section("Moisture");
		String moisture = ids.next("seedOffset");
		String again = ids.next("seedOffset");
		assertNotEquals(temperature, moisture);
		assertNotEquals(moisture, again);
	}
}
