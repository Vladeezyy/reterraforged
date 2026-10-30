package raccoonman.reterraforged.world.worldgen.cell.rivermap.lake;

import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil.Vec2f;
import raccoonman.reterraforged.world.worldgen.util.Boundsf;

public class Lake {
    protected float valley;
    protected float valley2;
    protected float lakeDistance2;
    protected float valleyDistance2;
    protected float bankAlphaMin;
    protected float bankAlphaMax;
    protected float bankAlphaRange;
    private float depth;
    private float bankMin;
    private float bankMax;
    // LakeConfig: the lake's radius, the steepest valley side (height per block, 0: no limit) and how far past the
    // shore that limit reaches
    private float lakeRadius;
    private float slope;
    private float slopeReach;
    protected Vec2f center;
    
    public Lake(Vec2f center, float radius, float multiplier, LakeConfig config) {
        float lake = radius * multiplier;
        // as in ReTerraForged 0.0.6: a valley of 275 blocks around the lake (times the preset's valley width)
        float valley = 275.0F * multiplier * config.valleyWidth;
        this.lakeRadius = lake;
        this.slope = config.valleySlope;
        // past the shore, as far as twice the valley: a lake as wide as its valley gets a slope too
        this.slopeReach = 2.0F * 275.0F * multiplier * config.valleyWidth;
        // 确保 valley 不小于 lake
        if (valley < lake) {
            valley = lake;
        }
        this.valley = valley;
        this.valley2 = valley * valley;
        this.center = center;
        this.depth = config.depth;
        this.bankMin = config.bankMin;
        this.bankMax = config.bankMax;
        this.bankAlphaMin = config.bankMin;
        this.bankAlphaMax = Math.min(1.0F, this.bankAlphaMin + 0.275F);
        this.bankAlphaRange = this.bankAlphaMax - this.bankAlphaMin;
        this.lakeDistance2 = lake * lake;
        this.valleyDistance2 = this.valley2 - this.lakeDistance2;
        // 防止 valleyDistance2 为零或负数
        if (this.valleyDistance2 <= 0.0F) {
            this.valleyDistance2 = 1.0E-6F;
        }
    }
    
    public void apply(Cell cell, float x, float z) {
        float distance2 = this.getDistance2(x, z);
        if (this.slope > 0.0F) {
            this.limitSlope(cell, distance2);
        }
        if (distance2 > this.valley2) {
            return;
        }
        float bankHeight = this.getBankHeight(cell);
        if (distance2 <= this.lakeDistance2) {
            cell.height = Math.min(bankHeight, cell.height);
            if (distance2 < this.lakeDistance2) {
                float depthAlpha = 1.0F - distance2 / this.lakeDistance2;
                if (depthAlpha < 0.0F) {
                    depthAlpha = 0.0F;
                } else if (depthAlpha > 1.0F) {
                    depthAlpha = 1.0F;
                }
                float lakeDepth = Math.min(cell.height, this.depth);
                cell.height = NoiseUtil.lerp(cell.height, lakeDepth, depthAlpha);
                cell.terrain = TerrainType.LAKE;
                cell.riverMask = Math.min(cell.riverMask, 1.0F - depthAlpha);
            }
            return;
        }
        if (cell.height < bankHeight) {
            return;
        }
        float valleyAlpha = 1.0F - (distance2 - this.lakeDistance2) / this.valleyDistance2;
        if (valleyAlpha < 0.0F) {
            valleyAlpha = 0.0F;
        } else if (valleyAlpha > 1.0F) {
            valleyAlpha = 1.0F;
        }
        cell.height = NoiseUtil.lerp(cell.height, bankHeight, valleyAlpha);
        cell.riverMask *= 1.0F - valleyAlpha;
        cell.riverMask = Math.min(cell.riverMask, 1.0F - valleyAlpha);
    }
    
    // as for the rivers: the ground around the lake is cut down to a cone rising from its banks at the steepest slope,
    // steeper and steeper until it cuts nothing slopeReach past the shore
    private void limitSlope(Cell cell, float distance2) {
        float shore = (float) Math.sqrt(distance2) - this.lakeRadius;
        if (shore <= 0.0F || shore >= this.slopeReach) {
            return;
        }
        float x = shore / this.slopeReach;
        float x2 = x * x;
        float limit = this.getBankHeight(cell) + this.slope * shore / (1.0F - x2 * x2);
        if (cell.height > limit) {
            cell.height = limit;
        }
    }
    
    public void recordBounds(Boundsf.Builder builder) {
        float extent = this.valley * 1.2F;
        if (this.slope > 0.0F) {
            extent = Math.max(extent, (this.lakeRadius + this.slopeReach) * 1.2F);
        }
        builder.record(this.center.x() - extent, this.center.y() - extent);
        builder.record(this.center.x() + extent, this.center.y() + extent);
    }
    
    public boolean overlaps(float x, float z, float radius2) {
        float dist2 = this.getDistance2(x, z);
        return dist2 < this.lakeDistance2 + radius2;
    }
    
    protected float getDistance2(float x, float z) {
        float dx = this.center.x() - x;
        float dz = this.center.y() - z;
        return dx * dx + dz * dz;
    }
    
    protected float getBankHeight(Cell cell) {
        float bankHeightAlpha = NoiseUtil.map(cell.height, this.bankAlphaMin, this.bankAlphaMax, this.bankAlphaRange);
        return NoiseUtil.lerp(this.bankMin, this.bankMax, bankHeightAlpha);
    }
}
