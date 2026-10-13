package raccoonman.reterraforged.world.worldgen.feature.placement;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import raccoonman.reterraforged.platform.RegistryUtil;
import raccoonman.reterraforged.world.worldgen.feature.placement.poisson.FastPoissonModifier;

public class RTFPlacementModifiers {

    public static void bootstrap() {
    	register("blacklist_dimension", BlacklistDimensionFilter.CODEC);
    	register("fast_poission", FastPoissonModifier.CODEC);
    	register("legacy_count_extra", LegacyCountExtraModifier.CODEC);
    }
    
    @SafeVarargs
    @Deprecated(forRemoval = true) // use biome modifiers to remove features instead
	public static BlacklistDimensionFilter blacklistDimensions(ResourceKey<LevelStem>... levels) {
    	return new BlacklistDimensionFilter(ImmutableList.copyOf(levels));
    }
	
    public static FastPoissonModifier poisson(int radius, float scale, float biomeFade, int densityVariationScale, float densityVariation) {
		 return poisson(radius, scale, 0.8F, biomeFade, densityVariationScale, densityVariation);
	}
	
    public static FastPoissonModifier poisson(int radius, float scale, float jitter, float biomeFade, int densityVariationScale, float densityVariation) {
		 return new FastPoissonModifier(radius, scale, jitter, biomeFade, densityVariationScale, densityVariation);
	}
    
    @Deprecated
    public static LegacyCountExtraModifier countExtra(int count, float extraChance, int extraCount) {
    	return new LegacyCountExtraModifier(count, extraChance, extraCount);
    }
    
    private static void register(String name, MapCodec<? extends PlacementModifier> codec) {
		RegistryUtil.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, name, codec);
    }
}
