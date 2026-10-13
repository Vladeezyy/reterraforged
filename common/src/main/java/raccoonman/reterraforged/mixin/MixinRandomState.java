package raccoonman.reterraforged.mixin;

import java.util.function.BiPredicate;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.biome.Climate;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.concurrent.ThreadPools;
import raccoonman.reterraforged.config.PerformanceConfig;
import raccoonman.reterraforged.data.worldgen.compat.terrablender.TBNoiseRouterData;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.tags.RTFDensityFunctionTags;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.densityfunction.CellSampler;
import raccoonman.reterraforged.world.worldgen.densityfunction.RTFCompileContext;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.terrablender.TBClimateSampler;
import raccoonman.reterraforged.world.worldgen.terrablender.TBCompat;

@Mixin(RandomState.class)
@Implements(@Interface(iface = RTFRandomState.class, prefix = "reterraforged$RTFRandomState$"))
class MixinRandomState {
	@Shadow
	@Final
	private long seed;
	@Shadow
	@Final
	private NoiseRouter router;

	@Nullable
	private GeneratorContext generatorContext;
	@Nullable
	private Preset preset;
	@Nullable
	private BlockPos spawnSearchCenter;
	@Nullable
	private BiPredicate<Integer, Integer> spawnWater;
	// TerraBlender's region choice from the preset's biome regions (MixinParameterList)
	@Nullable
	private DensityFunction tbUniqueness;

	// 26.3: density functions are compiled with this context; the mod's functions reach this RandomState through it
	@ModifyArg(
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/densityfunction/DensityFunctionCompiler;<init>(Lnet/minecraft/world/level/levelgen/densityfunction/DensityFunction$CompileContext;)V"
		),
		method = "<init>",
		require = 1
	)
	private DensityFunction.CompileContext wrapCompileContext(DensityFunction.CompileContext context) {
		return new RTFCompileContext(context, (RandomState) (Object) this);
	}

	public void reterraforged$RTFRandomState$initialize(ServerLevel level) {
		RegistryAccess registries = level.registryAccess();

		if(!level.dimension().equals(Level.OVERWORLD)) {
			this.clearGeneratorContext();
			return;
		}

		// 只在主世界应用RTF地形
		RegistryLookup<Preset> presets = registries.lookupOrThrow(RTFRegistries.PRESET);
		RegistryLookup<Noise> noises = registries.lookupOrThrow(RTFRegistries.NOISE);

		if(TBCompat.isEnabled()) {
			registries.lookupOrThrow(Registries.DENSITY_FUNCTION).get(TBNoiseRouterData.UNIQUENESS).ifPresent((uniqueness) -> {
				this.tbUniqueness = uniqueness.value();
			});
		}

		presets.get(Preset.KEY).ifPresent((presetHolder) -> {
			this.preset = presetHolder.value();

			if(this.usesCellSamplers(registries)) {
				PerformanceConfig config = PerformanceConfig.read(PerformanceConfig.DEFAULT_FILE_PATH)
					.resultOrPartial(RTFCommon.LOGGER::error)
					.orElseGet(PerformanceConfig::makeDefault);
				this.generatorContext = GeneratorContext.makeCached(this.preset, noises, (int) this.seed, config.tileSize(), config.batchCount(), ThreadPools.availableProcessors() > 4);
				// where the world spawn is searched from (MixinNoiseBasedChunkGenerator)
				this.spawnSearchCenter = this.preset.world().properties.spawnType.getSearchCenter(this.generatorContext);
				// the climate takes rivers and lakes for land: without this the spawn could be in a lake too wide for
				// the spawn chunk search (a player in the water)
				GeneratorContext spawnContext = this.generatorContext;
				this.spawnWater = (x, z) -> {
					Cell cell = new Cell();
					spawnContext.generator.getHeightmap().apply(cell, x, z, false);
					return cell.height <= spawnContext.levels.water;
				};
			}
		});
	}

	@Inject(
		at = @At("RETURN"),
		method = "createClimateSampler"
	)
	private void createClimateSampler(SamplerContext context, CallbackInfoReturnable<Climate.Sampler> callback) {
		if(this.tbUniqueness != null && this.generatorContext != null && (Object) callback.getReturnValue() instanceof TBClimateSampler tbClimateSampler) {
			tbClimateSampler.setUniqueness(((RandomState) (Object) this).samplersWithContext(context).get(this.tbUniqueness));
		}
	}

	// the generator context is only made when the noise router (or the functions tagged as additional router
	// functions) samples the preset's cells
	private boolean usesCellSamplers(RegistryAccess registries) {
		Set<DensityFunction> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		boolean[] found = new boolean[1];
		DfRewriteRule finder = new DfRewriteRule() {

			@Override
			public DensityFunction rewrite(DensityFunction function) {
				if(function instanceof CellSampler.Marker) {
					found[0] = true;
				} else if(!found[0] && visited.add(function)) {
					function.rewriteChildren(this);
				}
				return function;
			}
		};
		NoiseRouter router = this.router;
		for(DensityFunction function : new DensityFunction[] { router.temperature(), router.vegetation(), router.continents(), router.erosion(), router.depth(), router.ridges(), router.chunkSurfaceLevel(), router.finalDensity() }) {
			finder.rewrite(function);
		}
		registries.lookupOrThrow(Registries.DENSITY_FUNCTION).get(RTFDensityFunctionTags.ADDITIONAL_NOISE_ROUTER_FUNCTIONS).ifPresent((set) -> {
			set.forEach((function) -> finder.rewrite(function.value()));
		});
		return found[0];
	}

	@Nullable
	public Preset reterraforged$RTFRandomState$preset() {
		return this.preset;
	}

	@Nullable
	public GeneratorContext reterraforged$RTFRandomState$generatorContext() {
		return this.generatorContext;
	}

	@Nullable
	public BlockPos reterraforged$RTFRandomState$spawnSearchCenter() {
		return this.spawnSearchCenter;
	}

	@Nullable
	public BiPredicate<Integer, Integer> reterraforged$RTFRandomState$spawnWater() {
		return this.spawnWater;
	}

	public Noise reterraforged$RTFRandomState$seed(Noise noise) {
		return Noises.shiftSeed(noise, (int) this.seed);
	}

	private void clearGeneratorContext() {
		this.generatorContext = null;
		this.preset = null;
		this.spawnSearchCenter = null;
		this.spawnWater = null;
		this.tbUniqueness = null;
	}
}
