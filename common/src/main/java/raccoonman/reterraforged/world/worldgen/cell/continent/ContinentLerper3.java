package raccoonman.reterraforged.world.worldgen.cell.continent;

import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.CellPopulator;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.function.Interpolation;

public class ContinentLerper3 implements CellPopulator {
    private CellPopulator lower;
    private CellPopulator middle;
    private CellPopulator upper;
    private Interpolation interpolation;
    private float midpoint;
    private float blendLower;
    private float blendUpper;
    
    public ContinentLerper3(CellPopulator lower, CellPopulator middle, CellPopulator upper, float min, float mid, float max) {
        this(lower, middle, upper, min, mid, max, Interpolation.CURVE3);
    }
    
    public ContinentLerper3(CellPopulator lower, CellPopulator middle, CellPopulator upper, float min, float mid, float max, Interpolation interpolation) {
        this.lower = lower;
        this.upper = upper;
        this.middle = middle;
        this.interpolation = interpolation;
        this.midpoint = mid;
        this.blendLower = min;
        this.blendUpper = max;
    }
    
    @Override
    public void apply(Cell cell, float x, float y) {
        float select = cell.continentEdge;
        if (select < this.blendLower) {
            this.lower.apply(cell, x, y);
            return;
        }
        if (select > this.blendUpper) {
            this.upper.apply(cell, x, y);
            return;
        }
        if (select < this.midpoint) {
            float alpha = this.interpolation.apply(NoiseUtil.alpha(select, this.blendLower, this.midpoint));
            this.lower.apply(cell, x, y);
            float lowerHeight = cell.height;
            this.middle.apply(cell, x, y);
            cell.height = NoiseUtil.lerp(lowerHeight, cell.height, alpha);
        } else {
            float alpha = this.interpolation.apply(NoiseUtil.alpha(select, this.midpoint, this.blendUpper));
            this.middle.apply(cell, x, y);
            float lowerHeight = cell.height;
            this.upper.apply(cell, x, y);
            cell.height = NoiseUtil.lerp(lowerHeight, cell.height, alpha);
        }
    }
}
