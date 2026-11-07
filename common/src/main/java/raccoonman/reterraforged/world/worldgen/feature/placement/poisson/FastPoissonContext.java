package raccoonman.reterraforged.world.worldgen.feature.placement.poisson;

import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

public record FastPoissonContext(float radius, float jitter, float frequency, Noise density) {

	public FastPoissonContext {
    	frequency = Math.min(0.5F, frequency);
    	jitter = NoiseUtil.clamp(jitter, 0.0F, 1.0F);
    }
    
    public float scale() {
    	return 1.0F / this.frequency;
    }
    
    public float radiusSq() {
    	return this.radius * this.radius;
    }

    public int halfRadius() {
    	return (int) (this.radius / 2.0F);
    }
    
    public float pad() {
    	return (1.0F - this.jitter) * 0.5F;
    }
}
