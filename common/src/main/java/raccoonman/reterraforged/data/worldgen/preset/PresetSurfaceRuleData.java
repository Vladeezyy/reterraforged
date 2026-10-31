package raccoonman.reterraforged.data.worldgen.preset;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.MiscellaneousSettings;
import raccoonman.reterraforged.tags.RTFBlockTags;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.surface.rule.RTFSurfaceRules;
import raccoonman.reterraforged.world.worldgen.surface.rule.StrataRule;
import raccoonman.reterraforged.world.worldgen.surface.rule.StrataRule.Strata;

//TODO add forest surfaces
// maybe have a custom meadow or cherry forest surface ?
public class PresetSurfaceRuleData {
    
    // base: the overworld's surface rules without ours (base(registries)); vanilla's in a world without mods
    public static SurfaceRules.RuleSource overworld(Preset preset, HolderGetter<Noise> noise, SurfaceRules.RuleSource base) {
		// the strata only with the strata decorator on, as in ReTerraForged 0.0.6 (the 1.21.1 port always added them)
		if(!preset.miscellaneous().strataDecorator) {
			return base;
		}
		return SurfaceRules.sequence(
				base,
				makeStrataRule(preset, noise)
		);
    }

    // the surface rules of the overworld noise settings the registries had before the preset: a datapack biome mod
    // (Terralith) writes its biomes' surfaces (sand in its oases and sandstone valleys) there, and replacing them with
    // vanilla's left those biomes grass (#28). Our strata are taken out (the registries of a world that has the preset
    // already); without overworld noise settings, vanilla's.
    public static SurfaceRules.RuleSource base(RegistryAccess registries) {
		SurfaceRules.RuleSource rule = registries.lookup(Registries.NOISE_SETTINGS)
			.flatMap((settings) -> settings.getOptional(NoiseGeneratorSettings.OVERWORLD))
			.map(NoiseGeneratorSettings::surfaceRule)
			.orElse(null);
		if(rule == null) {
			return SurfaceRuleData.overworld();
		}
		if(rule instanceof SurfaceRules.SequenceRuleSource sequence && sequence.sequence().stream().anyMatch((r) -> r instanceof StrataRule)) {
			List<SurfaceRules.RuleSource> without = sequence.sequence().stream().filter((r) -> !(r instanceof StrataRule)).toList();
			return without.size() == 1 ? without.get(0) : SurfaceRules.sequence(without.toArray(SurfaceRules.RuleSource[]::new));
		}
		return rule;
    }
    
	private static SurfaceRules.RuleSource makeStrataRule(Preset preset, HolderGetter<Noise> noise) {
		Holder<Noise> depth = noise.getOrThrow(PresetStrataNoise.STRATA_DEPTH);
		MiscellaneousSettings miscellaneous = preset.miscellaneous();
		MiscellaneousSettings.Strata rock = miscellaneous.strata;
		// "Ore Compatible Stone Only": only the rock that ores generate in (vanilla's four stones are)
		Optional<TagKey<Block>> rockRequired = miscellaneous.oreCompatibleStoneOnly ? Optional.of(RTFBlockTags.ORE_COMPATIBLE) : Optional.empty();
		
		List<Strata> strata = new ArrayList<>();
		strata.add(new Strata(RTFBlockTags.SOIL, depth, 3, 0, 1, 0.1F, 0.25F));
		strata.add(new Strata(RTFBlockTags.SEDIMENT, depth, 3, 0, 2, 0.05F, 0.15F));
		strata.add(new Strata(RTFBlockTags.CLAY, depth, 3, 0, 2, 0.05F, 0.1F));
		strata.add(new Strata(RTFBlockTags.ROCK, depth, 3, Math.min(rock.minLayers, rock.maxLayers), Math.max(rock.minLayers, rock.maxLayers), Math.min(rock.minDepth, rock.maxDepth), Math.max(rock.minDepth, rock.maxDepth), rockRequired));
		return RTFSurfaceRules.strata(RTFCommon.location("overworld_strata"), noise.getOrThrow(PresetStrataNoise.STRATA_SELECTOR), strata, 100);
	}
}
