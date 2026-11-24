package raccoonman.reterraforged.world.worldgen.surface.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.material.rule.SequenceRule;

/**
 * The preset's material rule is sequence(vanilla's rules, our strata) (PresetSurfaceRuleData.overworld). A mod may
 * append its own rules to the noise settings' rule while the world runs, sequence(the preset's rule, the mod's rules):
 * Citadel's SurfaceRulesManager does that on 1.21.1 (Alex's Caves, unofficial port). There our strata came first and
 * turned the rock of its cave biomes to the strata's stone (a quarter of the Candy Cavity's chocolate). This puts the
 * appended rules before the strata. Anything else is left as it is: no strata (a world without the preset), nothing
 * appended, a mod's wrapper around the rule (Alex's Caves Continued tries its own rules first anyway).
 * 26.3: the surface rules are material rules (SequenceRule in place of SurfaceRules.SequenceRuleSource).
 */
public final class StrataLast {
	private record Cached(MaterialRule in, MaterialRule out) {
	}

	// the noise settings give buildSurface the same rule object for every chunk (Citadel merges once)
	private static volatile Cached cached;

	private StrataLast() {
	}

	public static MaterialRule apply(MaterialRule rule) {
		Cached last = cached;
		if (last != null && last.in() == rule) {
			return last.out();
		}
		MaterialRule reordered = reorder(rule, (r) -> r instanceof StrataRule);
		cached = new Cached(rule, reordered);
		return reordered;
	}

	// isStrata: StrataRule (a test without the mod's registries gives a rule of its own)
	static MaterialRule reorder(MaterialRule rule, Predicate<MaterialRule> isStrata) {
		if (!(rule instanceof SequenceRule outer) || outer.sequence().size() < 2) {
			return rule;
		}
		List<MaterialRule> rules = outer.sequence();
		if (!(rules.get(0) instanceof SequenceRule preset)) {
			return rule;
		}
		List<MaterialRule> presetRules = preset.sequence();
		if (presetRules.isEmpty() || !isStrata.test(presetRules.get(presetRules.size() - 1))) {
			return rule;
		}
		List<MaterialRule> ordered = new ArrayList<>(presetRules.subList(0, presetRules.size() - 1));
		ordered.addAll(rules.subList(1, rules.size()));
		ordered.add(presetRules.get(presetRules.size() - 1));
		return MaterialRules.sequence(ordered);
	}
}
