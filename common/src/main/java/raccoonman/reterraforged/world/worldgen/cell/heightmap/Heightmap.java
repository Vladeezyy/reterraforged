package raccoonman.reterraforged.world.worldgen.cell.heightmap;

import net.minecraft.core.HolderGetter;
import raccoonman.reterraforged.data.worldgen.preset.PresetNoiseData;
import raccoonman.reterraforged.data.worldgen.preset.PresetTerrainTypeNoise;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.TerrainSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.IslandSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings.ControlPoints;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.biome.Erosion;
import raccoonman.reterraforged.world.worldgen.biome.Humidity;
import raccoonman.reterraforged.world.worldgen.biome.Temperature;
import raccoonman.reterraforged.world.worldgen.biome.Weirdness;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.CellPopulator;
import raccoonman.reterraforged.world.worldgen.cell.climate.Climate;
import raccoonman.reterraforged.world.worldgen.cell.continent.Continent;
import raccoonman.reterraforged.world.worldgen.cell.continent.ContinentLerper2;
import raccoonman.reterraforged.world.worldgen.cell.continent.ContinentLerper3;
import raccoonman.reterraforged.world.worldgen.cell.rivermap.Rivermap;
import raccoonman.reterraforged.world.worldgen.cell.terrain.Blender;
import raccoonman.reterraforged.world.worldgen.cell.terrain.ITerrain;
import raccoonman.reterraforged.world.worldgen.cell.terrain.IslandBlender;
import raccoonman.reterraforged.world.worldgen.cell.terrain.Populators;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainCategory;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.cell.terrain.fakewater.FakeWaterBiomeResolver;
import raccoonman.reterraforged.world.worldgen.cell.terrain.populator.ArchipelagoPopulator;
import raccoonman.reterraforged.world.worldgen.cell.terrain.populator.VolcanoPopulator;
import raccoonman.reterraforged.world.worldgen.cell.terrain.provider.TerrainProvider;
import raccoonman.reterraforged.world.worldgen.cell.terrain.region.RegionLerper;
import raccoonman.reterraforged.world.worldgen.cell.terrain.region.RegionModule;
import raccoonman.reterraforged.world.worldgen.cell.terrain.region.RegionSelector;
import raccoonman.reterraforged.world.worldgen.cell.terrain.region.SwampBasins;
import raccoonman.reterraforged.world.worldgen.noise.function.DistanceFunction;
import raccoonman.reterraforged.world.worldgen.noise.function.EdgeFunction;
import raccoonman.reterraforged.world.worldgen.noise.function.Interpolation;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.util.Seed;

public record Heightmap(CellPopulator terrain, CellPopulator region, Continent continent, Climate climate, Levels levels, ControlPoints controlPoints, float terrainFrequency, Noise beachNoise, FakeWaterBiomeResolver fakeWaterBiomeResolver, float mountainBiomeUsage, Noise mountainBiomeMask, float lowlandSwampTop, SwampBasins swampBasins, boolean mixedLowlands) {
	
	public void apply(Cell cell, float x, float z, boolean applyClimate) {
		this.applyTerrain(cell, x, z);
		this.applyRivers(cell, x, z, this.continent.getRivermap(cell));
		this.applyClimate(cell, x, z, applyClimate);
		// after the climate: the same climate as a tile's cell (TileGenerator), so a biome asked here is the chunk's
		if (cell.terrain == TerrainType.ISLAND_BEACH) {
			cell.terrain = TerrainType.BEACH;
		}
	}
	
	public void applyTerrain(Cell cell, float x, float z) {
        cell.terrain = TerrainType.FLATS;
        cell.beachNoise = this.beachNoise.compute(x, z, 0);
        this.continent.apply(cell, x, z);
        this.region.apply(cell, x, z);
        this.terrain.apply(cell, x * this.terrainFrequency, z * this.terrainFrequency);
        this.keepFinite(cell);
	}
	
	public void applyRivers(Cell cell, float x, float z, Rivermap rivermap) {
        rivermap.apply(cell, x, z);
        VolcanoPopulator.modifyVolcanoType(cell, this.levels);
        this.keepFinite(cell);
	}

