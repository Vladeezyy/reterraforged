package raccoonman.reterraforged.data.worldgen.preset;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.data.worldgen.features.OreFeatures;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.data.worldgen.preset.settings.MiscellaneousSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.world.worldgen.feature.placement.RTFPlacementModifiers;

public class PresetPlacedFeatures {
	public static final ResourceKey<PlacedFeature> ERODE = createKey("erode");
	public static final ResourceKey<PlacedFeature> DECORATE_SNOW = createKey("decorate_snow");
	public static final ResourceKey<PlacedFeature> SWAMP_SURFACE = createKey("swamp_surface");

	public static final ResourceKey<PlacedFeature> OAK_SMALL = createKey("oak/small");
	public static final ResourceKey<PlacedFeature> OAK_FOREST = createKey("oak/forest");
	public static final ResourceKey<PlacedFeature> OAK_LARGE = createKey("oak/large");
	public static final ResourceKey<PlacedFeature> BIRCH_SMALL = createKey("birch/small");
	public static final ResourceKey<PlacedFeature> BIRCH_FOREST = createKey("birch/forest");
	public static final ResourceKey<PlacedFeature> BIRCH_LARGE = createKey("birch/large");
	public static final ResourceKey<PlacedFeature> ACACIA_BUSH = createKey("acacia/bush");
	public static final ResourceKey<PlacedFeature> ACACIA_SMALL = createKey("acacia/small");
	public static final ResourceKey<PlacedFeature> ACACIA_LARGE = createKey("acacia/large");
	public static final ResourceKey<PlacedFeature> DARK_OAK_SMALL = createKey("dark_oak/small");
	public static final ResourceKey<PlacedFeature> DARK_OAK_LARGE = createKey("dark_oak/large");
	public static final ResourceKey<PlacedFeature> HUGE_BROWN_MUSHROOM = createKey("mushrooms/huge_brown_mushroom");
	public static final ResourceKey<PlacedFeature> HUGE_RED_MUSHROOM = createKey("mushrooms/huge_red_mushroom");
	public static final ResourceKey<PlacedFeature> WILLOW_SMALL = createKey("willow/small");
	public static final ResourceKey<PlacedFeature> WILLOW_LARGE = createKey("willow/large");
	public static final ResourceKey<PlacedFeature> PINE = createKey("pine");
	public static final ResourceKey<PlacedFeature> SPRUCE_SMALL = createKey("spruce/small");
	public static final ResourceKey<PlacedFeature> SPRUCE_LARGE = createKey("spruce/large");
	public static final ResourceKey<PlacedFeature> REDWOOD_LARGE = createKey("redwood/large");
	public static final ResourceKey<PlacedFeature> REDWOOD_HUGE = createKey("redwood/huge");
	public static final ResourceKey<PlacedFeature> JUNGLE_SMALL = createKey("jungle/small");
	public static final ResourceKey<PlacedFeature> JUNGLE_LARGE = createKey("jungle/large");
	public static final ResourceKey<PlacedFeature> JUNGLE_HUGE = createKey("jungle/huge");


	public static final ResourceKey<PlacedFeature> FOREST_GRASS = createKey("forest_grass");
	public static final ResourceKey<PlacedFeature> BIRCH_FOREST_GRASS = createKey("birch_forest_grass");
	
