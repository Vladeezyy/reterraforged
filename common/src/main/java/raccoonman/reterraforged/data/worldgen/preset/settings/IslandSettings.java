package raccoonman.reterraforged.data.worldgen.preset.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class IslandSettings {
	public static final Codec<IslandSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.BOOL.fieldOf("enableArchipelago").forGetter((o) -> o.enableArchipelago),
		Codec.FLOAT.fieldOf("islandDensity").forGetter((o) -> o.islandDensity),
		Codec.FLOAT.fieldOf("islandSize").forGetter((o) -> o.islandSize),
		Codec.FLOAT.fieldOf("islandHeight").forGetter((o) -> o.islandHeight),
		Codec.FLOAT.fieldOf("islandBaseScale").forGetter((o) -> o.islandBaseScale),
		Codec.FLOAT.fieldOf("islandVerticalScale").forGetter((o) -> o.islandVerticalScale),
		Codec.FLOAT.fieldOf("islandHorizontalScale").forGetter((o) -> o.islandHorizontalScale),
		Codec.FLOAT.fieldOf("mountainChance").forGetter((o) -> o.mountainChance),
		Codec.FLOAT.fieldOf("volcanoChance").forGetter((o) -> o.volcanoChance),
		Codec.FLOAT.fieldOf("offshoreDepth").forGetter((o) -> o.offshoreDepth),
		Codec.FLOAT.fieldOf("beachWidth").forGetter((o) -> o.beachWidth),
		Codec.FLOAT.fieldOf("beachCoverage").forGetter((o) -> o.beachCoverage),
		Codec.INT.optionalFieldOf("seedOffset", 0).forGetter((o) -> o.seedOffset),
		CoastNoise.CODEC.optionalFieldOf("coastNoise", CoastNoise.makeDefault()).forGetter((o) -> o.coastNoise),
		Codec.FLOAT.optionalFieldOf("continentDistance", 0.0F).forGetter((o) -> o.continentDistance),
		Codec.BOOL.optionalFieldOf("continentMountains", false).forGetter((o) -> o.continentMountains)
	).apply(instance, IslandSettings::new));
	
	public boolean enableArchipelago;
	public float islandDensity;
	public float islandSize;
	public float islandHeight;
	public float islandBaseScale;
	public float islandVerticalScale;
	public float islandHorizontalScale;
	public float mountainChance;
	public float volcanoChance;
	public float offshoreDepth;
	public float beachWidth;
	public float beachCoverage;
	public int seedOffset;
	// the noise of the islands' outline, as continentNoise of a continent's
	public CoastNoise coastNoise;
	// 0 islands up to the continents' coasts, 1 only out in the open sea
	public float continentDistance;
	// the mountains of the continents instead of the islands' own ridges
	public boolean continentMountains;
	
	public IslandSettings(boolean enableArchipelago, float islandDensity, float islandSize, float islandHeight, float islandBaseScale, float islandVerticalScale, float islandHorizontalScale, float mountainChance, float volcanoChance, float offshoreDepth, float beachWidth, float beachCoverage, int seedOffset, CoastNoise coastNoise, float continentDistance, boolean continentMountains) {
		this.enableArchipelago = enableArchipelago;
		this.islandDensity = islandDensity;
		this.islandSize = islandSize;
		this.islandHeight = islandHeight;
		this.islandBaseScale = islandBaseScale;
		this.islandVerticalScale = islandVerticalScale;
		this.islandHorizontalScale = islandHorizontalScale;
		this.mountainChance = mountainChance;
		this.volcanoChance = volcanoChance;
		this.offshoreDepth = offshoreDepth;
		this.beachWidth = beachWidth;
		this.beachCoverage = beachCoverage;
		this.seedOffset = seedOffset;
		this.coastNoise = coastNoise;
		this.continentDistance = continentDistance;
		this.continentMountains = continentMountains;
	}
	
	public IslandSettings copy() {
		return new IslandSettings(this.enableArchipelago, this.islandDensity, this.islandSize, this.islandHeight, this.islandBaseScale, this.islandVerticalScale, this.islandHorizontalScale, this.mountainChance, this.volcanoChance, this.offshoreDepth, this.beachWidth, this.beachCoverage, this.seedOffset, this.coastNoise.copy(), this.continentDistance, this.continentMountains);
	}
	
	public static IslandSettings makeDefault() {
		return new IslandSettings(false, 0.5F, 200.0F, 0.5F, 0.3F, 1.0F, 1.0F, 0.3F, 0.1F, 0.5F, 0.15F, 0.3F, 0, CoastNoise.makeDefault(), 0.0F, false);
	}
	
	public static class CoastNoise {
		public static final Codec<CoastNoise> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("octaves").forGetter((o) -> o.octaves),
			Codec.FLOAT.fieldOf("gain").forGetter((o) -> o.gain),
			Codec.FLOAT.fieldOf("lacunarity").forGetter((o) -> o.lacunarity)
		).apply(instance, CoastNoise::new));
		
		public int octaves;
		public float gain;
		public float lacunarity;
		
		public CoastNoise(int octaves, float gain, float lacunarity) {
			this.octaves = octaves;
			this.gain = gain;
			this.lacunarity = lacunarity;
		}
		
		public CoastNoise copy() {
			return new CoastNoise(this.octaves, this.gain, this.lacunarity);
		}
		
		public static CoastNoise makeDefault() {
			return new CoastNoise(5, 0.5F, 2.0F);
		}
	}
}
