package raccoonman.reterraforged.world.worldgen.cell.terrain.populator;

import raccoonman.reterraforged.data.worldgen.preset.settings.TerrainSettings;
import raccoonman.reterraforged.world.worldgen.biome.Erosion;
import raccoonman.reterraforged.world.worldgen.biome.Weirdness;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.CellPopulator;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.RegionConfig;
import raccoonman.reterraforged.world.worldgen.cell.terrain.Terrain;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.function.CellFunction;
import raccoonman.reterraforged.world.worldgen.noise.function.DistanceFunction;
import raccoonman.reterraforged.world.worldgen.noise.function.EdgeFunction;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.util.Seed;

public class VolcanoPopulator implements CellPopulator, WeightedPopulator {
    private Noise cone;
    private Noise height;
    private Noise lowlands;
    private float inversionPoint;
    private float blendLower;
    private float blendUpper;
    private float blendRange;
    private float bias;
    private Terrain inner;
    private Terrain outer;
    
    private float weight;
    private float baseScale;
    private float verticalScale;
    
    // the volcano terrain settings: weight, and since they did nothing before, base scale (the ground under it),
    // vertical scale (the cone's height) and horizontal scale (the cone's and the lowlands' size); at 1 a volcano is
    // the same as before
    public VolcanoPopulator(Seed seed, RegionConfig region, Levels levels, TerrainSettings.Terrain settings) {
        float h = Math.max(0.01F, settings.horizontalScale);
        boolean scaled = settings.horizontalScale != 1.0F;
        float midpoint = 0.3F;
        float range = 0.3F;
        Noise heightLookup = Noises.perlin(seed.next(), 2, 1);
        heightLookup = Noises.map(heightLookup, 0.45F, 0.65F);
        
        Noise heightNoise = Noises.worley(region.seed(), region.scale(), CellFunction.NOISE_LOOKUP, DistanceFunction.EUCLIDEAN, heightLookup);
        heightNoise = Noises.warp(heightNoise, region.warpX(), region.warpZ(), region.warpStrength());
        this.height = heightNoise;
        
        Noise cone = Noises.worleyEdge(region.seed(), region.scale(), EdgeFunction.DISTANCE_2_DIV, DistanceFunction.EUCLIDEAN);
        if (scaled) {
            // the distance ratio to the region's center, shrunk or stretched: a wider or narrower cone in its region
            cone = Noises.clamp(Noises.mul(cone, 1.0F / h), 0.0F, 1.0F);
        }
        cone = Noises.invert(cone);
        cone = Noises.warp(cone, region.warpX(), region.warpZ(), region.warpStrength());
        cone = Noises.powCurve(cone, 11.0F);
        cone = Noises.clamp(cone, 0.475F, 1.0F);
        cone = Noises.map(cone, 0.0F, 1.0F);
        cone = Noises.gradient(cone, 0.0F, 0.5F, 0.5F);
        cone = scaled ? Noises.warpPerlin(cone, seed.next(), Math.max(1, Math.round(15 * h)), 2, 10.0F * h) : Noises.warpPerlin(cone, seed.next(), 15, 2, 10.0F);
        cone = Noises.mul(cone, this.height);
        
        this.cone = cone;
        
        Noise lowlands = scaled ? Noises.perlinRidge(seed.next(), Math.max(1, Math.round(150 * h)), 3) : Noises.perlinRidge(seed.next(), 150, 3);
        lowlands = scaled ? Noises.warpPerlin(lowlands, seed.next(), Math.max(1, Math.round(30 * h)), 1, 30.0F * h) : Noises.warpPerlin(lowlands, seed.next(), 30, 1, 30.0F);
        lowlands = Noises.mul(lowlands, 0.1F);
        
        this.lowlands = lowlands;
        this.inversionPoint = 0.94F;
        this.blendLower = midpoint - range / 2.0F;
        this.blendUpper = this.blendLower + range;
        this.blendRange = this.blendUpper - this.blendLower;
        this.outer = TerrainType.VOLCANO;
        this.inner = TerrainType.VOLCANO_PIPE;
        this.bias = levels.ground;
        
        this.weight = settings.weight;
        this.baseScale = settings.baseScale;
        this.verticalScale = settings.verticalScale;
    }
    
    public float weight() {
    	return this.weight;
    }
    
    @Override
    public void apply(Cell cell, float x, float z) {
        float value = this.cone.compute(x, z, 0);
        float limit = this.height.compute(x, z, 0);
        float maxHeight = limit * this.inversionPoint;
        cell.weirdness = Weirdness.LOW_SLICE_NORMAL_DESCENDING.mid();
        cell.erosion = Erosion.LEVEL_4.mid();
        if (value > maxHeight) {
            float steepnessModifier = 1.0F;
            float delta = (value - maxHeight) * steepnessModifier;
            float range = limit - maxHeight;
            float alpha = delta / range;
            if (alpha > 0.925F) {
                cell.terrain = this.inner;
            }
            value = maxHeight - maxHeight / 5.0F * alpha;
        } else if (value < this.blendLower) {
            value += this.lowlands.compute(x, z, 0);
            cell.terrain = this.outer;
        } else if (value < this.blendUpper) {
            float alpha2 = 1.0F - (value - this.blendLower) / this.blendRange;
            value += this.lowlands.compute(x, z, 0) * alpha2;
            cell.terrain = this.outer;
        }
        cell.height = this.bias * this.baseScale + value * this.verticalScale;
    }
    
    public static void modifyVolcanoType(Cell cell, Levels levels) {
        if (cell.terrain == TerrainType.VOLCANO_PIPE && (cell.height < levels.water || cell.riverMask < 0.85F)) {
            cell.terrain = TerrainType.VOLCANO;
        }
    }
}
