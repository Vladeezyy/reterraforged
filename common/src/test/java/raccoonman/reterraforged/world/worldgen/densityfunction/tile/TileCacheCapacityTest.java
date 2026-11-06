package raccoonman.reterraforged.world.worldgen.densityfunction.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import raccoonman.reterraforged.world.worldgen.util.PosUtil;

class TileCacheCapacityTest {
	private static final long GB = 1024L * 1024L * 1024L;

	private static long cells(int tileSize) {
		return Size.blocks(tileSize, 4).arraySize();
	}

	@Test
	void largeTilesKeepToAThirdOfTheHeap() {
		long cells = cells(5);
		int capacity = TileCache.capacity(cells, 4L * GB);
		assertTrue(capacity * cells * 132L <= 4L * GB / 3L || capacity == 8, "capacity " + capacity);
		assertTrue(capacity < 256, "capacity " + capacity);
	}

	@Test
	void smallTilesOnALargeHeapStopAtTheLimit() {
		assertEquals(256, TileCache.capacity(cells(1), 64L * GB));
	}

	// fewer than the threads waiting on tiles would make them again and again
	@Test
	void neverFewerThanAFewTiles() {
		assertEquals(8, TileCache.capacity(cells(5), 256L * 1024L * 1024L));
		assertEquals(256, TileCache.capacity(0L, GB));
	}

	@Test
	void positionsPackAndUnpack() {
		for (int left : new int[] { 0, 1, -1, 30_000_000, -30_000_000, Integer.MAX_VALUE, Integer.MIN_VALUE }) {
			for (int right : new int[] { 0, 1, -1, 30_000_000, -30_000_000, Integer.MAX_VALUE, Integer.MIN_VALUE }) {
				long packed = PosUtil.pack(left, right);
				assertEquals(left, PosUtil.unpackLeft(packed));
				assertEquals(right, PosUtil.unpackRight(packed));
			}
		}
	}

	@Test
	void anIndexOfASizeIsInsideIt() {
		Size size = Size.chunks(3, 2);
		assertEquals(size.total() * size.total() - 1, size.indexOf(size.total() - 1, size.total() - 1));
		assertEquals(0, size.indexOf(0, 0));
	}
}
