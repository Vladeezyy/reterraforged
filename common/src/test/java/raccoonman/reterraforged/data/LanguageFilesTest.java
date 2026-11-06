package raccoonman.reterraforged.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

class LanguageFilesTest {
	private static final Pattern PLACEHOLDER = Pattern.compile("%(\\d+\\$)?[sd%]");
	private static final Pattern ANY_PERCENT = Pattern.compile("%");

	private static Map<String, String> read(String language) throws IOException {
		try (InputStream in = LanguageFilesTest.class.getResourceAsStream("/assets/reterraforged/lang/" + language + ".json")) {
			assertNotNull(in, language);
			return new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), new TypeToken<Map<String, String>>() { }.getType());
		}
	}

	private static List<String> placeholders(String text) {
		List<String> found = new ArrayList<>();
		Matcher matcher = PLACEHOLDER.matcher(text);
		while (matcher.find()) {
			found.add(matcher.group());
		}
		return found;
	}

	@ParameterizedTest
	@ValueSource(strings = { "ru_ru", "uk_ua", "zh_cn", "de_de", "fr_fr", "es_es", "pt_br", "ja_jp", "ko_kr", "pl_pl" })
	void hasTheKeysAndPlaceholdersOfEnglish(String language) throws IOException {
		Map<String, String> english = read("en_us");
		Map<String, String> other = read(language);
		Set<String> missing = new TreeSet<>(english.keySet());
		missing.removeAll(other.keySet());
		Set<String> extra = new TreeSet<>(other.keySet());
		extra.removeAll(english.keySet());
		assertTrue(missing.isEmpty(), language + " misses " + missing);
		assertTrue(extra.isEmpty(), language + " has extra " + extra);
		for (Map.Entry<String, String> entry : english.entrySet()) {
			String text = other.get(entry.getKey());
			assertFalse(text.isBlank(), entry.getKey());
			assertEquals(placeholders(entry.getValue()), placeholders(text), language + " " + entry.getKey());
		}
	}

	// beside a placeholder a lone % stops the formatting: the game then shows the text as it is, with its %s
	@Test
	void aPlaceholderHasNoLonePercentBesideIt() throws IOException {
		for (String language : List.of("en_us", "ru_ru", "uk_ua", "zh_cn", "de_de", "fr_fr", "es_es", "pt_br", "ja_jp", "ko_kr", "pl_pl")) {
			for (Map.Entry<String, String> entry : read(language).entrySet()) {
				if (placeholders(entry.getValue()).isEmpty()) {
					continue;
				}
				long percents = ANY_PERCENT.matcher(entry.getValue()).results().count();
				long known = placeholders(entry.getValue()).stream().mapToLong((p) -> p.equals("%%") ? 2 : 1).sum();
				assertEquals(known, percents, language + " " + entry.getKey());
			}
		}
	}
}
