package raccoonman.reterraforged.world.worldgen.biome.modifier.fabric;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;

import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.tags.FeatureTags;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import raccoonman.reterraforged.mixin.MixinBiomeGenerationSettings;
import raccoonman.reterraforged.world.worldgen.biome.modifier.BiomeModifier;

public interface FabricBiomeModifier extends BiomeModifier {
	void apply(BiomeSelectionContext selectionContext, BiomeModificationContext modificationContext);
	
	default void rebuildFlowerFeatures(BiomeGenerationSettings generationSettings) {
		if(generationSettings instanceof MixinBiomeGenerationSettings biomeGenerationSettings) {
			biomeGenerationSettings.setFlowerFeatures(Suppliers.memoize(() -> {
				return biomeGenerationSettings.getFeatures().stream().flatMap(HolderSet::stream).flatMap((placed) -> placed.value().getFeatures()).filter((configuredFeature) -> {
					return configuredFeature.is(FeatureTags.CAN_SPAWN_FROM_BONE_MEAL);
				}).map(Holder::value).collect(ImmutableList.<ConfiguredFeature<?, ?>>toImmutableList());
			}));
		}
	}
}
