package raccoonman.reterraforged.world.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** The full seed (generation version 1): a seed that fits in an int keeps its terrain, the upper bits count */
class SeedFoldTest {

	@Test
	void anIntSeedIsTheSame() {
		for(long seed : new long[] { 0L, 1L, -1L, 12345L, Integer.MAX_VALUE, Integer.MIN_VALUE, "Glacier".hashCode(), "".hashCode() }) {
			assertEquals((int) seed, GeneratorContext.foldSeed(seed));
		}
		Random random = new Random(7);
		for(int i = 0; i < 10000; i++) {
			int seed = random.nextInt();
			assertEquals(seed, GeneratorContext.foldSeed(seed));
		}
	}

	@Test
	void theUpperBitsCount() {
		long low = 5L;
		assertNotEquals(GeneratorContext.foldSeed(low), GeneratorContext.foldSeed((1L << 32) | low));
		assertNotEquals(GeneratorContext.foldSeed((1L << 32) | low), GeneratorContext.foldSeed((2L << 32) | low));
		// an int seed's bits with another sign half: not the int seed
		assertNotEquals(-1, GeneratorContext.foldSeed(0x00000000FFFFFFFFL));
		assertNotEquals(0, GeneratorContext.foldSeed(0xFFFFFFFF00000000L));
		// random 64-bit seeds (a new world without a seed) that share their low half: nearly all different
		Random random = new Random(11);
		Set<Integer> folded = new HashSet<>();
		for(int i = 0; i < 1000; i++) {
			folded.add(GeneratorContext.foldSeed(((long) random.nextInt() << 32) | 0x1234L));
		}
		assertTrue(folded.size() >= 998, folded.size() + " of 1000");
	}
}
