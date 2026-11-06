package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** What a preset, a file in the presets folder, can be called */
final class PresetNames {
	private static final int MAX_LENGTH = 64;
	// letters and digits of any language: the game has 11 languages of the mod's own, and "Latin only" was a dead red text box
	private static final Pattern ALLOWED = Pattern.compile("^[\\p{L}\\p{M}\\p{N}\\-_ ]+$");
	// file names Windows refuses, with any extension
	private static final Set<String> RESERVED = Set.of("con", "prn", "aux", "nul", "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9");

	private PresetNames() {
	}

	static boolean isValid(String name) {
		return name.length() <= MAX_LENGTH
			&& ALLOWED.matcher(name).matches()
			// Windows drops a space at the end of a file name; a name of spaces alone is no name
			&& name.equals(name.strip())
			&& !RESERVED.contains(name.toLowerCase(Locale.ROOT));
	}

	/** The same file on macOS and Windows, whose file names ignore case */
	static boolean sameFile(String name, String other) {
		return name.equalsIgnoreCase(other);
	}
}
