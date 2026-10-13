package raccoonman.reterraforged.world.worldgen.terrablender;

import java.util.List;

import com.google.common.collect.ImmutableList;

import raccoonman.reterraforged.platform.ModLoaderUtil;

public class TBCompat {
	public static final List<String> TERRABLENDER_COMPAT_MIXINS = ImmutableList.of(mixinClass("terrablender.MixinClimateSampler"), mixinClass("terrablender.MixinParameterList"), mixinClass("terrablender.MixinTargetPoint"), mixinClass("terrablender.MixinMultiNoiseBiomeSource"));
	
	// the preset's biome region of the place a chunk's biome resolver looks up (MixinMultiNoiseBiomeSource), for points
	// made from climate volumes, which have none
	public static final ThreadLocal<Float> CHUNK_UNIQUENESS = new ThreadLocal<>();

	// "rtf biomemods probe" (tests only): the regions' trees are searched with the points as they are
	public static volatile boolean probing;

	public static boolean isEnabled() {
		return ModLoaderUtil.isLoaded("terrablender");
	}
	
	public static boolean isTBMixin(String mixinClassName) {
		return TERRABLENDER_COMPAT_MIXINS.contains(mixinClassName);
	}
	
	private static String mixinClass(String className) {
		return "raccoonman.reterraforged.mixin." + className;
	}
}
