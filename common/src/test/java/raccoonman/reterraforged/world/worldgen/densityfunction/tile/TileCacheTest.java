package raccoonman.reterraforged.world.worldgen.densityfunction.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.generation.TileGenerator;

class TileCacheTest {

	// the tiles are whatever the test completes them with; one chunk each, so one drop finishes a tile
	private static final class Generator extends TileGenerator {
		final List<CompletableFuture<Tile>> started = new ArrayList<>();
		final List<BooleanSupplier> cancelled = new ArrayList<>();
		Runnable whileStarting = () -> {};

		Generator() {
			super(null, null, 0, 1, 1);
		}

		@Override
		public synchronized CompletableFuture<Tile> generate(int tileX, int tileZ, BooleanSupplier cancelled) {
			this.whileStarting.run();
			CompletableFuture<Tile> future = new CompletableFuture<>();
			this.started.add(future);
			this.cancelled.add(cancelled);
			return future;
		}

		synchronized int count() {
			return this.started.size();
		}
	}

	private static Tile tile(int x, int z) {
		return new Tile(x, z, 0, 1, Size.blocks(0, 1), Size.chunks(0, 1), new Cell[0], new Tile.Chunk[0]);
	}

	@Test
	void aTileIsGeneratedOnce() {
		Generator generator = new Generator();
		TileCache cache = new TileCache(0, true, generator);
		cache.queue(1, 2);
		cache.queue(1, 2);
		Tile tile = tile(1, 2);
		generator.started.get(0).complete(tile);
		assertSame(tile, cache.provide(1, 2));
		assertSame(tile, cache.provide(1, 2));
		assertSame(tile, cache.provideIfPresent(1, 2));
		assertEquals(1, generator.count());
	}

	// the features step of a chunk does not wait for a tile, nor fail with it
	@Test
	void dropNeitherWaitsNorThrows() {
		Generator generator = new Generator();
		TileCache cache = new TileCache(0, true, generator);
		cache.queue(0, 0);
		cache.drop(0, 0);
		assertNull(cache.provideIfPresent(0, 0));
		generator.started.get(0).completeExceptionally(new IllegalStateException("on purpose"));
		cache.drop(0, 0);
		assertNull(cache.provideIfPresent(0, 0));
	}

	@Test
	void aFailedTileIsMadeAgain() {
		Generator generator = new Generator();
		TileCache cache = new TileCache(0, true, generator);
		cache.queue(3, 3);
		generator.started.get(0).completeExceptionally(new IllegalStateException("on purpose"));
		assertThrows(IllegalStateException.class, () -> cache.provide(3, 3));
		cache.queue(3, 3);
		assertEquals(2, generator.count());
		Tile tile = tile(3, 3);
		generator.started.get(1).complete(tile);
		assertSame(tile, cache.provide(3, 3));
	}

	// other threads read the cache while a tile's cells are made
	@Test
	void aTileIsNotStartedUnderTheLock() {
		Generator generator = new Generator();
		TileCache cache = new TileCache(0, true, generator);
		AtomicBoolean read = new AtomicBoolean();
		generator.whileStarting = () -> {
			try {
				CompletableFuture.runAsync(() -> cache.provideIfPresent(9, 9)).get(5, TimeUnit.SECONDS);
				read.set(true);
			} catch (Exception e) {
				// a failure here becomes the tile's, so it is only noted
			}
		};
		cache.queue(0, 0);
		assertEquals(1, generator.count());
		assertTrue(read.get(), "the cache was locked while the tile started");
	}

	@Test
	void closingCancelsTheTilesStartedBefore() {
		Generator generator = new Generator();
		TileCache cache = new TileCache(0, true, generator);
		cache.queue(0, 0);
		assertFalse(generator.cancelled.get(0).getAsBoolean());
		cache.close();
		assertTrue(generator.cancelled.get(0).getAsBoolean());
		// as TileGenerator ends a cancelled tile: not a failure that ends the game
		generator.started.get(0).completeExceptionally(new CancellationException("stopped"));

		cache.queue(0, 0);
		assertEquals(2, generator.count());
		assertFalse(generator.cancelled.get(1).getAsBoolean());
		Tile tile = tile(0, 0);
		generator.started.get(1).complete(tile);
		assertSame(tile, cache.provide(0, 0));
	}

	@Test
	void aCancelledTileIsNotAGenerationFailure() {
		Generator generator = new Generator();
		TileCache cache = new TileCache(0, true, generator);
		cache.queue(0, 0);
		generator.started.get(0).completeExceptionally(new CancellationException("stopped"));
		assertThrows(CancellationException.class, () -> cache.provide(0, 0));
		// and it left the cache
		cache.queue(0, 0);
		assertEquals(2, generator.count());
	}
}
