package raccoonman.reterraforged.data.worldgen.preset;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.material.VanillaMaterialRules;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.tags.RTFBlockTags;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.surface.rule.RTFSurfaceRules;
import raccoonman.reterraforged.world.worldgen.surface.rule.StrataRule.Strata;

//TODO add forest surfaces
// maybe have a custom meadow or cherry forest surface ?
public class PresetSurfaceRuleData {
    
    // 26.3: vanilla's overworld material rule, assembled like vanilla's (bedrock floor, ore veins, surface above the
    // preliminary surface, underground) so the ore veins follow the preset's large ore veins setting, then the strata
    public static Holder<MaterialRule> overworld(Preset preset, HolderGetter<MaterialRule> rules, HolderGetter<Noise> noise) {
    	List<MaterialRule> sequence = new ArrayList<>();
    	sequence.add(MaterialRules.getRule(rules, VanillaMaterialRules.BEDROCK_FLOOR));
    	if(preset.caves().largeOreVeins) {
    		sequence.add(MaterialRules.getRule(rules, vanillaRule("overworld/copper_ore_vein")));
    		sequence.add(MaterialRules.getRule(rules, vanillaRule("overworld/iron_ore_vein")));
    	}
    	sequence.add(MaterialRules.ifTrue(MaterialRules.abovePreliminarySurface(), MaterialRules.getRule(rules, vanillaRule("overworld/surface"))));
    	sequence.add(MaterialRules.getRule(rules, vanillaRule("overworld/underground")));
    	// the strata only with the strata decorator on, as in ReTerraForged 0.0.6 (the 1.21.1 port always added them)
    	if(preset.miscellaneous().strataDecorator) {
    		sequence.add(makeStrataRule(noise));
    	}
		return Holder.direct(MaterialRules.sequence(sequence));
    }

    private static ResourceKey<MaterialRule> vanillaRule(String name) {
    	return ResourceKey.create(Registries.MATERIAL_RULE, Identifier.withDefaultNamespace(name));
    }
    
	private static MaterialRule makeStrataRule(HolderGetter<Noise> noise) {
		Holder<Noise> depth = noise.getOrThrow(PresetStrataNoise.STRATA_DEPTH);
		
		List<Strata> strata = new ArrayList<>();
		strata.add(new Strata(RTFBlockTags.SOIL, depth, 3, 0, 1, 0.1F, 0.25F));
		strata.add(new Strata(RTFBlockTags.SEDIMENT, depth, 3, 0, 2, 0.05F, 0.15F));
		strata.add(new Strata(RTFBlockTags.CLAY, depth, 3, 0, 2, 0.05F, 0.1F));
		strata.add(new Strata(RTFBlockTags.ROCK, depth, 3, 10, 30, 0.1F, 1.5F));
		return RTFSurfaceRules.strata(RTFCommon.location("overworld_strata"), noise.getOrThrow(PresetStrataNoise.STRATA_SELECTOR), strata, 100);
	}
}
