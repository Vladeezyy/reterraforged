package raccoonman.reterraforged.client.gui.screen.presetconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PresetListLayoutTest {
	// the widest Russian name ("TerraForged - Huge Biomes (старый)" with the marker), in GUI units
	private static final int NAMES = 200;

	private static void check(PresetListLayout layout, int total) {
		assertEquals(total, layout.listWidth() + (layout.infoWidth() > 0 ? PresetListLayout.GAP + layout.infoWidth() : 0));
	}

	@Test
	void guiScale2HasTheWholeNamesAndABigMap() {
		// 1600x900 at scale 2: 800 wide, the list and map between x 10 and the buttons at 556
		PresetListLayout layout = PresetListLayout.split(542, NAMES);
		check(layout, 542);
		assertTrue(layout.listWidth() >= NAMES + PresetListLayout.LIST_PADDING);
		assertEquals(264, layout.infoWidth());
	}

	@Test
	void guiScales3And4KeepTheMap() {
		// 1600x900 at scale 3 (533 wide) and 1920x1080 at scale 4 (480 wide): the map's column no matter the buttons
		for (int total : new int[] { 355, 312 }) {
			PresetListLayout layout = PresetListLayout.split(total, NAMES);
			check(layout, total);
			assertTrue(layout.infoWidth() >= 104, "info " + layout.infoWidth() + " of " + total);
		}
	}

	@Test
	void shortNamesLeaveMoreToTheMap() {
		PresetListLayout layout = PresetListLayout.split(355, 120);
		check(layout, 355);
		assertTrue(layout.infoWidth() > 150);
	}

	@Test
	void theSmallestWindowKeepsTheList() {
		// 320x240 at scale 1: no room for a map next to a usable list
		PresetListLayout layout = PresetListLayout.split(161, NAMES);
		assertEquals(161, layout.listWidth());
		assertEquals(0, layout.infoWidth());
	}
}
