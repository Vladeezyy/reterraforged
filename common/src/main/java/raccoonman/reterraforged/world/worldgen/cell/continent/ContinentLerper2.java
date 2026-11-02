package raccoonman.reterraforged.world.worldgen.cell.continent;

import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.CellPopulator;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.function.Interpolation;

public class ContinentLerper2 implements CellPopulator {
	private final CellPopulator lower;
    private final CellPopulator upper;
    private final Interpolation interpolation;
    private final float blendLower;
    private final float blendUpper;
    
    public ContinentLerper2(CellPopulator lower, CellPopulator upper, float min, float max) {
        this(lower, upper, min, max, Interpolation.LINEAR);
    }
    
    public ContinentLerper2(CellPopulator lower, CellPopulator upper, float min, float max, Interpolation interpolation) {
        this.lower = lower;
        this.upper = upper;
        this.interpolation = interpolation;
        this.blendLower = min;
        this.blendUpper = max;
    }
    
    @Override
    public void apply(Cell cell, float x, float y) {
        if (cell.continentEdge < this.blendLower) {
            this.lower.apply(cell, x, y);
            return;
        }
        if (cell.continentEdge > this.blendUpper) {
            this.upper.apply(cell, x, y);
            return;
        }
        float alpha = this.interpolation.apply(NoiseUtil.alpha(cell.continentEdge, this.blendLower, this.blendUpper));
        this.lower.apply(cell, x, y);
        float lowerHeight = cell.height;
        this.upper.apply(cell, x, y);
        float upperHeight = cell.height;
        cell.height = NoiseUtil.lerp(lowerHeight, upperHeight, alpha);
    }
}
