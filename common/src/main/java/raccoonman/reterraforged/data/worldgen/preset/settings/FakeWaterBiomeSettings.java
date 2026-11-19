package raccoonman.reterraforged.data.worldgen.preset.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class FakeWaterBiomeSettings {
	public static final Codec<FakeWaterBiomeSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.BOOL.fieldOf("enableFakeWaterBiomes").forGetter((o) -> o.enableFakeWaterBiomes),
		Codec.BOOL.fieldOf("steppeBelowSeaLevelRiverBiomes").forGetter((o) -> o.steppeBelowSeaLevelRiverBiomes),
		Codec.BOOL.fieldOf("badlandsBelowSeaLevelOceanBiomes").forGetter((o) -> o.badlandsBelowSeaLevelOceanBiomes),
		Codec.FLOAT.fieldOf("heightOffset").forGetter((o) -> o.heightOffset),
		Codec.BOOL.optionalFieldOf("plateauBelowSeaLevelRiverBiomes", false).forGetter((o) -> o.plateauBelowSeaLevelRiverBiomes),
		Codec.BOOL.optionalFieldOf("hillsBelowSeaLevelRiverBiomes", false).forGetter((o) -> o.hillsBelowSeaLevelRiverBiomes),
		Codec.BOOL.optionalFieldOf("mountainsBelowSeaLevelRiverBiomes", false).forGetter((o) -> o.mountainsBelowSeaLevelRiverBiomes),
		Codec.BOOL.optionalFieldOf("plainsBelowSeaLevelRiverBiomes", false).forGetter((o) -> o.plainsBelowSeaLevelRiverBiomes)
	).apply(instance, FakeWaterBiomeSettings::new));
	
	public boolean enableFakeWaterBiomes;
	public boolean steppeBelowSeaLevelRiverBiomes;
	// rivers, not oceans (the resolver answers RIVER for both; the name is the presets' key and stays)
	public boolean badlandsBelowSeaLevelOceanBiomes;
	public float heightOffset;
	// 0.0.32: the other land the sea can stand in (a canyon of a plateau below sea level kept its land biome). Off in
	// presets saved before and by default: a world made with Fake Water Biomes on keeps its biomes
	public boolean plateauBelowSeaLevelRiverBiomes;
	public boolean hillsBelowSeaLevelRiverBiomes;
	public boolean mountainsBelowSeaLevelRiverBiomes;
	public boolean plainsBelowSeaLevelRiverBiomes;
	
	public FakeWaterBiomeSettings(boolean enableFakeWaterBiomes, boolean steppeBelowSeaLevelRiverBiomes, boolean badlandsBelowSeaLevelOceanBiomes, float heightOffset) {
		this(enableFakeWaterBiomes, steppeBelowSeaLevelRiverBiomes, badlandsBelowSeaLevelOceanBiomes, heightOffset, false, false, false, false);
	}

	public FakeWaterBiomeSettings(boolean enableFakeWaterBiomes, boolean steppeBelowSeaLevelRiverBiomes, boolean badlandsBelowSeaLevelOceanBiomes, float heightOffset, boolean plateauBelowSeaLevelRiverBiomes, boolean hillsBelowSeaLevelRiverBiomes, boolean mountainsBelowSeaLevelRiverBiomes, boolean plainsBelowSeaLevelRiverBiomes) {
		this.enableFakeWaterBiomes = enableFakeWaterBiomes;
		this.steppeBelowSeaLevelRiverBiomes = steppeBelowSeaLevelRiverBiomes;
		this.badlandsBelowSeaLevelOceanBiomes = badlandsBelowSeaLevelOceanBiomes;
		this.heightOffset = heightOffset;
		this.plateauBelowSeaLevelRiverBiomes = plateauBelowSeaLevelRiverBiomes;
		this.hillsBelowSeaLevelRiverBiomes = hillsBelowSeaLevelRiverBiomes;
		this.mountainsBelowSeaLevelRiverBiomes = mountainsBelowSeaLevelRiverBiomes;
		this.plainsBelowSeaLevelRiverBiomes = plainsBelowSeaLevelRiverBiomes;
	}
	
	public FakeWaterBiomeSettings copy() {
		return new FakeWaterBiomeSettings(this.enableFakeWaterBiomes, this.steppeBelowSeaLevelRiverBiomes, this.badlandsBelowSeaLevelOceanBiomes, this.heightOffset, this.plateauBelowSeaLevelRiverBiomes, this.hillsBelowSeaLevelRiverBiomes, this.mountainsBelowSeaLevelRiverBiomes, this.plainsBelowSeaLevelRiverBiomes);
	}
	
	public static FakeWaterBiomeSettings makeDefault() {
		return new FakeWaterBiomeSettings(false, true, true, 0.0F);
	}
}
