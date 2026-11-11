package raccoonman.reterraforged.world.worldgen.densityfunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

// 26.3: the compiled samplers read a 2D field once per column, so only the thread's cell cache is left to test
class CellSamplerCacheTest {

	@Test
	void threadCacheSlotsDifferForAlignedBlocksAndQuartsInARow() {
		CellSampler.Cache2d cache = new CellSampler.Cache2d(CellSampler.Cache2d.THREAD_BITS);
		for(int start : new int[] { -64, -8, 0, 8, 1000 }) {
			Set<Integer> blocks = new HashSet<>();
			Set<Integer> quarts = new HashSet<>();
			for(int i = 0; i < 8; i++) {
				blocks.add(cache.slot(start + i, 0));
				// quarts from any start
				quarts.add(cache.slot((start + 3 + i) * 4, 0));
			}
			assertEquals(8, blocks.size());
			assertEquals(8, quarts.size());
		}
		assertNotEquals(cache.slot(0, 0), cache.slot(0, 1));
		CellSampler.Cache2d one = new CellSampler.Cache2d();
		assertEquals(0, one.slot(12345, -678));
	}
}
