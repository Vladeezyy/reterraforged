package raccoonman.reterraforged.world.worldgen.cell.heightmap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;

/** The columns past 2^24 blocks (generation version 1): interpolated between the floats beside them, nearer ones as before */
class FarColumnsTest {
	private static final int LINE = 1 << 24;

	// a terrain of float coordinates: a smooth height and fields, a terrain type and a weirdness sign that change in steps
	private static final class Field implements FarColumns.Sampler {
		int calls;

		@Override
		public void sample(Cell cell, float x, float z) {
			this.calls++;
			cell.height = (float) (0.5 + 0.1 * Math.sin(x / 37.0) + 0.1 * Math.cos(z / 53.0));
			cell.riverMask = (float) (0.5 + 0.4 * Math.sin(x / 91.0 + z / 77.0));
			cell.continentEdge = (float) (0.5 + 0.4 * Math.cos(x / 61.0 - z / 43.0));
			cell.beachNoise = (float) (0.01 * Math.sin(x / 13.0));
			long cellX = (long) Math.floor(x / 4.0);
			cell.terrain = (cellX & 1) == 0 ? TerrainType.FLATS : TerrainType.BEACH;
			cell.weirdness = (cellX & 1) == 0 ? 0.6F : -0.6F;
			cell.continentX = (int) cellX;
		}
	}

	private static Cell direct(float x, float z) {
		Cell cell = new Cell();
		new Field().sample(cell, x, z);
		return cell;
	}

	private static Cell far(int x, int z) {
		Cell cell = new Cell();
		FarColumns.apply(cell, x, z, new Field());
		return cell;
	}

	@Test
	void theFloatsBesideAColumn() {
		for (int v = LINE - 64; v <= LINE; v++) {
			assertTrue(FarColumns.exact(v));
			assertTrue(FarColumns.exact(-v));
		}
		assertFalse(FarColumns.exact(LINE + 1));
		assertFalse(FarColumns.exact(-LINE - 1));
		assertTrue(FarColumns.exact(LINE + 2));
		assertTrue(FarColumns.exact(20_000_000));
		assertFalse(FarColumns.exact(20_000_001));
		assertFalse(FarColumns.exact(29_999_983));
		assertTrue(FarColumns.exact(29_999_984));
		assertEquals(20_000_000.0F, FarColumns.below(20_000_001));
		assertEquals(20_000_002.0F, FarColumns.above(20_000_001));
		assertEquals(0.5F, FarColumns.weight(20_000_001));
		assertEquals(-20_000_002.0F, FarColumns.below(-20_000_001));
		assertEquals(-20_000_000.0F, FarColumns.above(-20_000_001));
		// to the nearest, a tie to the even mantissa: 20 000 001 was 20 000 000, 20 000 003 was 20 000 004
		assertFalse(FarColumns.roundsUp(20_000_001));
		assertTrue(FarColumns.roundsUp(20_000_003));
		assertTrue(FarColumns.roundsUp(-20_000_001));
		// past 2^25 (beyond the world border) there are three columns between two floats
		assertEquals(0.25F, FarColumns.weight((1 << 25) + 1));
		assertEquals(0.75F, FarColumns.weight((1 << 25) + 3));
		assertTrue(FarColumns.reaches(LINE, LINE + 15));
		assertFalse(FarColumns.reaches(LINE - 16, LINE - 1));
		assertFalse(FarColumns.reaches(-LINE, -LINE + 15));
		assertTrue(FarColumns.reaches(-LINE - 16, -LINE - 1));
	}

	@Test
	void aNearColumnIsTheSame() {
		for (int[] p : new int[][] { { 0, 0 }, { 1234567, -7654321 }, { LINE, LINE }, { -LINE, LINE - 1 }, { 20_000_000, 3 }, { 20_000_002, -20_000_004 } }) {
			Field field = new Field();
			Cell cell = new Cell();
			FarColumns.apply(cell, p[0], p[1], field);
			Cell expected = direct(p[0], p[1]);
			assertEquals(1, field.calls);
			assertEquals(Float.floatToIntBits(expected.height), Float.floatToIntBits(cell.height));
			assertEquals(Float.floatToIntBits(expected.riverMask), Float.floatToIntBits(cell.riverMask));
			assertSame(expected.terrain, cell.terrain);
		}
	}

	@Test
	void anOddColumnIsBetweenItsNeighbours() {
		int z = 1000;
		for (int x = 20_000_001; x < 20_000_401; x += 2) {
			Cell low = direct(x - 1, z), high = direct(x + 1, z), cell = far(x, z);
			// a few ulps apart the mean may round to one of them
			if (Math.abs(low.height - high.height) > 4.0F * Math.ulp(low.height)) {
				assertTrue(Math.min(low.height, high.height) < cell.height && cell.height < Math.max(low.height, high.height), "x " + x);
			}
			// what is not interpolated is the cell the column had before (x rounded to a float)
			Cell before = direct(x, z);
			assertSame(before.terrain, cell.terrain);
			assertEquals(before.weirdness, cell.weirdness);
			assertEquals(before.continentX, cell.continentX);
		}
		// the same on z, and on the negative side
		Cell cell = far(500, -20_000_001);
		Cell low = direct(500, -20_000_002), high = direct(500, -20_000_000);
		assertNotEquals(low.height, high.height);
		assertTrue(Math.min(low.height, high.height) < cell.height && cell.height < Math.max(low.height, high.height));
	}

	@Test
	void farOnBothAxesTakesFourCells() {
		Field field = new Field();
		Cell cell = new Cell();
		FarColumns.apply(cell, 20_000_001, -20_000_001, field);
		assertEquals(4, field.calls);
		float min = Float.MAX_VALUE, max = -Float.MAX_VALUE, sum = 0.0F;
		for (int dx : new int[] { -1, 1 }) for (int dz : new int[] { -1, 1 }) {
			float h = direct(20_000_001 + dx, -20_000_001 + dz).height;
			min = Math.min(min, h);
			max = Math.max(max, h);
			sum += h;
		}
		assertTrue(min <= cell.height && cell.height <= max);
		assertEquals(sum / 4.0F, cell.height, 1.0E-6F);
		// the corner the column rounded to: x to 20 000 000, z to -20 000 000
		assertSame(direct(20_000_000, -20_000_000).terrain, cell.terrain);
		Field one = new Field();
		FarColumns.apply(new Cell(), 20_000_001, 7, one);
		assertEquals(2, one.calls);
	}

	@Test
	void theGroundGoesOnAcrossTheLine() {
		// a slope along x: before, the columns past the line went in steps (the same height three times), now each is higher
		FarColumns.Sampler slope = (cell, x, z) -> cell.height = (x - (float) LINE) * 0.01F;
		float last = -Float.MAX_VALUE;
		for (int x = LINE - 40; x <= LINE + 40; x++) {
			Cell cell = new Cell();
			FarColumns.apply(cell, x, 0, slope);
			assertTrue(cell.height > last, "x " + x);
			assertEquals((x - LINE) * 0.01F, cell.height, 1.0E-5F);
			last = cell.height;
		}
	}

	@Test
	void theOrderOfTheColumnsDoesNotCount() {
		Cell first = far(20_000_001, -20_000_001);
		far(25_000_003, 25_000_005);
		far(7, 21_000_001);
		Cell again = far(20_000_001, -20_000_001);
		assertEquals(Float.floatToIntBits(first.height), Float.floatToIntBits(again.height));
		assertSame(first.terrain, again.terrain);
	}
}
