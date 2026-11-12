package raccoonman.reterraforged.world.worldgen.cell.heightmap;

import raccoonman.reterraforged.world.worldgen.cell.Cell;

/**
 * The block columns a float does not hold. The terrain is computed with float coordinates: past 2^24 (16 777 216) blocks
 * from the centre an odd x or z is rounded to an even one (ties to a multiple of 4), so the ground went in steps: three
 * columns of the same cell, then one. Such a column's cell is made from the cells of the floats on both sides of it (two, four on both axes):
 * the smooth fields interpolated, the others those of the float the coordinate rounds to, the cell it had before.
 * Generation version 1 only (Heightmap.farColumns): the chunks of older worlds may be there already. A column nearer than
 * 2^24 on both axes is computed as before.
 */
public final class FarColumns {
	// every int up to this is a float
	public static final int EXACT = 1 << 24;
	// the cells on the sides of a column; a sampler never comes back here
	private static final ThreadLocal<Cell[]> SIDES = ThreadLocal.withInitial(() -> new Cell[] { new Cell(), new Cell(), new Cell(), new Cell() });

	private FarColumns() {
	}

	public interface Sampler {
		void sample(Cell cell, float x, float z);
	}

	public static boolean exact(int v) {
		// (long): an int compared with a float is made a float first, the same rounding
		return (v <= EXACT && v >= -EXACT) || (long) (float) v == v;
	}

	public static boolean exact(int x, int z) {
		return exact(x) && exact(z);
	}

	// one of the columns min to max is not a float (a tile's chunk: one test for its 16 x 16 columns)
	public static boolean reaches(int min, int max) {
		return max > EXACT || min < -EXACT;
	}

	// the float at or below v
	public static float below(int v) {
		float f = (float) v;
		return (long) f > v ? Math.nextDown(f) : f;
	}

	// the float at or above v
	public static float above(int v) {
		float f = (float) v;
		return (long) f < v ? Math.nextUp(f) : f;
	}

	// how far v is from below(v) to above(v): 0.5 for an odd v up to 2^25 (the world border is below that)
	public static float weight(int v) {
		double low = below(v);
		double high = above(v);
		return high == low ? 0.0F : (float) ((v - low) / (high - low));
	}

	// v as a float is above(v) (to the nearest, a tie to the even mantissa): the cell the column had before
	public static boolean roundsUp(int v) {
		return (long) (float) v > v;
	}

	public static void apply(Cell cell, int x, int z, Sampler sampler) {
		boolean alongX = !exact(x);
		boolean alongZ = !exact(z);
		if (!alongX && !alongZ) {
			sampler.sample(cell, x, z);
			return;
		}
		Cell[] sides = SIDES.get();
		if (alongX && alongZ) {
			float x0 = below(x), x1 = above(x), z0 = below(z), z1 = above(z);
			float tx = weight(x);
			boolean upX = roundsUp(x);
			sampler.sample(sides[0].reset(), x0, z0);
			sampler.sample(sides[1].reset(), x1, z0);
			sampler.sample(sides[2].reset(), x0, z1);
			sampler.sample(sides[3].reset(), x1, z1);
			blend(sides[0], sides[0], sides[1], tx, upX);
			blend(sides[2], sides[2], sides[3], tx, upX);
			blend(cell, sides[0], sides[2], weight(z), roundsUp(z));
		} else if (alongX) {
			sampler.sample(sides[0].reset(), below(x), z);
			sampler.sample(sides[1].reset(), above(x), z);
			blend(cell, sides[0], sides[1], weight(x), roundsUp(x));
		} else {
			sampler.sample(sides[0].reset(), x, below(z));
			sampler.sample(sides[1].reset(), x, above(z));
			blend(cell, sides[0], sides[1], weight(z), roundsUp(z));
		}
	}

	/**
	 * The cell t of the way from a to b (cell may be a). Interpolated: the height and the fields that go into the noise
	 * router with it per block (the river mask, the continent's edge, the beach noise). The rest from the nearer side: the
	 * terrain and biome types, the regions, and the climate (the erosion and weirdness are set by terrain types and change
	 * sign between macro biomes, a mean of them would be a third biome; the biomes are sampled on even places anyway).
	 */
	public static void blend(Cell cell, Cell a, Cell b, float t, boolean nearB) {
		float height = lerp(a.height, b.height, t);
		float riverMask = lerp(a.riverMask, b.riverMask, t);
		float continentEdge = lerp(a.continentEdge, b.continentEdge, t);
		float beachNoise = lerp(a.beachNoise, b.beachNoise, t);
		cell.copyFrom(nearB ? b : a);
		cell.height = height;
		cell.riverMask = riverMask;
		cell.continentEdge = continentEdge;
		cell.beachNoise = beachNoise;
	}

	private static float lerp(float a, float b, float t) {
		return a + t * (b - a);
	}
}
