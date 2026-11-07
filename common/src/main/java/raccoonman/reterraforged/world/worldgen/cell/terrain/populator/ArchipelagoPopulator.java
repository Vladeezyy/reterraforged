package raccoonman.reterraforged.world.worldgen.cell.terrain.populator;

import raccoonman.reterraforged.data.worldgen.preset.settings.IslandSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings.ControlPoints;
import raccoonman.reterraforged.world.worldgen.biome.Erosion;
import raccoonman.reterraforged.world.worldgen.biome.Weirdness;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.CellPopulator;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;

public class ArchipelagoPopulator implements CellPopulator {
	// Island Coast and Island Inland together: the ground still takes this much of the profile to rise, not a wall
	private static final float MIN_RISE = 0.05F;
	// blocks the low land of the coast rises above the beach, with the swells up to twice as much at most
	private static final int COAST_RISE = 2;
	// the beaches go on over this share of the low land of the coast, their border with the land behind it wandering by this much
	private static final float COAST_BEACH = 0.5F;
	private static final float COAST_BEACH_BORDER = 0.4F;
	// of the continent edge, over which the islands fade out towards a continent
	private static final float CONTINENT_FADE = 0.05F;
	// the weight of the ridges in the islands' relief; the continents' mountains stand out as they do on the continents
	private static final float RIDGE_WEIGHT = 0.5F, CONTINENT_RIDGE_WEIGHT = 1.0F;
	
	private final IslandSettings settings;
	private final Levels levels;
	private final ControlPoints controlPoints;
	// WorldSettings.Properties.generationFixes
	private final boolean fixes;
	// no island at a continent edge above this
	private final float continentLimit;
	private final float ridgeWeight;
	
	private final Noise sizeNoise;
	private final Noise densityNoise;
	private final Noise ridgeHeight;
	private final Noise hillHeight;
	private final Noise volcanoHeight;
	private final Noise mountainSelector;
	private final Noise volcanoSelector;
	private final Noise islandErosion;
	private final Noise islandWeirdness;
	private final Noise beachErosion;
	private final Noise beachWeirdness;
	
	public ArchipelagoPopulator(IslandSettings settings, Levels levels, ControlPoints controlPoints, int worldSeed, boolean fixes, Noise continentMountains) {
		this.settings = settings;
		this.levels = levels;
		this.controlPoints = controlPoints;
		this.fixes = fixes;
		// constant seeds before: the same islands in every world
		int seed = (fixes ? worldSeed : 0) + settings.seedOffset;
		// the square root: the open sea takes the lowest values of the continent edge, so a straight line changed
		// almost nothing up to the middle of the slider
		this.continentLimit = NoiseUtil.lerp(controlPoints.coast, CONTINENT_FADE, NoiseUtil.sqrt(settings.continentDistance));
		this.ridgeWeight = continentMountains != null ? CONTINENT_RIDGE_WEIGHT : RIDGE_WEIGHT;
		
		int size = Math.round(settings.islandSize);
		float hScale = Math.max(0.1F, settings.islandHorizontalScale);
		// the scale of the islands' terrain: it also scaled their outline before, so it changed their size
		float shapeScale = fixes ? 1.0F : hScale;
		
		IslandSettings.CoastNoise coast = settings.coastNoise;
		Noise sizeN = Noises.simplex(seed + 1273, Math.max(1, Math.round(size * 3.5F / shapeScale)), coast.octaves, coast.lacunarity, coast.gain);
		sizeN = Noises.warpPerlin(sizeN, seed + 1273, Math.max(1, Math.round(size * 2.0F / shapeScale)), 2, size * 0.5F / shapeScale);
		sizeN = Noises.warpPerlin(sizeN, seed + 4830, Math.max(1, Math.round(size * 0.5F / shapeScale)), 1, size * 0.3F / shapeScale);
		sizeN = Noises.clamp(sizeN, 0.0F, 1.0F);
		this.sizeNoise = sizeN;
		
		Noise densityN = Noises.simplex(seed + 9735, 4000, 3);
		densityN = Noises.warpPerlin(densityN, seed + 9735, 2000, 2, 1000.0F);
		densityN = Noises.clamp(densityN, 0.0F, 1.0F);
		this.densityNoise = densityN;
		
		Noise ridge = Noises.perlinRidge(seed + 4829, Math.max(1, Math.round(size * 1.6F / hScale)), 4, 2.1F, 0.82F);
		ridge = Noises.warpPerlin(ridge, seed + 4830, Math.max(1, Math.round(size * 0.9F / hScale)), 2, size * 0.35F / hScale);
		ridge = Noises.clamp(ridge, 0.0F, 1.0F);
		this.ridgeHeight = continentMountains != null ? Noises.clamp(continentMountains, 0.0F, 1.0F) : ridge;
		
		Noise hills = Noises.billow(seed + 3811, Math.max(1, Math.round(size * 0.35F / hScale)), 3, 2.25F, 0.55F);
		hills = Noises.warpPerlin(hills, seed + 3812, Math.max(1, Math.round(size * 0.7F / hScale)), 1, size * 0.2F / hScale);
		hills = Noises.clamp(hills, 0.0F, 1.0F);
		this.hillHeight = hills;
		
		Noise volcano = Noises.perlinRidge(seed + 6721, Math.max(1, Math.round(size * 0.45F / hScale)), 3, 2.4F, 0.9F);
		volcano = Noises.powCurve(volcano, 1.8F);
		volcano = Noises.clamp(volcano, 0.0F, 1.0F);
		this.volcanoHeight = volcano;
		
		this.mountainSelector = Noises.clamp(Noises.simplex(seed + 11867, Math.max(1, Math.round(size * 1.25F / hScale)), 2), 0.0F, 1.0F);
		this.volcanoSelector = Noises.clamp(Noises.simplex(seed + 22193, Math.max(1, Math.round(size * 1.75F / hScale)), 2), 0.0F, 1.0F);
		
		this.islandErosion = Erosion.LEVEL_4.source();
		this.islandWeirdness = Weirdness.MID_SLICE_NORMAL_DESCENDING.source();
		this.beachErosion = Erosion.LEVEL_4.source();
		this.beachWeirdness = Weirdness.MID_SLICE_NORMAL_DESCENDING.source();
	}

