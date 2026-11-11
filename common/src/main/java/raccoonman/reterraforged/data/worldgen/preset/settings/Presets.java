package raccoonman.reterraforged.data.worldgen.preset.settings;

import raccoonman.reterraforged.data.worldgen.preset.settings.ClimateSettings.BiomeNoise;
import raccoonman.reterraforged.data.worldgen.preset.settings.ClimateSettings.BiomeShape;
import raccoonman.reterraforged.data.worldgen.preset.settings.ClimateSettings.RangeValue;
import raccoonman.reterraforged.data.worldgen.preset.settings.FilterSettings.Erosion;
import raccoonman.reterraforged.data.worldgen.preset.settings.FilterSettings.Smoothing;
import raccoonman.reterraforged.data.worldgen.preset.settings.RiverSettings.Lake;
import raccoonman.reterraforged.data.worldgen.preset.settings.RiverSettings.River;
import raccoonman.reterraforged.data.worldgen.preset.settings.RiverSettings.Wetland;
import raccoonman.reterraforged.data.worldgen.preset.settings.TerrainSettings.General;
import raccoonman.reterraforged.data.worldgen.preset.settings.TerrainSettings.Terrain;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings.Continent;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings.ControlPoints;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings.Properties;
import raccoonman.reterraforged.world.worldgen.noise.function.DistanceFunction;

public class Presets {

	// the "Default" of ReTerraForged 0.0.6: TerraForged's default settings
	public static Preset makeDefault() {
		Preset preset = makeLegacyDefault();
		// new presets keep the biome mods' swamps low (#28); without them all swamps are that low anyway
		preset.climate().swampMaxHeight = 12;
		return preset;
	}

	// presets that each show one side of the mod; all are "Default" with a few settings changed

	// continents with groups of big islands off their coasts, the spawn on an island
	public static Preset makeArchipelago() {
		Preset preset = makeDefault();
		preset.island().enableArchipelago = true;
		preset.island().islandDensity = 0.8F;
		preset.island().islandSize = 450.0F;
		preset.island().continentDistance = 0.3F;
		preset.island().volcanoChance = 0.15F;
		preset.world().properties.spawnType = SpawnType.ISLANDS;
		return preset;
	}

	// wide swamps just above the sea, more lakes and wetlands
	public static Preset makeWetlands() {
		Preset preset = makeDefault();
		preset.climate().swampBasins = 0.25F;
		preset.climate().lowlandSwamps = 12;
		preset.rivers().lakes.chance = 0.5F;
		preset.rivers().wetlands.chance = 0.9F;
		return preset;
	}

	// a world 384 blocks high where mountain chains take a third of the land and reach about 215 blocks above the sea
	// (generation is slower than "Default")
	public static Preset makeToweringPeaks() {
		Preset preset = makeDefault();
		preset.world().properties.worldHeight = 384;
		preset.climate().biomeShape.biomeSize = 300;
		preset.terrain().mountains.weight = 10.0F;
		preset.terrain().mountains.verticalScale = 1.5F;
		preset.terrain().hills.weight = 3.0F;
		preset.terrain().plains.weight = 0.8F;
		preset.terrain().steppe.weight = 0.3F;
		preset.terrain().dales.weight = 0.8F;
		return preset;
	}

	// one land without oceans, crossed by more rivers and lakes
	public static Preset makeEndlessContinent() {
		Preset preset = makeDefault();
		preset.world().continent.continentType = ContinentType.INFINITE;
		preset.rivers().riverCount = 12;
		preset.rivers().lakes.chance = 0.45F;
		return preset;
	}

	// many rivers in wide valleys that follow their bends, gorges and lakes
	public static Preset makeRiverValleys() {
		Preset preset = makeDefault();
		preset.rivers().riverCount = 14;
		preset.rivers().valleyWidth = 1.6F;
		preset.rivers().valleyFollowsMeanders = true;
		preset.rivers().gorgeChance = 0.45F;
		preset.rivers().lakes.chance = 0.5F;
		return preset;
	}

	// dense forests, grass and flowers
	public static Preset makeLushWilds() {
		Preset preset = makeDefault();
		preset.miscellaneous().treeDensity = 1.6F;
		preset.miscellaneous().grassDensity = 1.5F;
		preset.miscellaneous().plantDensity = 1.8F;
		return preset;
	}

	// wide, gentle plains and steppes with few mountains: room to build
	public static Preset makeRollingPlains() {
		Preset preset = makeDefault();
		preset.terrain().plains.weight = 5.0F;
		preset.terrain().steppe.weight = 4.0F;
		preset.terrain().hills.weight = 1.0F;
		preset.terrain().mountains.weight = 0.6F;
		preset.terrain().volcano.weight = 1.0F;
		preset.terrain().general.globalVerticalScale = 0.75F;
		return preset;
	}

