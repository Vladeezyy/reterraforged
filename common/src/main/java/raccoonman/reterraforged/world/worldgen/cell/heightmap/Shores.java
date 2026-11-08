package raccoonman.reterraforged.world.worldgen.cell.heightmap;

import raccoonman.reterraforged.world.worldgen.biome.Erosion;
import raccoonman.reterraforged.world.worldgen.biome.Weirdness;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainCategory;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;

/**
 * Steep shores (#38, #40). Where the ground rises STEEP_RISE blocks or more within NEAR blocks of a shore, the shore gets
 * the climate of the biome table's stony shores (the coast's continentalness, a low erosion, a middle slice of the
 * weirdness) instead of a beach's. Every shore used to be a beach: at the foot of a slope the beach biome (picked every
 * 4 blocks, with a jitter) carried its sand a few blocks up, where the erosion decorator turned the grass and dirt of the
 * slope to rock and left the sand (ErodeFeature); and no shore was eroded enough for the biomes of steep coasts (Regions Unexplored 0.6's
 * Chalk Cliffs: a coast's continentalness, erosion -0.6 or less, temperature -0.1 to 0.2, through Lithostitched). The
 * erosion is the lowest level's, where the biome table has stony shores too. The tiles decide it from their
 * neighbour cells (SteepShores, the chunks' biomes), the lookups without a tile from the heights before the tile's erosion
 * (WorldLookup).
 */
public final class Shores {
	public static final int NEAR = 8;
	public static final int STEEP_RISE = 6;
	public static final float EROSION = Erosion.LEVEL_0.mid();
	public static final float WEIRDNESS = Weirdness.MID_SLICE_NORMAL_DESCENDING.mid();

	private Shores() {
	}

	/** The shores CellSampler gives the coast's continentalness: an island's beach, a beach up to a few blocks above the sea */
	public static boolean isShore(Cell cell, Levels levels) {
		return cell.terrain == TerrainType.ISLAND_BEACH || (cell.terrain != null && cell.terrain.getDelegate() == TerrainCategory.BEACH && cell.height + cell.beachNoise < levels.water(5));
	}

	/** The highest the ground may stand NEAR blocks from a shore that is not steep */
	public static float limit(Cell shore, Levels levels) {
		return Math.max(shore.height, levels.water) + levels.scale(STEEP_RISE);
	}

	public static void makeStony(Cell cell) {
		// CellSampler gives an island's beach a beach's erosion and weirdness whatever the cell holds: a beach like any other
		if (cell.terrain == TerrainType.ISLAND_BEACH) {
			cell.terrain = TerrainType.BEACH;
		}
		cell.erosion = EROSION;
		cell.weirdness = WEIRDNESS;
	}
}
