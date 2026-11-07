package raccoonman.reterraforged.world.worldgen.biome.modifier.neoforge;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo.BiomeInfo;
import raccoonman.reterraforged.neoforge.mixin.MixinBiomeGenerationSettingsPlainsBuilder;
import raccoonman.reterraforged.world.worldgen.biome.modifier.VegetationDensity;

record DensityModifier(VegetationDensity density) implements ForgeBiomeModifier {
	public static final MapCodec<DensityModifier> CODEC = VegetationDensity.CODEC.xmap(DensityModifier::new, DensityModifier::density);

	@Override
	public void modify(Holder<Biome> biome, BiomeModifier.Phase phase, BiomeInfo.Builder builder) {
		if(phase == BiomeModifier.Phase.AFTER_EVERYTHING && VegetationDensity.appliesTo(biome)) {
			if(builder.getGenerationSettings() instanceof MixinBiomeGenerationSettingsPlainsBuilder builderAccessor) {
				List<List<Holder<PlacedFeature>>> featureSteps = builderAccessor.getFeatures();
				int index = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
				if (index < featureSteps.size()) {
					featureSteps.get(index).replaceAll(this.density::scale);
				}
			} else {
				throw new IllegalStateException();
			}
		}
	}

	@Override
	public MapCodec<DensityModifier> codec() {
		return CODEC;
	}
}