	// plateaus, badlands, torridonian steps, hills and volcanoes instead of plains
	public static Preset makeRuggedHighlands() {
		Preset preset = makeDefault();
		preset.terrain().plateau.weight = 6.0F;
		preset.terrain().badlands.weight = 5.0F;
		preset.terrain().torridonian.weight = 6.0F;
		preset.terrain().hills.weight = 4.0F;
		preset.terrain().volcano.weight = 6.0F;
		preset.terrain().plains.weight = 0.5F;
		preset.terrain().steppe.weight = 0.3F;
		preset.terrain().dales.weight = 0.5F;
		return preset;
	}

	public static Preset makeLegacyDefault() {
		return new Preset(
			new WorldSettings(
				new Continent(ContinentType.MULTI_IMPROVED, DistanceFunction.EUCLIDEAN, 3000, 0.7F, 0.25F, 0.25F, 5, 0.26F, 4.33F),
				new ControlPoints(ControlPoints.DEFAULT_ISLAND_INLAND, ControlPoints.DEFAULT_ISLAND_COAST, 0.1F, 0.25F, 0.327F, 0.448F, 0.502F), 
				new Properties(SpawnType.CONTINENT_CENTER, 320, 64, 63, -54)
			), 
			new SurfaceSettings(new SurfaceSettings.Erosion(30, 140, 40, 95, 0.65F, 0.475F, 0.4F)),
			new CaveSettings(0.0F, 1.5625F, 1.0F, 1.0F, 1.0F, 0.14285715F, 0.07F, 0.02F, true, false),
			new ClimateSettings(
				new RangeValue(0, 6, 2, 0.0F, 0.98F, 0.05F), 
				new RangeValue(0, 6, 1, 0.0F, 1.0F, 0.0F), 
				new BiomeShape(225, 8, 150, 80),
				new BiomeNoise(ClimateSettings.BiomeNoise.EdgeType.SIMPLEX, 24, 2, 0.5F, 2.65F, 14)
			), 
			new TerrainSettings(
				new General(0, 1200, 0.98F, 1.0F, true, true),
				new Terrain(1.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F),
				new Terrain(1.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(1.5F, 1.0F, 1.0F, 1.0F), 
				new Terrain(1.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(5.0F, 1.0F, 1.0F, 1.0F)
		    ), 
			IslandSettings.makeDefault(),
			FakeWaterBiomeSettings.makeDefault(),
			new RiverSettings(
				0, 8, 
				new River(5, 2, 6, 20, 8, 0.75F),
				new River(4, 1, 4, 14, 5, 0.975F), 
				new Lake(0.3F, 0.0F, 0.0F, 10, 75, 150, 2, 10),
				new Wetland(0.6F, 175, 225)
			), 
			new FilterSettings(
				new Erosion(135, 12, 0.7F, 0.7F, 0.5F, 0.5F),
				new Smoothing(1, 1.8F, 0.9F)
			), 
			new StructureSettings(),
			new MiscellaneousSettings(true, 600, true, true, true, false, true, true, true, true, true, 1.0F, 0.0F)
		); 
	}
	
	public static Preset makeLegacyVanillaish() {
		return new Preset(
			new WorldSettings(
				new Continent(ContinentType.MULTI, DistanceFunction.EUCLIDEAN, 2000, 0.763F, 0.25F, 0.25F, 5, 0.26F, 4.33F),
				new ControlPoints(ControlPoints.DEFAULT_ISLAND_INLAND, ControlPoints.DEFAULT_ISLAND_COAST, 0.1F, 0.25F, 0.326F, 0.448F, 0.5F), 
				new Properties(SpawnType.WORLD_ORIGIN, 320, 64, 63, -54)
			), 
			new SurfaceSettings(new SurfaceSettings.Erosion(30, 140, 40, 95, 0.65F, 0.475F, 0.4F)),
			new CaveSettings(1.0F, 1.5625F, 1.0F, 1.0F, 1.0F, 0.15F, 0.07F, 0.021F, true, false),
			new ClimateSettings(
				new RangeValue(0, 4, 1, 0.0F, 0.98F, 0.05F), 
				new RangeValue(0, 5, 1, 0.0F, 1.0F, 0.0F), 
				new BiomeShape(176, 6, 150, 80),
				new BiomeNoise(ClimateSettings.BiomeNoise.EdgeType.SIMPLEX, 24, 2, 0.5F, 2.65F, 14)
			), 
			new TerrainSettings(
				new General(0, 690, 0.629F, 0.629F, false, true),
				new Terrain(1.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(1.25F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F),
				new Terrain(2.5F, 1.0F, 1.0F, 1.0F), 
				new Terrain(1.5F, 1.0F, 1.0F, 1.0F), 
				new Terrain(3.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(3.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(6.0F, 1.0F, 1.0F, 1.0F)
			), 
			IslandSettings.makeDefault(),
			FakeWaterBiomeSettings.makeDefault(),
			new RiverSettings(
				0, 11, 
				new River(5, 2, 6, 20, 8, 0.507F),
				new River(4, 1, 4, 14, 5, 0.493F), 
				new Lake(0.462F, 0.0F, 0.0F, 10, 75, 150, 2, 10),
				new Wetland(0.796F, 196, 255)
			), 
			new FilterSettings(
				new Erosion(100, 12, 0.699F, 0.699F, 0.5F, 0.5F),
				new Smoothing(2, 1.8F, 0.75F)
			), 
			new StructureSettings(),
			new MiscellaneousSettings(false, 600, false, true, false, false, false, false, true, true, true, 1.0F, 0.0F)
		); 
	}
	
	//TODO make mushroom islands bigger
	public static Preset makeLegacyBeautiful() {
		return new Preset(
			new WorldSettings(
				new Continent(ContinentType.MULTI, DistanceFunction.EUCLIDEAN, 3000, 0.8F, 0.25F, 0.25F, 5, 0.26F, 4.33F),
				new ControlPoints(ControlPoints.DEFAULT_ISLAND_INLAND, ControlPoints.DEFAULT_ISLAND_COAST, 0.1F, 0.25F, 0.326F, 0.448F, 0.5F), 
				new Properties(SpawnType.CONTINENT_CENTER, 320, 64, 63, -54)
			), 
			new SurfaceSettings(new SurfaceSettings.Erosion(30, 140, 40, 95, 0.65F, 0.475F, 0.4F)),
			new CaveSettings(0.0F, 1.5625F, 1.0F, 1.0F, 1.0F, 0.14285715F, 0.07F, 0.02F, true, false),
			new ClimateSettings(
				new RangeValue(0, 7, 1, 0.0F, 1.0F, -0.004F), 
				new RangeValue(0, 6, 1, 0.0F, 1.0F, 0.0F), 
				new BiomeShape(185, 8, 150, 80),
				new BiomeNoise(ClimateSettings.BiomeNoise.EdgeType.SIMPLEX, 24, 2, 0.5F, 2.65F, 14)
			), 
			new TerrainSettings(
				new General(0, 1356, 1.0F, 1.175F, true, true),
				new Terrain(1.519F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.164F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.706F, 1.0F, 1.0F, 1.0F),
				new Terrain(1.184F, 1.0F, 1.0F, 1.0F),
				new Terrain(2.576F, 1.0F, 1.0F, 1.0F), 
				new Terrain(1.493F, 1.0F, 1.0F, 1.0F), 
				new Terrain(3.555F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.911F, 1.0F, 1.0F, 1.0F),
				new Terrain(7.5F, 1.0F, 1.0F, 1.0F)
			), 
			IslandSettings.makeDefault(),
			FakeWaterBiomeSettings.makeDefault(),
			new RiverSettings(
				0, 14, 
				new River(5, 1, 4, 20, 5, 0.504F),
				new River(3, 1, 2, 12, 4, 0.5F), 
				new Lake(0.671F, 0.0F, 0.0F, 8, 75, 150, 2, 7),
				new Wetland(0.865F, 134, 201)
			), 
			new FilterSettings(
				new Erosion(175, 12, 0.648F, 0.657F, 0.5F, 0.5F),
				new Smoothing(1, 1.855F, 0.916F)
			), 
			new StructureSettings(),
			new MiscellaneousSettings(true, 684, true, true, true, false, true, true, true, true, false, 1.0F, 0.0F)
		); 
	}
	
	public static Preset makeLegacyLite() {
		return new Preset(
			new WorldSettings(
				new Continent(ContinentType.MULTI, DistanceFunction.EUCLIDEAN, 2000, 0.765F, 0.25F, 0.25F, 5, 0.26F, 4.33F),
				new ControlPoints(ControlPoints.DEFAULT_ISLAND_INLAND, ControlPoints.DEFAULT_ISLAND_COAST, 0.1F, 0.25F, 0.326F, 0.448F, 0.5F), 
				new Properties(SpawnType.CONTINENT_CENTER, 320, 64, 63, -54)
			),
			new SurfaceSettings(new SurfaceSettings.Erosion(30, 140, 40, 95, 0.65F, 0.475F, 0.4F)),
			new CaveSettings(0.0F, 1.5625F, 1.0F, 1.0F, 1.0F, 0.14285715F, 0.07F, 0.02F, false, false),
			new ClimateSettings(
				new RangeValue(0, 4, 1, 0.0F, 0.98F, 0.05F),
				new RangeValue(0, 5, 1, 0.0F, 1.0F, 0.0F), 
				new BiomeShape(176, 6, 150, 80),
				new BiomeNoise(ClimateSettings.BiomeNoise.EdgeType.SIMPLEX, 24, 2, 0.5F, 2.65F, 14)
			), 
			new TerrainSettings(
				new General(0, 690, 0.629F, 0.629F, false, true),
				new Terrain(1.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(1.25F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F),
				new Terrain(2.5F, 1.0F, 1.0F, 1.0F), 
				new Terrain(1.5F, 1.0F, 1.0F, 1.0F), 
				new Terrain(3.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(3.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(6.0F, 1.0F, 1.0F, 1.0F)
			), 
			IslandSettings.makeDefault(),
			FakeWaterBiomeSettings.makeDefault(),
			new RiverSettings(
				0, 11, 
				new River(5, 2, 6, 20, 8, 0.507F),
				new River(4, 1, 4, 14, 5, 0.493F), 
				new Lake(0.462F, 0.0F, 0.0F, 10, 75, 150, 2, 10),
				new Wetland(0.796F, 196, 255)
			), 
			new FilterSettings(
				new Erosion(100, 12, 0.699F, 0.699F, 0.5F, 0.5F),
				new Smoothing(2, 1.799F, 0.75F)
			), 
			new StructureSettings(),
			new MiscellaneousSettings(true, 600, false, true, true, false, true, true, true, true, true, 1.0F, 0.0F)
		);
	}
	
	public static Preset makeLegacyHugeBiomes() {
		return new Preset(
			new WorldSettings(
				new Continent(ContinentType.MULTI, DistanceFunction.EUCLIDEAN, 4029, 0.8F, 0.25F, 0.25F, 5, 0.26F, 4.33F),
				new ControlPoints(ControlPoints.DEFAULT_ISLAND_INLAND, ControlPoints.DEFAULT_ISLAND_COAST, 0.1F, 0.25F, 0.326F, 0.448F, 0.5F), 
				new Properties(SpawnType.CONTINENT_CENTER, 320, 64, 63, -54)
			), 
			new SurfaceSettings(new SurfaceSettings.Erosion(30, 140, 40, 95, 0.65F, 0.475F, 0.4F)),
			new CaveSettings(0.0F, 1.5625F, 1.0F, 1.0F, 1.0F, 0.14285715F, 0.07F, 0.02F, true, false),
			new ClimateSettings(
				new RangeValue(0, 4, 2, 0.0F, 1.0F, 0.097F), 
				new RangeValue(0, 3, 1, 0.0F, 1.0F, 0.0F), 
				new BiomeShape(402, 5, 180, 110),
				new BiomeNoise(ClimateSettings.BiomeNoise.EdgeType.PERLIN2, 24, 1, 0.5F, 2.65F, 60)
			), 
			new TerrainSettings(
				new General(0, 1507, 1.0F, 1.175F, true, true),
				new Terrain(1.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F),
				new Terrain(1.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(1.5F, 1.0F, 1.0F, 1.0F), 
				new Terrain(1.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.0F, 1.0F, 1.0F, 1.0F), 
				new Terrain(2.5F, 1.0F, 1.0F, 1.0F),
				new Terrain(5.0F, 1.0F, 1.0F, 1.0F)
			), 
			IslandSettings.makeDefault(),
			FakeWaterBiomeSettings.makeDefault(),
			new RiverSettings(
				0, 13, 
				new River(5, 1, 5, 28, 9, 0.27F),
				new River(3, 1, 3, 18, 3, 0.7F), 
				new Lake(0.595F, 0.0F, 0.0F, 10, 75, 150, 2, 10),
				new Wetland(0.86F, 175, 225)
			), 
			new FilterSettings(
				new Erosion(165, 15, 0.612F, 0.652F, 0.5F, 0.5F),
				new Smoothing(1, 1.799F, 0.898F)
			), 
			new StructureSettings(),
			new MiscellaneousSettings(true, 721, true, true, true, false, true, true, true, true, false, 1.0F, 0.0F)
		);
	}
}
