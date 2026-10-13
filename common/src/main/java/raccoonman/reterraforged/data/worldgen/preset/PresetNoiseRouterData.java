package raccoonman.reterraforged.data.worldgen.preset;

import java.util.List;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SpawnTargetPoint;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.data.worldgen.preset.settings.CaveSettings.CompatibilityMode;
import raccoonman.reterraforged.data.worldgen.preset.settings.CaveSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;
import raccoonman.reterraforged.platform.ModLoaderUtil;
import raccoonman.reterraforged.world.worldgen.densityfunction.CellSampler;
import raccoonman.reterraforged.world.worldgen.densityfunction.RTFDensityFunctions;
import raccoonman.reterraforged.world.worldgen.terrablender.TBCompat;

// 26.3: the overworld functions are named by NoiseRouterData.OVERWORLD_FUNCTIONS, the router has no aquifer and ore
// vein functions (aquifers are in the noise settings, ore veins in the material rule) and adds the beardifier itself,
// interpolation takes the cell size (4 x 8 as in the preset's noise settings before 26.3)
public class PresetNoiseRouterData {
	public static final ResourceKey<DensityFunction> HEIGHT = createKey("height");
	public static final ResourceKey<DensityFunction> GRADIENT = createKey("gradient");
	public static final ResourceKey<DensityFunction> HEIGHT_EROSION = createKey("erosion");
	public static final ResourceKey<DensityFunction> SEDIMENT = createKey("sediment");

	private static final float SCALER = 128.0F;
	private static final float UNIT = 1.0F / SCALER;
	private static final float UNDERGROUND_COMPAT_DEPTH_MIN = 0.2F;
	private static final int CAVE_BIOME_COMPAT_Y_MAX = 48;
	private static final int CELL_WIDTH = 4;
	private static final int CELL_HEIGHT = 8;

    public static void bootstrap(Preset preset, BootstrapContext<DensityFunction> ctx) {
        HolderGetter<DensityFunction> densityFunctions = ctx.lookup(Registries.DENSITY_FUNCTION);
        HolderGetter<NormalNoise> noiseParams = ctx.lookup(Registries.NOISE);

    	WorldSettings worldSettings = preset.world();
    	WorldSettings.Properties properties = worldSettings.properties;

    	CaveSettings caveSettings = preset.caves();

        int worldHeight = properties.worldHeight;
        int worldDepth = properties.worldDepth;

        ctx.register(NoiseRouterData.OVERWORLD_FUNCTIONS.temperature(), RTFDensityFunctions.cell(CellSampler.Field.TEMPERATURE));
        ctx.register(NoiseRouterData.OVERWORLD_FUNCTIONS.vegetation(), RTFDensityFunctions.cell(CellSampler.Field.MOISTURE));
        ctx.register(NoiseRouterData.OVERWORLD_FUNCTIONS.continents(), RTFDensityFunctions.cell(CellSampler.Field.CONTINENT));
        ctx.register(NoiseRouterData.OVERWORLD_FUNCTIONS.erosion(), RTFDensityFunctions.cell(CellSampler.Field.EROSION));
        ctx.register(NoiseRouterData.RIDGES, RTFDensityFunctions.cell(CellSampler.Field.WEIRDNESS));

        DensityFunction height = registerAndWrap(ctx, HEIGHT, RTFDensityFunctions.cell(CellSampler.Field.HEIGHT));
        DensityFunction offset = registerAndWrap(ctx, NoiseRouterData.OVERWORLD_FUNCTIONS.offset(), DensityFunctions.add(DensityFunctions.constant(NoiseRouterData.GLOBAL_OFFSET - 0.5F), DensityFunctions.mul(RTFDensityFunctions.clampToNearestUnit(height, properties.terrainScaler()), DensityFunctions.constant(2.0F))));
        DensityFunction depth = registerAndWrap(ctx, NoiseRouterData.OVERWORLD_FUNCTIONS.depth(), DensityFunctions.add(DensityFunctions.yClampedGradient(-worldDepth, worldHeight, yGradientRange(-worldDepth), yGradientRange(worldHeight)), offset));
        ctx.register(NoiseRouterData.BASE_3D_NOISE_OVERWORLD, DensityFunctions.zero());
        ctx.register(NoiseRouterData.OVERWORLD_FUNCTIONS.jaggedness(), jaggednessPerformanceHack());

        ctx.register(NoiseRouterData.NOODLE, noodle(-worldDepth, worldHeight, 1.0F - caveSettings.noodleCaveProbability, densityFunctions, noiseParams));
        ctx.register(NoiseRouterData.ENTRANCES, probabilityDensity(caveSettings.entranceCaveProbability, NoiseRouterData.entrances(densityFunctions, noiseParams)));
        ctx.register(NoiseRouterData.SPAGHETTI_2D, probabilityDensity(caveSettings.spaghettiCaveProbability, spaghetti2D(-worldDepth, worldHeight, densityFunctions, noiseParams)));

        // the surface the router and the aquifers search: the same as before 26.3, from the preset's own depth
        DensityFunction factor = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.factor());
        DensityFunction initialDensity = noiseGradientDensity(factor, depth);
        DensityFunction surfaceLevel = registerAndWrap(ctx, NoiseRouterData.OVERWORLD_FUNCTIONS.preliminarySurfaceLevel(), preliminarySurfaceLevel(slideOverworld(DensityFunctions.add(initialDensity, DensityFunctions.constant(UNIT * -90)).clamp(-64.0F, 64.0F), -worldDepth), worldDepth, worldHeight));
        ctx.register(NoiseRouterData.OVERWORLD_FUNCTIONS.chunkSurfaceLevel(), DensityFunctions.interpolated(surfaceLevel, 16, 1));

