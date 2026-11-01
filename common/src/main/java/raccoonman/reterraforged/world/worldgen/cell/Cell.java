package raccoonman.reterraforged.world.worldgen.cell;

import raccoonman.reterraforged.world.worldgen.cell.biome.type.BiomeType;
import raccoonman.reterraforged.world.worldgen.cell.terrain.Terrain;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.cell.terrain.fakewater.FakeWaterBiomeTarget;

public class Cell {
    private static final Cell DEFAULTS = new Cell();
    private static final Cell EMPTY = new Cell() {

    	@Override
        public boolean isAbsent() {
            return true;
        }
    };
    public float height;
    public float heightErosion;
    public float sediment;
    public float gradient;
    public float regionMoisture;
    public float regionTemperature;
    public float continentId;
    public float continentEdge;
    public float continentDistance;
    public float terrainRegionId;
    public float terrainRegionEdge;
    public float terrainRegionCenterX;
    public float terrainRegionCenterZ;
    public float biomeRegionId;
    public float biomeRegionEdge;
    public float macroBiomeId;
    public float riverMask;
    public int continentX;
    public int continentZ;
    public boolean erosionMask;
    public Terrain terrain;
    public BiomeType biome;
    public float erosion;
    public float weirdness;
    public float temperature;
    public float moisture;
    public FakeWaterBiomeTarget fakeWaterBiome;
    
    public float beachNoise;

    public Cell() {
        this.regionMoisture = 0.5F;
        this.regionTemperature = 0.5F;
        this.biomeRegionEdge = 1.0F;
        this.riverMask = 1.0F;
        this.erosionMask = false;
        this.terrain = TerrainType.NONE;
        this.biome = BiomeType.GRASSLAND;
        this.fakeWaterBiome = FakeWaterBiomeTarget.NONE;
    }
    
    public void copyFrom(Cell other) {
        this.height = other.height;
        this.heightErosion = other.heightErosion;
        this.sediment = other.sediment;
        this.gradient = other.gradient;
        this.regionMoisture = other.regionMoisture;
        this.regionTemperature = other.regionTemperature;
        this.continentId = other.continentId;
        this.continentEdge = other.continentEdge;
        this.continentDistance = other.continentDistance;
        this.terrainRegionId = other.terrainRegionId;
        this.terrainRegionEdge = other.terrainRegionEdge;
        this.terrainRegionCenterX = other.terrainRegionCenterX;
        this.terrainRegionCenterZ = other.terrainRegionCenterZ;
        this.biomeRegionId = other.biomeRegionId;
        this.biomeRegionEdge = other.biomeRegionEdge;
        this.macroBiomeId = other.macroBiomeId;
        this.riverMask = other.riverMask;
        this.continentX = other.continentX;
        this.continentZ = other.continentZ;
        this.erosionMask = other.erosionMask;
        this.terrain = other.terrain;
        this.biome = other.biome;
        this.erosion = other.erosion;
        this.weirdness = other.weirdness;
        this.temperature = other.temperature;
        this.moisture = other.moisture;
        this.fakeWaterBiome = other.fakeWaterBiome;
        this.beachNoise = other.beachNoise;
    }

    public Cell reset() {
        this.copyFrom(Cell.DEFAULTS);
        return this;
    }

    public boolean isAbsent() {
        return false;
    }

    public static Cell empty() {
        return Cell.EMPTY;
    }

    public interface Visitor {
        void visit(Cell cell, int x, int z);
    }
}