	public static final ResourceKey<PlacedFeature> PLAINS_TREES = createKey("plains_trees");
	public static final ResourceKey<PlacedFeature> FOREST_TREES = createKey("forest_trees");
	public static final ResourceKey<PlacedFeature> FLOWER_FOREST_TREES = createKey("flower_forest_trees");
	public static final ResourceKey<PlacedFeature> BIRCH_TREES = createKey("birch_trees");
	public static final ResourceKey<PlacedFeature> DARK_FOREST_TREES = createKey("dark_forest_trees");
	public static final ResourceKey<PlacedFeature> WOODED_BADLANDS_TREES = createKey("wooded_badlands_trees");
	public static final ResourceKey<PlacedFeature> SAVANNA_TREES = createKey("savanna_trees");
	public static final ResourceKey<PlacedFeature> SWAMP_TREES = createKey("swamp_trees");
	public static final ResourceKey<PlacedFeature> RIVER_TREES = createKey("river_trees");
	public static final ResourceKey<PlacedFeature> FROZEN_RIVER_TREES = createKey("frozen_river_trees");
	public static final ResourceKey<PlacedFeature> MEADOW_TREES = createKey("meadow_trees");
	public static final ResourceKey<PlacedFeature> FIR_TREES = createKey("fir_trees");
	public static final ResourceKey<PlacedFeature> WINDSWEPT_HILLS_FIR_TREES = createKey("windswept_hills_fir_trees");
	public static final ResourceKey<PlacedFeature> PINE_TREES = createKey("pine_trees");
	public static final ResourceKey<PlacedFeature> SPRUCE_TREES = createKey("spruce_trees");
	public static final ResourceKey<PlacedFeature> SPRUCE_TUNDRA_TREES = createKey("spruce_tundra_trees");
	public static final ResourceKey<PlacedFeature> REDWOOD_TREES = createKey("redwood_trees");
	public static final ResourceKey<PlacedFeature> JUNGLE_TREES = createKey("jungle_trees");
	public static final ResourceKey<PlacedFeature> JUNGLE_EDGE_TREES = createKey("jungle_edge_trees");
	public static final ResourceKey<PlacedFeature> BAMBOO_JUNGLE_TREES = createKey("bamboo_jungle_trees");
    
