package raccoonman.reterraforged.world.worldgen.densityfunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

class ColumnCacheTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void keepsTheValueOfEachColumnOfItsChunk() {
		Counting function = new Counting();
		ColumnCache cache = new ColumnCache(function, -2, 3);
		for(int y = -64; y < 320; y += 7) {
			for(int z = 48; z < 64; z++) {
				for(int x = -32; x < -16; x++) {
					assertEquals(Double.doubleToRawLongBits(Counting.value(x, z)), Double.doubleToRawLongBits(cache.compute(new DensityFunction.SinglePointContext(x, y, z))));
				}
			}
		}
		assertEquals(256, function.calls);
	}

	@Test
	void computesThePlacesOutsideItsChunk() {
		Counting function = new Counting();
		ColumnCache cache = new ColumnCache(function, 0, 0);
		int[][] outside = { { -1, 0 }, { 16, 0 }, { 0, -1 }, { 0, 16 }, { 100, 5 } };
		for(int i = 0; i < 2; i++) {
			for(int[] place : outside) {
				assertEquals(Counting.value(place[0], place[1]), cache.compute(new DensityFunction.SinglePointContext(place[0], 70, place[1])));
			}
		}
		assertEquals(outside.length * 2, function.calls);
	}

	@Test
	void hasTheBoundsOfItsFunction() {
		Counting function = new Counting();
		ColumnCache cache = new ColumnCache(function, 0, 0);
		assertEquals(function.minValue(), cache.minValue());
		assertEquals(function.maxValue(), cache.maxValue());
	}

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

	// a different value for every column, whatever the y; counts its computations
	private static final class Counting implements DensityFunction.SimpleFunction {
		private int calls;

		static double value(int x, int z) {
			return (float) (x * 31 + z) / 7.0F + 0.1D;
		}

		@Override
		public double compute(FunctionContext ctx) {
			this.calls++;
			return value(ctx.blockX(), ctx.blockZ());
		}

		@Override
		public double minValue() {
			return -3.5D;
		}

		@Override
		public double maxValue() {
			return 4.25D;
		}

		@Override
		public KeyDispatchDataCodec<? extends DensityFunction> codec() {
			throw new UnsupportedOperationException();
		}
	}
}
