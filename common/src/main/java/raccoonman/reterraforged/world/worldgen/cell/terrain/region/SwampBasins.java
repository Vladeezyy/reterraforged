package raccoonman.reterraforged.world.worldgen.cell.terrain.region;

import raccoonman.reterraforged.world.worldgen.biome.Humidity;
import raccoonman.reterraforged.world.worldgen.biome.Temperature;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.CellPopulator;
import raccoonman.reterraforged.world.worldgen.cell.biome.type.BiomeType;
import raccoonman.reterraforged.world.worldgen.cell.climate.ClimateModule;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.cell.terrain.Populators;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.cell.terrain.populator.TerrainPopulator;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.util.Seed;

/**
 * Swamp Basins (ClimateSettings.swampBasins, #28): some of the terrain regions whose center has a wet, not cold climate
 * are flat land just above the sea, with or without a river. Lowland Swamps alone can only give a swamp's climate to low land
 * that exists, and that is mostly river valleys, so its swamps stay narrow. The other terrain regions keep what they
 * have, and the region's border is the same plains as every terrain region's.
 *
 * The climate of a whole basin is that of its center, so it is one swamp and not one per biome cell (225 blocks, which
 * would leave no room for a big structure). That includes the biome cell's id: it is TerraBlender's uniqueness, and with
 * Biomes O' Plenty each biome cell could be in another TerraBlender region and have another swamp. The biome itself is still
 * picked by the biome tables. Not built with the setting off.
 */
public final class SwampBasins implements CellPopulator {
	// added to a basin's biome cell id, TerraBlender's uniqueness: MixinParameterList takes it off again and does not shift the
	// climate of another mod's region, which over a basin would give many swamps side by side
	public static final float HELD_CLIMATE = 2.0F;
	// a basin's temperature and humidity are this far from its center's, up or down: the basins differ, so all of a biome
	// mod's swamps get their turn; the region shift of MixinParameterList did that per place
	private static final float VARIETY = 0.25F;
	// the blocks above the sea that belong to a basin: the rim, where its border rises to the plains, up to here
	private static final int TOP = 8;
	// the mounds' largest height in blocks; the bottom is flush with the water, where the swamps' surface rules make pools
	private static final int MOUNDS = 4;
	// how much sooner than other terrain the floor of a basin takes over from the plains of the region's border: with their
	// blend the flat floor is a small part of the region, and in the rest only the carved river valleys are low enough
	private static final float BLEND = 2.0F;

	private final CellPopulator regions;
	private final CellPopulator border;
	private final TerrainPopulator basin;
	private final ClimateModule climate;
	private final int seed;
	private final int threshold;
	private final float top;
	// a region only changes every few hundred cells, and only the chosen ones get here
	private final ThreadLocal<Center> center = ThreadLocal.withInitial(Center::new);

	private static final class Center {
		private boolean valid;
		private float id, temperature, moisture, biomeRegion;
		private boolean basin;
	}

	private SwampBasins(CellPopulator regions, CellPopulator border, TerrainPopulator basin, ClimateModule climate, int seed, int percent, float top) {
		this.regions = regions;
		this.border = border;
		this.basin = basin;
		this.climate = climate;
		this.seed = seed;
		this.threshold = (int) (Integer.MAX_VALUE * (double) Math.min(percent, 100) / 100.0D);
		this.top = top;
	}

	/**
	 * @param regions the terrain regions blended into their border
	 * @param border the terrain of the regions' border
	 * @param percent of the terrain regions with a wet, not cold center
	 */
	public static SwampBasins make(CellPopulator regions, CellPopulator border, Seed seed, Levels levels, ClimateModule climate, int percent) {
		// its own seed: the other noises keep theirs
		Seed basinSeed = seed.offset(6173);
		Noise mounds = Noises.perlin(basinSeed.next(), 96, 2);
		mounds = Noises.mul(Noises.pow(mounds, 2.0F), levels.scale(MOUNDS));
		Noise bottom = Noises.constant(levels.water + levels.unit * 0.25F);
		TerrainPopulator basin = new TerrainPopulator(TerrainType.FLATS, bottom, mounds, Populators.DEFAULT_EROSION, Populators.LOWLAND_WEIRDNESS, 1.0F);
		return new SwampBasins(regions, border, basin, climate, basinSeed.next(), percent, levels.water(TOP));
	}

	@Override
	public void apply(Cell cell, float x, float z) {
		if (this.center(cell) == null) {
			this.regions.apply(cell, x, z);
			return;
		}
		float alpha = cell.terrainRegionEdge * BLEND;
		if (alpha >= 1.0F) {
			this.basin.apply(cell, x, z);
			return;
		}
		this.border.apply(cell, x, z);
		float height = cell.height;
		float erosion = cell.erosion;
		float weirdness = cell.weirdness;
		this.basin.apply(cell, x, z);
		cell.height = NoiseUtil.lerp(height, cell.height, alpha);
		cell.erosion = NoiseUtil.lerp(erosion, cell.erosion, alpha);
		cell.weirdness = NoiseUtil.lerp(weirdness, cell.weirdness, alpha);
	}

	/** The low land of a basin: at most the height of its rim, in a basin's region. Most land stops at the height */
	public boolean contains(Cell cell) {
		return cell.height <= this.top && this.center(cell) != null;
	}

	/** A cell this contains: the climate of its center */
	public void applyClimate(Cell cell) {
		Center center = this.center(cell);
		cell.temperature = center.temperature;
		cell.moisture = center.moisture;
		cell.biomeRegionId = center.biomeRegion;
	}

	private Center center(Cell cell) {
		float id = cell.terrainRegionId;
		if (!this.chosen(id)) {
			return null;
		}
		Center center = this.center.get();
		if (!center.valid || center.id != id) {
			Cell sample = new Cell();
			// the climate is what the center's biome cell has; mask false: the terrain is not looked at
			this.climate.apply(sample, cell.terrainRegionCenterX, cell.terrainRegionCenterZ, cell.terrainRegionCenterX, cell.terrainRegionCenterZ, false);
			center.id = id;
			center.basin = isWet(sample.biome);
			int hash = this.hash(id);
			float temperature = sample.temperature + VARIETY * ((hash >>> 8 & 0xFF) / 127.5F - 1.0F);
			float moisture = Math.max(sample.moisture, Humidity.LEVEL_3.mid()) + VARIETY * ((hash >>> 16 & 0xFF) / 127.5F - 1.0F);
			center.temperature = Math.max(temperature, Temperature.LEVEL_1.min());
			center.moisture = Math.max(moisture, Humidity.LEVEL_3.min());
			center.biomeRegion = sample.biomeRegionId + HELD_CLIMATE;
			center.valid = true;
		}
		return center.basin ? center : null;
	}

	// a region's id is uniform but picks its terrain (RegionSelector): mixed with the seed, the chosen regions are not all of one terrain
	private boolean chosen(float id) {
		return (this.hash(id) & Integer.MAX_VALUE) < this.threshold;
	}

	private int hash(float id) {
		int hash = Float.floatToRawIntBits(id) ^ this.seed;
		hash *= 0x9E3779B1;
		hash ^= hash >>> 15;
		hash *= 0x85EBCA77;
		hash ^= hash >>> 13;
		return hash;
	}

	// the wet, not cold types of the climate; deserts have wet biome cells too, but no swamps belong in them
	private static boolean isWet(BiomeType type) {
		return type == BiomeType.TROPICAL_RAINFOREST || type == BiomeType.TEMPERATE_RAINFOREST || type == BiomeType.TEMPERATE_FOREST || type == BiomeType.TAIGA;
	}
}
