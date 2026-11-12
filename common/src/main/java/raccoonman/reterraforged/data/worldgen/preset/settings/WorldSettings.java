package raccoonman.reterraforged.data.worldgen.preset.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import raccoonman.reterraforged.world.worldgen.noise.function.DistanceFunction;

public class WorldSettings {
	public static final Codec<WorldSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Continent.CODEC.fieldOf("continent").forGetter((o) -> o.continent),
		ControlPoints.CODEC.fieldOf("controlPoints").forGetter((o) -> o.controlPoints),
		Properties.CODEC.fieldOf("properties").forGetter((o) -> o.properties)
	).apply(instance, WorldSettings::new));
	
    public final Continent continent;
    public final ControlPoints controlPoints;
    public final Properties properties;
    
    public WorldSettings(Continent continent, ControlPoints controlPoints, Properties properties) {
        this.continent = continent;
        this.controlPoints = controlPoints;
        this.properties = properties;
    }
    
    public WorldSettings copy() {
    	return new WorldSettings(this.continent.copy(), this.controlPoints.copy(), this.properties.copy());
    }
    
    public static class Continent {
    	public static final Codec<Continent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    		ContinentType.CODEC.fieldOf("continentType").forGetter((o) -> o.continentType),
    		DistanceFunction.CODEC.optionalFieldOf("continentShape", DistanceFunction.EUCLIDEAN).forGetter((o) -> o.continentShape),
    		Codec.INT.fieldOf("continentScale").forGetter((o) -> o.continentScale),
    		Codec.FLOAT.fieldOf("continentJitter").forGetter((o) -> o.continentJitter),
    		Codec.FLOAT.optionalFieldOf("continentSkipping", 0.25F).forGetter((o) -> o.continentSkipping),
    		Codec.FLOAT.optionalFieldOf("continentSizeVariance", 0.25F).forGetter((o) -> o.continentSizeVariance),
    		Codec.INT.optionalFieldOf("continentNoiseOctaves", 5).forGetter((o) -> o.continentNoiseOctaves),
    		Codec.FLOAT.optionalFieldOf("continentNoiseGain", 0.26F).forGetter((o) -> o.continentNoiseGain),
    		Codec.FLOAT.optionalFieldOf("continentNoiseLacunarity", 4.33F).forGetter((o) -> o.continentNoiseLacunarity)
    	).apply(instance, Continent::new));
    	
        public ContinentType continentType;
        public DistanceFunction continentShape;
        public int continentScale;
        public float continentJitter;
        public float continentSkipping;
        public float continentSizeVariance;
        public int continentNoiseOctaves;
        public float continentNoiseGain;
        public float continentNoiseLacunarity;
        
        public Continent(ContinentType continentType, DistanceFunction continentShape, int continentScale, float continentJitter, float continentSkipping, float continentSizeVariance, int continentNoiseOctaves, float continentNoiseGain, float continentNoiseLacunarity) {
            this.continentType = continentType;
            this.continentShape = continentShape;
            this.continentScale = continentScale;
            this.continentJitter = continentJitter;
            this.continentSkipping = continentSkipping;
            this.continentSizeVariance = continentSizeVariance;
            this.continentNoiseOctaves = continentNoiseOctaves;
            this.continentNoiseGain = continentNoiseGain;
            this.continentNoiseLacunarity = continentNoiseLacunarity;
        }
        
        public Continent copy() {
        	return new Continent(this.continentType, this.continentShape, this.continentScale, this.continentJitter, this.continentSkipping, this.continentSizeVariance, this.continentNoiseOctaves, this.continentNoiseGain, this.continentNoiseLacunarity);
        }
    }
    
    public static class ControlPoints {
    	// points of an island's profile, ArchipelagoPopulator
    	public static final float DEFAULT_ISLAND_COAST = 0.3F;
    	public static final float DEFAULT_ISLAND_INLAND = 0.6F;
    	public static final Codec<ControlPoints> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        	Codec.FLOAT.optionalFieldOf("islandInland", DEFAULT_ISLAND_INLAND).forGetter((o) -> o.islandInland),
        	Codec.FLOAT.optionalFieldOf("islandCoast", DEFAULT_ISLAND_COAST).forGetter((o) -> o.islandCoast),
    		Codec.FLOAT.fieldOf("deepOcean").forGetter((o) -> o.deepOcean),
    		Codec.FLOAT.fieldOf("shallowOcean").forGetter((o) -> o.shallowOcean),
    		Codec.FLOAT.fieldOf("beach").forGetter((o) -> o.beach),
    		Codec.FLOAT.fieldOf("coast").forGetter((o) -> o.coast),
    		Codec.FLOAT.fieldOf("inland").forGetter((o) -> o.inland)
        ).apply(instance, ControlPoints::new));

    	public float islandInland;
    	public float islandCoast;
        public float deepOcean;
        public float shallowOcean;
        public float beach;
        public float coast;
        public float inland;
        
        public ControlPoints(float islandInland, float islandCoast, float deepOcean, float shallowOcean, float beach, float coast, float inland) {
        	this.islandInland = islandInland;
        	this.islandCoast = islandCoast;
            this.deepOcean = deepOcean;
            this.shallowOcean = shallowOcean;
            this.beach = beach;
            this.coast = coast;
            this.inland = inland;
        }
        
        public float coastMarker() {
        	return this.coast + (this.inland - this.coast) / 2.0F;
        }
        
        public ControlPoints copy() {
        	return new ControlPoints(this.islandInland, this.islandCoast, this.deepOcean, this.shallowOcean, this.beach, this.coast, this.inland);
        }
    }
    
    public static class Properties {
    	public static final int DEFAULT_OCEAN_DEPTH = 64;
    	// the fixes after 0.0.31 (with Generation Fixes on): 1 deep structures under rivers and lakes, Perlin noise of the
    	// world's seed, the full 64 bits of the seed. Raise it with every fix that changes a world
    	public static final int CURRENT_GENERATION_VERSION = 1;
    	
    	public static final Codec<Properties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    		SpawnType.CODEC.fieldOf("spawnType").forGetter((o) -> o.spawnType),
    		Codec.INT.fieldOf("worldHeight").forGetter((o) -> o.worldHeight),
    		Codec.INT.optionalFieldOf("worldDepth", 64).forGetter((o) -> o.worldDepth),
    		Codec.INT.fieldOf("seaLevel").forGetter((o) -> o.seaLevel),
    		Codec.INT.optionalFieldOf("oceanDepth", DEFAULT_OCEAN_DEPTH).forGetter((o) -> o.oceanDepth),
    		Codec.INT.optionalFieldOf("lavaLevel", -54).forGetter((o) -> o.lavaLevel),
    		// off in a preset saved before 0.0.23 (its worlds keep their snow line); the built-in presets have it on
    		Codec.BOOL.optionalFieldOf("altitudeCoolingFollowsHeight", false).forGetter((o) -> o.altitudeCoolingFollowsHeight),
    		// added in 0.0.27 (#32): 0 is vanilla's deepslate, from y 8 down to y 0
    		Codec.INT.optionalFieldOf("deepslateLevel", 0).forGetter((o) -> o.deepslateLevel),
    		// off in a preset saved before 0.0.30: its worlds keep their terrain
    		Codec.BOOL.optionalFieldOf("generationFixes", false).forGetter((o) -> o.generationFixes),
    		// none in a preset saved before 0.0.32: the world of its datapack keeps the fixes it was made with (0 is not
    		// written, so such a preset saved again stays the same file)
    		Codec.INT.optionalFieldOf("generationVersion", 0).forGetter((o) -> o.generationVersion)
    	).apply(instance, (spawnType, worldHeight, worldDepth, seaLevel, oceanDepth, lavaLevel, altitudeCoolingFollowsHeight, deepslateLevel, generationFixes, generationVersion) -> {
    		Properties properties = new Properties(spawnType, worldHeight, worldDepth, seaLevel, oceanDepth, lavaLevel, altitudeCoolingFollowsHeight);
    		properties.deepslateLevel = deepslateLevel;
    		properties.generationFixes = generationFixes;
    		properties.generationVersion = generationVersion;
    		return properties;
    	}));
    	
        public SpawnType spawnType;
        public int worldHeight;
        public int worldDepth;
        public int seaLevel;
        public int oceanDepth;
        public int lavaLevel;
        // vanilla's cooling with height (rain to snow) counts the height as in a world of the vanilla height (AltitudeCooling)
        public boolean altitudeCoolingFollowsHeight;
        // the Y where stone gives way to deepslate: the stone turns into it over the 8 blocks above it, below it is all
        // deepslate (vanilla: 0)
        public int deepslateLevel;
        // the fixes that change a world (0.0.30), on in new presets: islands of the world's seed with their beaches above
        // the water and the horizontal scale only on their terrain, the floor of swamps, lowland swamps of the biome mods
        // and on mixed terrains, land with the Infinite continent type, terrain in other dimensions on the overworld's noise settings
        public boolean generationFixes = true;
        // which of the later fixes a world has (CURRENT_GENERATION_VERSION); only read with generationFixes on
        public int generationVersion = CURRENT_GENERATION_VERSION;
        
        public Properties(SpawnType spawnType, int worldHeight, int worldDepth, int seaLevel, int lavaLevel) {
        	this(spawnType, worldHeight, worldDepth, seaLevel, DEFAULT_OCEAN_DEPTH, lavaLevel);
        }
        
        public Properties(SpawnType spawnType, int worldHeight, int worldDepth, int seaLevel, int oceanDepth, int lavaLevel) {
        	this(spawnType, worldHeight, worldDepth, seaLevel, oceanDepth, lavaLevel, true);
        }
        
        public Properties(SpawnType spawnType, int worldHeight, int worldDepth, int seaLevel, int oceanDepth, int lavaLevel, boolean altitudeCoolingFollowsHeight) {
        	this.altitudeCoolingFollowsHeight = altitudeCoolingFollowsHeight;
        	this.spawnType = spawnType;
        	this.worldHeight = worldHeight;
        	this.worldDepth = worldDepth;
        	this.seaLevel = seaLevel;
        	this.oceanDepth = oceanDepth;
        	this.lavaLevel = lavaLevel;
        }
        
        public Properties copy() {
        	Properties copy = new Properties(this.spawnType, this.worldHeight, this.worldDepth, this.seaLevel, this.oceanDepth, this.lavaLevel, this.altitudeCoolingFollowsHeight);
        	copy.deepslateLevel = this.deepslateLevel;
        	copy.generationFixes = this.generationFixes;
        	copy.generationVersion = this.generationVersion;
        	return copy;
        }

        // the fixes of that generation version are on in this world
        public boolean fixes(int version) {
        	return this.generationFixes && this.generationVersion >= version;
        }

        // a preset that makes a new world (from the library, the toggle switched on) gets the fixes of this version;
        // never for the preset of an existing world (its datapack), whose chunks stay as they were made
        public void upgradeGeneration() {
        	this.generationVersion = CURRENT_GENERATION_VERSION;
        }
        
        public int terrainScaler() {
        	return Math.min(this.worldHeight, 256);
        }
    }
}
