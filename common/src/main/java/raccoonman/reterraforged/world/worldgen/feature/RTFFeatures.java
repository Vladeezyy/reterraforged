package raccoonman.reterraforged.world.worldgen.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import raccoonman.reterraforged.platform.RegistryUtil;
import raccoonman.reterraforged.world.worldgen.feature.chance.ChanceFeature;
import raccoonman.reterraforged.world.worldgen.feature.template.TemplateFeature;

// 26.3: a feature is its own configuration, these are the feature types
public class RTFFeatures {
	
	public static void bootstrap() {
		register("template", TemplateFeature.CODEC);
		register("bush", BushFeature.CODEC);
		register("disk", DiskFeature.CODEC);
		register("chance", ChanceFeature.CODEC);
		register("erode", ErodeFeature.CODEC);
		register("decorate_snow", DecorateSnowFeature.CODEC);
		register("swamp_surface", SwampSurfaceFeature.CODEC);
	}
	
	private static void register(String name, MapCodec<? extends Feature> type) {
		RegistryUtil.register(BuiltInRegistries.FEATURE_TYPE, name, type);
	}
}
