package raccoonman.reterraforged.world.worldgen.cell.terrain.fakewater;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import raccoonman.reterraforged.data.worldgen.preset.settings.FakeWaterBiomeSettings;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.cell.terrain.Terrain;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;

// Fake Water Biomes: land below sea level that the sea stands in gets the river biome, per kind of terrain. Steppe and
// badlands since 0.0.21; plateau, hills, mountains and plains since 0.0.32 (a canyon of a plateau kept its land biome)
class FakeWaterBiomeResolverTest {
	private static final Levels LEVELS = new Levels(256, 63);

	private static FakeWaterBiomeTarget resolve(FakeWaterBiomeSettings settings, Terrain terrain, float height) {
		Cell cell = new Cell();
		cell.terrain = terrain;
		cell.height = height;
		new FakeWaterBiomeResolver(settings, LEVELS).apply(cell);
		return cell.fakeWaterBiome;
	}

	private static FakeWaterBiomeSettings on() {
		FakeWaterBiomeSettings settings = FakeWaterBiomeSettings.makeDefault();
		settings.enableFakeWaterBiomes = true;
		return settings;
	}

	@Test
	void aPresetWithoutTheNewSwitchesKeepsItsBiomes() {
		// what a world made by 0.0.31 with Fake Water Biomes on has: steppe and badlands only
		FakeWaterBiomeSettings settings = new FakeWaterBiomeSettings(true, true, true, 0.0F);
		float below = LEVELS.water - 0.02F;
		assertEquals(FakeWaterBiomeTarget.RIVER, resolve(settings, TerrainType.STEPPE, below));
		assertEquals(FakeWaterBiomeTarget.RIVER, resolve(settings, TerrainType.BADLANDS, below));
		for (Terrain terrain : new Terrain[] {TerrainType.PLATEAU, TerrainType.HILLS, TerrainType.MOUNTAINS_1, TerrainType.MOUNTAIN_CHAIN, TerrainType.FLATS}) {
			assertEquals(FakeWaterBiomeTarget.NONE, resolve(settings, terrain, below), terrain.getName());
		}
		// and the default of a new preset is the same
		for (Terrain terrain : new Terrain[] {TerrainType.PLATEAU, TerrainType.HILLS, TerrainType.MOUNTAINS_1, TerrainType.FLATS}) {
			assertEquals(FakeWaterBiomeTarget.NONE, resolve(on(), terrain, below), terrain.getName());
		}
	}

	@Test
	void eachSwitchTakesItsTerrainBelowTheSea() {
		float below = LEVELS.water - 0.02F, above = LEVELS.water + 0.02F;
		FakeWaterBiomeSettings plateau = on();
		plateau.plateauBelowSeaLevelRiverBiomes = true;
		assertEquals(FakeWaterBiomeTarget.RIVER, resolve(plateau, TerrainType.PLATEAU, below));
		assertEquals(FakeWaterBiomeTarget.NONE, resolve(plateau, TerrainType.PLATEAU, above));
		assertEquals(FakeWaterBiomeTarget.NONE, resolve(plateau, TerrainType.HILLS, below));
		// where two kinds meet the terrain is named after both
		assertEquals(FakeWaterBiomeTarget.RIVER, resolve(plateau, TerrainType.registerComposite(TerrainType.HILLS, TerrainType.PLATEAU), below));

		FakeWaterBiomeSettings hills = on();
		hills.hillsBelowSeaLevelRiverBiomes = true;
		assertEquals(FakeWaterBiomeTarget.RIVER, resolve(hills, TerrainType.HILLS, below));
		assertEquals(FakeWaterBiomeTarget.NONE, resolve(hills, TerrainType.FLATS, below));

		FakeWaterBiomeSettings mountains = on();
		mountains.mountainsBelowSeaLevelRiverBiomes = true;
		for (Terrain terrain : new Terrain[] {TerrainType.MOUNTAINS_1, TerrainType.MOUNTAINS_2, TerrainType.MOUNTAINS_3, TerrainType.MOUNTAIN_CHAIN}) {
			assertEquals(FakeWaterBiomeTarget.RIVER, resolve(mountains, terrain, below), terrain.getName());
		}
		// the mountains of islands stand in the ocean: their ground below the sea is the sea floor
		assertEquals(FakeWaterBiomeTarget.NONE, resolve(mountains, TerrainType.ISLAND_MOUNTAINS, below));
		assertEquals(FakeWaterBiomeTarget.NONE, resolve(mountains, TerrainType.VOLCANO, below));

		FakeWaterBiomeSettings plains = on();
		plains.plainsBelowSeaLevelRiverBiomes = true;
		assertEquals(FakeWaterBiomeTarget.RIVER, resolve(plains, TerrainType.FLATS, below));
		assertEquals(FakeWaterBiomeTarget.NONE, resolve(plains, TerrainType.PLATEAU, below));
	}

	@Test
	void theSeaAndTheRiversAreNotTouched() {
		FakeWaterBiomeSettings all = on();
		all.plateauBelowSeaLevelRiverBiomes = all.hillsBelowSeaLevelRiverBiomes = all.mountainsBelowSeaLevelRiverBiomes = all.plainsBelowSeaLevelRiverBiomes = true;
		float below = LEVELS.water - 0.02F;
		for (Terrain terrain : new Terrain[] {TerrainType.DEEP_OCEAN, TerrainType.SHALLOW_OCEAN, TerrainType.COAST, TerrainType.BEACH, TerrainType.RIVER, TerrainType.LAKE, TerrainType.WETLAND, TerrainType.ISLAND, TerrainType.ARCHIPELAGO}) {
			assertEquals(FakeWaterBiomeTarget.NONE, resolve(all, terrain, below), terrain.getName());
		}
		// the master switch
		all.enableFakeWaterBiomes = false;
		assertEquals(FakeWaterBiomeTarget.NONE, resolve(all, TerrainType.PLATEAU, below));
	}
}
