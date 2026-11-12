package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

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

	/**
	 * What the preset screen keeps a preset's edits under: a built-in preset's own id, or the file of a player's preset.
	 * The shown name was the key, and a player's "Default" or two files shown alike shared one copy, so an edit of one
	 * went into the other or was lost
	 */
	static String workingKey(@Nullable String builtinId, Path file) {
		return builtinId != null ? "builtin:" + builtinId : "file:" + file.toAbsolutePath().normalize();
	}
}
