package raccoonman.reterraforged.world.worldgen.surface.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;

/** Rules a mod appends to the preset's surface rules while the world runs go before our strata (Alex's Caves) */
class StrataLastTest {
	private static SurfaceRules.RuleSource vanilla, deepslate, modA, modB;
	// stands for StrataRule, whose codec needs the mod's registries
	private static SurfaceRules.RuleSource strata;

	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		vanilla = SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState());
		deepslate = SurfaceRules.state(Blocks.DEEPSLATE.defaultBlockState());
		modA = SurfaceRules.state(Blocks.CAKE.defaultBlockState());
		modB = SurfaceRules.state(Blocks.CALCITE.defaultBlockState());
		strata = SurfaceRules.state(Blocks.ANDESITE.defaultBlockState());
	}

	private static List<SurfaceRules.RuleSource> rules(SurfaceRules.RuleSource rule) {
		assertTrue(rule instanceof SurfaceRules.SequenceRuleSource, "a sequence: " + rule);
		return ((SurfaceRules.SequenceRuleSource) rule).sequence();
	}

	@Test
	void appendedRulesGoBeforeTheStrata() {
		// Citadel's SurfaceRulesManager.mergeRules: sequence(the noise settings' rules, the mod's rules)
		SurfaceRules.RuleSource preset = SurfaceRules.sequence(vanilla, deepslate, strata);
		SurfaceRules.RuleSource merged = SurfaceRules.sequence(preset, modA, modB);
		assertEquals(List.of(vanilla, deepslate, modA, modB, strata), rules(StrataLast.reorder(merged, StrataLastTest::isStrata)));
	}

	@Test
	void thePresetsOwnRulesStayAsTheyAre() {
		// nothing appended: the preset's rule itself, sequence(base, strata), whose first element is not one with strata
		SurfaceRules.RuleSource base = SurfaceRules.sequence(vanilla, deepslate);
		SurfaceRules.RuleSource preset = SurfaceRules.sequence(base, strata);
		assertSame(preset, StrataLast.reorder(preset, StrataLastTest::isStrata));
	}

	@Test
	void rulesWithoutOurStrataStayAsTheyAre() {
		// vanilla's own world, or another mod's sequence in front
		SurfaceRules.RuleSource merged = SurfaceRules.sequence(SurfaceRules.sequence(vanilla, deepslate), modA);
		assertSame(merged, StrataLast.reorder(merged, StrataLastTest::isStrata));
		assertSame(vanilla, StrataLast.reorder(vanilla, StrataLastTest::isStrata));
	}

	@Test
	void anotherRuleIsNotTakenForTheStrata() {
		// apply() looks for StrataRule only
		SurfaceRules.RuleSource merged = SurfaceRules.sequence(SurfaceRules.sequence(vanilla, strata), modA);
		SurfaceRules.RuleSource first = StrataLast.apply(merged);
		assertSame(merged, first);
		assertSame(first, StrataLast.apply(merged));
	}

	private static boolean isStrata(SurfaceRules.RuleSource rule) {
		return rule == strata;
	}
}
