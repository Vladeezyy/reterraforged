package raccoonman.reterraforged.data.worldgen.preset.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class RiverSettings {
	// the steepest valley side in degrees; 90: no limit (the valley's own shape, which can be a cliff)
	public static final int NO_SLOPE_LIMIT = 90;
	public static final Codec<RiverSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.fieldOf("seedOffset").forGetter((o) -> o.seedOffset),
		Codec.INT.fieldOf("riverCount").forGetter((o) -> o.riverCount),
		River.CODEC.fieldOf("mainRivers").forGetter((o) -> o.mainRivers),
		River.CODEC.fieldOf("branchRivers").forGetter((o) -> o.branchRivers),
		Lake.CODEC.fieldOf("lakes").forGetter((o) -> o.lakes),
		Wetland.CODEC.fieldOf("wetlands").forGetter((o) -> o.wetlands),
		// added in 0.0.20: off, the branch rivers take the sizes of their own sliders; on (presets saved before), the
		// size of the river they branch from where they meet it, as in TerraForged (the branch sliders did nothing)
		Codec.BOOL.optionalFieldOf("branchSizeFromMain", true).forGetter((o) -> o.branchSizeFromMain),
		// added in 0.0.27 (#29), the defaults keep the valleys of ReTerraForged 0.0.6
		Codec.FLOAT.optionalFieldOf("valleyWidth", 1.0F).forGetter((o) -> o.valleyWidth),
		Codec.INT.optionalFieldOf("valleySlope", NO_SLOPE_LIMIT).forGetter((o) -> o.valleySlope),
		Codec.BOOL.optionalFieldOf("valleyFollowsMeanders", false).forGetter((o) -> o.valleyFollowsMeanders)
	).apply(instance, (seedOffset, riverCount, mainRivers, branchRivers, lakes, wetlands, branchSizeFromMain, valleyWidth, valleySlope, valleyFollowsMeanders) -> {
		RiverSettings settings = new RiverSettings(seedOffset, riverCount, mainRivers, branchRivers, lakes, wetlands);
		settings.branchSizeFromMain = branchSizeFromMain;
		settings.valleyWidth = valleyWidth;
		settings.valleySlope = valleySlope;
		settings.valleyFollowsMeanders = valleyFollowsMeanders;
		return settings;
	}));
	
    public int seedOffset;
    public int riverCount;
    public River mainRivers;
    public River branchRivers;
    public Lake lakes;
    public Wetland wetlands;
    public boolean branchSizeFromMain = true;
    // how wide the valleys of the rivers and lakes are, times their width in 0.0.6
    public float valleyWidth = 1.0F;
    // the steepest side of a valley in degrees (NO_SLOPE_LIMIT: as steep as its shape makes it)
    public int valleySlope = NO_SLOPE_LIMIT;
    // true: the valley follows the meandering river only; false: also the straight line the river meanders around,
    // which leaves a dry valley floor beside the river where it meanders away (as in 0.0.6)
    public boolean valleyFollowsMeanders = false;
    
    public RiverSettings(int seedOffset, int riverCount, River mainRivers, River branchRivers, Lake lakes, Wetland wetlands) {
        this.seedOffset = seedOffset;
        this.riverCount = riverCount;
        this.mainRivers = mainRivers;
        this.branchRivers = branchRivers;
        this.lakes = lakes;
        this.wetlands = wetlands;
    }
    
    // the steepest valley side as height per horizontal block, 0 for no limit
    public float valleySlopeGradient() {
    	if (this.valleySlope >= NO_SLOPE_LIMIT) {
    		return 0.0F;
    	}
    	return (float) Math.tan(Math.toRadians(Math.max(1, this.valleySlope)));
    }
    
    public RiverSettings copy() {
    	RiverSettings copy = new RiverSettings(this.seedOffset, this.riverCount, this.mainRivers.copy(), this.branchRivers.copy(), this.lakes.copy(), this.wetlands.copy());
    	copy.branchSizeFromMain = this.branchSizeFromMain;
    	copy.valleyWidth = this.valleyWidth;
    	copy.valleySlope = this.valleySlope;
    	copy.valleyFollowsMeanders = this.valleyFollowsMeanders;
    	return copy;
    }

    public static class River {
    	public static final Codec<River> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    		Codec.INT.fieldOf("bedDepth").forGetter((o) -> o.bedDepth),
    		Codec.INT.fieldOf("minBankHeight").forGetter((o) -> o.minBankHeight),
    		Codec.INT.fieldOf("maxBankHeight").forGetter((o) -> o.maxBankHeight),
    		Codec.INT.fieldOf("bankWidth").forGetter((o) -> o.bankWidth),
    		Codec.INT.fieldOf("bedWidth").forGetter((o) -> o.bedWidth),
    		Codec.FLOAT.fieldOf("fade").forGetter((o) -> o.fade)
    	).apply(instance, River::new));
    	
        public int bedDepth;
        public int minBankHeight;
        public int maxBankHeight;
        public int bedWidth;
        public int bankWidth;
        public float fade;
        
        public River(int bedDepth, int minBankHeight, int maxBankHeight, int bankWidth, int bedWidth, float fade) {
            this.bedDepth = bedDepth;
            this.minBankHeight = minBankHeight;
            this.maxBankHeight = maxBankHeight;
            this.bankWidth = bankWidth;
            this.bedWidth = bedWidth;
            this.fade = fade;
        }
        
        public River copy() {
        	return new River(this.bedDepth, this.minBankHeight, this.maxBankHeight, this.bankWidth, this.bedWidth, this.fade);
        }
    }
    
    public static class Lake {
    	public static final Codec<Lake> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    		Codec.FLOAT.fieldOf("chance").forGetter((o) -> o.chance),
    		Codec.FLOAT.fieldOf("minStartDistance").forGetter((o) -> o.minStartDistance),
    		Codec.FLOAT.fieldOf("maxStartDistance").forGetter((o) -> o.maxStartDistance),
    		Codec.INT.fieldOf("depth").forGetter((o) -> o.depth),
    		Codec.INT.fieldOf("sizeMin").forGetter((o) -> o.sizeMin),
    		Codec.INT.fieldOf("sizeMax").forGetter((o) -> o.sizeMax),
    		Codec.INT.fieldOf("minBankHeight").forGetter((o) -> o.minBankHeight),
    		Codec.INT.fieldOf("maxBankHeight").forGetter((o) -> o.maxBankHeight)
    	).apply(instance, Lake::new));

        public float chance;
        public float minStartDistance;
        public float maxStartDistance;
        public int depth;
        public int sizeMin;
        public int sizeMax;
        public int minBankHeight;
        public int maxBankHeight;
        
        public Lake(float chance, float minStartDistance, float maxStartDistance, int depth, int sizeMin, int sizeMax, int minBankHeight, int maxBankHeight) {
        	this.chance = chance;
        	this.minStartDistance = minStartDistance;
        	this.maxStartDistance = maxStartDistance;
        	this.depth = depth;
        	this.sizeMin= sizeMin;
        	this.sizeMax = sizeMax;
        	this.minBankHeight = minBankHeight;
        	this.maxBankHeight = maxBankHeight;
        }
        
        public Lake copy() {
        	return new Lake(this.chance, this.minStartDistance, this.maxStartDistance, this.depth, this.sizeMin, this.sizeMax, this.minBankHeight, this.maxBankHeight);
        }
    }
    
    public static class Wetland {
    	public static final Codec<Wetland> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    		Codec.FLOAT.fieldOf("chance").forGetter((o) -> o.chance),
    		Codec.INT.fieldOf("sizeMin").forGetter((o) -> o.sizeMin),
    		Codec.INT.fieldOf("sizeMax").forGetter((o) -> o.sizeMax)
    	).apply(instance, Wetland::new));
    	
        public float chance;
        public int sizeMin;
        public int sizeMax;
        
        public Wetland(float chance, int sizeMin, int sizeMax) {
        	this.chance = chance;
        	this.sizeMin = sizeMin;
        	this.sizeMax = sizeMax;
        }
        
        public Wetland copy() {
        	return new Wetland(this.chance, this.sizeMin, this.sizeMax);
        }
    }
}
