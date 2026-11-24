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
import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.material.rule.SequenceRule;

/** Rules a mod appends to the preset's material rule while the world runs go before our strata (Alex's Caves) */
class StrataLastTest {
	private static MaterialRule vanilla, deepslate, modA, modB;
	// stands for StrataRule, whose codec needs the mod's registries
	private static MaterialRule strata;

	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		vanilla = MaterialRules.state(Blocks.GRASS_BLOCK.defaultBlockState());
		deepslate = MaterialRules.state(Blocks.DEEPSLATE.defaultBlockState());
		modA = MaterialRules.state(Blocks.CAKE.defaultBlockState());
		modB = MaterialRules.state(Blocks.CALCITE.defaultBlockState());
		strata = MaterialRules.state(Blocks.ANDESITE.defaultBlockState());
	}

	private static List<MaterialRule> rules(MaterialRule rule) {
		assertTrue(rule instanceof SequenceRule, "a sequence: " + rule);
		return ((SequenceRule) rule).sequence();
	}

	@Test
	void appendedRulesGoBeforeTheStrata() {
		// Citadel's SurfaceRulesManager.mergeRules (1.21.1): sequence(the noise settings' rules, the mod's rules)
		MaterialRule preset = MaterialRules.sequence(vanilla, deepslate, strata);
		MaterialRule merged = MaterialRules.sequence(preset, modA, modB);
		assertEquals(List.of(vanilla, deepslate, modA, modB, strata), rules(StrataLast.reorder(merged, StrataLastTest::isStrata)));
	}

	@Test
	void thePresetsOwnRulesStayAsTheyAre() {
		// nothing appended: the preset's rule itself, whose first element is not a sequence ending in the strata
		MaterialRule base = MaterialRules.sequence(vanilla, deepslate);
		MaterialRule preset = MaterialRules.sequence(base, strata);
		assertSame(preset, StrataLast.reorder(preset, StrataLastTest::isStrata));
		MaterialRule flat = MaterialRules.sequence(vanilla, deepslate, strata);
		assertSame(flat, StrataLast.reorder(flat, StrataLastTest::isStrata));
	}

	@Test
	void rulesWithoutOurStrataStayAsTheyAre() {
		// vanilla's own world, or another mod's sequence in front
		MaterialRule merged = MaterialRules.sequence(MaterialRules.sequence(vanilla, deepslate), modA);
		assertSame(merged, StrataLast.reorder(merged, StrataLastTest::isStrata));
		assertSame(vanilla, StrataLast.reorder(vanilla, StrataLastTest::isStrata));
	}

	@Test
	void anotherRuleIsNotTakenForTheStrata() {
		// apply() looks for StrataRule only
		MaterialRule merged = MaterialRules.sequence(MaterialRules.sequence(vanilla, strata), modA);
		MaterialRule first = StrataLast.apply(merged);
		assertSame(merged, first);
		assertSame(first, StrataLast.apply(merged));
	}

	private static boolean isStrata(MaterialRule rule) {
		return rule == strata;
	}
}
