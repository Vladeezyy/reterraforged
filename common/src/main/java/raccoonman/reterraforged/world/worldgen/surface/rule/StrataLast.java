package raccoonman.reterraforged.world.worldgen.surface.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * The preset's surface rules are sequence(the overworld's rules, our strata) (PresetSurfaceRuleData.overworld). A mod
 * may append its own rules to the noise settings' rules while the world runs: Alex's Caves through Citadel's
 * SurfaceRulesManager, sequence(the preset's rules, the mod's rules) from surfaceRule(). There our strata came first
 * and turned the rock of its cave biomes to the strata's stone (a quarter of the Candy Cavity's chocolate). This puts
 * the appended rules before the strata, the order they had when the preset was built from the merged rules (which
 * copied the mod's rules into the world's datapack, NoiseGeneratorSettingsAccessor). Anything else is left as it is:
 * no strata (a world without the preset), nothing appended, TerraBlender's and Lithostitched's wrappers, and Alex's
 * Caves Continued's own copy of Citadel, whose wrapper already tries the mod's rules first (CitadelRules).
 */
public final class StrataLast {
	private record Cached(SurfaceRules.RuleSource in, SurfaceRules.RuleSource out) {
	}

	// the noise settings give buildSurface the same rule object for every chunk (Citadel merges once)
	private static volatile Cached cached;

	private StrataLast() {
	}

	public static SurfaceRules.RuleSource apply(SurfaceRules.RuleSource rule) {
		Cached last = cached;
		if (last != null && last.in() == rule) {
			return last.out();
		}
		SurfaceRules.RuleSource reordered = reorder(rule, (r) -> r instanceof StrataRule);
		cached = new Cached(rule, reordered);
		return reordered;
	}

	// isStrata: StrataRule (a test without the mod's registries gives a rule of its own)
	static SurfaceRules.RuleSource reorder(SurfaceRules.RuleSource rule, Predicate<SurfaceRules.RuleSource> isStrata) {
		if (!(rule instanceof SurfaceRules.SequenceRuleSource outer) || outer.sequence().size() < 2) {
			return rule;
		}
		List<SurfaceRules.RuleSource> rules = outer.sequence();
		if (!(rules.get(0) instanceof SurfaceRules.SequenceRuleSource preset)) {
			return rule;
		}
		List<SurfaceRules.RuleSource> presetRules = preset.sequence();
		if (presetRules.isEmpty() || !isStrata.test(presetRules.get(presetRules.size() - 1))) {
			return rule;
		}
		List<SurfaceRules.RuleSource> ordered = new ArrayList<>(presetRules.subList(0, presetRules.size() - 1));
		ordered.addAll(rules.subList(1, rules.size()));
		ordered.add(presetRules.get(presetRules.size() - 1));
		return SurfaceRules.sequence(ordered.toArray(SurfaceRules.RuleSource[]::new));
	}
}
