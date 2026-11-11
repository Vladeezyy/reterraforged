package raccoonman.reterraforged.concurrent.cache.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class StampedLongMapTest {

	@Test
	void computesOncePerKey() {
		StampedLongMap<String> map = StampedLongMap.bounded(16);
		AtomicInteger computed = new AtomicInteger();
		assertEquals("5", map.computeIfAbsent(5L, (k) -> { computed.incrementAndGet(); return Long.toString(k); }));
		assertEquals("5", map.computeIfAbsent(5L, (k) -> { computed.incrementAndGet(); return "again"; }));
		assertEquals(1, computed.get());
		assertEquals(1, map.size());
	}

	@Test
	void removesAndClears() {
		StampedLongMap<String> map = StampedLongMap.unbounded(16);
		map.computeIfAbsent(1L, Long::toString);
		map.computeIfAbsent(2L, Long::toString);
		map.computeIfAbsent(3L, Long::toString);
		map.remove(2L);
		assertNull(map.get(2L));
		assertEquals(1, map.removeIf("1"::equals));
		assertEquals(1, map.size());
		map.clear();
		assertEquals(0, map.size());
	}

	// more keys than its size: the key of a tile is two ints packed, negative ones too
	@Test
	void keepsKeysPastItsInitialSize() {
		StampedLongMap<Long> map = StampedLongMap.unbounded(4);
		for (long key = -200L; key < 200L; key++) {
			long k = key;
			map.computeIfAbsent(key, (x) -> k);
		}
		assertEquals(400, map.size());
		for (long key = -200L; key < 200L; key++) {
			assertEquals(key, map.get(key));
		}
	}

	// threads that all missed the key under the read lock: one makes it, and none of the others pushes an entry out for it
	@Test
	void aKeyAskedForTogetherTakesOnePlace() throws Exception {
		ExecutorService threads = Executors.newFixedThreadPool(8);
		try {
			for (int round = 0; round < 200; round++) {
				StampedLongMap<String> map = StampedLongMap.bounded(4);
				map.computeIfAbsent(1L, Long::toString);
				map.computeIfAbsent(2L, Long::toString);
				map.computeIfAbsent(3L, Long::toString);
				CountDownLatch go = new CountDownLatch(1);
				AtomicInteger computed = new AtomicInteger();
				Future<?>[] asked = new Future<?>[8];
				for (int i = 0; i < asked.length; i++) {
					asked[i] = threads.submit(() -> {
						go.await();
						return map.computeIfAbsent(99L, (k) -> { computed.incrementAndGet(); return "99"; });
					});
				}
				go.countDown();
				for (Future<?> future : asked) {
					assertEquals("99", future.get());
				}
				assertEquals(1, computed.get());
				assertNotNull(map.get(1L), "round " + round + ": an entry left for a key that was already there");
				assertEquals(4, map.size());
			}
		} finally {
			threads.shutdownNow();
		}
	}

	// the tile cache: a tile still being made does not leave for a new one while a finished one can
	@Test
	void theOldestEvictableLeaves() {
		StampedLongMap<String> map = StampedLongMap.bounded(3, (value) -> !value.startsWith("busy"));
		map.computeIfAbsent(1L, (k) -> "busy1");
		map.computeIfAbsent(2L, (k) -> "done2");
		map.computeIfAbsent(3L, (k) -> "done3");
		map.computeIfAbsent(4L, (k) -> "done4");
		assertNotNull(map.get(1L));
		assertNull(map.get(2L));
		assertEquals(3, map.size());
	}

	// none can leave: the limit still holds, the oldest goes
	@Test
	void aFullMapOfBusyEntriesStillKeepsItsSize() {
		StampedLongMap<String> map = StampedLongMap.bounded(2, (value) -> false);
		map.computeIfAbsent(1L, (k) -> "busy1");
		map.computeIfAbsent(2L, (k) -> "busy2");
		map.computeIfAbsent(3L, (k) -> "busy3");
		assertNull(map.get(1L));
		assertEquals(2, map.size());
	}

	// a failed tile's thread removes its entry, not the new one under the key
	@Test
	void removesOnlyTheValueGiven() {
		StampedLongMap<String> map = StampedLongMap.unbounded(4);
		String first = map.computeIfAbsent(7L, (k) -> new String("first"));
		map.remove(7L);
		String second = map.computeIfAbsent(7L, (k) -> new String("second"));
		assertFalse(map.remove(7L, first));
		assertEquals(second, map.get(7L));
		assertTrue(map.remove(7L, second));
		assertNull(map.get(7L));
	}
}
