package raccoonman.reterraforged.mixin;

import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.densityfunction.C2MECompat;
import raccoonman.reterraforged.world.worldgen.densityfunction.CellSampler;
import raccoonman.reterraforged.world.worldgen.densityfunction.ClampToNearestUnit;
import raccoonman.reterraforged.world.worldgen.densityfunction.ColumnCache;
import raccoonman.reterraforged.world.worldgen.densityfunction.ConditionalFlatCache;
import raccoonman.reterraforged.world.worldgen.densityfunction.LinearSplineFunction;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;

@Mixin(NoiseChunk.class)
class MixinNoiseChunk {
	@Unique
	private RandomState randomState;
	@Unique
	private int chunkX, chunkZ;
	@Unique
	@Nullable
	private Tile.Chunk chunk;
	// made with the first cell sampler: a noise chunk of a world without ReTerraForged has none
	@Unique
	@Nullable
	private CellSampler.Cache2d cache2d;
	@Unique
	private static final boolean C2ME_PRESENT = isC2MEPresent();

	@Unique
	private static boolean isC2MEPresent() {
		try {
			Class.forName("com.ishland.c2me.base.common.GlobalExecutors");
			return true;
		} catch(ClassNotFoundException e) {
			return false;
		}
	}

	@Shadow
    @Final
	int firstNoiseX;
	@Shadow
    @Final
    int firstNoiseZ;
	@Shadow
    @Final
	private int cellCountXZ;
	
	// a wrap, not a @Redirect: another mod's wrap or redirect of the same call still applies
	@WrapOperation(
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/RandomState;router()Lnet/minecraft/world/level/levelgen/NoiseRouter;"
		),
		method = "<init>",
		require = 1
	)
	private NoiseRouter NoiseChunk(RandomState randomState1, Operation<NoiseRouter> original, int cellCountXZ, RandomState randomState2, int minBlockX, int minBlockZ, NoiseSettings noiseSettings, DensityFunctions.BeardifierOrMarker beardifierOrMarker, NoiseGeneratorSettings noiseGeneratorSettings, Aquifer.FluidPicker fluidPicker, Blender blender) {
		this.randomState = randomState1;
		this.chunkX = SectionPos.blockToSectionCoord(minBlockX);
		this.chunkZ = SectionPos.blockToSectionCoord(minBlockZ);
		GeneratorContext generatorContext;
		// Base-height and base-column queries use a one-cell NoiseChunk: as in ReTerraForged 0.0.6 they read the cells
		// of those places directly (CellSampler.Cache2d), not a whole tile for every place asked about (/locate,
		// explorer maps and dolphins ask about thousands).
		if((Object) randomState instanceof RTFRandomState rtfRandomState && (generatorContext = rtfRandomState.generatorContext()) != null
			&& cellCountXZ > 1) {
			if(C2ME_PRESENT) {
				generatorContext.cache.queueAtChunk(this.chunkX, this.chunkZ);
			}
			this.chunk = generatorContext.cache.provideAtChunk(this.chunkX, this.chunkZ).getChunkReader(this.chunkX, this.chunkZ);
		}
		return original.call(randomState1);
	}

	@ModifyVariable(
		method = "<init>",
		at = @At("HEAD"),
		name = "fluidPicker",
		index = 7,
		ordinal = 0,
		argsOnly = true
	)
	private static Aquifer.FluidPicker modifyFluidPicker(Aquifer.FluidPicker fluidPicker, int i, RandomState randomState, int j, int k, NoiseSettings noiseSettings, DensityFunctions.BeardifierOrMarker beardifierOrMarker, NoiseGeneratorSettings noiseGeneratorSettings) {
		if((Object) randomState instanceof RTFRandomState rtfRandomState) {
			@Nullable
			Preset preset = rtfRandomState.preset();
			if(preset != null && rtfRandomState.generatorContext() != null) {
				WorldSettings.Properties props = preset.world().properties;
				if(noiseGeneratorSettings.seaLevel() != props.seaLevel) {
					return fluidPicker;
				}
				// the noise settings' height is the whole world, depth + height (PresetNoiseGeneratorSettings): compared
				// with the height alone it never matched, the lava level stayed vanilla's -54 whatever the slider (#13)
				if(noiseSettings.minY() != -props.worldDepth || noiseSettings.height() != props.worldDepth + props.worldHeight) {
					return fluidPicker;
				}
				int lavaLevel = props.lavaLevel;
		        Aquifer.FluidStatus lava = new Aquifer.FluidStatus(lavaLevel, Blocks.LAVA.defaultBlockState());
		        int seaLevel = noiseGeneratorSettings.seaLevel();
		        Aquifer.FluidStatus defaultFluid = new Aquifer.FluidStatus(seaLevel, noiseGeneratorSettings.defaultFluid());
		        return (x, y, z) -> {
		            if (y < Math.min(lavaLevel, seaLevel)) {
		                return lava;
		            }
		            return defaultFluid;
		        };
			}
		}
		return fluidPicker;
	}

	@Inject(
		at = @At("HEAD"),
		method = "wrapNew",
		cancellable = true
	)
	private void wrapNew(DensityFunction function, CallbackInfoReturnable<DensityFunction> callback) {
		if((Object) this.randomState instanceof RTFRandomState randomState) {
			if(function instanceof CellSampler mapped) {
				callback.setReturnValue(this.wrapCellSampler(mapped, randomState));
				return;
			}
			// the rounded height of the offset reads the tile's cell of the column (children are wrapped first): the same
			// for every y. Not with C2ME, which wraps and compiles the functions its own way
			if(!C2ME_PRESENT && this.chunk != null && function instanceof ClampToNearestUnit clamp
				&& clamp.function() instanceof CellSampler.CacheChunk cacheChunk && cacheChunk.readsTileOf(this.chunk, this.chunkX, this.chunkZ)) {
				callback.setReturnValue(new ColumnCache(clamp, this.chunkX, this.chunkZ));
				return;
			}
			if(C2ME_PRESENT) {
				DensityFunction repaired = C2MECompat.repairShallowMapping(function, (mapped) -> {
					return this.wrapCellSampler(mapped, randomState);
				});
				if(repaired != function) {
					callback.setReturnValue(repaired);
				}
			}
		}
	}

	@Unique
	private DensityFunction wrapCellSampler(CellSampler sampler, RTFRandomState randomState) {
		if(randomState.generatorContext() != null) {
			// one for all the samplers of this chunk, as before
			if(this.cache2d == null) {
				this.cache2d = new CellSampler.Cache2d();
			}
			return sampler.new CacheChunk(this.chunk, this.cache2d, this.chunkX, this.chunkZ);
		}
		return DensityFunctions.zero();
	}
}
