package raccoonman.reterraforged.data.worldgen.compat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.MapCodec;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;

/** Alex's Caves Continued's copy of Citadel wraps the noise settings' rules in the field itself (CitadelRules) */
class CitadelRulesTest {
	private static SurfaceRules.RuleSource vanilla, modRules;

	// the shape of com.github.alexmodguy.alexscaves.citadel.server.generation.CitadelSurfaceRuleWrapper
	record CitadelSurfaceRuleWrapper(SurfaceRules.RuleSource vanillaRules, SurfaceRules.RuleSource citadelRules) implements SurfaceRules.RuleSource {
		@Override
		public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
			throw new UnsupportedOperationException();
		}

		@Override
		public MapCodec<? extends SurfaceRules.RuleSource> codec() {
			throw new UnsupportedOperationException();
		}
	}

	// another mod's rule of the same shape under another name stays
	record OtherWrapper(SurfaceRules.RuleSource vanillaRules, SurfaceRules.RuleSource citadelRules) implements SurfaceRules.RuleSource {
		@Override
		public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
			throw new UnsupportedOperationException();
		}

		@Override
		public MapCodec<? extends SurfaceRules.RuleSource> codec() {
			throw new UnsupportedOperationException();
		}
	}

	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		vanilla = SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState());
		modRules = SurfaceRules.state(Blocks.CAKE.defaultBlockState());
	}

	@Test
	void theWrapperGivesTheRulesUnderTheMods() {
		SurfaceRules.RuleSource preset = SurfaceRules.sequence(vanilla, modRules);
		assertSame(preset, CitadelRules.unwrapped(new CitadelSurfaceRuleWrapper(preset, modRules)));
		// wrapped again (SurfaceRulesManager.mergeRules keeps one level, a copy of the rules made with the mod has two)
		assertSame(preset, CitadelRules.unwrapped(new CitadelSurfaceRuleWrapper(new CitadelSurfaceRuleWrapper(preset, modRules), modRules)));
	}

	@Test
	void otherRulesStayAsTheyAre() {
		assertSame(vanilla, CitadelRules.unwrapped(vanilla));
		OtherWrapper other = new OtherWrapper(vanilla, modRules);
		assertSame(other, CitadelRules.unwrapped(other));
		// without the rules under the mod's the wrapper stays: nothing to put in its place
		CitadelSurfaceRuleWrapper modOnly = new CitadelSurfaceRuleWrapper(null, modRules);
		assertSame(modOnly, CitadelRules.unwrapped(modOnly));
	}

	// the shape of the 0.0.33 preset's overworld.json with Alex's Caves Continued on 26.2
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
