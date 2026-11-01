package raccoonman.reterraforged.world.worldgen.util;

import java.util.Random;

public record Variance(float min, float range) {
    
    public float apply(float value) {
        return this.min + value * this.range;
    }
    
    public float apply(float value, float scaler) {
        return this.apply(value) * scaler;
    }
    
    
    public float next(Random random) {
        return this.apply(random.nextFloat());
    }
    
    
    
    
    
    public static Variance of(float min, float range) {
        return new Variance(min, range);
    }
}