        ctx.register(GRADIENT, RTFDensityFunctions.cell(CellSampler.Field.GRADIENT));
        ctx.register(HEIGHT_EROSION, RTFDensityFunctions.cell(CellSampler.Field.HEIGHT_EROSION));
        ctx.register(SEDIMENT, RTFDensityFunctions.cell(CellSampler.Field.SEDIMENT));
    }

    protected static NoiseRouter overworld(Preset preset, HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise> noiseParams) {
    	WorldSettings worldSettings = preset.world();
    	WorldSettings.Properties properties = worldSettings.properties;
    	int worldDepth = properties.worldDepth;

    	CaveSettings caves = preset.caves();
    	float cheeseCaveDepthOffset = caves.cheeseCaveDepthOffset;

        DensityFunction depth = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.depth());
        DensityFunction slopedCheese = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.slopedCheese());

        DensityFunction entrances = caves.entranceCaveProbability > 0.0F ? DensityFunctions.min(slopedCheese, DensityFunctions.mul(DensityFunctions.constant(5.0F), DensityFunctions.interpolated(NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.ENTRANCES), CELL_WIDTH, CELL_HEIGHT))) : slopedCheese;

        DensityFunction slopedCheeseRange = DensityFunctions.mul(DensityFunctions.rangeChoice(slopedCheese, -1000000.0F, cheeseCaveDepthOffset, entrances, DensityFunctions.interpolated(slideOverworld(underground(caves.cheeseCaveProbability, densityFunctions, noiseParams, slopedCheese), -worldDepth), CELL_WIDTH, CELL_HEIGHT)), DensityFunctions.constant(0.64F)).squeeze();
        // the structure terrain adaptation, added to every router by NoiseChunk before 26.3
        DensityFunction finalDensity = DensityFunctions.add(DensityFunctions.min(slopedCheeseRange, NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.NOODLE)), DensityFunctions.beardifier());
        DensityFunction y = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.Y);
        return new NoiseRouter(
        	NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.temperature()),
        	NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.vegetation()),
        	NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.continents()),
        	NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.erosion()),
        	climateDepth(caves, depth, y, worldDepth, preset.miscellaneous().originalGeneration),
        	NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.RIDGES),
        	NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.chunkSurfaceLevel()),
        	finalDensity
        );
	}

    // the aquifer noises as before 26.3; the deep dark region keeps aquifers out of it like in vanilla
    protected static Aquifer.Config aquifers(HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise> noiseParams) {
    	DensityFunction aquiferBarrier = DensityFunctions.noise(noiseParams.getOrThrow(Noises.AQUIFER_BARRIER), 0.5);
        DensityFunction aquiferFluidLevelFloodedness = DensityFunctions.noise(noiseParams.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 0.67);
        DensityFunction aquiferFluidLevelSpread = DensityFunctions.noise(noiseParams.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 0.7142857142857143);
        DensityFunction aquiferLava = DensityFunctions.noise(noiseParams.getOrThrow(Noises.AQUIFER_LAVA));
        DensityFunction exclusion = OverworldBiomeBuilder.deepDarkRegion(NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.erosion()), NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.depth()));
        return new Aquifer.Config(aquiferBarrier, aquiferFluidLevelFloodedness, aquiferFluidLevelSpread, aquiferLava, exclusion, NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.OVERWORLD_FUNCTIONS.preliminarySurfaceLevel()));
    }

    protected static List<SpawnTargetPoint> spawnTarget(Preset preset, HolderGetter<DensityFunction> densityFunctions) {
    	return preset.world().properties.spawnType.getSpawnTarget(
    		densityFunctions.getOrThrow(NoiseRouterData.OVERWORLD_FUNCTIONS.temperature()),
    		densityFunctions.getOrThrow(NoiseRouterData.OVERWORLD_FUNCTIONS.vegetation()),
    		densityFunctions.getOrThrow(NoiseRouterData.OVERWORLD_FUNCTIONS.continents()),
    		densityFunctions.getOrThrow(NoiseRouterData.OVERWORLD_FUNCTIONS.erosion()),
    		densityFunctions.getOrThrow(NoiseRouterData.RIDGES)
    	);
    }

	// Since 1.21.9 the router has the preliminary surface height itself instead of the density without jaggedness that
	// NoiseChunk searched: the same search, from the top of the world down by cell height (8 in the preset's noise
	// settings) to the first point with density above 0.390625.
	private static DensityFunction preliminarySurfaceLevel(DensityFunction initialDensityWithoutJaggedness, int worldDepth, int worldHeight) {
		return DensityFunctions.findTopSurface(DensityFunctions.add(initialDensityWithoutJaggedness, DensityFunctions.constant(-0.390625F)), DensityFunctions.constant(worldHeight), -worldDepth, CELL_HEIGHT);
	}

    private static DensityFunction climateDepth(CaveSettings caves, DensityFunction depth, DensityFunction y, int worldDepth, boolean original) {
        // ReTerraForged 0.0.6: depth - 0.275, the cave biomes as deep as there; the 1.21.1 port moved them up (- 0.205)
        // and clamps the depth for TerraBlender (kept with 0.0.6's depth only when a cave biome mod needs it)
        DensityFunction climateDepth = DensityFunctions.add(depth, DensityFunctions.constant(original ? -0.275F : -0.205F));
        CompatibilityMode mode = caves.compatibilityMode;
        if(mode == CompatibilityMode.RTF || (mode == CompatibilityMode.AUTO && !TBCompat.isEnabled()) || (original && mode == CompatibilityMode.AUTO && !hasUndergroundBiomeCompatTarget())) {
            return climateDepth;
        }
        DensityFunction terraBlenderDepth = climateDepth.clamp(-1.0F, terraBlenderClimateDepthMax(mode));
        if(mode == CompatibilityMode.AUTO && hasUndergroundBiomeCompatTarget()) {
            DensityFunction undergroundDepth = terraBlenderDepth.clamp(UNDERGROUND_COMPAT_DEPTH_MIN, terraBlenderClimateDepthMax(mode));
            return DensityFunctions.rangeChoice(y, -worldDepth, CAVE_BIOME_COMPAT_Y_MAX, undergroundDepth, terraBlenderDepth);
        }
        return terraBlenderDepth;
    }

    private static float terraBlenderClimateDepthMax(CompatibilityMode mode) {
        return mode == CompatibilityMode.AUTO && ModLoaderUtil.isLoaded("darkerdepths") ? 0.9F : 1.0F;
    }

    private static boolean hasUndergroundBiomeCompatTarget() {
        return ModLoaderUtil.isLoaded("darkerdepths") || ModLoaderUtil.isLoaded("deeperdarker");
    }

    private static DensityFunction underground(float cheeseCaveProbability, HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise> noiseParams, DensityFunction slopedCheese) {
        DensityFunction spaghetti2d = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.SPAGHETTI_2D);
        DensityFunction spaghettiRoughnessFunction = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.SPAGHETTI_ROUGHNESS_FUNCTION);
        DensityFunction caveLayerNoise = DensityFunctions.noise(noiseParams.getOrThrow(Noises.CAVE_LAYER), 8.0);
        DensityFunction caveLayer = DensityFunctions.mul(DensityFunctions.constant(4.0F), caveLayerNoise.square());
        DensityFunction caveCheese = probabilityDensity(cheeseCaveProbability, DensityFunctions.noise(noiseParams.getOrThrow(Noises.CAVE_CHEESE), 0.6666666666666666));
        DensityFunction slopedCaves = DensityFunctions.add(DensityFunctions.add(DensityFunctions.constant(0.27F), caveCheese).clamp(-1.0F, 1.0F), DensityFunctions.add(DensityFunctions.constant(1.5F), DensityFunctions.mul(DensityFunctions.constant(-0.64F), slopedCheese)).clamp(0.0F, 0.5F));
        DensityFunction slopedCaveLayered = DensityFunctions.add(caveLayer, slopedCaves);
        DensityFunction underground = DensityFunctions.min(DensityFunctions.min(slopedCaveLayered, NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.ENTRANCES)), DensityFunctions.add(spaghetti2d, spaghettiRoughnessFunction));
        DensityFunction pillars = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.PILLARS);
        DensityFunction pillarRange = DensityFunctions.rangeChoice(pillars, -1000000.0F, 0.03F, DensityFunctions.constant(-1000000.0F), pillars);
        return DensityFunctions.max(underground, pillarRange);
    }

    private static DensityFunction spaghetti2D(int minY, int maxY, HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise> noiseParams) {
        DensityFunction modulator = DensityFunctions.noise(noiseParams.getOrThrow(Noises.SPAGHETTI_2D_MODULATOR), 2.0, 1.0);
        DensityFunction sampler = NoiseRouterData.QuantizedSpaghettiRarity.wrapRarity2d(modulator, noiseParams.getOrThrow(Noises.SPAGHETTI_2D));
        DensityFunction elevation = DensityFunctions.mappedNoise(noiseParams.getOrThrow(Noises.SPAGHETTI_2D_ELEVATION), 0.0, Math.floorDiv(minY, 8), 8.0F);
        DensityFunction thicknessModulator = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.SPAGHETTI_2D_THICKNESS_MODULATOR);
        DensityFunction elevationGradient = DensityFunctions.add(elevation, DensityFunctions.yClampedGradient(minY, maxY, minY / -8.0F, maxY / -8.0F)).abs();
        DensityFunction normal = DensityFunctions.add(elevationGradient, thicknessModulator).cube();
        DensityFunction weird = DensityFunctions.add(sampler, DensityFunctions.mul(DensityFunctions.constant(0.083F), thicknessModulator));
        return DensityFunctions.max(weird, normal).clamp(-1.0F, 1.0F);
    }

    private static DensityFunction noodle(int minY, int maxY, float threshold, HolderGetter<DensityFunction> densityFunctions, HolderGetter<NormalNoise> noiseParams) {
    	int baseY = minY + 4;

    	DensityFunction y = NoiseRouterData.getFunction(densityFunctions, NoiseRouterData.Y);
        DensityFunction selector = yLimitedInterpolatable(y, DensityFunctions.noise(noiseParams.getOrThrow(Noises.NOODLE), 1.0, 1.0), baseY, maxY, -1);
        DensityFunction thickness = yLimitedInterpolatable(y, DensityFunctions.mappedNoise(noiseParams.getOrThrow(Noises.NOODLE_THICKNESS), 1.0, 1.0, -0.05F, -0.1F), baseY, maxY, 0);
        DensityFunction ridgeA = yLimitedInterpolatable(y, DensityFunctions.noise(noiseParams.getOrThrow(Noises.NOODLE_RIDGE_A), 2.6666666666666665, 2.6666666666666665), baseY, maxY, 0);
        DensityFunction ridgeB = yLimitedInterpolatable(y, DensityFunctions.noise(noiseParams.getOrThrow(Noises.NOODLE_RIDGE_B), 2.6666666666666665, 2.6666666666666665), baseY, maxY, 0);
        DensityFunction ridge = DensityFunctions.mul(DensityFunctions.constant(1.5F), DensityFunctions.max(ridgeA.abs(), ridgeB.abs()));
        return DensityFunctions.rangeChoice(selector, -1000000.0F, threshold, DensityFunctions.constant(64.0F), DensityFunctions.add(thickness, ridge));
    }

    // as vanilla's (private there)
    private static DensityFunction yLimitedInterpolatable(DensityFunction y, DensityFunction whenInRange, int minYInclusive, int maxYInclusive, int whenOutOfRange) {
    	return DensityFunctions.interpolated(DensityFunctions.rangeChoice(y, minYInclusive, maxYInclusive + 1, whenInRange, DensityFunctions.constant(whenOutOfRange)), CELL_WIDTH, CELL_HEIGHT);
    }

    // as vanilla's (private there)
    private static DensityFunction noiseGradientDensity(DensityFunction factor, DensityFunction depthWithJaggedness) {
    	DensityFunction gradientUnscaled = DensityFunctions.mul(depthWithJaggedness, factor);
    	return gradientUnscaled.quarterNegative().mul(4.0F);
    }

    private static DensityFunction slideOverworld(DensityFunction function, int minY) {
        return slide(function, minY, 0, 24, UNIT * 15);
    }

    private static DensityFunction slide(DensityFunction function, int minY, int bottomGradientStart, int bottomGradientEnd, float bottomGradientTarget) {
        DensityFunction bottomGradient = DensityFunctions.yClampedGradient(minY + bottomGradientStart, minY + bottomGradientEnd, 0.0F, 1.0F);
        return DensityFunctions.lerp(bottomGradient, bottomGradientTarget, function);
    }

    /*
     * the multiply function doesnt sample the second input
     * if the first input is zero, however this optimization doesn't get
     * applied if either input is a Constant, so if we use
     * use DensityFunctions.zero() Noises.JAGGED will still get sampled
     */
    private static DensityFunction jaggednessPerformanceHack() {
    	return DensityFunctions.add(DensityFunctions.zero(), DensityFunctions.zero());
    }

    // do this a different way, since this affects the size of the cave as well
    @Deprecated
    private static DensityFunction probabilityDensity(float probability, DensityFunction function) {
    	if(probability == 0.0F) {
    		return DensityFunctions.constant(1.0F);
    	}
    	return DensityFunctions.add(DensityFunctions.constant(1.0F - probability), function);
    }

    private static float yGradientRange(float range) {
    	return 1.0F + (-range / SCALER);
    }

    private static DensityFunction registerAndWrap(BootstrapContext<DensityFunction> ctx, ResourceKey<DensityFunction> key, DensityFunction function) {
    	return new DensityFunctions.HolderHolder(ctx.register(key, function));
    }

    private static ResourceKey<DensityFunction> createKey(String string) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, RTFCommon.location(string));
    }
}
