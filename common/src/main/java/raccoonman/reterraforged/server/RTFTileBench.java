package raccoonman.reterraforged.server;

import java.util.Arrays;
import java.util.Locale;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;

/**
 * "rtf tilebench [tiles]" (with -Drtf.selftest=true, tools/tilebench.sh): the time the generator takes for a tile (its
 * cells, rivers, climate and the filters: erosion, smoothing), without Minecraft's own generation. Tiles far apart, one
 * after another, after a warm-up; prints the median and the fastest. Steadier than the chunk benchmark for the mod's
 * own code, whose share of a chunk is small.
 */
public final class RTFTileBench {
	private static final int WARMUP = 12;

	private RTFTileBench() {
	}

	public static String start(MinecraftServer server, int tiles) {
		ServerLevel level = server.overworld();
		if (!((Object) level.getChunkSource().randomState() instanceof RTFRandomState state) || state.generatorContext() == null) {
			return "[RTF tilebench] FAIL not a ReTerraForged world";
		}
		GeneratorContext context = state.generatorContext();
		Thread thread = new Thread(() -> {
			double[] ms = new double[tiles];
			for (int i = -WARMUP; i < tiles; i++) {
				// tiles 40 apart: no river or continent region is shared with the one before
				int tileX = 2000 + 40 * i, tileZ = -3000 + 17 * i;
				long t0 = System.nanoTime();
				context.generator.generate(tileX, tileZ).join();
				long t1 = System.nanoTime();
				if (i >= 0) {
					ms[i] = (t1 - t0) / 1.0E6;
				}
			}
			Arrays.sort(ms);
			String r = String.format(Locale.ROOT, "[RTF tilebench] %d tiles: median %.1f ms, fastest %.1f ms per tile", tiles, ms[tiles / 2], ms[0]);
			RTFCommon.LOGGER.info(r);
		}, "RTF tilebench");
		thread.setDaemon(true);
		thread.start();
		return "[RTF tilebench] started";
	}
}