	// The last guard: a height that is not a number would reach the noise router and the features. With the smallest
	// global horizontal scale the noise coordinates pass the int range near the world border, where the noises give
	// infinities; such a point is sea level with a neutral erosion and weirdness.
	private void keepFinite(Cell cell) {
		if (!Float.isFinite(cell.height)) {
			cell.height = this.levels.water;
		}
		if (!Float.isFinite(cell.riverMask)) {
			cell.riverMask = 1.0F;
		}
		if (!Float.isFinite(cell.erosion)) {
			cell.erosion = 0.0F;
		}
		if (!Float.isFinite(cell.weirdness)) {
			cell.weirdness = 0.0F;
		}
	}
	
	public void applyClimate(Cell cell, float x, float z, boolean applyClimate) {
		// Mountain Biome Usage: the share of the mountains with mountain biomes (peaks, slopes); the rest gets the
		// ordinary biomes of its climate (the erosion and weirdness of rolling land). 1 (presets saved before 0.0.20,
		// when the setting did nothing): all of them
		if (this.mountainBiomeUsage < 1.0F && cell.terrain.isMountain() && this.mountainBiomeMask.compute(x, z, 0) > this.mountainBiomeUsage) {
			cell.erosion = Erosion.LEVEL_4.mid();
			cell.weirdness = Weirdness.MID_SLICE_NORMAL_DESCENDING.mid();
		}
		float riverValleyThreshold = 0.675F;
        if(cell.riverMask < riverValleyThreshold && !isIslandTerrain(cell)) {
        	cell.erosion = 0.445F;
        	cell.weirdness = 0.34F;
        }
        
        if(cell.terrain.isRiver()) {
            cell.erosion = -0.05F;
            cell.weirdness = -0.03F;
        }
        
        if(cell.terrain.isLake() && cell.height < this.levels.water) {
            cell.erosion = Erosion.LEVEL_4.mid();
            cell.weirdness = -0.03F;
        }
        if(cell.terrain.isWetland()) {
        	cell.erosion = Erosion.LEVEL_6.mid();
        	cell.weirdness = Weirdness.VALLEY.mid();
        }
        
        this.climate.apply(cell, x, z, applyClimate);
        // Swamp Basins: one climate over the low land of a basin, so that it is one swamp
        if (applyClimate && this.swampBasins != null && this.swampBasins.contains(cell) && (this.isLowland(cell) || cell.terrain.isWetland())) {
        	this.swampBasins.applyClimate(cell);
        }
        // Lowland Swamps (ClimateSettings.lowlandSwamps): wet, not cold, flat or rolling land a few blocks above the
        // sea gets the swamps' climate as vanilla's flat lowlands do (the highest erosion, a valley's weirdness), so the
        // swamps can be wide; the climate is from the height before the tile's erosion, the same with and without tiles
        if (applyClimate && this.isLowlandSwamp(cell)) {
        	cell.erosion = Erosion.LEVEL_6.mid();
        	cell.weirdness = Weirdness.VALLEY.mid();
        }
        this.fakeWaterBiomeResolver.apply(cell);

        // not an island's beach: only this sign of the weirdness has beaches in the biome table
        if(cell.riverMask >= riverValleyThreshold && cell.macroBiomeId > 0.5F && cell.terrain != TerrainType.ISLAND_BEACH) { 
        	cell.weirdness = -cell.weirdness;
        }
	}
	
	// the cell's climate sampled: wet, not cold, flat or rolling land a few blocks above the sea (ClimateSettings.lowlandSwamps)
	public boolean isLowlandSwamp(Cell cell) {
		if (cell.height <= this.levels.water) {
			return false;
		}
		// the terrain last: with both settings off every column of land stops at the first of these
		boolean lowlandSwamp = cell.height <= this.lowlandSwampTop && cell.moisture >= Humidity.LEVEL_3.min() && cell.temperature >= Temperature.LEVEL_1.min();
		return (lowlandSwamp || this.swampBasins != null && this.swampBasins.contains(cell)) && this.isLowland(cell);
	}

