package raccoonman.reterraforged.world.worldgen.cell.rivermap.river;

import java.util.Random;

import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.function.CurveFunction;
import raccoonman.reterraforged.world.worldgen.noise.function.CurveFunctions;
import raccoonman.reterraforged.world.worldgen.noise.module.Line;

public class RiverCarver implements Comparable<RiverCarver> {
    public boolean main;
    private float fade;
    private float fadeInv;
    private Range bedWidth;
    private Range banksWidth;
    private Range valleyWidth;
    private Range bedDepth;
    private Range banksDepth;
    private float waterLine;
    // RiverConfig: the steepest valley side (height per block, 0: no limit), the squared distance it reaches, and a
    // valley along the meandering river only
    private float valleySlope;
    private float valleyReach2;
    private boolean valleyFollowsMeanders;
    public River river;
    public RiverWarp warp;
    public RiverConfig config;
    public CurveFunction valleyCurve;
    
    public RiverCarver(River river, RiverWarp warp, RiverConfig config, Settings settings, Levels levels) {
        this.fade = settings.fadeIn;
        this.fadeInv = 1.0F / settings.fadeIn;
        this.bedWidth = new Range(0.25F, (float)(config.bedWidth * config.bedWidth));
        this.banksWidth = new Range(1.5625F, (float)(config.bankWidth * config.bankWidth));
        float valleySize = settings.valleySize * config.valleyWidth;
        this.valleyWidth = new Range(valleySize * valleySize, valleySize * valleySize);
        // the slope limit reaches twice as far as the valley, so the tall ground beside it is cut back too
        float valleyReach = 2.0F * valleySize;
        this.valleySlope = config.valleySlope;
        this.valleyReach2 = valleyReach * valleyReach;
        this.valleyFollowsMeanders = config.valleyFollowsMeanders;
        this.river = river;
        this.warp = warp;
        this.config = config;
        this.main = config.main;
        this.waterLine = levels.water;
        this.bedDepth = new Range(levels.water, config.bedHeight);
        this.banksDepth = new Range(config.minBankHeight, config.maxBankHeight);
        this.valleyCurve = settings.valleyCurve;
    }

    @Override
    public int compareTo(RiverCarver o) {
        return Integer.compare(this.config.order, o.config.order);
    }
    
    // as in ReTerraForged 0.0.6: the river carves its valley, banks and bed the same everywhere; the valley settings
    // (width, steepest side, along the meanders only) are off by default
    public void carve(Cell cell, float px, float pz, float pt, float x, float z, float t) {
        float d2 = this.getDistance2(x, z, t);
        float pd2 = this.getDistance2(px, pz, pt);
        // 0.0.6: the valley follows both the meandering river (x, z) and the straight line it meanders around (px, pz),
        // so where the river meanders away a dry valley floor stays beside it
        float valleyD2 = this.valleyFollowsMeanders ? d2 : Math.min(d2, pd2);
        if (this.valleySlope > 0.0F) {
            this.limitSlope(cell, t, valleyD2);
        }
        float valleyAlpha = this.getDistanceAlpha(pt, valleyD2, this.valleyWidth);
        if (valleyAlpha == 0.0F) {
            return;
        }
        float bankHeight = this.getScaledSize(t, this.banksDepth);
        valleyAlpha = this.valleyCurve.apply(valleyAlpha);
        cell.riverMask = Math.min(cell.riverMask, 1.0F - valleyAlpha);
        cell.height = Math.min(NoiseUtil.lerp(cell.height, bankHeight, valleyAlpha), cell.height);
        float mouthModifier = getMouthModifier(cell);
        float bedHeight = this.getScaledSize(t, this.bedDepth);
        float banksAlpha = this.getDistanceAlpha(t, d2 * mouthModifier, this.banksWidth);
        if (banksAlpha == 0.0F) {
            return;
        }
        if (cell.height > bedHeight) {
            cell.height = Math.min(NoiseUtil.lerp(cell.height, bedHeight, banksAlpha), cell.height);
            this.tag(cell, bedHeight);
        }
        float bedAlpha = this.getDistanceAlpha(t, d2, this.bedWidth);
        if (bedAlpha != 0.0F && cell.height > bedHeight) {
            cell.height = NoiseUtil.lerp(cell.height, bedHeight, bedAlpha);
            this.tag(cell, bedHeight);
        }
    }
    
    // the ground above a cone around the river, rising from the banks at the steepest slope, is cut down to it; from
    // half the reach on the cut fades out, gone twice as far as the valley reaches (a cone that rose faster towards its
    // end left a new wall there in tall terrain); lower ground is left as it is
    private void limitSlope(Cell cell, float t, float dist2) {
        if (dist2 >= this.valleyReach2) {
            return;
        }
        float dist = (float) Math.sqrt(dist2);
        float above = cell.height - (this.getScaledSize(t, this.banksDepth) + this.valleySlope * dist);
        if (above > 0.0F) {
            cell.height -= above * slopeLimitWeight(dist2 / this.valleyReach2);
        }
    }