	@Override
	public void apply(Cell cell, float x, float z) {
		float sizeValue = this.sizeNoise.compute(x, z, 0);
		float densityValue = this.densityNoise.compute(x, z, 0);
		float densityThreshold = NoiseUtil.clamp(1.0F - this.settings.islandDensity * 0.8F, 0.05F, 0.98F);
		
		float shapeAlpha = smoothStep(0.5F, 1.0F, sizeValue);
		float densityFade = NoiseUtil.clamp((1.0F - densityThreshold) * 0.5F, 0.04F, 0.12F);
		float densityAlpha = smoothStep(densityThreshold, densityThreshold + densityFade, densityValue);
		float islandAlpha = shapeAlpha * densityAlpha;
		if (this.settings.continentDistance > 0.0F) {
			islandAlpha *= 1.0F - smoothStep(this.continentLimit - CONTINENT_FADE, this.continentLimit, cell.continentEdge);
		}
		if (islandAlpha <= 0.001F) {
			return;
		}
		
		float beachWidth = NoiseUtil.clamp(Math.max(0.05F, this.settings.beachWidth), 0.05F, 0.45F);
		float beachCoverage = NoiseUtil.clamp(this.settings.beachCoverage, 0.0F, 1.0F);
		float shelfEnd = NoiseUtil.clamp(beachWidth * 0.65F, 0.04F, 0.35F);
		float beachEnd = NoiseUtil.clamp(shelfEnd + beachWidth * (0.5F + beachCoverage * 1.5F), shelfEnd + 0.05F, 0.85F);
		
		float oceanHeight = cell.height;
		int offshoreDepth = Math.max(2, Math.round(4.0F + this.settings.offshoreDepth * 10.0F));
		float shelfTarget = Math.max(oceanHeight, this.levels.water(-offshoreDepth));
		float shelfAlpha = smoothStep(0.0F, shelfEnd, islandAlpha);
		float shelfHeight = NoiseUtil.lerp(oceanHeight, shelfTarget, shelfAlpha);
		
		float beachAlpha = smoothStep(shelfEnd, beachEnd, islandAlpha);
		float beachHeight;
		if (this.fixes) {
			// out of the water in the first quarter of the band, the rest of it a beach rising 2 blocks. Before, one curve
			// from the shelf to the ground: only the last fifth of the band came out of the water, by one block
			beachHeight = beachAlpha < 0.25F ? NoiseUtil.lerp(shelfHeight, this.levels.ground, beachAlpha / 0.25F) : NoiseUtil.lerp(this.levels.ground, this.levels.ground(2), (beachAlpha - 0.25F) / 0.75F);
		} else {
			beachHeight = NoiseUtil.lerp(shelfHeight, this.levels.ground, beachAlpha);
		}
		
		// Island Coast and Island Inland are points of the island's profile (0 its outer edge, 1 the middle of the largest islands),
		// as Coast and Inland are of a continent's: up to Island Coast the low land of the coast, from there the ground rises, the
		// more gradually the further apart they are, and from Island Inland on it is the island's inland with its whole relief
		float coastPoint = Math.max(this.controlPoints.islandCoast, beachEnd);
		float inlandPoint = Math.max(this.controlPoints.islandInland, coastPoint + MIN_RISE);
		float riseAlpha = smoothStep(coastPoint, inlandPoint, islandAlpha);
		
		float mountainGate = chanceMask(this.mountainSelector, this.settings.mountainChance, x, z);
		float volcanoGate = chanceMask(this.volcanoSelector, this.settings.volcanoChance, x, z);
		float hills = this.hillHeight.compute(x, z, 0);
		// rolling hills inland on every island, a little more where there are mountains
		float hillValue = hills * (0.35F + mountainGate * 0.55F);
		float ridgeValue = this.ridgeHeight.compute(x, z, 0) * mountainGate;
		float volcanoValue = this.volcanoHeight.compute(x, z, 0) * volcanoGate;
		float mountainValue = NoiseUtil.clamp(hillValue * 0.35F + ridgeValue * this.ridgeWeight + volcanoValue * 0.75F, 0.0F, 1.0F);
		
		float baseHeight = this.settings.islandHeight * (0.015F + this.settings.islandBaseScale * 0.08F);
		float reliefHeight = mountainValue * riseAlpha * this.settings.islandHeight * this.settings.islandVerticalScale * 0.3F;
		float targetHeight = this.levels.ground + baseHeight + reliefHeight;
		// the coast's own land is not a flat disc: a few blocks up towards the point, with low swells
		float coastAlpha = coastPoint > beachEnd ? NoiseUtil.clamp((islandAlpha - beachEnd) / (coastPoint - beachEnd), 0.0F, 1.0F) : 1.0F;
		float coastHeight = beachHeight + (coastPoint > beachEnd ? coastAlpha * this.levels.scale(COAST_RISE) * (0.5F + hills) : 0.0F);
		cell.height = NoiseUtil.lerp(coastHeight, Math.max(targetHeight, coastHeight), riseAlpha);
		cell.continentEdge = Math.max(cell.continentEdge, continentEdge(islandAlpha, shelfEnd, coastPoint, inlandPoint));
		
		if (islandAlpha < shelfEnd) {
			cell.terrain = TerrainType.SHALLOW_OCEAN;
		} else if (islandAlpha < beachEnd || coastAlpha + (hills - 0.5F) * COAST_BEACH_BORDER < COAST_BEACH) {
			cell.terrain = TerrainType.ISLAND_BEACH;
		} else if (riseAlpha > 0.35F && mountainValue > 0.35F && (mountainGate > 0.1F || volcanoGate > 0.2F)) {
			cell.terrain = TerrainType.ISLAND_MOUNTAINS;
		} else {
			cell.terrain = TerrainType.ISLAND;
		}
		
		if (islandAlpha >= shelfEnd) {
			if (cell.terrain == TerrainType.ISLAND_BEACH) {
				cell.erosion = this.beachErosion.compute(x, z, 0);
				cell.weirdness = this.beachWeirdness.compute(x, z, 0);
			} else {
				// the inland: far from the sea this level of erosion has the biomes of high ground, the level of the rest of the land the
				// same biome whatever the continentalness, so the biome table itself cannot tell a coast's land from an inland
				cell.erosion = islandAlpha >= inlandPoint ? Erosion.LEVEL_2.mid() : this.islandErosion.compute(x, z, 0);
				cell.weirdness = this.islandWeirdness.compute(x, z, 0);
			}
		}
	}
	
