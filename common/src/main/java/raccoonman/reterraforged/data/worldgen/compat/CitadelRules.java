package raccoonman.reterraforged.data.worldgen.compat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Alex's Caves Continued (1.1.3, Modrinth alexs-caves-continued, every version) carries its own copy of Citadel, whose
 * noise settings mixin writes CitadelSurfaceRuleWrapper(the rules, the mod's rules) over the settings' rules (on 26.3 it
 * replaces the Holder of the material rule with Holder.direct(wrapper)). The codec that writes the preset's noise
 * settings then wrote the wrapper into the preset datapack ("alexscaves:citadel_wrapper", alexscaves blocks and
 * conditions), and a world made with the mod did not load without it. The exported files get the wrapper's
 * vanilla_rules inline in its place. Recognised by the codec's name: the mod is not a dependency.
 */
public final class CitadelRules {
	private static final String WRAPPER_CLASS = "CitadelSurfaceRuleWrapper";
	private static final String WRAPPER_TYPE = "citadel_wrapper";
	private static final String VANILLA_RULES = "vanilla_rules";
	// as the data providers write: two spaces, no HTML escapes; the keys keep the order they were read in
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private CitadelRules() {
	}

	// The wrappers taken out of the worldgen files of an exported pack, at any depth, each replaced with the rules it
	// holds under the mod's. Only a file that has one is read and written again; a pack exported without the mod stays
	// byte for byte the same
	public static void unwrapPack(Path pack) throws IOException {
		Path data = pack.resolve("data");
		if (!Files.isDirectory(data)) {
			return;
		}
		List<Path> files;
		try (Stream<Path> walk = Files.walk(data)) {
			files = walk.filter((path) -> path.toString().endsWith(".json") && Files.isRegularFile(path)).toList();
		}
		for (Path file : files) {
			String text = Files.readString(file, StandardCharsets.UTF_8);
			if (!text.contains(WRAPPER_TYPE)) {
				continue;
			}
			JsonElement json = JsonParser.parseString(text);
			JsonElement unwrapped = unwrapped(json);
			if (unwrapped != json) {
				Files.writeString(file, GSON.toJson(unwrapped) + "\n", StandardCharsets.UTF_8);
			}
		}
	}

	// the same element if there is no wrapper in it
	static JsonElement unwrapped(JsonElement json) {
		if (json instanceof JsonObject object) {
			if (isWrapper(object)) {
				return unwrapped(object.get(VANILLA_RULES));
			}
			JsonObject copy = null;
			for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
				JsonElement value = unwrapped(entry.getValue());
				if (value != entry.getValue() && copy == null) {
					copy = new JsonObject();
					for (Map.Entry<String, JsonElement> before : object.entrySet()) {
						if (before.getKey().equals(entry.getKey())) {
							break;
						}
						copy.add(before.getKey(), before.getValue());
					}
				}
				if (copy != null) {
					copy.add(entry.getKey(), value);
				}
			}
			return copy == null ? object : copy;
		}
		if (json instanceof JsonArray array) {
			JsonArray copy = null;
			for (int i = 0; i < array.size(); i++) {
				JsonElement value = unwrapped(array.get(i));
				if (value != array.get(i) && copy == null) {
					copy = new JsonArray();
					for (int j = 0; j < i; j++) {
						copy.add(array.get(j));
					}
				}
				if (copy != null) {
					copy.add(value);
				}
			}
			return copy == null ? array : copy;
		}
		return json;
	}

	// {"type": "<any namespace>:citadel_wrapper", "vanilla_rules": ..., "citadel_rules": ...}
	private static boolean isWrapper(JsonObject object) {
		JsonElement type = object.get("type");
		if (type == null || !type.isJsonPrimitive() || !type.getAsJsonPrimitive().isString() || !object.has(VANILLA_RULES)) {
			return false;
		}
		String id = type.getAsString();
		return id.substring(id.indexOf(':') + 1).equals(WRAPPER_TYPE);
	}
}
