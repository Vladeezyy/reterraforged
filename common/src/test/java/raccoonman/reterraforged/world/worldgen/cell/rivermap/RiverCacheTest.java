package raccoonman.reterraforged.world.worldgen.cell.rivermap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import sun.misc.Unsafe;

class RiverCacheTest {

	// many threads asking for one map make it once; a thread asking for another map does not wait for it
	@Test
	void aMapIsMadeOnceAndOutsideTheLock() throws Exception {
		AtomicInteger made = new AtomicInteger();
		CountDownLatch slowStarted = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		RiverGenerator generator = (x, z, id) -> {
			made.incrementAndGet();
			if (x == 0) {
				slowStarted.countDown();
				try {
					release.await(10, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			return emptyRivermap();
		};
		RiverCache cache = new RiverCache(generator);
		ExecutorService pool = Executors.newFixedThreadPool(9);
		try {
			List<Future<Rivermap>> slow = new ArrayList<>();
			for (int i = 0; i < 8; i++) {
				slow.add(pool.submit(() -> cache.getRivers(0, 0)));
			}
			assertEquals(true, slowStarted.await(10, TimeUnit.SECONDS));
			// made under the map's write lock, this waited for the slow one
			assertNotNull(pool.submit(() -> cache.getRivers(5, 7)).get(5, TimeUnit.SECONDS));
			release.countDown();
			Rivermap first = slow.get(0).get(10, TimeUnit.SECONDS);
			for (Future<Rivermap> f : slow) {
				assertSame(first, f.get(10, TimeUnit.SECONDS));
			}
			assertEquals(2, made.get());
		} finally {
			release.countDown();
			pool.shutdownNow();
		}
	}

	// a map without its noises: GenWarp's codecs need the game's registries, which a unit test does not have
	private static Rivermap emptyRivermap() {
		try {
			Field field = Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			return (Rivermap) ((Unsafe) field.get(null)).allocateInstance(Rivermap.class);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}
}
