package raccoonman.reterraforged.mixin;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

//TODO do this with access wideners instead
@Deprecated
@Mixin(BiomeGenerationSettings.class)
public interface MixinBiomeGenerationSettings {
	@Accessor
	List<HolderSet<PlacedFeature>> getFeatures();

	// 26.1: the flower features are the bone meal features (feature tag can_spawn_from_bone_meal)
	@Mutable
	@Accessor("boneMealFeatures")
	void setFlowerFeatures(Supplier<List<Feature>> flowerFeatures);
}
