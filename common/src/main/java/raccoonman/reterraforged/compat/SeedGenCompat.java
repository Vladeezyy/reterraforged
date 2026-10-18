package raccoonman.reterraforged.compat;

import java.util.concurrent.atomic.AtomicBoolean;

import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.platform.ModLoaderUtil;

/**
 * Distant Horizons SeedGen (mod id seedrenderer) draws distant terrain from the seed. The preset's datapack rewrites the
 * vanilla biomes, so SeedGen places all of their features itself, into level chunks that keep no post-processing
 * marks: Minecraft logs a warning for every such block (millions in a minute, seconds of the world creation spent on
 * logging). Only with SeedGen installed those warnings are dropped; the warning has no other effect. Nothing changes
 * without SeedGen.
 */
public final class SeedGenCompat {
	private static final boolean LOADED = ModLoaderUtil.isLoaded("seedrenderer");
	private static final AtomicBoolean NOTED = new AtomicBoolean();

	private SeedGenCompat() {
	}

	// true: drop Minecraft's "Trying to mark a block for post processing" warning
	public static boolean dropPostProcessingWarning() {
		if (!LOADED) return false;
		if (NOTED.compareAndSet(false, true)) {
			RTFCommon.LOGGER.info("Distant Horizons SeedGen places features into chunks without post-processing; ReTerraForged leaves out Minecraft's warning for each of those blocks");
		}
		return true;
	}
}
