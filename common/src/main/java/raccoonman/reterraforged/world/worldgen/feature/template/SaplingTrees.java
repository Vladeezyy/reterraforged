package raccoonman.reterraforged.world.worldgen.feature.template;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import raccoonman.reterraforged.data.worldgen.preset.PresetConfiguredFeatures;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.feature.template.template.TemplateContext;

/**
 * The mod's trees grown from saplings (#41): with the preset's custom biome features, which put the mod's template
 * trees in place of Minecraft's, a sapling grows one of them too, not Minecraft's tree. Minecraft still decides
 * everything else (TreeGrower.growTree, MixinTreeGrower): which tree, a 2 x 2 one from four saplings, the one with a
 * bee nest next to flowers, the saplings taken away first and put back when the tree does not fit, and on NeoForge
 * the event that lets other mods change the tree. Only Minecraft's own trees are replaced, and only when the event
 * left them so; a sapling of another mod, a cherry, mangrove or azalea grows as before.
 */
public final class SaplingTrees {
	// a vanilla tree's chance of a bee nest grown next to flowers (its *_bees_005 features)
	private static final float BEES = 0.05F;
	private static final Map<ResourceKey<Feature>, Tree> TREES = Map.ofEntries(
		Map.entry(TreeFeatures.OAK, new Tree(false, 0.0F, PresetConfiguredFeatures.OAK_SMALL, PresetConfiguredFeatures.OAK_FOREST)),
		Map.entry(TreeFeatures.OAK_BEES_005, new Tree(false, BEES, PresetConfiguredFeatures.OAK_SMALL, PresetConfiguredFeatures.OAK_FOREST)),
		Map.entry(TreeFeatures.FANCY_OAK, new Tree(false, 0.0F, PresetConfiguredFeatures.OAK_LARGE)),
		Map.entry(TreeFeatures.FANCY_OAK_BEES_005, new Tree(false, BEES, PresetConfiguredFeatures.OAK_LARGE)),
		Map.entry(TreeFeatures.BIRCH, new Tree(false, 0.0F, PresetConfiguredFeatures.BIRCH_SMALL, PresetConfiguredFeatures.BIRCH_FOREST)),
		Map.entry(TreeFeatures.BIRCH_BEES_005, new Tree(false, BEES, PresetConfiguredFeatures.BIRCH_SMALL, PresetConfiguredFeatures.BIRCH_FOREST)),
		Map.entry(TreeFeatures.SPRUCE, new Tree(false, 0.0F, PresetConfiguredFeatures.SPRUCE_SMALL, PresetConfiguredFeatures.SPRUCE_LARGE)),
		Map.entry(TreeFeatures.MEGA_SPRUCE, new Tree(true, 0.0F, PresetConfiguredFeatures.REDWOOD_LARGE)),
		Map.entry(TreeFeatures.MEGA_PINE, new Tree(true, 0.0F, PresetConfiguredFeatures.REDWOOD_LARGE)),
		Map.entry(TreeFeatures.JUNGLE_TREE_NO_VINE, new Tree(false, 0.0F, PresetConfiguredFeatures.JUNGLE_SMALL)),
		Map.entry(TreeFeatures.MEGA_JUNGLE_TREE, new Tree(true, 0.0F, PresetConfiguredFeatures.JUNGLE_LARGE)),
		Map.entry(TreeFeatures.ACACIA, new Tree(false, 0.0F, PresetConfiguredFeatures.ACACIA_SMALL, PresetConfiguredFeatures.ACACIA_LARGE)),
		Map.entry(TreeFeatures.DARK_OAK, new Tree(true, 0.0F, PresetConfiguredFeatures.DARK_OAK_SMALL, PresetConfiguredFeatures.DARK_OAK_LARGE))
	);

	// the mod's trees grown from saplings, for the self test
	public static int grown;

	private SaplingTrees() {
	}

	/**
	 * Grows the mod's tree for Minecraft's tree feature about to be placed by a sapling at pos (the north-west sapling
	 * of a 2 x 2 one), or returns null when that stays Minecraft's tree.
	 */
	public static Boolean grow(WorldGenLevel level, Feature feature, RandomSource random, BlockPos pos) {
		if (!(level instanceof ServerLevel serverLevel) || !((Object) serverLevel.getChunkSource().randomState() instanceof RTFRandomState state) || state.preset() == null || !state.preset().miscellaneous().customBiomeFeatures) {
			return null;
		}
		Registry<Feature> features = serverLevel.registryAccess().lookupOrThrow(Registries.FEATURE);
		Tree tree = features.getResourceKey(feature).map(TREES::get).orElse(null);
		if (tree == null) {
			return null;
		}
		// the world's own tree: a world made by an older version, or a datapack, may have none or another feature
		Feature template = features.getValue(tree.templates().get(random.nextInt(tree.templates().size())));
		if (!(template instanceof TemplateFeature templateFeature)) {
			return null;
		}
		boolean placed = TemplateFeature.growSapling(serverLevel, random, pos, withoutBushes(templateFeature.config()), tree.twoByTwo(), tree.beeNest());
		if (placed) {
			grown++;
		}
		return placed;
	}

	// a sapling grows a tree, as Minecraft's: not one of the bushes 3 or 4 blocks high the small trees' lists have
	private static <T extends TemplateContext> TemplateFeature.Config<T> withoutBushes(TemplateFeature.Config<T> config) {
		List<Identifier> trees = config.templates().stream().filter((template) -> !template.getPath().contains("bush")).toList();
		return trees.isEmpty() || trees.size() == config.templates().size() ? config : new TemplateFeature.Config<>(trees, config.placement(), config.paste(), config.decorator());
	}

	private record Tree(boolean twoByTwo, float beeNest, List<ResourceKey<Feature>> templates) {

		@SafeVarargs
		Tree(boolean twoByTwo, float beeNest, ResourceKey<Feature>... templates) {
			this(twoByTwo, beeNest, List.of(templates));
		}
	}
}
