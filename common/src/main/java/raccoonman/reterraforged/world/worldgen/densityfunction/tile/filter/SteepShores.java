package raccoonman.reterraforged.world.worldgen.densityfunction.tile.filter;

import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Shores;

// The stony shores of a tile (Shores, #38, #40): the last filter, after the beaches are spread over whole quarts
// (NoiseCorrection), on the heights the chunks get. Only the shores read their neighbours.
public record SteepShores(Levels levels) implements Filter {

	@Override
	public void apply(Filterable map, int seedX, int seedZ, int iterations) {
		int total = map.getBlockSize().total();
		for (int z = 0; z < total; z++) {
			for (int x = 0; x < total; x++) {
				Cell cell = map.getCellRaw(x, z);
				if (!Shores.isShore(cell, this.levels)) {
					continue;
				}
				float limit = Shores.limit(cell, this.levels);
				if (rises(map, total, x + Shores.NEAR, z, limit) || rises(map, total, x - Shores.NEAR, z, limit)
					|| rises(map, total, x, z + Shores.NEAR, limit) || rises(map, total, x, z - Shores.NEAR, limit)) {
					Shores.makeStony(cell);
				}
			}
		}
	}

	// getCellRaw takes an index: a place off the tile's side would be one on the next row
	private static boolean rises(Filterable map, int total, int x, int z, float limit) {
		if (x < 0 || z < 0 || x >= total || z >= total) {
			return false;
		}
		Cell neighbour = map.getCellRaw(x, z);
		return !neighbour.isAbsent() && neighbour.height >= limit;
	}
}