    // 1 up to half the reach, then down to 0 at the reach in an s-curve; x2: the squared share of the reach
    public static float slopeLimitWeight(float x2) {
        float fade = NoiseUtil.clamp(((float) Math.sqrt(x2) - 0.5F) * 2.0F, 0.0F, 1.0F);
        return 1.0F - fade * fade * (3.0F - 2.0F * fade);
    }
    
    // how much farther than the river's bounds (River pads them by twice 275 blocks, room for 0.0.6's widest valley of
    // 412.5 blocks and the river's warp) the carving reaches with a wider valley or the slope limit
    public float extraBounds() {
        float reach2 = this.valleySlope > 0.0F ? this.valleyReach2 : this.valleyWidth.max();
        return Math.max(0.0F, (float) Math.sqrt(reach2) - 275.0F * 1.5F);
    }
    
    public RiverConfig createForkConfig(float t, Levels levels) {
        int bedHeight = levels.scale(this.getScaledSize(t, this.bedDepth));
        int bedWidth = (int)Math.round(Math.sqrt(this.getScaledSize(t, this.bedWidth)) * 0.75);
        int bankWidth = (int)Math.round(Math.sqrt(this.getScaledSize(t, this.banksWidth)) * 0.75);
        bedWidth = Math.max(1, bedWidth);
        bankWidth = Math.max(bedWidth + 1, bankWidth);
        return this.config.createFork(bedHeight, bedWidth, bankWidth, levels);
    }
    
    private float getDistance2(float x, float y, float t) {
        if (t <= 0.0F) {
            return Line.distSq(x, y, this.river.x1, this.river.z1);
        }
        if (t >= 1.0F) {
            return Line.distSq(x, y, this.river.x2, this.river.z2);
        }
        float px = this.river.x1 + t * this.river.dx;
        float py = this.river.z1 + t * this.river.dz;
        return Line.distSq(x, y, px, py);
    }
    
    private float getDistanceAlpha(float t, float dist2, Range range) {
        float size2 = this.getScaledSize(t, range);
        if (dist2 >= size2) {
            return 0.0F;
        }
        return 1.0F - dist2 / size2;
    }
    
    private float getScaledSize(float t, Range range) {
        if (t < 0.0F) {
            return range.min();
        }
        if (t > 1.0F) {
            return range.max();
        }
        if (range.min() == range.max()) {
            return range.min();
        }
        if (t >= this.fade) {
            return range.max();
        }
        return NoiseUtil.lerp(range.min(), range.max(), t * this.fadeInv);
    }
    
    private void tag(Cell cell, float bedHeight) {
        if (cell.terrain.overridesRiver() && (cell.height < bedHeight || cell.height > this.waterLine)) {
            return;
        }
        cell.erosionMask = true;
        if (cell.height <= this.waterLine) {
            cell.terrain = TerrainType.RIVER;
        }
    }
    
    private static float getMouthModifier(Cell cell) {
        float modifier = NoiseUtil.map(cell.continentEdge, 0.0F, 0.5F, 0.5F);
        modifier *= modifier;
        return modifier;
    }
    
    public static CurveFunction getValleyType(Random random) {
        int value = random.nextInt(100);
        if (value < 5) {
            return CurveFunctions.scurve(0.4F, 1.0F);
        }
        if (value < 30) {
            return CurveFunctions.scurve(4.0F, 5.0F);
        }
        if (value < 50) {
            return CurveFunctions.scurve(3.0F, 0.25F);
        }
        return CurveFunctions.scurve(2.0F, -0.5F);
    }
    
    public static RiverCarver create(float x1, float z1, float x2, float z2, RiverConfig config, Levels levels, Random random) {
        River river = new River(x1, z1, x2, z2);
        RiverWarp warp = RiverWarp.create(0.35F, random);
        float valleyWidth = 275.0F * River.MAIN_VALLEY.next(random);
        Settings settings = creatSettings(random);
        settings.connecting = false;
        settings.fadeIn = config.fade;
        settings.valleySize = valleyWidth;
        return new RiverCarver(river, warp, config, settings, levels);
    }
    
    private static Settings creatSettings(Random random) {
        Settings settings = new Settings();
        settings.valleyCurve = getValleyType(random);
        return settings;
    }
    
    public static class Settings {
        public float valleySize;
        public float fadeIn;
        public boolean connecting;
        public CurveFunction valleyCurve;
        
        public Settings() {
            this.valleySize = 275.0F;
            this.fadeIn = 0.7F;
            this.connecting = false;
            this.valleyCurve = CurveFunctions.scurve(2.0F, -0.5F);
        }
    }
}