	// low enough for Lowland Swamps in any climate: the swamps' climate is not given to this land, but a biome mod may keep a
	// swamp for a dry or cold place (Terralith's orchid swamp and ice marsh, SwampHeights)
	public boolean isLowSwampLand(Cell cell) {
		return cell.height > this.levels.water && cell.height <= this.lowlandSwampTop && this.isLowland(cell);
	}

	// flat or rolling land (not beaches, coasts, rivers, lakes, wetlands, islands, mountains or volcanoes)
	// mixedLowlands (generation fixes): also the mixed terrains. The delegate of one is a terrain, never a category, so all of them
	// failed here and Lowland Swamps left most terrain regions out; presets saved before keep that, and their swamps
	private boolean isLowland(Cell cell) {
		ITerrain category = this.mixedLowlands ? cell.terrain.getCategory() : cell.terrain.getDelegate();
		return (category == TerrainCategory.FLATLAND || category == TerrainCategory.LOWLAND) && !cell.terrain.isMountain() && !cell.terrain.isVolcano();
	}

	public static Heightmap make(GeneratorContext ctx) {
    	HolderGetter<Noise> noiseLookup = ctx.noiseLookup;
    	
        Preset preset = ctx.preset;
        WorldSettings world = ctx.preset.world();
        ControlPoints controlPoints = world.controlPoints;

        TerrainSettings terrainSettings = preset.terrain();
        TerrainSettings.General general = terrainSettings.general;
        float globalVerticalScale = general.globalVerticalScale;
        
        Seed regionWarp = ctx.seed.offset(8934);
        int regionWarpScale = 400;
        int regionWarpStrength = 200;
        
        RegionConfig regionConfig = new RegionConfig(
        	ctx.seed.root() + 789124, 
        	general.terrainRegionSize, 
        	Noises.simplex(regionWarp.next(), regionWarpScale, 1),
        	Noises.simplex(regionWarp.next(), regionWarpScale, 1), 
        	regionWarpStrength
        );
        Levels levels = ctx.levels;
        float terrainFrequency = 1.0F / terrainSettings.general.globalHorizontalScale;
        CellPopulator region = new RegionModule(regionConfig);

        Seed mountainSeed = ctx.seed.offset(general.terrainSeedOffset);
        Noise mountainShape = Noises.worleyEdge(mountainSeed.next(), general.legacyMountainScaling ? 1000 : 2250, EdgeFunction.DISTANCE_2_ADD, DistanceFunction.EUCLIDEAN);
        mountainShape = Noises.warpPerlin(mountainShape, mountainSeed.next(), 333, 2, 250.0F);
        mountainShape = Noises.curve(mountainShape, Interpolation.CURVE3);
        mountainShape = Noises.clamp(mountainShape, 0.0F, 0.9F);
        mountainShape = Noises.map(mountainShape, 0.0F, 1.0F);
        // where the mountain chains are follows the mountains' horizontal scale too, with legacy scaling as well (#9)
        mountainShape = Populators.horizontal(mountainShape, terrainSettings.mountains.horizontalScale);

        Noise ground = PresetNoiseData.getNoise(noiseLookup, PresetTerrainTypeNoise.GROUND);
        
        CellPopulator terrainRegions = new RegionSelector(TerrainProvider.generateTerrain(ctx.seed, terrainSettings, regionConfig, levels, noiseLookup));
        CellPopulator terrainRegionBorders = Populators.makeBorder(ctx.seed, ground, terrainSettings.plains, terrainSettings.steppe, globalVerticalScale);
        CellPopulator mountains = Populators.makeMountainChain(mountainSeed, ground, terrainSettings.mountains, terrainSettings.general.legacyMountainScaling ? 1.0F : terrainSettings.mountains.horizontalScale * 2.25F, terrainSettings.general.legacyMountainScaling ? globalVerticalScale : globalVerticalScale * terrainSettings.mountains.verticalScale, general.fancyMountains, general.legacyMountainScaling);
        Continent continent = world.continent.continentType.create(ctx.seed, ctx);
        Climate climate = Climate.make(continent, ctx);
        // no seed is taken from ctx.seed here: without basins the seeds of everything else are the same
        CellPopulator terrainBlend = new RegionLerper(terrainRegionBorders, terrainRegions);
        SwampBasins swampBasins = preset.climate().swampBasins > 0.0F ? SwampBasins.make(terrainBlend, terrainRegionBorders, ctx.seed, levels, climate.biomeNoise(), preset.climate().swampBasins) : null;
        if (swampBasins != null) {
        	terrainBlend = swampBasins;
        }
        CellPopulator land = new Blender(mountainShape, terrainBlend, mountains, 0.3F, 0.8F, 0.575F);
        
        CellPopulator deepOcean = Populators.makeDeepOcean(ctx.seed.next(), levels.water, world.properties.oceanDepth, oceanFloor(world.properties, levels));
        CellPopulator shallowOcean = Populators.makeShallowOcean(ctx.levels);
        CellPopulator coast = Populators.makeCoast(ctx.levels);
        
        //pass coast/ocean spline to makeIslandPopulator instead of deepOcean
        CellPopulator oceans = new ContinentLerper3(deepOcean, shallowOcean, coast, controlPoints.deepOcean, controlPoints.shallowOcean, controlPoints.coast);
        CellPopulator terrain = new ContinentLerper2(oceans, land, controlPoints.shallowOcean, controlPoints.inland);
        
        if (ctx.preset.island().enableArchipelago) {
            terrain = new IslandBlender(terrain, new ArchipelagoPopulator(ctx.preset.island(), ctx.levels, controlPoints, ctx.seed.root(), world.properties.generationFixes, islandMountains(ctx, ground, terrainSettings)), ctx.levels);
        }

        Noise beachNoise = Noises.perlin2(ctx.seed.next(), 20, 1);
        beachNoise = Noises.mul(beachNoise, ctx.levels.scale(5));
        FakeWaterBiomeResolver fakeWaterBiomeResolver = new FakeWaterBiomeResolver(ctx.preset.fakeWaterBiomes(), ctx.levels);
        // large patches, about the size of a mountain range; its own seed, so the other noises keep theirs
        Noise mountainBiomeMask = Noises.perlin(ctx.seed.offset(5781).next(), 700, 2);
        // off: no height is at most the water's
        int lowlandSwamps = preset.climate().lowlandSwamps;
        float lowlandSwampTop = lowlandSwamps > 0 ? levels.water(lowlandSwamps) : levels.water;
        return new Heightmap(terrain, region, continent, climate, levels, controlPoints, terrainFrequency, beachNoise, fakeWaterBiomeResolver, Math.min(1.0F, preset.miscellaneous().mountainBiomeUsage), mountainBiomeMask, lowlandSwampTop, swampBasins, world.properties.generationFixes);
	}

