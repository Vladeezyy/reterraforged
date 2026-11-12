package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The settings search: every word of the query somewhere in the text, in any case, without accents ("hohe" finds
 * "Höhe") and with "ё" as "е" (Russian writes both). Without the screen, so the unit tests can run it.
 */
final class SettingSearch {
	private static final Pattern ACCENTS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	private static final Pattern SPACES = Pattern.compile("\\s+");
	private static final Pattern FORMATTING = Pattern.compile("§.");
	private final String[] words;

	private SettingSearch(String[] words) {
		this.words = words;
	}

	static SettingSearch of(String query) {
		String normalized = normalize(query);
		return new SettingSearch(normalized.isEmpty() ? new String[0] : SPACES.split(normalized));
	}

	boolean isEmpty() {
		return this.words.length == 0;
	}

	boolean matches(String text) {
		if (this.isEmpty()) {
			return false;
		}
		String normalized = normalize(text);
		for (String word : this.words) {
			if (!normalized.contains(word)) {
				return false;
			}
		}
		return true;
	}

	static String normalize(String text) {
		String plain = FORMATTING.matcher(text).replaceAll("");
		// only the Latin accents: the voicing marks of Japanese kana are marks too, and without them other words matched
		plain = Normalizer.normalize(ACCENTS.matcher(Normalizer.normalize(plain, Normalizer.Form.NFD)).replaceAll(""), Normalizer.Form.NFC);
		plain = plain.toLowerCase(Locale.ROOT).replace('ё', 'е');
		return SPACES.matcher(plain.trim()).replaceAll(" ");
	}
}
