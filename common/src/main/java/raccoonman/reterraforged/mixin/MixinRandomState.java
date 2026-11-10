package raccoonman.reterraforged.mixin; 

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.world.worldgen.biome.RTFClimateSampler;
import raccoonman.reterraforged.concurrent.ThreadPools;
import raccoonman.reterraforged.config.PerformanceConfig;
import raccoonman.reterraforged.data.worldgen.compat.terrablender.TBNoiseRouterData;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.tags.RTFDensityFunctionTags;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.OverworldRegistries;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.WorldLookup;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.densityfunction.CellSampler;
import raccoonman.reterraforged.world.worldgen.densityfunction.NoiseFunction;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.terrablender.TBClimateSampler;
import raccoonman.reterraforged.world.worldgen.terrablender.TBCompat;

@Mixin(RandomState.class)
@Implements(@Interface(iface = RTFRandomState.class, prefix = "reterraforged$RTFRandomState$"))
class MixinRandomState {
	private DensityFunction.Visitor densityFunctionWrapper;
	@Shadow
	@Final
	private Climate.Sampler sampler;
	@Shadow
	@Final
    private SurfaceSystem surfaceSystem;
	
	private volatile boolean hasContext;
	// volatile: generation threads read it while the lazy initialization below sets it
	@Nullable
	private volatile GeneratorContext generatorContext;
	@Nullable
	private volatile Preset preset;
	// the noise router has ReTerraForged density functions (set while the router is built, never cleared)
	private boolean reTerraForgedFunctions;
	
	private long seed;
	private NoiseGeneratorSettings noiseGeneratorSettings;
	// initialize started (also while it runs: a call back into it from the same thread gets no context, not a loop)
	private volatile boolean initialized;
	// initialize finished: only then may a missing context be taken for "no context". With the flag above alone, a thread
	// that came while another one built the context got none, and its chunks were made with a height of 0 (a sea)
	private volatile boolean ready;
	
