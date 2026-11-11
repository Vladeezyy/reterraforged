package raccoonman.reterraforged.world.worldgen.densityfunction;

import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * A function of the place's x and z alone in one noise chunk, kept for each column of the chunk: the preset's offset
 * rounds the cell's height (ClampToNearestUnit over the chunk's CacheChunk), and the router reads it for every block, not
 * interpolated, twice (sloped_cheese is the input of the range choice and in the entrances' min). A kept value is the one
 * the function returned for the column, as a double; places outside the chunk are computed as before.
 */
public final class ColumnCache implements MarkerFunction.Mapped {
	private final DensityFunction function;
	private final int minBlockX, minBlockZ;
	private final double[] values = new double[16 * 16];
	// a bit per column: kept
	private final long[] kept = new long[16 * 16 / 64];

	public ColumnCache(DensityFunction function, int chunkX, int chunkZ) {
		this.function = function;
		this.minBlockX = chunkX << 4;
		this.minBlockZ = chunkZ << 4;
	}

	public DensityFunction function() {
		return this.function;
	}

	@Override
	public double compute(FunctionContext ctx) {
		int localX = ctx.blockX() - this.minBlockX;
		int localZ = ctx.blockZ() - this.minBlockZ;
		if(((localX | localZ) & ~15) != 0) {
			return this.function.compute(ctx);
		}
		int index = localX | localZ << 4;
		long bit = 1L << (index & 63);
		if((this.kept[index >>> 6] & bit) != 0) {
			return this.values[index];
		}
		double value = this.function.compute(ctx);
		this.values[index] = value;
		this.kept[index >>> 6] |= bit;
		return value;
	}

	@Override
	public double minValue() {
		return this.function.minValue();
	}

	@Override
	public double maxValue() {
		return this.function.maxValue();
	}
}
