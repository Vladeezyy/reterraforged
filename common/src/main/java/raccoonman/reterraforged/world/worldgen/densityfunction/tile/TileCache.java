package raccoonman.reterraforged.world.worldgen.densityfunction.tile;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;

import raccoonman.reterraforged.concurrent.cache.Cache;
import raccoonman.reterraforged.concurrent.cache.CacheEntry;
import raccoonman.reterraforged.concurrent.cache.CacheManager;
import raccoonman.reterraforged.concurrent.cache.map.StampedLongMap;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.generation.TileGenerator;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

public class TileCache implements TileFactory {
	private static final int MAX_TILES = 256;
	// a Cell object and its place in the array
	private static final long BYTES_PER_CELL = 132L;
	// a finished tile is used a little longer by the next tile's chunks: it stays about 20 to 40 s after its last use
	// instead of 60 to 80 s. Discarded at once, 47 % of the tiles of a long pregeneration were generated a second time
	private static final long FINISHED_SOONER_MS = 40_000L;
	// the worlds' caches, weakly: closeAll when a server stops
	private static final Set<TileCache> CACHES = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
	private static final AtomicInteger NEXT_ID = new AtomicInteger();
	// a thread's last tile: WorldLookup asks for one per cell, and every thread took the map's read lock and wrote the
	// entry's time for each. Weakly, so a thread does not keep a tile (or a left world's) alive. A published tile does not
	// change, so one that left the cache meanwhile is still right
	private static final ThreadLocal<Recent> RECENT = ThreadLocal.withInitial(Recent::new);
	private final int id = NEXT_ID.incrementAndGet();
	// raised by close: the tiles started before it are not finished
	private final AtomicInteger generation = new AtomicInteger();
	private final int tileSize;
	private final boolean queue;
	private final Cache<CacheEntry<Entry>> cache;
	private final TileGenerator generator;

	public TileCache(int tileSize, boolean queue, TileGenerator generator) {
		this.tileSize = tileSize;
		this.queue = queue;
		// a tile still being generated does not leave for a new one: its next chunk would start it again
		this.cache = CacheManager.createCache(capacity(generator.tileCells(), Runtime.getRuntime().maxMemory()), 60L, 20L, TimeUnit.SECONDS, (size) -> StampedLongMap.<CacheEntry<Entry>>bounded(size, CacheEntry::isDone));
		this.generator = generator;
		CACHES.add(this);
	}


	// at most a third of the heap: 256 tiles of size 5 would be 10 GB. The tiles are the same whenever they are made, so a
	// smaller cache only makes some of them again
	static int capacity(long cells, long maxMemory) {
		return (int) Math.max(8L, Math.min(MAX_TILES, maxMemory / 3L / Math.max(1L, cells * BYTES_PER_CELL)));
	}

	/**
	 * The worlds of a server that stopped: the generation threads are all worlds', and the tiles it had queued were
	 * generated ahead of the next world's first one. A cache used again afterwards makes its tiles anew
	 */
	public static void closeAll() {
		List<TileCache> caches;
		synchronized (CACHES) {
			caches = new ArrayList<>(CACHES);
		}
		for (TileCache cache : caches) {
			cache.close();
		}
	}

	public void close() {
		this.generation.incrementAndGet();
		this.cache.clear();
	}

	/** The tile if it is cached and done: neither waits nor throws */
	@Nullable
	public Tile provideIfPresent(int tileX, int tileZ) {
		@Nullable
		CacheEntry<Entry> entry = this.cache.get(PosUtil.pack(tileX, tileZ));
		@Nullable
		Entry done = entry != null ? entry.getNow() : null;
		return done != null ? done.tile : null;
	}

	@Override
	public Tile provide(int tileX, int tileZ) {
		Recent recent = RECENT.get();
		if (recent.cache == this.id && recent.tileX == tileX && recent.tileZ == tileZ) {
			@SuppressWarnings("unchecked")
			CacheEntry<Entry> entry = (CacheEntry<Entry>) recent.entry.get();
			// getNow sets its time too (once a second): the tile does not expire while this thread uses it
			@Nullable
			Entry done = entry != null ? entry.getNow() : null;
			if (done != null) {
				return done.tile;
			}
		}
		long key = PosUtil.pack(tileX, tileZ);
		CacheEntry<Entry> entry = this.computeEntry(tileX, tileZ, key);
		Entry done;
		try {
			done = entry.get();
		} catch (RuntimeException e) {
			// a failed tile does not stay in the cache to fail for every chunk until it expires; only this entry, not a new
			// one another thread put after it
			this.cache.discard(key, entry);
			throw e;
		}
		recent.cache = this.id;
		recent.tileX = tileX;
		recent.tileZ = tileZ;
		recent.entry = new WeakReference<>(entry);
		return done.tile;
	}

	@Override
	public void queue(int tileX, int tileZ) {
		if(this.queue) {
			this.computeEntry(tileX, tileZ, PosUtil.pack(tileX, tileZ));
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
		// null when the tile already left the cache (unused for a minute, or the cache was full). Not waited for: one being
		// made again or failed is not the one this chunk had; uncounted, it only stays its full time
		CacheEntry<Entry> entry = this.cache.get(packedTilePos);
		@Nullable
		Entry done = entry != null ? entry.getNow() : null;
		if(done != null && done.drop()) {
			entry.expireSooner(FINISHED_SOONER_MS);
		}
	}

	@Override
	public int chunkToTile(int chunkCoord) {
		return chunkCoord >> this.tileSize;
	}

	// Only the empty entry is made under the map's write lock: the tile's cells (millions with a large tileSize) were made
	// there, and every thread asking for any tile waited for them. The thread that put the entry starts the tile after it
	private CacheEntry<Entry> computeEntry(int tileX, int tileZ, long key) {
		@Nullable
		CacheEntry<Entry> entry = this.cache.get(key);
		if (entry != null) {
			return entry;
		}
		CacheEntry<Entry> made = CacheEntry.pending();
		entry = this.cache.computeIfAbsent(key, (k) -> made);
		if (entry == made) {
			this.start(made, tileX, tileZ);
		}
		return entry;
	}

	private void start(CacheEntry<Entry> entry, int tileX, int tileZ) {
		int generation = this.generation.get();
		try {
			entry.complete(this.generator.generate(tileX, tileZ, () -> this.generation.get() != generation).thenApply(Entry::new));
		} catch (Throwable e) {
			// out of memory for the cells: the entry's waiters get it as the tile's failure (CacheEntry.get), not this thread
			entry.fail(e);
		}
	}

	private static final class Entry {
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

	private static final class Recent {
		private int cache;
		private int tileX, tileZ;
		private WeakReference<CacheEntry<?>> entry = new WeakReference<>(null);
	}
}