	public static void bootstrap(Preset preset, BootstrapContext<PlacedFeature> ctx) {
		HolderGetter<Feature> features = ctx.lookup(Registries.FEATURE);
		MiscellaneousSettings miscellaneous = preset.miscellaneous();

		PlacementModifier blacklistOverworld = RTFPlacementModifiers.blacklistDimensions(LevelStem.OVERWORLD);
		
		if(miscellaneous.erosionDecorator) {
			PlacementUtils.register(ctx, ERODE, features.getOrThrow(PresetConfiguredFeatures.ERODE));
		}

		if(miscellaneous.naturalSnowDecorator || miscellaneous.smoothLayerDecorator || miscellaneous.snowLine.enabled) {
			PlacementUtils.register(ctx, DECORATE_SNOW, features.getOrThrow(PresetConfiguredFeatures.DECORATE_SNOW));
		}
		
		PlacementUtils.register(ctx, SWAMP_SURFACE, features.getOrThrow(PresetConfiguredFeatures.SWAMP_SURFACE));
		
        if(!miscellaneous.vanillaSprings) {
        	PlacementUtils.register(ctx, MiscOverworldPlacements.SPRING_WATER, features.getOrThrow(MiscOverworldFeatures.SPRING_WATER), blacklistOverworld);
        }
        
        if(!miscellaneous.vanillaLavaSprings) {
        	PlacementUtils.register(ctx, MiscOverworldPlacements.SPRING_LAVA, features.getOrThrow(MiscOverworldFeatures.SPRING_LAVA_OVERWORLD), blacklistOverworld);
        	PlacementUtils.register(ctx, MiscOverworldPlacements.SPRING_LAVA_FROZEN, features.getOrThrow(MiscOverworldFeatures.SPRING_LAVA_FROZEN), blacklistOverworld);
        }
        
        if(!miscellaneous.vanillaLavaLakes) {
            PlacementUtils.register(ctx, MiscOverworldPlacements.LAKE_LAVA_SURFACE, features.getOrThrow(MiscOverworldFeatures.LAKE_LAVA), blacklistOverworld);
            PlacementUtils.register(ctx, MiscOverworldPlacements.LAKE_LAVA_UNDERGROUND, features.getOrThrow(MiscOverworldFeatures.LAKE_LAVA), blacklistOverworld);
        }

        if(miscellaneous.strataDecorator) {
            PlacementUtils.register(ctx, OrePlacements.ORE_DIRT, features.getOrThrow(OreFeatures.ORE_DIRT), blacklistOverworld);
            PlacementUtils.register(ctx, OrePlacements.ORE_GRAVEL, features.getOrThrow(OreFeatures.ORE_GRAVEL), blacklistOverworld);
            PlacementUtils.register(ctx, OrePlacements.ORE_GRANITE_UPPER, features.getOrThrow(OreFeatures.ORE_GRANITE), blacklistOverworld);
            PlacementUtils.register(ctx, OrePlacements.ORE_DIORITE_UPPER, features.getOrThrow(OreFeatures.ORE_DIORITE), blacklistOverworld);
            PlacementUtils.register(ctx, OrePlacements.ORE_ANDESITE_UPPER, features.getOrThrow(OreFeatures.ORE_ANDESITE), blacklistOverworld);
            PlacementUtils.register(ctx, OrePlacements.ORE_GRANITE_LOWER, features.getOrThrow(OreFeatures.ORE_GRANITE), blacklistOverworld);
            PlacementUtils.register(ctx, OrePlacements.ORE_DIORITE_LOWER, features.getOrThrow(OreFeatures.ORE_DIORITE), blacklistOverworld);
            PlacementUtils.register(ctx, OrePlacements.ORE_ANDESITE_LOWER, features.getOrThrow(OreFeatures.ORE_ANDESITE), blacklistOverworld);
        }
        
        if(miscellaneous.customBiomeFeatures) {
        	PlacementUtils.register(ctx, OAK_SMALL, features.getOrThrow(PresetConfiguredFeatures.OAK_SMALL), PlacementUtils.filteredByBlockSurvival(Blocks.OAK_SAPLING));
        	PlacementUtils.register(ctx, OAK_FOREST, features.getOrThrow(PresetConfiguredFeatures.OAK_FOREST), PlacementUtils.filteredByBlockSurvival(Blocks.OAK_SAPLING));
            PlacementUtils.register(ctx, OAK_LARGE, features.getOrThrow(PresetConfiguredFeatures.OAK_LARGE), PlacementUtils.filteredByBlockSurvival(Blocks.OAK_SAPLING));
            PlacementUtils.register(ctx, BIRCH_SMALL, features.getOrThrow(PresetConfiguredFeatures.BIRCH_SMALL), PlacementUtils.filteredByBlockSurvival(Blocks.BIRCH_SAPLING));
            PlacementUtils.register(ctx, BIRCH_FOREST, features.getOrThrow(PresetConfiguredFeatures.BIRCH_FOREST), PlacementUtils.filteredByBlockSurvival(Blocks.BIRCH_SAPLING));
            PlacementUtils.register(ctx, BIRCH_LARGE, features.getOrThrow(PresetConfiguredFeatures.BIRCH_LARGE), PlacementUtils.filteredByBlockSurvival(Blocks.BIRCH_SAPLING));
            PlacementUtils.register(ctx, ACACIA_BUSH, features.getOrThrow(PresetConfiguredFeatures.ACACIA_BUSH), PlacementUtils.filteredByBlockSurvival(Blocks.ACACIA_SAPLING));
            PlacementUtils.register(ctx, ACACIA_SMALL, features.getOrThrow(PresetConfiguredFeatures.ACACIA_SMALL), PlacementUtils.filteredByBlockSurvival(Blocks.ACACIA_SAPLING));
            PlacementUtils.register(ctx, ACACIA_LARGE, features.getOrThrow(PresetConfiguredFeatures.ACACIA_LARGE), PlacementUtils.filteredByBlockSurvival(Blocks.ACACIA_SAPLING));
            PlacementUtils.register(ctx, DARK_OAK_SMALL, features.getOrThrow(PresetConfiguredFeatures.DARK_OAK_SMALL), PlacementUtils.filteredByBlockSurvival(Blocks.DARK_OAK_SAPLING));
            PlacementUtils.register(ctx, DARK_OAK_LARGE, features.getOrThrow(PresetConfiguredFeatures.DARK_OAK_LARGE), PlacementUtils.filteredByBlockSurvival(Blocks.DARK_OAK_SAPLING));
            PlacementUtils.register(ctx, HUGE_BROWN_MUSHROOM, features.getOrThrow(PresetConfiguredFeatures.HUGE_BROWN_MUSHROOM));
            PlacementUtils.register(ctx, HUGE_RED_MUSHROOM, features.getOrThrow(PresetConfiguredFeatures.HUGE_RED_MUSHROOM));
            PlacementUtils.register(ctx, WILLOW_SMALL, features.getOrThrow(PresetConfiguredFeatures.WILLOW_SMALL), PlacementUtils.filteredByBlockSurvival(Blocks.OAK_SAPLING));
            PlacementUtils.register(ctx, WILLOW_LARGE, features.getOrThrow(PresetConfiguredFeatures.WILLOW_LARGE), PlacementUtils.filteredByBlockSurvival(Blocks.OAK_SAPLING));
            PlacementUtils.register(ctx, PINE, features.getOrThrow(PresetConfiguredFeatures.PINE), PlacementUtils.filteredByBlockSurvival(Blocks.SPRUCE_SAPLING));
            PlacementUtils.register(ctx, SPRUCE_SMALL, features.getOrThrow(PresetConfiguredFeatures.SPRUCE_SMALL), onSoilOrSnow(Blocks.SPRUCE_SAPLING));
            PlacementUtils.register(ctx, SPRUCE_LARGE, features.getOrThrow(PresetConfiguredFeatures.SPRUCE_LARGE), onSoilOrSnow(Blocks.SPRUCE_SAPLING));
            PlacementUtils.register(ctx, REDWOOD_LARGE, features.getOrThrow(PresetConfiguredFeatures.REDWOOD_LARGE), PlacementUtils.filteredByBlockSurvival(Blocks.SPRUCE_SAPLING));
            PlacementUtils.register(ctx, REDWOOD_HUGE, features.getOrThrow(PresetConfiguredFeatures.REDWOOD_HUGE), PlacementUtils.filteredByBlockSurvival(Blocks.SPRUCE_SAPLING));
            PlacementUtils.register(ctx, JUNGLE_SMALL, features.getOrThrow(PresetConfiguredFeatures.JUNGLE_SMALL), PlacementUtils.filteredByBlockSurvival(Blocks.JUNGLE_SAPLING));
            PlacementUtils.register(ctx, JUNGLE_LARGE, features.getOrThrow(PresetConfiguredFeatures.JUNGLE_LARGE), PlacementUtils.filteredByBlockSurvival(Blocks.JUNGLE_SAPLING));
            PlacementUtils.register(ctx, JUNGLE_HUGE, features.getOrThrow(PresetConfiguredFeatures.JUNGLE_HUGE), PlacementUtils.filteredByBlockSurvival(Blocks.JUNGLE_SAPLING));
            
            
            PlacementUtils.register(ctx, FOREST_GRASS, features.getOrThrow(PresetConfiguredFeatures.FOREST_GRASS), worldSurfaceSquaredWithCount(7));
            PlacementUtils.register(ctx, BIRCH_FOREST_GRASS, features.getOrThrow(PresetConfiguredFeatures.BIRCH_FOREST_GRASS), worldSurfaceSquaredWithCount(5));
            
            // the trees placed by a count or a rarity are spread over the chunk (InSquarePlacement): ReTerraForged 0.0.6
            // put them all on the chunk's corner, so swamps, plains, savannas, meadows, windswept hills and snowy plains
            // had their trees on a 16 block grid (#34). The poisson placements spread their own.
            // The trees' height is the ground's, under grass and leaves (#37). From the top block (0.0.6) a tree under the
            // leaves of another or on a column with grass did not grow: half of the poisson places in forests and jungles
            // stayed empty, the more so the denser their grass.
            // The poisson numbers, against vanilla's trees per chunk and without its clearings (#37): 0.0.6 had half of
            // vanilla's trees, spaced 7 and with a density noise that left thin regions up to 3 times less dense than others.
            PlacementModifier ground = PlacementUtils.HEIGHTMAP_NO_LEAVES;
            float trees = miscellaneous.treeDensity;
            PlacementUtils.register(ctx, PLAINS_TREES, features.getOrThrow(PresetConfiguredFeatures.PLAINS_TREES), RTFPlacementModifiers.countExtra(0, 0.05F, 1), InSquarePlacement.spread(), ground, BiomeFilter.biome());
            // the rivers' own trees (vanilla's trees_water: one tree in about ten chunks, on dry ground by the water) are
            // ours: oaks on rivers, pines on frozen rivers (#34)
            PlacementUtils.register(ctx, RIVER_TREES, features.getOrThrow(PresetConfiguredFeatures.PLAINS_TREES), VegetationPlacements.treePlacement(PlacementUtils.countExtra(0, 0.1F, 1)));
            PlacementUtils.register(ctx, FROZEN_RIVER_TREES, features.getOrThrow(PresetConfiguredFeatures.PINE), VegetationPlacements.treePlacement(PlacementUtils.countExtra(0, 0.1F, 1)));
            PlacementUtils.register(ctx, FOREST_TREES, features.getOrThrow(PresetConfiguredFeatures.FOREST_TREES), poisson(trees, 5, 0.25F, 0.3F, 150, 0.5F), ground, BiomeFilter.biome());
            PlacementUtils.register(ctx, FLOWER_FOREST_TREES, features.getOrThrow(PresetConfiguredFeatures.FLOWER_FOREST_TREES), poisson(trees, 6, 0.25F, 0.1F, 500, 0.5F), ground, BiomeFilter.biome());
            PlacementUtils.register(ctx, BIRCH_TREES, features.getOrThrow(PresetConfiguredFeatures.BIRCH_TREES), poisson(trees, 5, 0.25F, 0.25F, 175, 0.5F), ground, BiomeFilter.biome());
            PlacementUtils.register(ctx, DARK_FOREST_TREES, features.getOrThrow(PresetConfiguredFeatures.DARK_FOREST_TREES), poisson(trees, 5, 0.3F, 0.2F, 300, 0.5F), ground, BiomeFilter.biome());
            PlacementUtils.register(ctx, SAVANNA_TREES, features.getOrThrow(PresetConfiguredFeatures.SAVANNA_TREES), RTFPlacementModifiers.countExtra(1, 0.1F, 1), InSquarePlacement.spread(), ground, BiomeFilter.biome());
            PlacementUtils.register(ctx, WOODED_BADLANDS_TREES, features.getOrThrow(PresetConfiguredFeatures.WOODED_BADLANDS_TREES), poisson(trees, 5, 0.2F, 0.25F, 150, 0.5F), ground, BiomeFilter.biome());
            PlacementUtils.register(ctx, SWAMP_TREES, features.getOrThrow(PresetConfiguredFeatures.SWAMP_TREES), RTFPlacementModifiers.countExtra(3, 0.05F, 1), InSquarePlacement.spread(), ground, BiomeFilter.biome());
            PlacementUtils.register(ctx, MEADOW_TREES, features.getOrThrow(PresetConfiguredFeatures.MEADOW_TREE), RarityFilter.onAverageOnceEvery(30), InSquarePlacement.spread(), ground, BiomeFilter.biome());
            PlacementUtils.register(ctx, FIR_TREES, features.getOrThrow(PresetConfiguredFeatures.FIR_TREES), poisson(trees, 4.5F, 0.25F, 0.3F, 300, 0.5F), ground, BiomeFilter.biome());
            PlacementUtils.register(ctx, WINDSWEPT_HILLS_FIR_TREES, features.getOrThrow(PresetConfiguredFeatures.FIR_TREES), RarityFilter.onAverageOnceEvery(30), InSquarePlacement.spread(), ground, BiomeFilter.biome());
            PlacementUtils.register(ctx, PINE_TREES, features.getOrThrow(PresetConfiguredFeatures.PINE), poisson(trees, 5, 0.25F, 0.25F, 250, 0.5F), ground, BiomeFilter.biome());
            PlacementUtils.register(ctx, SPRUCE_TREES, features.getOrThrow(PresetConfiguredFeatures.PINE), poisson(trees, 5, 0.3F, 0.25F, 250, 0.5F), ground, BiomeFilter.biome());
            PlacementUtils.register(ctx, SPRUCE_TUNDRA_TREES, features.getOrThrow(PresetConfiguredFeatures.PINE), RarityFilter.onAverageOnceEvery(20), InSquarePlacement.spread(), ground, BiomeFilter.biome());
            PlacementUtils.register(ctx, REDWOOD_TREES, features.getOrThrow(PresetConfiguredFeatures.REDWOOD_TREES), poisson(trees, 5, 0.3F, 0.25F, 250, 0.5F), ground, BiomeFilter.biome());
            PlacementUtils.register(ctx, JUNGLE_TREES, features.getOrThrow(PresetConfiguredFeatures.JUNGLE_TREES), poisson(trees, 5, 0.4F, 0.2F, 400, 0.5F), ground, BiomeFilter.biome());
            PlacementUtils.register(ctx, JUNGLE_EDGE_TREES, features.getOrThrow(PresetConfiguredFeatures.JUNGLE_EDGE_TREES), poisson(trees, 8, 0.35F, 0.25F, 350, 0.5F), ground, BiomeFilter.biome());
            // the bamboo has the room, as in vanilla's bamboo jungles: a third of a jungle's trees
            PlacementUtils.register(ctx, BAMBOO_JUNGLE_TREES, features.getOrThrow(PresetConfiguredFeatures.JUNGLE_TREES), poisson(trees, 8, 0.4F, 0.2F, 400, 0.5F), ground, BiomeFilter.biome());

//			TODO shattered savanna
//        	PlacementUtils.register(ctx, VegetationPlacements.TREES_WINDSWEPT_SAVANNA, features.getOrThrow(VegetationFeatures.TREES_SAVANNA), RTFPlacementModifiers.disabled());
        }
	}
	
