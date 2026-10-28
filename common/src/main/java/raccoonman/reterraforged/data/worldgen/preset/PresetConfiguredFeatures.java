package raccoonman.reterraforged.data.worldgen.preset;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.data.worldgen.placement.TreePlacements;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.RandomSelectorFeature;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import raccoonman.reterraforged.world.worldgen.feature.template.decorator.TemplateDecorators;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.data.worldgen.preset.settings.MiscellaneousSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.SurfaceSettings;
import raccoonman.reterraforged.world.worldgen.feature.DecorateSnowFeature;
import raccoonman.reterraforged.world.worldgen.feature.DiskFeature;
import raccoonman.reterraforged.world.worldgen.feature.ErodeFeature;
import raccoonman.reterraforged.world.worldgen.feature.SwampSurfaceFeature;
import raccoonman.reterraforged.world.worldgen.feature.chance.ChanceFeature;
import raccoonman.reterraforged.world.worldgen.feature.chance.ChanceModifier;
import raccoonman.reterraforged.world.worldgen.feature.chance.RTFChanceModifiers;
import raccoonman.reterraforged.world.worldgen.feature.template.TemplateFeature;
import raccoonman.reterraforged.world.worldgen.feature.template.decorator.DecoratorConfig;
import raccoonman.reterraforged.world.worldgen.feature.template.decorator.TemplateDecorator;
import raccoonman.reterraforged.world.worldgen.feature.template.decorator.TreeContext;
import raccoonman.reterraforged.world.worldgen.feature.template.paste.PasteConfig;
import raccoonman.reterraforged.world.worldgen.feature.template.placement.TemplatePlacements;

public class PresetConfiguredFeatures {
	public static final ResourceKey<Feature> ERODE = createKey("erode");
	public static final ResourceKey<Feature> DECORATE_SNOW = createKey("decorate_snow");
	public static final ResourceKey<Feature> SWAMP_SURFACE = createKey("swamp_surface");

	public static final ResourceKey<Feature> OAK_SMALL = createKey("oak/small");
	public static final ResourceKey<Feature> OAK_FOREST = createKey("oak/forest");
	public static final ResourceKey<Feature> OAK_LARGE = createKey("oak/large");
	public static final ResourceKey<Feature> BIRCH_SMALL = createKey("birch/small");
	public static final ResourceKey<Feature> BIRCH_FOREST = createKey("birch/forest");
	public static final ResourceKey<Feature> BIRCH_LARGE = createKey("birch/large");
	public static final ResourceKey<Feature> ACACIA_BUSH = createKey("acacia/bush");
	public static final ResourceKey<Feature> ACACIA_SMALL = createKey("acacia/small");
	public static final ResourceKey<Feature> ACACIA_LARGE = createKey("acacia/large");
	public static final ResourceKey<Feature> DARK_OAK_SMALL = createKey("dark_oak/small");
	public static final ResourceKey<Feature> DARK_OAK_LARGE = createKey("dark_oak/large");
	public static final ResourceKey<Feature> HUGE_BROWN_MUSHROOM = createKey("mushrooms/huge_brown_mushroom");
	public static final ResourceKey<Feature> HUGE_RED_MUSHROOM = createKey("mushrooms/huge_red_mushroom");
	public static final ResourceKey<Feature> WILLOW_SMALL = createKey("willow/small");
	public static final ResourceKey<Feature> WILLOW_LARGE = createKey("willow/large");
	public static final ResourceKey<Feature> PINE = createKey("pine");
	public static final ResourceKey<Feature> SPRUCE_SMALL = createKey("spruce/small");
	public static final ResourceKey<Feature> SPRUCE_LARGE = createKey("spruce/large");
	public static final ResourceKey<Feature> REDWOOD_LARGE = createKey("redwood/large");
	public static final ResourceKey<Feature> REDWOOD_HUGE = createKey("redwood/huge");
	public static final ResourceKey<Feature> JUNGLE_SMALL = createKey("jungle/small");
	public static final ResourceKey<Feature> JUNGLE_LARGE = createKey("jungle/large");
	public static final ResourceKey<Feature> JUNGLE_HUGE = createKey("jungle/huge");


	// a meadow's oak with a bee nest, as the vanilla meadow trees always have one
	public static final ResourceKey<Feature> MEADOW_TREE = createKey("oak/meadow");

