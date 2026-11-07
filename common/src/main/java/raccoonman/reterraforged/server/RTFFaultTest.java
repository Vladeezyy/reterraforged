package raccoonman.reterraforged.server;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import raccoonman.reterraforged.concurrent.ThreadPools;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.TileCache;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

/**
 * "rtf fault" (with -Drtf.selftest=true and -Drtf.faulttest=true, tools/servertest.sh FAULT=1): what an exception in a
 * tile's generation thread does. A tile that fails: the caller gets the error, the cache does not keep the failed
 * entry, the same tile asked again is made and equals one made anew, the generation threads are still there.
 * "rtf fault ticket": a tile fails under a chunk the chunk system makes in the background, as for a player that walks.
 * "rtf fault block": the same while the server thread waits for that chunk, as for a teleport or /forceload.
 * The game ends by itself after either; tools/servertest.sh FAULT=ticket|block checks that it really does.
 */
public final class RTFFaultTest {
	public static final String MESSAGE = "RTF fault test: this tile fails on purpose";
	// chunks far from the spawn and from the other tests' places, in different tiles
	private static final int CACHE_CHUNK = 18750;
	private static final int FORCED_CHUNK = -18750;
	private static final long NONE = Long.MIN_VALUE;
	private static volatile long failing = NONE;

	private RTFFaultTest() {
	}

	// MixinTileFault, in a generation thread
	public static void onTileWork(Tile tile) {
		if (failing == PosUtil.pack(tile.getX(), tile.getZ())) {
			throw new IllegalStateException(MESSAGE);
		}
	}

	public static String run(MinecraftServer server) {
		GeneratorContext context = context(server.overworld());
		if (context == null || context.cache == null) {
			return "[RTF fault] FAIL not a ReTerraForged world, or the -Drtf.faulttest flag is missing";
		}
		TileCache cache = context.cache;
		int tileX = cache.chunkToTile(CACHE_CHUNK);
		int tileZ = cache.chunkToTile(CACHE_CHUNK);
		int workers = ThreadPools.WORLD_GEN.getPoolSize();
		List<String> bad = new ArrayList<>();

		long start = System.nanoTime();
		failing = PosUtil.pack(tileX, tileZ);
		try {
			cache.provide(tileX, tileZ);
			bad.add("no error reached the caller");
		} catch (RuntimeException e) {
			if (!String.valueOf(e.getMessage()).contains(MESSAGE)) {
				bad.add("another error reached the caller: " + e);
			}
		} finally {
			failing = NONE;
		}
		long failedMs = (System.nanoTime() - start) / 1_000_000L;

		try {
			if (cache.provideIfPresent(tileX, tileZ) != null) {
				bad.add("a tile is in the cache after the failure");
			}
		} catch (RuntimeException e) {
			bad.add("the cache kept the failed entry: " + e);
		}

		Tile again = cache.provide(tileX, tileZ);
		Tile anew = context.generator.generate(tileX, tileZ).join();
		int blocks = again.getChunksSize().size() * 16;
		int blockX = tileX * blocks;
		int blockZ = tileZ * blocks;
		int different = 0;
		for (int z = 0; z < blocks; z++) {
			for (int x = 0; x < blocks; x++) {
				Cell a = again.lookup(blockX + x, blockZ + z);
				Cell b = anew.lookup(blockX + x, blockZ + z);
				if (a.height != b.height || a.temperature != b.temperature || a.moisture != b.moisture) {
					different++;
				}
			}
		}
		if (different > 0) {
			bad.add(different + " cells of the tile made again differ");
		}

		int workersAfter = ThreadPools.WORLD_GEN.getPoolSize();
		if (workersAfter < workers) {
			bad.add("generation threads " + workers + " -> " + workersAfter);
		}
		String facts = "error after " + failedMs + " ms, " + (blocks * blocks) + " cells of the tile made again compared, generation threads " + workers + " -> " + workersAfter;
		return bad.isEmpty() ? "[RTF fault] PASS " + facts : "[RTF fault] FAIL " + String.join("; ", bad) + "; " + facts;
	}

	public static String chunk(MinecraftServer server, boolean block) {
		ServerLevel level = server.overworld();
		GeneratorContext context = context(level);
		if (context == null || context.cache == null) {
			return "[RTF fault] chunk not armed: not a ReTerraForged world";
		}
		failing = PosUtil.pack(context.cache.chunkToTile(FORCED_CHUNK), context.cache.chunkToTile(FORCED_CHUNK));
		ChunkPos pos = new ChunkPos(FORCED_CHUNK, FORCED_CHUNK);
		if (block) {
			// the server thread stays in here, waiting for the chunk
			level.getChunk(FORCED_CHUNK, FORCED_CHUNK);
		} else {
			level.getChunkSource().addTicketWithRadius(TicketType.FORCED, pos, 2);
		}
		return "[RTF fault] chunk armed: " + FORCED_CHUNK + " " + FORCED_CHUNK;
	}

	private static GeneratorContext context(ServerLevel level) {
		if (System.getProperty("rtf.faulttest") == null || !((Object) level.getChunkSource().randomState() instanceof RTFRandomState state)) {
			return null;
		}
		return state.generatorContext();
	}
}
