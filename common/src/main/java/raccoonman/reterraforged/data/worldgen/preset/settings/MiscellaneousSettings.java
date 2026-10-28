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
		Strata.CODEC.optionalFieldOf("strata").forGetter((s) -> java.util.Optional.of(s.strata)),
		// added in 0.0.25: presets saved before have no snow line (off)
		SnowLine.CODEC.optionalFieldOf("snowLine").forGetter((s) -> java.util.Optional.of(s.snowLine)),
		// presets saved before 0.0.20 have none: PresetMigration gives their settings that did nothing the values
		// that keep their worlds as they were
		Codec.INT.optionalFieldOf("settingsVersion", 0).forGetter((s) -> s.settingsVersion)
	).apply(instance, (smoothLayerDecorator, strataRegionSize, strataDecorator, oreCompatibleStoneOnly, erosionDecorator, plainStoneErosion, naturalSnowDecorator, customBiomeFeatures, vanillaSprings, vanillaLavaLakes, vanillaLavaSprings, mountainBiomeUsage, volcanoBiomeUsage, strata, snowLine, settingsVersion) -> {
		MiscellaneousSettings settings = new MiscellaneousSettings(smoothLayerDecorator, strataRegionSize, strataDecorator, oreCompatibleStoneOnly, erosionDecorator, plainStoneErosion, naturalSnowDecorator, customBiomeFeatures, vanillaSprings, vanillaLavaLakes, vanillaLavaSprings, mountainBiomeUsage, volcanoBiomeUsage, strata.orElseGet(Strata::makeDefault));
		settings.snowLine = snowLine.orElseGet(SnowLine::makeDefault);
		settings.settingsVersion = settingsVersion;
		return settings;
	}));
	public static final int SETTINGS_VERSION = 1;
	
	public boolean smoothLayerDecorator;
	public int strataRegionSize;
	public boolean strataDecorator;
	public boolean oreCompatibleStoneOnly;
	public boolean erosionDecorator;
	public boolean plainStoneErosion;
	public int settingsVersion = SETTINGS_VERSION;
	public boolean naturalSnowDecorator;
	public boolean customBiomeFeatures;
	public boolean vanillaSprings;
	public boolean vanillaLavaLakes;
	public boolean vanillaLavaSprings;
    public float mountainBiomeUsage;
    public float volcanoBiomeUsage;
    public Strata strata;
    public SnowLine snowLine = SnowLine.makeDefault();
	
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
		MiscellaneousSettings copy = new MiscellaneousSettings(this.smoothLayerDecorator, this.strataRegionSize, this.strataDecorator, this.oreCompatibleStoneOnly, this.erosionDecorator, this.plainStoneErosion, this.naturalSnowDecorator, this.customBiomeFeatures, this.vanillaSprings, this.vanillaLavaLakes, this.vanillaLavaSprings, this.mountainBiomeUsage, this.volcanoBiomeUsage, this.strata.copy());
		copy.snowLine = this.snowLine.copy();
		copy.settingsVersion = this.settingsVersion;
		return copy;
	}

	/**
	 * Snow above a height whatever the biome (a mountain keeps the biome of its climate, so a warm one had no snow on
	 * its peaks): above Y {@code height}, give or take {@code variation} blocks, the ground and the trees get a layer of
	 * snow and still water freezes. Off by default: ReTerraForged 0.0.6 had none.
	 */
	public static class SnowLine {
		public static final Codec<SnowLine> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("enabled").forGetter((s) -> s.enabled),
			Codec.INT.fieldOf("height").forGetter((s) -> s.height),
			Codec.INT.fieldOf("variation").forGetter((s) -> s.variation)
		).apply(instance, SnowLine::new));

		public boolean enabled;
		public int height;
		public int variation;

		public SnowLine(boolean enabled, int height, int variation) {
			this.enabled = enabled;
			this.height = height;
			this.variation = variation;
		}

		public static SnowLine makeDefault() {
			return new SnowLine(false, DEFAULT_HEIGHT, 12);
		}

		public SnowLine copy() {
			return new SnowLine(this.enabled, this.height, this.variation);
		}

		public static final int DEFAULT_HEIGHT = 140;
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