	public static final ResourceKey<Feature> FOREST_GRASS = createKey("forest_grass");
	public static final ResourceKey<Feature> BIRCH_FOREST_GRASS = createKey("birch_forest_grass");
	
	public static final ResourceKey<Feature> PLAINS_TREES = createKey("plains_trees");
	public static final ResourceKey<Feature> FOREST_TREES = createKey("forest_trees");
	public static final ResourceKey<Feature> FLOWER_FOREST_TREES = createKey("flower_forest_trees");
	public static final ResourceKey<Feature> BIRCH_TREES = createKey("birch_trees");
	public static final ResourceKey<Feature> DARK_FOREST_TREES = createKey("dark_forest_trees");
	public static final ResourceKey<Feature> SAVANNA_TREES = createKey("savanna_trees");
	public static final ResourceKey<Feature> WOODED_BADLANDS_TREES = createKey("wooded_badlands_trees");
	public static final ResourceKey<Feature> SWAMP_TREES = createKey("swamp_trees");
	public static final ResourceKey<Feature> FIR_TREES = createKey("fir_trees");
	public static final ResourceKey<Feature> SPRUCE_TREES = createKey("spruce_trees");
	public static final ResourceKey<Feature> SPRUCE_TUNDRA_TREES = createKey("spruce_tundra_trees");
	public static final ResourceKey<Feature> REDWOOD_TREES = createKey("redwood_trees");
	public static final ResourceKey<Feature> JUNGLE_TREES = createKey("jungle_trees");
	public static final ResourceKey<Feature> JUNGLE_EDGE_TREES = createKey("jungle_edge_trees");
	
	public static void bootstrap(Preset preset, BootstrapContext<Feature> ctx) {
		MiscellaneousSettings miscellaneous = preset.miscellaneous();
		SurfaceSettings surface = preset.surface();
		SurfaceSettings.Erosion erosion = surface.erosion();
		
		ErodeFeature.Config erodeConfig = ErodeFeature.Config.of(preset);
		if(miscellaneous.erosionDecorator) {
			ctx.register(ERODE, new ErodeFeature(erodeConfig));
		}
		
		if(miscellaneous.naturalSnowDecorator || miscellaneous.smoothLayerDecorator || miscellaneous.snowLine.enabled) {
			ctx.register(DECORATE_SNOW, new DecorateSnowFeature(new DecorateSnowFeature.Config(miscellaneous.naturalSnowDecorator, miscellaneous.smoothLayerDecorator, erodeConfig, miscellaneous.snowLine.enabled ? miscellaneous.snowLine.height : -1, miscellaneous.snowLine.variation)));
		}
		
		ctx.register(SWAMP_SURFACE, new SwampSurfaceFeature(new SwampSurfaceFeature.Config(Blocks.CLAY.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), Blocks.DIRT.defaultBlockState())));
		
