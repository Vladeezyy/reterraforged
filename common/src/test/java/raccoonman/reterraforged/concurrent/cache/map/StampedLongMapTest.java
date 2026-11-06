package raccoonman.reterraforged.concurrent.cache.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