	@Redirect(
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/NoiseRouter;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/NoiseRouter;"
		),
		method = "<init>",
		require = 1
	)
	private NoiseRouter RandomState(NoiseRouter router, DensityFunction.Visitor visitor, NoiseGeneratorSettings noiseGeneratorSettings, HolderGetter<NormalNoise.NoiseParameters> params, final long seed) {
		this.seed = seed;
		this.noiseGeneratorSettings = noiseGeneratorSettings;
		// one for all the samplers of this state, not one per visit: CellSampler is a record, and NoiseChunk.wrap tells
		// functions apart by equals. With a lambda per visit every use of a function over a cell was a function of its own,
		// so a cache_once around it (Terralith's overworld/sloped_cheese) became several CacheOnce in a NoiseChunk. The one
		// only read through compute keeps one value for the whole cell while the cell is filled: the terrain went in steps
		// of 8 (D-42). lithostitched 1.8+ hides this with its MapAllCache, 1.7.2 on 1.21.11 does not have it.
		Supplier<WorldLookup> lookup = () -> {
			GeneratorContext context = this.generatorContext;
			if(context == null && !this.ready) {
				context = this.initializeLazily();
			}
			return context != null ? context.lookup : null;
		};
		this.densityFunctionWrapper = new DensityFunction.Visitor() {
			
			@Override
			public DensityFunction apply(DensityFunction function) {
				if(function.getClass().getName().startsWith("raccoonman.reterraforged.")) {
					MixinRandomState.this.reTerraForgedFunctions = true;
				}
				if(function instanceof NoiseFunction.Marker marker) {
					return new NoiseFunction(marker.noise(), (int) seed);
				}
				if(function instanceof CellSampler.Marker marker) {
					MixinRandomState.this.hasContext = true;
					return new CellSampler(lookup, marker.field());
				}
				return visitor.apply(function);
			}

			@Override
			public NoiseHolder visitNoise(NoiseHolder noiseHolder) {
	            return visitor.visitNoise(noiseHolder);
	        }
		};
		return router.mapAll(this.densityFunctionWrapper);
	}

	public synchronized void reterraforged$RTFRandomState$initialize(ServerLevel level) {
		RegistryAccess registries = level.registryAccess();
		this.initialized = true;
		try {
			if(level.dimension().equals(Level.OVERWORLD)) {
				OverworldRegistries.set(registries);
			} else if(!this.hasContext || !generationFixes(registries)) {
				this.clearGeneratorContext();
				return;
			}
			// also another dimension on the overworld's noise settings (a mod's mining dimension, a datapack's): our functions
			// are in its noise router, and without a context they gave a sea over a flat floor everywhere. Not in a world with a
			// preset saved before: what was explored of such a dimension stays as it is
			this.initialize(registries);
		} finally {
			this.ready = true;
		}
	}

	private static boolean generationFixes(RegistryAccess registries) {
		return registries.lookupOrThrow(RTFRegistries.PRESET).get(Preset.KEY).map((preset) -> preset.value().world().properties.generationFixes).orElse(false);
	}

	// a random state no ChunkMap set up: our noise settings are the overworld's, so it gets the overworld's preset
	@Nullable
	// synchronized: the other threads wait here until the context is made, and get it
	private synchronized GeneratorContext initializeLazily() {
		RegistryAccess registries = OverworldRegistries.get();
		if(!this.initialized && registries != null) {
			this.initialized = true;
			RTFCommon.LOGGER.info("[RTF] a ReTerraForged random state made outside a ChunkMap (another mod's generator) takes the overworld's preset");
			try {
				this.initialize(registries);
			} finally {
				this.ready = true;
			}
		}
		return this.generatorContext;
	}

	private void initialize(RegistryAccess registries) {
		// 只在主世界应用RTF地形
		RegistryLookup<Preset> presets = registries.lookupOrThrow(RTFRegistries.PRESET);
		RegistryLookup<Noise> noises = registries.lookupOrThrow(RTFRegistries.NOISE);
		RegistryLookup<DensityFunction> functions = registries.lookupOrThrow(Registries.DENSITY_FUNCTION);

		functions.get(RTFDensityFunctionTags.ADDITIONAL_NOISE_ROUTER_FUNCTIONS).ifPresent((set) -> {
			set.forEach((function) -> function.value().mapAll(this.densityFunctionWrapper));
		});
		
		if((Object) this.sampler instanceof TBClimateSampler tbClimateSampler && TBCompat.isEnabled()) {
			functions.get(TBNoiseRouterData.UNIQUENESS).ifPresent((uniqueness) -> {
				tbClimateSampler.setUniqueness(uniqueness.value().mapAll(this.densityFunctionWrapper));
			});
		}
		
		presets.get(Preset.KEY).ifPresent((presetHolder) -> {
			this.preset = presetHolder.value();

			if(this.hasContext) {
				PerformanceConfig config = PerformanceConfig.read(PerformanceConfig.defaultFile())
					.resultOrPartial(RTFCommon.LOGGER::error)
					.orElseGet(PerformanceConfig::makeDefault);
				this.generatorContext = GeneratorContext.makeCached(this.preset, noises, (int) this.seed, config.tileSize(), config.batchCount(), ThreadPools.availableProcessors() > 4);
				// where the world spawn is searched from (setInitialSpawn runs after the levels are created)
				if((Object) this.sampler instanceof RTFClimateSampler rtfClimateSampler) {
					rtfClimateSampler.setSpawnSearchCenter(this.preset.world().properties.spawnType.getSearchCenter(this.generatorContext));
					// the climate takes rivers and lakes for land: without this the spawn could be in a lake too wide
					// for the spawn chunk search (a player in the water)
					GeneratorContext spawnContext = this.generatorContext;
					rtfClimateSampler.setSpawnWater((x, z) -> {
						Cell cell = new Cell();
						spawnContext.generator.getHeightmap().apply(cell, x, z, false);
						return cell.height <= spawnContext.levels.water;
					});
				}
			}
		});
	}
	
	@Nullable
	public Preset reterraforged$RTFRandomState$preset() {
		return this.preset;
	}
	
	public boolean reterraforged$RTFRandomState$hasReTerraForgedFunctions() {
		return this.reTerraForgedFunctions;
	}

	@Nullable
	public GeneratorContext reterraforged$RTFRandomState$generatorContext() {
		GeneratorContext context = this.generatorContext;
		// a random state no ChunkMap set up: a NoiseChunk asks for the context before any density function did, and
		// without it the chunk had no ReTerraForged terrain at all
		if(context == null && !this.ready && this.hasContext) {
			context = this.initializeLazily();
		}
		return context;
	}

	@Nullable
	public DensityFunction reterraforged$RTFRandomState$wrap(DensityFunction function) {
		return function.mapAll(this.densityFunctionWrapper);
	}

	public Noise reterraforged$RTFRandomState$seed(Noise noise) {
		return Noises.shiftSeed(noise, (int) this.seed);
	}

	private void clearGeneratorContext() {
		this.hasContext = false;
		this.generatorContext = null;
		this.preset = null;
	}
}
