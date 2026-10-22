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
		// added after 0.0.17: presets saved before get the strata of ReTerraForged 0.0.6
		Strata.CODEC.optionalFieldOf("strata").forGetter((s) -> java.util.Optional.of(s.strata))
	).apply(instance, (smoothLayerDecorator, strataRegionSize, strataDecorator, oreCompatibleStoneOnly, erosionDecorator, plainStoneErosion, naturalSnowDecorator, customBiomeFeatures, vanillaSprings, vanillaLavaLakes, vanillaLavaSprings, mountainBiomeUsage, volcanoBiomeUsage, strata) -> new MiscellaneousSettings(smoothLayerDecorator, strataRegionSize, strataDecorator, oreCompatibleStoneOnly, erosionDecorator, plainStoneErosion, naturalSnowDecorator, customBiomeFeatures, vanillaSprings, vanillaLavaLakes, vanillaLavaSprings, mountainBiomeUsage, volcanoBiomeUsage, strata.orElseGet(Strata::makeDefault))));
	
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
    public Strata strata;
	
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
		this(smoothLayerDecorator, strataRegionSize, strataDecorator, oreCompatibleStoneOnly, erosionDecorator, plainStoneErosion, naturalSnowDecorator, customBiomeFeatures, vanillaSprings, vanillaLavaLakes, vanillaLavaSprings, mountainBiomeUsage, volcanoBiomeUsage, Strata.makeDefault());
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
	    Strata strata
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
		this.strata = strata;
	}
	
	public MiscellaneousSettings copy() {
		return new MiscellaneousSettings(this.smoothLayerDecorator, this.strataRegionSize, this.strataDecorator, this.oreCompatibleStoneOnly, this.erosionDecorator, this.plainStoneErosion, this.naturalSnowDecorator, this.customBiomeFeatures, this.vanillaSprings, this.vanillaLavaLakes, this.vanillaLavaSprings, this.mountainBiomeUsage, this.volcanoBiomeUsage, this.strata.copy());
	}

	/**
	 * The rock bands of the strata decorator: how many layers a strata region has and how thick they are relative to
	 * each other (a layer's thickness is its depth over the sum of all depths, times the height of the column).
	 * ReTerraForged 0.0.6: 10 to 30 layers, depths 0.1 to 1.5.
	 */
	public static class Strata {
		public static final Codec<Strata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("minLayers").forGetter((s) -> s.minLayers),
			Codec.INT.fieldOf("maxLayers").forGetter((s) -> s.maxLayers),
			Codec.FLOAT.fieldOf("minDepth").forGetter((s) -> s.minDepth),
			Codec.FLOAT.fieldOf("maxDepth").forGetter((s) -> s.maxDepth)
		).apply(instance, Strata::new));
		
		public int minLayers;
		public int maxLayers;
		public float minDepth;
		public float maxDepth;
		
		public Strata(int minLayers, int maxLayers, float minDepth, float maxDepth) {
			this.minLayers = minLayers;
			this.maxLayers = maxLayers;
			this.minDepth = minDepth;
			this.maxDepth = maxDepth;
		}
		
		public static Strata makeDefault() {
			return new Strata(10, 30, 0.1F, 1.5F);
		}
		
		public Strata copy() {
			return new Strata(this.minLayers, this.maxLayers, this.minDepth, this.maxDepth);
		}
	}
}
