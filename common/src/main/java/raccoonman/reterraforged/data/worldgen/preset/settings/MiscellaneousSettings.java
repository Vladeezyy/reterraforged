package raccoonman.reterraforged.data.worldgen.preset.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class MiscellaneousSettings {
	public static final Codec<MiscellaneousSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.BOOL.fieldOf("smoothLayerDecorator").forGetter((s) -> s.smoothLayerDecorator),
		Codec.INT.fieldOf("strataRegionSize").forGetter((s) -> s.strataRegionSize),
		Codec.BOOL.fieldOf("strataDecorator").forGetter((s) -> s.strataDecorator),
		Codec.BOOL.fieldOf("oreCompatibleStoneOnly").forGetter((s) -> s.oreCompatibleStoneOnly),
		Codec.BOOL.fieldOf("erosionDecorator").forGetter((s) -> s.erosionDecorator),
		Codec.BOOL.fieldOf("plainStoneErosion").forGetter((s) -> s.plainStoneErosion),
		Codec.BOOL.fieldOf("naturalSnowDecorator").forGetter((s) -> s.naturalSnowDecorator),
		Codec.BOOL.fieldOf("customBiomeFeatures").forGetter((s) -> s.customBiomeFeatures),
		Codec.BOOL.fieldOf("vanillaSprings").forGetter((s) -> s.vanillaSprings),
		Codec.BOOL.fieldOf("vanillaLavaLakes").forGetter((s) -> s.vanillaLavaLakes),
		Codec.BOOL.fieldOf("vanillaLavaSprings").forGetter((s) -> s.vanillaLavaSprings),
		Codec.FLOAT.fieldOf("mountainBiomeUsage").forGetter((s) -> s.mountainBiomeUsage),
		Codec.FLOAT.fieldOf("volcanoBiomeUsage").forGetter((s) -> s.volcanoBiomeUsage),
		// left out (false) in presets made before it existed: those worlds keep the generation they were made with
		Codec.BOOL.optionalFieldOf("originalGeneration", false).forGetter((s) -> s.originalGeneration)
	).apply(instance, MiscellaneousSettings::new));
	
	public boolean smoothLayerDecorator;
	public int strataRegionSize;
	public boolean strataDecorator;
	public boolean oreCompatibleStoneOnly;
	public boolean erosionDecorator;
	public boolean plainStoneErosion;
	public boolean naturalSnowDecorator;
	public boolean customBiomeFeatures;
	public boolean vanillaSprings;
	public boolean vanillaLavaLakes;
	public boolean vanillaLavaSprings;
    public float mountainBiomeUsage;
    public float volcanoBiomeUsage;
    // the generation of ReTerraForged 0.0.6 (1.20.1) where the 1.21.1 port changed it: the climate of mountain
    // regions, the weirdness of lowland terrain, rivers, lakes and wetlands, structures on mountains, desert slopes,
    // bushes, the depth offset and the random source of the noise settings; off: the 1.21.1 port's generation
    public boolean originalGeneration;
	
	public MiscellaneousSettings(
		boolean smoothLayerDecorator,
		int strataRegionSize,
		boolean strataDecorator,
		boolean oreCompatibleStoneOnly,
		boolean erosionDecorator,
		boolean plainStoneErosion,
		boolean naturalSnowDecorator,
		boolean customBiomeFeatures,
		boolean vanillaSprings,
		boolean vanillaLavaLakes,
		boolean vanillaLavaSprings,
	    float mountainBiomeUsage,
	    float volcanoBiomeUsage
	) {
		this(smoothLayerDecorator, strataRegionSize, strataDecorator, oreCompatibleStoneOnly, erosionDecorator, plainStoneErosion, naturalSnowDecorator, customBiomeFeatures, vanillaSprings, vanillaLavaLakes, vanillaLavaSprings, mountainBiomeUsage, volcanoBiomeUsage, false);
	}

	public MiscellaneousSettings(
		boolean smoothLayerDecorator,
		int strataRegionSize,
		boolean strataDecorator,
		boolean oreCompatibleStoneOnly,
		boolean erosionDecorator,
		boolean plainStoneErosion,
		boolean naturalSnowDecorator,
		boolean customBiomeFeatures,
		boolean vanillaSprings,
		boolean vanillaLavaLakes,
		boolean vanillaLavaSprings,
	    float mountainBiomeUsage,
	    float volcanoBiomeUsage,
	    boolean originalGeneration
	) {
		this.smoothLayerDecorator = smoothLayerDecorator;
		this.strataRegionSize = strataRegionSize;
		this.strataDecorator = strataDecorator;
		this.oreCompatibleStoneOnly = oreCompatibleStoneOnly;
		this.erosionDecorator = erosionDecorator;
		this.plainStoneErosion = plainStoneErosion;
		this.naturalSnowDecorator = naturalSnowDecorator;
		this.customBiomeFeatures = customBiomeFeatures;
		this.vanillaSprings = vanillaSprings;
		this.vanillaLavaLakes = vanillaLavaLakes;
		this.vanillaLavaSprings = vanillaLavaSprings;
		this.mountainBiomeUsage = mountainBiomeUsage;
		this.volcanoBiomeUsage = volcanoBiomeUsage;
		this.originalGeneration = originalGeneration;
	}
	
	public MiscellaneousSettings copy() {
		return new MiscellaneousSettings(this.smoothLayerDecorator, this.strataRegionSize, this.strataDecorator, this.oreCompatibleStoneOnly, this.erosionDecorator, this.plainStoneErosion, this.naturalSnowDecorator, this.customBiomeFeatures, this.vanillaSprings, this.vanillaLavaLakes, this.vanillaLavaSprings, this.mountainBiomeUsage, this.volcanoBiomeUsage, this.originalGeneration);
	}
}
