package raccoonman.reterraforged.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.tags.RTFBlockTags;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;

/**
 * "rtf strata [chunks]" (with -Drtf.selftest=true): the rock of the strata decorator under the ground (issue #1:
 * Quark's stones, "Ore Compatible Stone Only", the layer sliders). Builds chunks x chunks chunks 2000 blocks east of
 * the spawn and goes down every 4th column from 8 blocks under the ground to 96 blocks under it, counting the blocks of
 * the strata rock tag (reterraforged:rock) and the changes from one rock to another (the layer borders). Prints
 * "[RTF strata] <rtf|vanilla> columns=N layers=L rock=<block:share,...>" where L is the average number of rock changes
 * per column.
 */
public final class RTFStrataTest {
	private static final int TOP = 8, DEPTH = 96;

	private RTFStrataTest() {
	}

	public static String run(MinecraftServer server, int chunks) {
		try {
			return test(server, chunks);
		} catch (Throwable t) {
			RTFCommon.LOGGER.error("[RTF strata] exception", t);
			return "[RTF strata] FAIL exception: " + t;
		}
	}

	/** The rock blocks under the ground of these chunks and the average rock changes per column. */
	public record Result(int columns, double layers, Map<Block, Integer> rock) {
		public long total() {
			return this.rock.values().stream().mapToLong(Integer::longValue).sum();
		}

		public String describe() {
			List<Map.Entry<Block, Integer>> sorted = new ArrayList<>(this.rock.entrySet());
			sorted.sort((a, b) -> b.getValue() - a.getValue());
			StringBuilder s = new StringBuilder();
			for (Map.Entry<Block, Integer> e : sorted) {
				s.append(s.length() > 0 ? "," : "").append(BuiltInRegistries.BLOCK.getKey(e.getKey()))
					.append(String.format(Locale.ROOT, ":%.1f%%", 100.0 * e.getValue() / Math.max(1, this.total())));
			}
			return String.format(Locale.ROOT, "columns=%d layers=%.1f rock=%s", this.columns, this.layers, s);
		}
	}

	public static Result sample(ServerLevel level, int cx0, int cz0, int chunks) {
		Map<Block, Integer> rock = new HashMap<>();
		int columns = 0;
		long changes = 0;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int cx = cx0; cx < cx0 + chunks; cx++) for (int cz = cz0; cz < cz0 + chunks; cz++) {
			ChunkAccess c = level.getChunk(cx, cz, ChunkStatus.FULL, true);
			for (int x = 0; x < 16; x += 4) for (int z = 0; z < 16; z += 4) {
				int top = c.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - TOP;
				int bottom = Math.max(level.getMinY() + 8, top - DEPTH);
				Block last = null;
				for (int y = top; y > bottom; y--) {
					BlockState state = c.getBlockState(pos.set(x, y, z));
					// only the strata's rock: ores, caves, water and the deepslate below break no layer
					if (!state.is(RTFBlockTags.ROCK)) continue;
					rock.merge(state.getBlock(), 1, Integer::sum);
					if (last != null && last != state.getBlock()) changes++;
					last = state.getBlock();
				}
				columns++;
			}
		}
		return new Result(columns, (double) changes / Math.max(1, columns), rock);
	}

	private static String test(MinecraftServer server, int chunks) {
		ServerLevel level = server.overworld();
		boolean rtf = (Object) level.getChunkSource().randomState() instanceof RTFRandomState s && s.generatorContext() != null;
		BlockPos spawn = level.getRespawnData().pos();
		long t0 = System.nanoTime();
		Result result = sample(level, ((spawn.getX() + 2000) >> 4) - chunks / 2, (spawn.getZ() >> 4) - chunks / 2, chunks);
		RTFCommon.LOGGER.info("[RTF strata] {} chunks in {} ms", chunks * chunks, (System.nanoTime() - t0) / 1_000_000);
		String r = "[RTF strata] " + (rtf ? "rtf " : "vanilla ") + result.describe();
		RTFCommon.LOGGER.info(r);
		return r;
	}
}
