package raccoonman.reterraforged.world.worldgen.biome.modifier.fabric;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import raccoonman.reterraforged.world.worldgen.biome.modifier.VegetationDensity;

record DensityModifier(VegetationDensity density) implements FabricBiomeModifier {
	public static final MapCodec<DensityModifier> CODEC = VegetationDensity.CODEC.xmap(DensityModifier::new, DensityModifier::density);

	@Override
	public void apply(BiomeSelectionContext selectionContext, BiomeModificationContext modificationContext) {
		if(!VegetationDensity.appliesTo(selectionContext.getBiomeRegistryEntry())) {
			return;
		}

		BiomeGenerationSettings generationSettings = selectionContext.getBiome().getGenerationSettings();
		List<HolderSet<PlacedFeature>> featureSteps = generationSettings.features();
		int index = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
		if (index < featureSteps.size()) {
			featureSteps.set(index, HolderSet.direct(featureSteps.get(index).stream().map(this.density::scale).toList()));
			this.rebuildFlowerFeatures(generationSettings);
		}
	}

	@Override
	public MapCodec<DensityModifier> codec() {
		return CODEC;
	}
}
