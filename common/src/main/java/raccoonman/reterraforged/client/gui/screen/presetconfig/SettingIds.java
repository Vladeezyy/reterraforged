package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Names for the settings of a page that are the same in two builds of it (the edited preset and Default): the section
 * (the label above the setting), the setting's translation key and how many settings with that key came before it in
 * the section. Not the position on the page: the structure page lists its sets in the order of the preset's map, which
 * differs between presets, and all its sets have the same keys. Without the screen, so the unit tests can run it.
 */
final class SettingIds {
	private final Map<String, Integer> seen = new HashMap<>();
	private String section = "";

	void section(String name) {
		this.section = name;
	}

	String next(String key) {
		String base = this.section + '\u0000' + key;
		int index = this.seen.merge(base, 1, Integer::sum) - 1;
		return base + '#' + index;
	}
}