	// the noise of the continents' mountains for the islands, at the islands' scale; its own seed, so the other noises keep theirs
	private static Noise islandMountains(GeneratorContext ctx, Noise ground, TerrainSettings terrain) {
		IslandSettings island = ctx.preset.island();
		if (!island.continentMountains) {
			return null;
		}
		Noise height = Populators.makeMountains(ctx.seed.offset(6143 + island.seedOffset), ground, terrain.mountains, 1.0F, 1.0F, terrain.general.fancyMountains, false).height();
		return Populators.horizontal(height, island.islandHorizontalScale);
	}

	// The lowest the deep ocean floor may go. It stopped at y 0 (heights are not negative), so an ocean depth above
	// about 96 changed nearly nothing: now it may go down to 24 blocks above the world bottom (where the bottom slide
	// starts) and stays above the lava level (water below it would be lava). Only for a depth above the default: the
	// default noise dips a little below 0 too, and its worlds must stay as they are.
	private static float oceanFloor(WorldSettings.Properties properties, Levels levels) {
		if (properties.oceanDepth <= WorldSettings.Properties.DEFAULT_OCEAN_DEPTH) {
			return 0.0F;
		}
		// 8 above the lava level: the noise interpolation and caves take parts of the floor a few blocks lower
		int floorY = Math.max(-properties.worldDepth + 24, properties.lavaLevel + 8);
		return Math.min(floorY, 0) / (float) levels.worldHeight;
	}

	private static boolean isIslandTerrain(Cell cell) {
	    return cell.terrain == TerrainType.ISLAND || cell.terrain == TerrainType.ISLAND_BEACH || cell.terrain == TerrainType.ISLAND_MOUNTAINS;
	}
}