	// as on a continent: below Beach the coast, from Beach to Inland land further and further from it
	private float continentEdge(float islandAlpha, float shelfEnd, float coastPoint, float inlandPoint) {
		if (islandAlpha < shelfEnd) {
			float alpha = smoothStep(0.0F, shelfEnd, islandAlpha);
			return NoiseUtil.lerp(this.controlPoints.deepOcean, this.controlPoints.shallowOcean, alpha);
		}
		if (islandAlpha < coastPoint) {
			float alpha = smoothStep(shelfEnd, coastPoint, islandAlpha);
			return NoiseUtil.lerp(this.controlPoints.shallowOcean, this.controlPoints.beach, alpha);
		}
		float alpha = smoothStep(coastPoint, inlandPoint, islandAlpha);
		return NoiseUtil.lerp(this.controlPoints.beach, this.controlPoints.inland, alpha);
	}
	
	private static float chanceMask(Noise selector, float chance, float x, float z) {
		chance = NoiseUtil.clamp(chance, 0.0F, 1.0F);
		if (chance <= 0.0F) {
			return 0.0F;
		}
		if (chance >= 1.0F) {
			return 1.0F;
		}
		float threshold = 1.0F - chance;
		return smoothStep(threshold, Math.min(1.0F, threshold + 0.2F), selector.compute(x, z, 0));
	}
	
	private static float smoothStep(float min, float max, float value) {
		if (max <= min) {
			return value >= max ? 1.0F : 0.0F;
		}
		float alpha = NoiseUtil.clamp((value - min) / (max - min), 0.0F, 1.0F);
		return alpha * alpha * (3.0F - 2.0F * alpha);
	}
}
