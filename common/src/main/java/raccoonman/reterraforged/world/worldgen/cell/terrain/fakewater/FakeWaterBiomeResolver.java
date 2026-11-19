package raccoonman.reterraforged.world.worldgen.cell.terrain.fakewater;

import raccoonman.reterraforged.data.worldgen.preset.settings.FakeWaterBiomeSettings;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.cell.terrain.Terrain;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;

public class FakeWaterBiomeResolver {
	private final FakeWaterBiomeSettings settings;
	private final Levels levels;
	
	public FakeWaterBiomeResolver(FakeWaterBiomeSettings settings, Levels levels) {
		this.settings = settings;
		this.levels = levels;
	}
	
	public void apply(Cell cell) {
		cell.fakeWaterBiome = this.resolve(cell);
	}
	
	private FakeWaterBiomeTarget resolve(Cell cell) {
		float waterline = this.levels.water - Math.max(0.0F, this.settings.heightOffset);
		if (!this.settings.enableFakeWaterBiomes || cell.height >= waterline) {
			return FakeWaterBiomeTarget.NONE;
		}
		Terrain terrain = cell.terrain;
		if (this.settings.steppeBelowSeaLevelRiverBiomes && isSteppe(terrain)) {
			return FakeWaterBiomeTarget.RIVER;
		}
		if (this.settings.badlandsBelowSeaLevelOceanBiomes && isBadlands(terrain)) {
			return FakeWaterBiomeTarget.RIVER;
		}
		if (this.settings.plateauBelowSeaLevelRiverBiomes && isPlateau(terrain)) {
			return FakeWaterBiomeTarget.RIVER;
		}
		if (this.settings.hillsBelowSeaLevelRiverBiomes && isHills(terrain)) {
			return FakeWaterBiomeTarget.RIVER;
		}
		if (this.settings.mountainsBelowSeaLevelRiverBiomes && isMountains(terrain)) {
			return FakeWaterBiomeTarget.RIVER;
		}
		if (this.settings.plainsBelowSeaLevelRiverBiomes && isPlains(terrain)) {
			return FakeWaterBiomeTarget.RIVER;
		}
		return FakeWaterBiomeTarget.NONE;
	}
	
	public static boolean isSteppe(Terrain terrain) {
		return terrain == TerrainType.STEPPE || terrain.getName().contains("steppe");
	}
	
	public static boolean isBadlands(Terrain terrain) {
		return terrain == TerrainType.BADLANDS || terrain.getName().contains("badlands");
	}

	// by the name, as steppe and badlands: where two kinds of terrain meet the cell has a terrain named after both
	public static boolean isPlateau(Terrain terrain) {
		return terrain == TerrainType.PLATEAU || terrain.getName().contains("plateau");
	}

	public static boolean isHills(Terrain terrain) {
		return terrain == TerrainType.HILLS || terrain.getName().contains("hills");
	}

	// the mountains of the mainland: those of islands stand in the ocean, their ground below the sea is the sea floor
	public static boolean isMountains(Terrain terrain) {
		String name = terrain.getName();
		return name.contains("mountain") && !name.contains("island");
	}

	// "Plains" on the terrain page
	public static boolean isPlains(Terrain terrain) {
		return terrain == TerrainType.FLATS || terrain.getName().contains("flats");
	}
}
