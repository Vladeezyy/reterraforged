package raccoonman.reterraforged.data.worldgen.preset.settings;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.StringRepresentable;

public class FilterSettings {
	public static final Codec<FilterSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Erosion.CODEC.fieldOf("erosion").forGetter((o) -> o.erosion),
		Smoothing.CODEC.fieldOf("smoothing").forGetter((o) -> o.smoothing),
		// added in 0.0.32-ridges-test: presets saved before have no ridges (strength 0), their worlds stay as they were
		Ridges.CODEC.optionalFieldOf("ridges").forGetter((o) -> Optional.of(o.ridges))
	).apply(instance, (erosion, smoothing, ridges) -> new FilterSettings(erosion, smoothing, ridges.orElseGet(Ridges::new))));
	
    public final Erosion erosion;
    public final Smoothing smoothing;
    public final Ridges ridges;
    
    public FilterSettings(Erosion erosion, Smoothing smoothing) {
    	this(erosion, smoothing, new Ridges());
    }
    
    public FilterSettings(Erosion erosion, Smoothing smoothing, Ridges ridges) {
    	this.erosion = erosion;
    	this.smoothing = smoothing;
    	this.ridges = ridges;
    }
    
    public FilterSettings copy() {
    	return new FilterSettings(this.erosion.copy(), this.smoothing.copy(), this.ridges.copy());
    }
    
    /**
     * The erosion filter (Rune Skovbo Johansen's, ErosionFilterKernel): branching ridges and gullies carved into the
     * mountains and hills. Strength 0 makes no filter, the world is the same as without it.
     */
    public static class Ridges {
    	public static final Codec<Ridges> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    		Codec.FLOAT.optionalFieldOf("strength", 0.0F).forGetter((o) -> o.strength),
    		RidgeStyle.CODEC.optionalFieldOf("style", RidgeStyle.SHARP).forGetter((o) -> o.style)
    	).apply(instance, Ridges::new));
    	
    	public float strength;
    	public RidgeStyle style;
    	
    	public Ridges() {
    		this(0.0F, RidgeStyle.SHARP);
    	}
    	
    	public Ridges(float strength, RidgeStyle style) {
    		this.strength = strength;
    		this.style = style;
    	}
    	
    	public Ridges copy() {
    		return new Ridges(this.strength, this.style);
    	}
    }
    
    public enum RidgeStyle implements StringRepresentable {
    	// the filter's first settings: fine ridges at a scale of 160 blocks, 5 octaves
    	SHARP,
    	// wide ridges (scale 300, 3 octaves), the summits keep their shape (ErosionFilterProto.SMOOTH)
    	SMOOTH;
    	
    	public static final Codec<RidgeStyle> CODEC = StringRepresentable.fromEnum(RidgeStyle::values);
    	
    	@Override
    	public String getSerializedName() {
    		return this.name().toLowerCase(java.util.Locale.ROOT);
    	}
    }
    
    public static class Erosion {
    	public static final Codec<Erosion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    		Codec.INT.fieldOf("dropletsPerChunk").forGetter((o) -> o.dropletsPerChunk),
    		Codec.INT.fieldOf("dropletLifetime").forGetter((o) -> o.dropletLifetime),
    		Codec.FLOAT.fieldOf("dropletVolume").forGetter((o) -> o.dropletVolume),
    		Codec.FLOAT.fieldOf("dropletVelocity").forGetter((o) -> o.dropletVelocity),
    		Codec.FLOAT.fieldOf("erosionRate").forGetter((o) -> o.erosionRate),
    		Codec.FLOAT.fieldOf("depositeRate").forGetter((o) -> o.depositeRate),
    		// added in 0.0.22: presets saved before keep the fixed minimum sediment capacity (their worlds stay as they were)
    		Codec.BOOL.optionalFieldOf("legacyMinCapacity", true).forGetter((o) -> o.legacyMinCapacity)
    	).apply(instance, (dropletsPerChunk, dropletLifetime, dropletVolume, dropletVelocity, erosionRate, depositeRate, legacyMinCapacity) -> {
    		Erosion erosion = new Erosion(dropletsPerChunk, dropletLifetime, dropletVolume, dropletVelocity, erosionRate, depositeRate);
    		erosion.legacyMinCapacity = legacyMinCapacity;
    		return erosion;
    	}));
    	
    	public int dropletsPerChunk;
        public int dropletLifetime;
        public float dropletVolume;
        public float dropletVelocity;
        public float erosionRate;
        public float depositeRate;
        // true: the droplets' minimum sediment capacity is a fixed 0.01 (TerraForged), which made the lower part of the
        // Droplet Volume and Droplet Velocity sliders do nothing; false: it shrinks with small droplets (Erosion)
        public boolean legacyMinCapacity = false;
        
        public Erosion(int dropletsPerChunk, int dropletsLifetime, float dropletVolume, float dropletVelocity, float erosionRate, float depositeRate) {
        	this.dropletsPerChunk = dropletsPerChunk;
        	this.dropletLifetime = dropletsLifetime;
        	this.dropletVolume = dropletVolume;
        	this.dropletVelocity = dropletVelocity;
        	this.erosionRate = erosionRate;
        	this.depositeRate = depositeRate;
        }
        
        public Erosion copy() {
        	Erosion copy = new Erosion(this.dropletsPerChunk, this.dropletLifetime, this.dropletVolume, this.dropletVelocity, this.erosionRate, this.depositeRate);
        	copy.legacyMinCapacity = this.legacyMinCapacity;
        	return copy;
        }
    }
    
    public static class Smoothing {
    	public static final Codec<Smoothing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    		Codec.INT.fieldOf("iterations").forGetter((o) -> o.iterations),
    		Codec.FLOAT.fieldOf("smoothingRadius").forGetter((o) -> o.smoothingRadius),
    		Codec.FLOAT.fieldOf("smoothingRate").forGetter((o) -> o.smoothingRate)    		
    	).apply(instance, Smoothing::new));
    	
        public int iterations;
        public float smoothingRadius;
        public float smoothingRate;
        
        public Smoothing(int iterations, float smoothingRadius, float smoothingRate) {
        	this.iterations = iterations;
        	this.smoothingRadius = smoothingRadius;
        	this.smoothingRate = smoothingRate;
        }
        
        public Smoothing copy() {
        	return new Smoothing(this.iterations, this.smoothingRadius, this.smoothingRate);
        }
    }
}
