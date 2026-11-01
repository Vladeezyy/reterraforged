package raccoonman.reterraforged.world.worldgen.densityfunction.tile.filter;

import raccoonman.reterraforged.data.worldgen.preset.settings.FilterSettings;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;

public record Smoothing(float smoothingRadius, float smoothingRate, Modifier modifier) implements Filter {

	@Override
	public void apply(Filterable map, int seedX, int seedZ, int iterations) {
		while (iterations-- > 0) {
			this.apply(map);
		}
	}

	// The heights in an array, changed in place in the same order as before (a cell sees its smoothed neighbours above
	// and to the left): the same result to the bit, without walking the Cell objects for every neighbour. The
	// neighbours are always inside the tile here (the loop keeps the radius from the edges).
	private void apply(Filterable cellMap) {
		int radius = NoiseUtil.round(this.smoothingRadius + 0.5F);
		float radiusSq = this.smoothingRadius * this.smoothingRadius;
		int total = cellMap.getBlockSize().total();
		Cell[] cells = cellMap.getBacking();

		// the neighbours within the radius and their weights, in the order they were summed
		int count = 0;
		int[] offsets = new int[(2 * radius + 1) * (2 * radius + 1)];
		float[] weightOf = new float[offsets.length];
		float weights = 0.0F;
		for (int dz = -radius; dz <= radius; ++dz) {
			for (int dx = -radius; dx <= radius; ++dx) {
				float dist2 = (float) (dx * dx + dz * dz);
				if (dist2 <= radiusSq) {
					offsets[count] = dz * total + dx;
					weightOf[count] = 1.0F - dist2 / radiusSq;
					weights += weightOf[count];
					count++;
				}
			}
		}
		if (count == 0 || !(weights > 0.0F)) {
			return;
		}

		float[] height = new float[cells.length];
		for (int i = 0; i < cells.length; i++) {
			height[i] = cells[i].height;
		}
		int max = total - radius;
		for (int z = radius; z < max; ++z) {
			for (int x = radius; x < max; ++x) {
				int index = z * total + x;
				Cell cell = cells[index];
				if (!cell.erosionMask) {
					float sum = 0.0F;
					for (int n = 0; n < count; n++) {
						sum += height[index + offsets[n]] * weightOf[n];
					}
					float dif = height[index] - sum / weights;
					height[index] -= this.modifier.getValueModifier(height[index]) * Modifier.strength(cell) * (dif * this.smoothingRate);
				}
			}
		}
		for (int i = 0; i < cells.length; i++) {
			cells[i].height = height[i];
		}
	}
	
	public static Smoothing make(FilterSettings.Smoothing settings, Levels levels) {
		return new Smoothing(settings.smoothingRadius, settings.smoothingRate, Modifier.range(levels.ground(1), levels.ground(120)).invert());
	}
}
