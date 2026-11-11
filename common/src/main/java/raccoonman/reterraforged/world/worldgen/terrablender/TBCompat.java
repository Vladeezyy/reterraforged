package raccoonman.reterraforged.world.worldgen.terrablender;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;

import raccoonman.reterraforged.platform.ModLoaderUtil;

public class TBCompat {
	public static final List<String> TERRABLENDER_COMPAT_MIXINS = ImmutableList.of(mixinClass("terrablender.MixinClimateSampler"), mixinClass("terrablender.MixinNoiseChunk"), mixinClass("terrablender.MixinParameterList"), mixinClass("terrablender.MixinTargetPoint"));
	
	// "rtf biomemods probe" (tests only): the regions' trees are searched with the points as they are, on the probe's
	// thread only (a global flag gave the chunks the generation threads made meanwhile the probe's regions, saved so)
	@Nullable
	private static volatile Thread probingThread;

	public static boolean isProbing() {
		Thread thread = probingThread;
		return thread != null && thread == Thread.currentThread();
	}

	public static void setProbing(boolean probing) {
		probingThread = probing ? Thread.currentThread() : null;
	}

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
