package raccoonman.reterraforged.data.worldgen.compat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/** Alex's Caves Continued's copy of Citadel wraps the noise settings' rules; the export takes it out (CitadelRules) */
class CitadelRulesTest {
	// the shape of the 0.0.33 preset's overworld.json with Alex's Caves Continued
	private static final String WRAPPED = """
		{"noise": {"size_vertical": 2}, "surface_rule": {"type": "alexscaves:citadel_wrapper",
		  "citadel_rules": {"type": "minecraft:sequence", "sequence": [{"type": "alexscaves:ac_biome"}]},
		  "vanilla_rules": {"type": "minecraft:sequence", "sequence": [
		    {"type": "alexscaves:citadel_wrapper", "citadel_rules": {"type": "alexscaves:ac_biome"},
		     "vanilla_rules": {"type": "minecraft:block", "result_state": {"Name": "minecraft:grass_block"}}},
		    {"type": "reterraforged:strata"}]}}, "sea_level": 63}""";
	private static final String UNWRAPPED = """
		{"noise": {"size_vertical": 2}, "surface_rule": {"type": "minecraft:sequence", "sequence": [
		  {"type": "minecraft:block", "result_state": {"Name": "minecraft:grass_block"}},
		  {"type": "reterraforged:strata"}]}, "sea_level": 63}""";

	@Test
	void theJsonLosesEveryWrapperAndKeepsItsOrder() {
		JsonElement out = CitadelRules.unwrapped(JsonParser.parseString(WRAPPED));
		assertEquals(JsonParser.parseString(UNWRAPPED), out);
		assertEquals(java.util.List.of("noise", "surface_rule", "sea_level"), java.util.List.copyOf(out.getAsJsonObject().keySet()));
		assertFalse(out.toString().contains("alexscaves"));
	}

	@Test
	void jsonWithoutAWrapperIsTheSameElement() {
		JsonElement json = JsonParser.parseString(UNWRAPPED);
		assertSame(json, CitadelRules.unwrapped(json));
	}

	@Test
	void onlyThePackFilesWithAWrapperAreWrittenAgain(@TempDir Path pack) throws Exception {
		Path settings = pack.resolve("data/minecraft/worldgen/noise_settings/overworld.json");
		Path other = pack.resolve("data/minecraft/worldgen/noise_settings/other.json");
		Files.createDirectories(settings.getParent());
		Files.writeString(settings, WRAPPED, StandardCharsets.UTF_8);
		byte[] untouched = "{\n  \"surface_rule\":   {\"type\": \"minecraft:sequence\", \"sequence\": []}\n}".getBytes(StandardCharsets.UTF_8);
		Files.write(other, untouched);
		CitadelRules.unwrapPack(pack);
		assertEquals(JsonParser.parseString(UNWRAPPED), JsonParser.parseString(Files.readString(settings, StandardCharsets.UTF_8)));
		assertArrayEquals(untouched, Files.readAllBytes(other));
	}
}
