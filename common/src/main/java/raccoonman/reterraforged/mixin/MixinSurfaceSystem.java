package raccoonman.reterraforged.mixin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.world.worldgen.surface.RTFSurfaceSystem;
import raccoonman.reterraforged.world.worldgen.surface.rule.StrataRule;

@Mixin(SurfaceSystem.class)
@Implements(@Interface(iface = RTFSurfaceSystem.class, prefix = RTFCommon.MOD_ID + "$RTFSurfaceSystem$"))
class MixinSurfaceSystem {
	private static final ResourceLocation GEOLOGY_RANDOM = RTFCommon.location("geology");
	private RandomState randomState;
	private Map<ResourceLocation, List<List<StrataRule.Layer>>> strata;
	
	@Inject(
		at = @At("TAIL"),
		method = "<init>"
	)
    public void SurfaceSystem(RandomState randomState, BlockState blockState, int i, PositionalRandomFactory positionalRandomFactory, CallbackInfo callback) {
    	this.randomState = randomState;
    	this.strata = new ConcurrentHashMap<>();
	}
	
	public List<List<StrataRule.Layer>> reterraforged$RTFSurfaceSystem$getOrCreateStrata(ResourceLocation name, Function<RandomSource, List<List<StrataRule.Layer>>> strata) {
		return this.strata.computeIfAbsent(name, (k) -> {
			PositionalRandomFactory factory = this.randomState.getOrCreateRandomFactory(GEOLOGY_RANDOM);
			return strata.apply(factory.fromHashOf(k));
		});
	}

	// The biome buildSurface looks up for each column is the column's surface biome to other mods: Ecliptic Seasons
	// keeps it as the biome its rain and snow are decided by (#22). With legacy_random_source, which the presets have
	// (the surface, aquifers and ore veins of ReTerraForged 0.0.6), vanilla looks it up at y 0 instead of at the surface,
	// in ReTerraForged's terrain often a cave biome (dripstone caves under plains: no rain with Ecliptic Seasons). The
	// lookup gives the surface biome, and vanilla's own uses of it below (eroded badlands spires, frozen ocean
	// icebergs) still get the one at y 0, so the terrain stays the same.
	@WrapOperation(
		method = "buildSurface",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/BiomeManager;getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;")
	)
	private Holder<Biome> surfaceBiome(BiomeManager biomes, BlockPos pos, Operation<Holder<Biome>> original, @Local(argsOnly = true) boolean legacyRandomSource, @Local(argsOnly = true) ChunkAccess chunk, @Share("legacyBiome") LocalRef<Holder<Biome>> legacyBiome) {
		Holder<Biome> biome = original.call(biomes, pos);
		if (!legacyRandomSource) {
			legacyBiome.set(null);
			return biome;
		}
		legacyBiome.set(biome);
		// the y vanilla uses without legacy_random_source
		int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX() & 15, pos.getZ() & 15) + 1;
		return original.call(biomes, new BlockPos(pos.getX(), surface, pos.getZ()));
	}

	@WrapOperation(
		method = "buildSurface",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Holder;is(Lnet/minecraft/resources/ResourceKey;)Z")
	)
	private boolean legacyBiomeIs(Holder<Biome> biome, ResourceKey<Biome> key, Operation<Boolean> original, @Share("legacyBiome") LocalRef<Holder<Biome>> legacyBiome) {
		return original.call(legacyBiome.get() != null ? legacyBiome.get() : biome, key);
	}

	@WrapOperation(
		method = "buildSurface",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Holder;value()Ljava/lang/Object;")
	)
	private Object legacyBiomeValue(Holder<Biome> biome, Operation<Object> original, @Share("legacyBiome") LocalRef<Holder<Biome>> legacyBiome) {
		return original.call(legacyBiome.get() != null ? legacyBiome.get() : biome);
	}
}
