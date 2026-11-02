package raccoonman.reterraforged.world.worldgen.cell.continent.simple;

import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings.ControlPoints;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.continent.ContinentDistances;
import raccoonman.reterraforged.world.worldgen.cell.continent.SimpleContinent;
import raccoonman.reterraforged.world.worldgen.cell.rivermap.LegacyRiverCache;
import raccoonman.reterraforged.world.worldgen.cell.rivermap.RiverCache;
import raccoonman.reterraforged.world.worldgen.cell.rivermap.Rivermap;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil.Vec2f;
import raccoonman.reterraforged.world.worldgen.noise.domain.Domain;
import raccoonman.reterraforged.world.worldgen.noise.domain.Domains;
import raccoonman.reterraforged.world.worldgen.noise.function.DistanceFunction;
import raccoonman.reterraforged.world.worldgen.noise.function.EdgeFunction;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;
import raccoonman.reterraforged.world.worldgen.util.Seed;

public abstract class ContinentGenerator implements SimpleContinent {
    protected final int seed;
    protected final float frequency;
    protected final int continentScale;
    private final DistanceFunction distanceFunc;
    private final ControlPoints controlPoints;
    private final float clampMin;
    private final float clampMax;
    private final float clampRange;
    private final float offsetAlpha;
    protected final Domain warp;
    protected final Noise shape;
    protected final RiverCache cache;
    
    public ContinentGenerator(Seed seed, GeneratorContext context) {
        WorldSettings settings = context.preset.world();
        int tectonicScale = settings.continent.continentScale * 4;
        this.continentScale = settings.continent.continentScale / 2;
        this.seed = seed.next();
        this.distanceFunc = settings.continent.continentShape;
        this.controlPoints = settings.controlPoints;
        this.frequency = 1.0F / tectonicScale;
        this.clampMin = 0.2F;
        this.clampMax = 1.0F;
        this.clampRange = this.clampMax - this.clampMin;
        this.offsetAlpha = context.preset.world().continent.continentJitter;
        
        Domain warp = Domains.domainPerlin(seed.next(), 20, 2, 20.0F);
        warp = Domains.compound(warp, Domains.domainSimplex(seed.next(), this.continentScale, 3, this.continentScale));
        this.warp = warp;
        
        Noise shape = Noises.simplex(seed.next(), settings.continent.continentScale * 2, 1);
        shape = Noises.add(shape, 0.65F);
        shape = Noises.clamp(shape, 0.0F, 1.0F);
        this.shape = shape;

        this.cache = new LegacyRiverCache(new SimpleRiverGenerator(this, context));
    }
    
    @Override
    public Rivermap getRivermap(int x, int y) {
        return this.cache.getRivers(x, y);
    }
    
    @Override
    public void apply(Cell cell, float x, float y) {
        this.locate(cell, x, y);
    }
    
    // a fresh cell for the answer (river generation and the climate's land value ask these, not every cell)
    @Override
    public float getEdgeValue(float x, float y) {
        Cell cell = new Cell();
        this.locate(cell, x, y);
        return cell.continentEdge;
    }
    
    @Override
    public long getNearestCenter(float x, float z) {
        Cell cell = new Cell();
        this.locate(cell, x, z);
        return PosUtil.pack(cell.continentX, cell.continentZ);
    }
    
    // the nearest continent cell: its id, center and edge value (SingleContinentGenerator.apply overrides apply, not this)
    private void locate(Cell cell, float x, float y) {
        float ox = this.warp.getOffsetX(x, y, 0);
        float oz = this.warp.getOffsetZ(x, y, 0);
        float px = x + ox;
        float py = y + oz;
        px *= this.frequency;
        py *= this.frequency;
        int xr = NoiseUtil.floor(px);
        int yr = NoiseUtil.floor(py);
        int cellX = xr;
        int cellY = yr;
        float centerX = px;
        float centerY = py;
        float edgeDistance = 999999.0F;
        float edgeDistance2 = 999999.0F;
        for (int dy = -1; dy <= 1; ++dy) {
            for (int dx = -1; dx <= 1; ++dx) {
                int cx = xr + dx;
                int cy = yr + dy;
                Vec2f vec = NoiseUtil.cell(this.seed, cx, cy);
                float cxf = cx + vec.x() * this.offsetAlpha;
                float cyf = cy + vec.y() * this.offsetAlpha;
                float distance = this.distanceFunc.apply(cxf - px, cyf - py);
                if (distance < edgeDistance) {
                    edgeDistance2 = edgeDistance;
                    edgeDistance = distance;
                    centerX = cxf;
                    centerY = cyf;
                    cellX = cx;
                    cellY = cy;
                }
                else if (distance < edgeDistance2) {
                    edgeDistance2 = distance;
                }
            }
        }
        cell.continentId = this.cellIdentity(this.seed, cellX, cellY);
        cell.continentEdge = this.cellEdgeValue(edgeDistance, edgeDistance2);
        cell.continentX = (int) (centerX / this.frequency);
        cell.continentZ = (int) (centerY / this.frequency);
        cell.continentEdge *= this.getShape(x, y, cell.continentEdge);
    }
    
    @Override
    public float getDistanceToOcean(int cx, int cz, float dx, float dz) {
        return ContinentDistances.toOcean(this, this.controlPoints.shallowOcean, cx, cz, dx, dz);
    }
    
    @Override
    public float getDistanceToEdge(int cx, int cz, float dx, float dz) {
        return ContinentDistances.toEdge(this, this.continentScale, cx, cz, dx, dz);
    }
    
    protected float cellIdentity(int seed, int cellX, int cellY) {
        float value = NoiseUtil.valCoord2D(seed, cellX, cellY);
        return NoiseUtil.map(value, -1.0F, 1.0F, 2.0F);
    }
    
    protected float cellEdgeValue(float distance, float distance2) {
    	EdgeFunction edge = EdgeFunction.DISTANCE_2_DIV;
        float value = edge.apply(distance, distance2);
        value = 1.0F - NoiseUtil.map(value, edge.min(), edge.max(), edge.range());
        if (value <= this.clampMin) {
            return 0.0F;
        }
        if (value >= this.clampMax) {
            return 1.0F;
        }
        return (value - this.clampMin) / this.clampRange;
    }
    
    protected float getShape(float x, float z, float edgeValue) {
        if (edgeValue >= this.controlPoints.inland) {
            return 1.0F;
        }
        float alpha = edgeValue / this.controlPoints.inland;
        return this.shape.compute(x, z, 0) * alpha;
    }
}