    // the density of the Tree Density slider in the numbers: the distance between the trees and the grid of the places
    // tried, each by the square root. At 0 the biome modifier takes the feature out (VegetationDensity).
    private static PlacementModifier poisson(float density, float radius, float scale, float biomeFade, int densityVariationScale, float densityVariation) {
        float spacing = density > 0.0F ? (float) Math.sqrt(density) : 1.0F;
        return RTFPlacementModifiers.poisson(radius / spacing, scale * spacing, biomeFade, densityVariationScale, densityVariation);
    }

    // where the sapling would grow or on snow, above the drifts of powder snow, as vanilla's spruces of groves: their ground
    // is snow all over, and there the mod's spruces did not grow at all (#37)
    private static List<PlacementModifier> onSoilOrSnow(Block sapling) {
        return List.of(
            EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.not(BlockPredicate.matchesBlocks(Blocks.POWDER_SNOW)), 8),
            BlockPredicateFilter.forPredicate(BlockPredicate.anyOf(BlockPredicate.wouldSurvive(sapling), BlockPredicate.matchesBlocks(new Vec3i(0, -1, 0), List.of(Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW))))
        );
    }

    public static List<PlacementModifier> worldSurfaceSquaredWithCount(int i) {
        return List.of(CountPlacement.of(i), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome());
    }
	
    private static ResourceKey<PlacedFeature> createKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, RTFCommon.location(name));
    }
}
