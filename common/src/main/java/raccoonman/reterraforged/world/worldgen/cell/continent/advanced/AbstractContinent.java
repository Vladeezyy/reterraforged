package raccoonman.reterraforged.world.worldgen.cell.continent.advanced;

import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings.ControlPoints;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.continent.ContinentDistances;
import raccoonman.reterraforged.world.worldgen.cell.continent.SimpleContinent;
import raccoonman.reterraforged.world.worldgen.cell.continent.simple.SimpleRiverGenerator;
import raccoonman.reterraforged.world.worldgen.cell.rivermap.RiverCache;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.util.Seed;

public abstract class AbstractContinent implements SimpleContinent {
    protected final int seed;
    protected final int skippingSeed;
    protected int continentScale;
    protected final float jitter;
    protected final boolean hasSkipping;
    protected final float skipThreshold;
    protected final RiverCache riverCache;
    protected final ControlPoints controlPoints;
    
    public AbstractContinent(Seed seed, GeneratorContext context) {
        WorldSettings settings = context.preset.world();
        this.seed = seed.next();
        this.skippingSeed = seed.next();
        this.continentScale = settings.continent.continentScale;
        this.jitter = settings.continent.continentJitter;
        this.skipThreshold = settings.continent.continentSkipping;
        this.hasSkipping = (this.skipThreshold > 0.0F);
        this.controlPoints = settings.controlPoints;
        this.riverCache = new RiverCache(new SimpleRiverGenerator(this, context));
    }
    
    @Override
    public float getDistanceToOcean(int cx, int cz, float dx, float dz) {
        return ContinentDistances.toOcean(this, this.controlPoints.shallowOcean, cx, cz, dx, dz);
    }
    
    @Override
    public float getDistanceToEdge(int cx, int cz, float dx, float dz) {
        return ContinentDistances.toEdge(this, this.continentScale, cx, cz, dx, dz);
    }

    protected boolean isDefaultContinent(int cellX, int cellY) {
        return cellX == 0 && cellY == 0;
    }
    
    protected boolean shouldSkip(int cellX, int cellY) {
        if (this.hasSkipping && !this.isDefaultContinent(cellX, cellY)) {
            float skipValue = getCellValue(this.skippingSeed, cellX, cellY);
            return skipValue < this.skipThreshold;
        }
        return false;
    }
    
    protected static float getCellValue(int seed, int cellX, int cellY) {
        return 0.5F + NoiseUtil.valCoord2D(seed, cellX, cellY) * 0.5F;
    }
}
