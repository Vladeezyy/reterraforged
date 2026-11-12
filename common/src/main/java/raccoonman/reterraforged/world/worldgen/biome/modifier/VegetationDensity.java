package raccoonman.reterraforged.world.worldgen.biome.modifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import raccoonman.reterraforged.world.worldgen.feature.placement.DensityFilter;
import raccoonman.reterraforged.world.worldgen.feature.placement.poisson.FastPoissonModifier;

/**
 * The density sliders of the preset (#37) on the vegetation of every overworld biome, the biomes of other mods too. The
 * biome modifier is in the preset only when a slider is off 1, so the usual amounts cost nothing. It runs once for each
 * biome when the world loads: a feature to scale gets a DensityFilter, chunks then roll one random number for each of
 * its positions, and only where the preset builds the world (a biome is also in other dimensions, DensityFilter). What a
 * feature is goes by its name, as nothing else tells across Minecraft versions and mods; a feature
 * with an unknown name stays as it is.
 */
public final class VegetationDensity {
	public static final MapCodec<VegetationDensity> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.FLOAT.fieldOf("grass").forGetter((d) -> d.grass),
		Codec.FLOAT.fieldOf("trees").forGetter((d) -> d.trees),
		Codec.FLOAT.fieldOf("plants").forGetter((d) -> d.plants)
	).apply(instance, VegetationDensity::new));

	// not plants of the ground, or under water or in caves: left alone whatever else the name has
	private static final String[] OTHER = { "seagrass", "sea_", "ocean", "underwater", "kelp", "coral", "cave", "azalea", "lichen", "dripleaf", "spore", "sculk", "fungi", "fungus", "crimson", "warped", "nether" };
	private static final String[] TREES = { "tree", "fallen", "dark_forest_vegetation", "pale_garden_vegetation", "birch_tall", "bamboo_vegetation", "mangrove_vegetation" };
	private static final String[] GRASS = { "grass", "fern" };
	private static final String[] PLANTS = { "flower", "patch", "bush", "shrub", "mushroom", "berr", "cactus", "cane", "vine", "lily", "lilies", "petal", "bamboo", "litter", "clover", "reed", "cattail", "pumpkin", "toadstool", "scrub" };

	private final float grass;
	private final float trees;
	private final float plants;
	// a feature has one stand-in for all biomes: the same number of features in the same order for Minecraft's sorting
	private final Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> scaled = new HashMap<>();

	public VegetationDensity(float grass, float trees, float plants) {
		this.grass = grass;
		this.trees = trees;
		this.plants = plants;
	}

	public static boolean appliesTo(Holder<Biome> biome) {
		return !biome.is(BiomeTags.IS_NETHER) && !biome.is(BiomeTags.IS_END);
	}

	public Holder<PlacedFeature> scale(Holder<PlacedFeature> feature) {
		return feature.unwrapKey().map((key) -> this.scaled.computeIfAbsent(key, (k) -> this.scale(feature, this.density(k.identifier().getPath())))).orElse(feature);
	}

	private Holder<PlacedFeature> scale(Holder<PlacedFeature> feature, float density) {
		if (density == 1.0F) {
			return feature;
		}
		PlacedFeature placed = feature.value();
		List<PlacementModifier> placement = new ArrayList<>(placed.placement());
		// the mod's poisson trees have their density in the preset's numbers (PresetPlacedFeatures): a copy of a position
		// would be the same tree again
		if (density > 0.0F && placement.stream().anyMatch(FastPoissonModifier.class::isInstance)) {
			return feature;
		}
		// less: fewer of the places the feature spreads over the chunk, so a patch stays as thick and goes away whole, and
		// the rest of its placement is not worked out. More: its whole placement again, with other random positions. None:
		// nothing to work out in chunks; the feature stays on the biome's list, that is where bone meal finds its flowers
		placement.add(density > 0.0F && density < 1.0F ? afterSpread(placement) : 0, new DensityFilter(density));
		return Holder.direct(new PlacedFeature(placed.feature(), placement));
	}

	private static int afterSpread(List<PlacementModifier> placement) {
		for (int i = 0; i < placement.size(); i++) {
			if (placement.get(i) instanceof InSquarePlacement) {
				return i + 1;
			}
		}
		return placement.size();
	}

	private float density(String path) {
		String name = path.toLowerCase(Locale.ROOT);
		return has(name, OTHER) ? 1.0F : has(name, TREES) ? this.trees : has(name, GRASS) ? this.grass : has(name, PLANTS) ? this.plants : 1.0F;
	}

	private static boolean has(String name, String[] parts) {
		for (String part : parts) {
			if (name.contains(part)) {
				return true;
			}
		}
		return false;
	}
}
