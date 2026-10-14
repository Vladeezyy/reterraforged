package raccoonman.reterraforged.world.worldgen.densityfunction.tile.filter;

import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.filter.Filter.Visitor;

// clamp: ReTerraForged 0.0.6 keeps the gradient at 1 at most (MiscellaneousSettings.originalGeneration); the 1.21.1
// port lets it grow on steep slopes, which the erosion and the snow then read as steeper still
public record Steepness(int radius, float scaler, float waterLevel, boolean clamp) implements Filter, Visitor {

	@Override
	public void apply(Filterable cellMap, int seedX, int seedZ, int iterations) {
		this.iterate(cellMap, this);
	}

	@Override
	public void visit(Filterable cellMap, Cell cell, int cx, int cz) {
		float totalHeightDif = 0.0F;
		for (int dz = -1; dz <= 2; ++dz) {
			for (int dx = -1; dx <= 2; ++dx) {
				if (dx != 0 || dz != 0) {
					int x = cx + dx * this.radius;
					int z = cz + dz * this.radius;
					Cell neighbour = cellMap.getCellRaw(x, z);
					if (!neighbour.isAbsent()) {
						float height = Math.max(neighbour.height, this.waterLevel);
						totalHeightDif += Math.abs(cell.height - height) / this.radius;
					}
				}
			}
		}
		cell.gradient = this.clamp ? Math.min(1.0F, totalHeightDif * this.scaler) : totalHeightDif * this.scaler;
	}
	
	public static Steepness make(int radius, float scaler, Levels levels, boolean clamp) {
		return new Steepness(radius, scaler, levels.water, clamp);
	}
}
