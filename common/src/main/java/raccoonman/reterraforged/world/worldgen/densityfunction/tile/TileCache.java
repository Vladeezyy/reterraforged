package raccoonman.reterraforged.world.worldgen.densityfunction.tile;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;

import raccoonman.reterraforged.concurrent.cache.Cache;
import raccoonman.reterraforged.concurrent.cache.CacheEntry;
import raccoonman.reterraforged.concurrent.cache.CacheManager;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.generation.TileGenerator;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

public class TileCache implements TileFactory {
	private static final int MAX_TILES = 256;
	// a Cell object and its place in the array
	private static final long BYTES_PER_CELL = 132L;
	// a finished tile is used a little longer by the next tile's chunks: it stays about 20 to 40 s after its last use
	// instead of 60 to 80 s. Discarded at once, 47 % of the tiles of a long pregeneration were generated a second time
	private static final long FINISHED_SOONER_MS = 40_000L;
	private final int tileSize;
	private final boolean queue;
	private final Cache<CacheEntry<Entry>> cache;
	private final TileGenerator generator;
	
	public TileCache(int tileSize, boolean queue, TileGenerator generator) {
		this.tileSize = tileSize;
		this.queue = queue;
		this.cache = CacheManager.createCache(capacity(generator.tileCells(), Runtime.getRuntime().maxMemory()), 60L, 20L, TimeUnit.SECONDS);
		this.generator = generator;
	}
	
	
	// at most a third of the heap: 256 tiles of size 5 would be 10 GB. The tiles are the same whenever they are made, so a
	// smaller cache only makes some of them again
	static int capacity(long cells, long maxMemory) {
		return (int) Math.max(8L, Math.min(MAX_TILES, maxMemory / 3L / Math.max(1L, cells * BYTES_PER_CELL)));
	}

	@Nullable
	public Tile provideIfPresent(int tileX, int tileZ) {
		@Nullable
		CacheEntry<Entry> entry = this.cache.get(PosUtil.pack(tileX, tileZ));
		return entry != null ? entry.get().tile : null;
	}

	@Override
	public Tile provide(int tileX, int tileZ) {
		CacheEntry<Entry> entry = this.computeEntry(tileX, tileZ);
		try {
			return entry.get().tile;
		} catch (RuntimeException e) {
			// a failed tile does not stay in the cache to fail for every chunk until it expires
			this.cache.discard(PosUtil.pack(tileX, tileZ));
			throw e;
		}
	}
	
	@Override
	public void queue(int tileX, int tileZ) {
		if(this.queue) {
			this.computeEntry(tileX, tileZ);
		}
	}

	/**
	 * A chunk of the tile has its features: once all of them have, the tile leaves the cache soon. It is not closed or
	 * its arrays reused: other threads may still read it (the next tile's chunks at the border, biome and structure
	 * height lookups), and its cells were reset under them, then filled with another tile's.
	 */
	@Override
	public void drop(int tileX, int tileZ) {
		long packedTilePos = PosUtil.pack(tileX, tileZ);
		// null when the tile already left the cache (unused for a minute, or the cache was full)
		CacheEntry<Entry> entry = this.cache.get(packedTilePos);
		if(entry != null && entry.get().drop()) {
			entry.expireSooner(FINISHED_SOONER_MS);
		}
	}

	@Override
	public int chunkToTile(int chunkCoord) {
		return chunkCoord >> this.tileSize;
	}
	
	private CacheEntry<Entry> computeEntry(int tileX, int tileZ) {
		return this.cache.computeIfAbsent(PosUtil.pack(tileX, tileZ), (k) -> {
			return CacheEntry.supply(this.generator.generate(tileX, tileZ).thenApply(Entry::new));
		});
	}
	
	private class Entry {
		private final AtomicInteger refCount;
		private final int chunkCount;
		private final Tile tile;
		
		public Entry(Tile tile) {
			int tileSize = tile.getChunksSize().size();
			this.refCount = new AtomicInteger();
			this.chunkCount = tileSize * tileSize;
			this.tile = tile;
		}
		
		public boolean drop() {
			return this.refCount.incrementAndGet() >= this.chunkCount;
		}
	}
}