		if(miscellaneous.customBiomeFeatures) {
			HolderGetter<PlacedFeature> placedFeatures = ctx.lookup(Registries.PLACED_FEATURE);
			Holder<PlacedFeature> oakSmall = placedFeatures.getOrThrow(PresetPlacedFeatures.OAK_SMALL);
			Holder<PlacedFeature> oakForest = placedFeatures.getOrThrow(PresetPlacedFeatures.OAK_FOREST);
			Holder<PlacedFeature> oakLarge = placedFeatures.getOrThrow(PresetPlacedFeatures.OAK_LARGE);
			Holder<PlacedFeature> birchSmall = placedFeatures.getOrThrow(PresetPlacedFeatures.BIRCH_SMALL);
			Holder<PlacedFeature> birchForest = placedFeatures.getOrThrow(PresetPlacedFeatures.BIRCH_FOREST);
			Holder<PlacedFeature> birchLarge = placedFeatures.getOrThrow(PresetPlacedFeatures.BIRCH_LARGE);
			Holder<PlacedFeature> acaciaBush = placedFeatures.getOrThrow(PresetPlacedFeatures.ACACIA_BUSH);
			Holder<PlacedFeature> acaciaSmall = placedFeatures.getOrThrow(PresetPlacedFeatures.ACACIA_SMALL);
			Holder<PlacedFeature> acaciaLarge = placedFeatures.getOrThrow(PresetPlacedFeatures.ACACIA_LARGE);
			Holder<PlacedFeature> darkOakSmall = placedFeatures.getOrThrow(PresetPlacedFeatures.DARK_OAK_SMALL);
			Holder<PlacedFeature> darkOakLarge = placedFeatures.getOrThrow(PresetPlacedFeatures.DARK_OAK_LARGE);
			Holder<PlacedFeature> hugeBrownMushroom = placedFeatures.getOrThrow(PresetPlacedFeatures.HUGE_BROWN_MUSHROOM);
			Holder<PlacedFeature> hugeRedMushroom = placedFeatures.getOrThrow(PresetPlacedFeatures.HUGE_RED_MUSHROOM);
			Holder<PlacedFeature> willowSmall = placedFeatures.getOrThrow(PresetPlacedFeatures.WILLOW_SMALL);
			Holder<PlacedFeature> willowLarge = placedFeatures.getOrThrow(PresetPlacedFeatures.WILLOW_LARGE);
			Holder<PlacedFeature> spruceSmall = placedFeatures.getOrThrow(PresetPlacedFeatures.SPRUCE_SMALL);
			Holder<PlacedFeature> spruceLarge = placedFeatures.getOrThrow(PresetPlacedFeatures.SPRUCE_LARGE);
			Holder<PlacedFeature> redwoodLarge = placedFeatures.getOrThrow(PresetPlacedFeatures.REDWOOD_LARGE);
			Holder<PlacedFeature> redwoodHuge = placedFeatures.getOrThrow(PresetPlacedFeatures.REDWOOD_HUGE);
			Holder<PlacedFeature> jungleSmall = placedFeatures.getOrThrow(PresetPlacedFeatures.JUNGLE_SMALL);
			Holder<PlacedFeature> jungleLarge = placedFeatures.getOrThrow(PresetPlacedFeatures.JUNGLE_LARGE);
			Holder<PlacedFeature> jungleHuge = placedFeatures.getOrThrow(PresetPlacedFeatures.JUNGLE_HUGE);
			Holder<PlacedFeature> jungleBush = placedFeatures.getOrThrow(TreePlacements.JUNGLE_BUSH);

			ctx.register(OAK_SMALL, new TemplateFeature(makeTree(PresetTemplatePaths.OAK_SMALL)));
			ctx.register(MEADOW_TREE, new TemplateFeature(makeTree(PresetTemplatePaths.OAK_SMALL, beeNest(1.0F), 3)));
			ctx.register(OAK_FOREST, new TemplateFeature(makeTree(PresetTemplatePaths.OAK_FOREST)));
			ctx.register(OAK_LARGE, new TemplateFeature(makeTree(PresetTemplatePaths.OAK_LARGE)));
			ctx.register(BIRCH_SMALL, new TemplateFeature(makeTree(PresetTemplatePaths.BIRCH_SMALL)));
			ctx.register(BIRCH_FOREST, new TemplateFeature(makeTree(PresetTemplatePaths.BIRCH_FOREST)));
			ctx.register(BIRCH_LARGE, new TemplateFeature(makeTree(PresetTemplatePaths.BIRCH_LARGE)));
			ctx.register(ACACIA_BUSH, new TemplateFeature(makeTree(PresetTemplatePaths.ACACIA_BUSH, 2)));
			ctx.register(ACACIA_SMALL, new TemplateFeature(makeTree(PresetTemplatePaths.ACACIA_SMALL)));
			ctx.register(ACACIA_LARGE, new TemplateFeature(makeTree(PresetTemplatePaths.ACACIA_LARGE)));
			ctx.register(DARK_OAK_SMALL, new TemplateFeature(makeTree(PresetTemplatePaths.DARK_OAK_SMALL)));
			ctx.register(DARK_OAK_LARGE, new TemplateFeature(makeTree(PresetTemplatePaths.DARK_OAK_LARGE)));
			ctx.register(HUGE_BROWN_MUSHROOM, new TemplateFeature(makeTree(PresetTemplatePaths.BROWN_MUSHROOM, 0)));
			ctx.register(HUGE_RED_MUSHROOM, new TemplateFeature(makeTree(PresetTemplatePaths.RED_MUSHROOM)));
			ctx.register(WILLOW_SMALL, new TemplateFeature(makeTree(PresetTemplatePaths.WILLOW_SMALL)));
			ctx.register(WILLOW_LARGE, new TemplateFeature(makeTree(PresetTemplatePaths.WILLOW_LARGE)));
			ctx.register(PINE, new TemplateFeature(makeTree(PresetTemplatePaths.PINE)));
			ctx.register(SPRUCE_SMALL, new TemplateFeature(makeTree(PresetTemplatePaths.SPRUCE_SMALL)));
			ctx.register(SPRUCE_LARGE, new TemplateFeature(makeTree(PresetTemplatePaths.SPRUCE_LARGE)));
			ctx.register(REDWOOD_LARGE, new TemplateFeature(makeTree(PresetTemplatePaths.REDWOOD_LARGE)));
			ctx.register(REDWOOD_HUGE, new TemplateFeature(makeTree(PresetTemplatePaths.REDWOOD_HUGE)));
			ctx.register(JUNGLE_SMALL, new TemplateFeature(makeTree(PresetTemplatePaths.JUNGLE_SMALL)));
			ctx.register(JUNGLE_LARGE, new TemplateFeature(makeTree(PresetTemplatePaths.JUNGLE_LARGE)));
			ctx.register(JUNGLE_HUGE, new TemplateFeature(makeTree(PresetTemplatePaths.JUNGLE_HUGE)));
			
			
			// bee nests, as on the vanilla trees these replace (plains 5 %, flower forests 2 %, forests and birch
			// forests 0.2 %): the decorator was there but never used, not in 0.0.6 either, and those biomes had no bees
			Holder<PlacedFeature> oakForestBees005 = makeTreeWithBees(PresetTemplatePaths.OAK_FOREST, 0.05F, Blocks.OAK_SAPLING);
			Holder<PlacedFeature> oakLargeBees005 = makeTreeWithBees(PresetTemplatePaths.OAK_LARGE, 0.05F, Blocks.OAK_SAPLING);
			ctx.register(PLAINS_TREES, makeRandom(oakForestBees005, List.of(
				makeWeighted(0.2F, oakForestBees005), 
				makeWeighted(0.3F, oakLargeBees005)
			)));
			Holder<PlacedFeature> oakForestBees0002 = makeTreeWithBees(PresetTemplatePaths.OAK_FOREST, 0.002F, Blocks.OAK_SAPLING);
			Holder<PlacedFeature> oakLargeBees0002 = makeTreeWithBees(PresetTemplatePaths.OAK_LARGE, 0.002F, Blocks.OAK_SAPLING);
			ctx.register(FOREST_TREES, makeRandom(oakForestBees0002, List.of(
				makeWeighted(0.2F, oakForestBees0002), 
				makeWeighted(0.3F, oakLargeBees0002)
			)));
			Holder<PlacedFeature> birchForestBees002 = makeTreeWithBees(PresetTemplatePaths.BIRCH_FOREST, 0.02F, Blocks.BIRCH_SAPLING);
			ctx.register(FLOWER_FOREST_TREES, makeRandom(birchForestBees002, List.of(
				makeWeighted(0.2F, birchForestBees002), 
				makeWeighted(0.2F, makeTreeWithBees(PresetTemplatePaths.BIRCH_LARGE, 0.02F, Blocks.BIRCH_SAPLING)), 
				makeWeighted(0.2F, makeTreeWithBees(PresetTemplatePaths.OAK_FOREST, 0.02F, Blocks.OAK_SAPLING)), 
				makeWeighted(0.2F, makeTreeWithBees(PresetTemplatePaths.OAK_LARGE, 0.02F, Blocks.OAK_SAPLING))
			)));
			ctx.register(BIRCH_TREES, new ChanceFeature(makeChance(
				makeChanceEntry(makeTreeWithBees(PresetTemplatePaths.BIRCH_LARGE, 0.002F, Blocks.BIRCH_SAPLING), 0.2F, RTFChanceModifiers.elevation(0.25F, 0.0F), RTFChanceModifiers.biomeEdge(0.1F, 0.3F)),
				makeChanceEntry(makeTreeWithBees(PresetTemplatePaths.BIRCH_FOREST, 0.002F, Blocks.BIRCH_SAPLING), 0.2F, RTFChanceModifiers.elevation(0.3F, 0.0F), RTFChanceModifiers.biomeEdge(0.05F, 0.2F)),
				makeChanceEntry(makeTreeWithBees(PresetTemplatePaths.BIRCH_SMALL, 0.002F, Blocks.BIRCH_SAPLING), 0.1F, RTFChanceModifiers.biomeEdge(0.25F, 0.0F)),
				makeChanceEntry(makeTreeWithBees(PresetTemplatePaths.BIRCH_SMALL, 0.002F, Blocks.BIRCH_SAPLING), 0.1F, RTFChanceModifiers.elevation(0.25F, 0.65F))
			)));
			ctx.register(DARK_FOREST_TREES, makeRandom(darkOakLarge, List.of(
				makeWeighted(0.025F, hugeBrownMushroom),
				makeWeighted(0.05F, hugeRedMushroom),
				makeWeighted(0.3F, darkOakLarge),
				makeWeighted(0.2F, darkOakSmall),
				makeWeighted(0.05F, birchForest),
				makeWeighted(0.025F, oakForest)
			)));
			ctx.register(SAVANNA_TREES, makeRandom(acaciaLarge, List.of(
				makeWeighted(0.4F, acaciaLarge), 
				makeWeighted(0.15F, acaciaSmall)
			)));
			ctx.register(WOODED_BADLANDS_TREES, makeRandom(oakSmall, List.of(
				makeWeighted(0.3F, oakSmall), 
				makeWeighted(0.2F, acaciaBush)
			)));
			ctx.register(SWAMP_TREES, makeRandom(willowLarge, List.of(
				makeWeighted(0.2F, willowSmall), 
				makeWeighted(0.35F, willowLarge)
			)));
			ctx.register(FIR_TREES, new ChanceFeature(makeChance(
				makeChanceEntry(spruceSmall, 0.1F, RTFChanceModifiers.elevation(0.55F, 0.2F)),
				makeChanceEntry(spruceLarge, 0.25F, RTFChanceModifiers.elevation(0.3F, 0.0F))
			)));
			ctx.register(REDWOOD_TREES, new ChanceFeature(makeChance(
				makeChanceEntry(redwoodHuge, 0.4F, RTFChanceModifiers.elevation(0.15F, 0.0F), RTFChanceModifiers.biomeEdge(0.1F, 0.3F)),
				makeChanceEntry(redwoodLarge, 0.2F, RTFChanceModifiers.elevation(0.25F, 0.0F), RTFChanceModifiers.biomeEdge(0.05F, 0.25F)),
				makeChanceEntry(spruceLarge, 0.4F, RTFChanceModifiers.elevation(0.35F, 0.15F)),
				makeChanceEntry(spruceSmall, 0.2F, RTFChanceModifiers.elevation(0.5F, 0.2F))
			)));
			ctx.register(JUNGLE_TREES, makeRandom(jungleSmall, List.of(
				makeWeighted(0.2F, jungleSmall), 
				makeWeighted(0.3F, jungleLarge),
				makeWeighted(0.4F, jungleHuge),
				makeWeighted(0.5F, jungleBush)
			)));
			ctx.register(JUNGLE_EDGE_TREES, makeRandom(jungleSmall, List.of(
				makeWeighted(0.2F, jungleSmall), 
				makeWeighted(0.3F, jungleLarge),
				makeWeighted(0.4F, jungleBush)
			)));
			ctx.register(FOREST_GRASS, makeRandom(
				makePatch(Blocks.SHORT_GRASS, 48),
				List.of(
					makeWeighted(0.5F, makePatch(Blocks.SHORT_GRASS, 56)),
					makeWeighted(0.4F, makePatch(Blocks.TALL_GRASS, 56)),
					makeWeighted(0.2F, makePatch(Blocks.LARGE_FERN, 48)),
					makeWeighted(0.2F, makePatch(Blocks.FERN, 24))
				)
			));
			ctx.register(BIRCH_FOREST_GRASS, makeRandom(
				makePatch(Blocks.TALL_GRASS, 48),
				List.of(
					makeWeighted(0.8F, makePatch(Blocks.TALL_GRASS, 56)),
					makeWeighted(0.5F, makePatch(Blocks.LILAC, 64)),
					makeWeighted(0.3F, makePatch(Blocks.LARGE_FERN, 48)),
					makeWeighted(0.2F, makePatch(Blocks.FERN, 24)),
					makeWeighted(0.1F, makePatch(Blocks.PEONY, 32))
				)
			));

	        ctx.register(MiscOverworldFeatures.DISK_CLAY, new DiskFeature(BlockStateProvider.holderOf(Blocks.CLAY), BlockPredicate.matchesBlocks(Blocks.DIRT, Blocks.CLAY), UniformInt.of(2, 3), 1));
	        ctx.register(MiscOverworldFeatures.DISK_GRAVEL, new DiskFeature(BlockStateProvider.holderOf(Blocks.GRAVEL), BlockPredicate.matchesBlocks(Blocks.DIRT, Blocks.GRASS_BLOCK), UniformInt.of(2, 5), 2));
	        ctx.register(MiscOverworldFeatures.DISK_SAND, new DiskFeature(Holder.direct(new RuleBasedStateProvider(BlockStateProvider.holderOf(Blocks.SAND), List.of(new RuleBasedStateProvider.Rule(BlockPredicate.matchesBlocks(Direction.DOWN.getUnitVec3i(), List.of(Blocks.AIR)), BlockStateProvider.holderOf(Blocks.SANDSTONE))))), BlockPredicate.matchesBlocks(Blocks.DIRT, Blocks.GRASS_BLOCK), UniformInt.of(2, 6), 2));
	    }
	}

	
	private static TemplateFeature.Config<?> makeTree(List<Identifier> templates) {
		return makeTree(templates, 3);
	}

	private static TemplateFeature.Config<?> makeTree(List<Identifier> templates, int baseExtension) {
		return makeTree(templates, ImmutableList.of(), baseExtension);
	}
	
	private static List<TemplateDecorator<TreeContext>> beeNest(float chance) {
		return ImmutableList.of(TemplateDecorators.beeNest(chance));
	}

	// a tree of a biome's trees with its chance of a bee nest, placed where its sapling would grow
	private static Holder<PlacedFeature> makeTreeWithBees(List<Identifier> templates, float chance, Block sapling) {
		return Holder.direct(new PlacedFeature(Holder.direct(new TemplateFeature(makeTree(templates, beeNest(chance), 3))), ImmutableList.of(PlacementUtils.filteredByBlockSurvival(sapling))));
	}

	private static TemplateFeature.Config<?> makeTree(List<Identifier> templates, List<TemplateDecorator<TreeContext>> decorators, int baseExtension) {
		return new TemplateFeature.Config<>(templates, TemplatePlacements.tree(), new PasteConfig(baseExtension, false, true, false, false), new DecoratorConfig<>(decorators, ImmutableMap.of()));
	}
	
	private static ChanceFeature.Config makeChance(ChanceFeature.Entry... entries) {
		return new ChanceFeature.Config(ImmutableList.copyOf(entries));
	}

	private static ChanceFeature.Entry makeChanceEntry(Holder<PlacedFeature> feature, float chance, ChanceModifier... modifiers) {
		return new ChanceFeature.Entry(feature, chance, ImmutableList.copyOf(modifiers));
	}
	
	private static RandomSelectorFeature makeRandom(Holder<PlacedFeature> defaultFeature, List<WeightedPlacedFeature> entries) {
		return new RandomSelectorFeature(entries, defaultFeature);
	}

    private static WeightedPlacedFeature makeWeighted(float weight, Holder<PlacedFeature> feature) {
    	return new WeightedPlacedFeature(feature, weight);
    }
    
    // 26.1 has no random patch feature: the block with the same tries, spread (7 / 3, triangular) and "only in air"
    // as placement modifiers, as vanilla's patches are now
    private static Holder<PlacedFeature> makePatch(Block state, int tries) {
        return Holder.direct(new PlacedFeature(Holder.direct(new SimpleBlockFeature(BlockStateProvider.of(state))), List.of(
            CountPlacement.of(tries),
            OffsetPlacement.ofTriangle(7, 3),
            BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
        )));
    }
    
	protected static ResourceKey<Feature> createKey(String name) {
        return ResourceKey.create(Registries.FEATURE, RTFCommon.location(name));
	}
}
