package raccoonman.reterraforged.mixin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.material.MaterialSystem;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.world.worldgen.surface.RTFSurfaceSystem;
import raccoonman.reterraforged.world.worldgen.surface.rule.StrataRule;

// 26.3: the surface system is MaterialSystem
@Mixin(MaterialSystem.class)
@Implements(@Interface(iface = RTFSurfaceSystem.class, prefix = RTFCommon.MOD_ID + "$RTFSurfaceSystem$"))
class MixinMaterialSystem {
	private static final Identifier GEOLOGY_RANDOM = RTFCommon.location("geology");
	private RandomState randomState;
	private Map<Identifier, List<List<StrataRule.Layer>>> strata;

	@Inject(
		at = @At("TAIL"),
		method = "<init>"
	)
    public void MaterialSystem(RandomState randomState, BlockState defaultBlock, int seaLevel, DensityFunction preliminarySurfaceFunction, PositionalRandomFactory noiseRandom, CallbackInfo callback) {
    	this.randomState = randomState;
    	this.strata = new ConcurrentHashMap<>();
	}

	@Inject(
		at = @At("HEAD"),
		method = "buildSurface"
	)
	private void buildSurfaceHead(RandomState randomState, BiomeManager biomeManager, WorldGenerationContext generationContext, ChunkAccess protoChunk, NoiseChunk noiseChunk, MaterialRule ruleSource, @Nullable Set<Holder<Biome>> possibleBiomes, CallbackInfo callback) {
		RTFSurfaceSystem.BUILDING_CHUNK.set(protoChunk);
	}

	@Inject(
		at = @At("RETURN"),
		method = "buildSurface"
	)
	private void buildSurfaceReturn(RandomState randomState, BiomeManager biomeManager, WorldGenerationContext generationContext, ChunkAccess protoChunk, NoiseChunk noiseChunk, MaterialRule ruleSource, @Nullable Set<Holder<Biome>> possibleBiomes, CallbackInfo callback) {
		RTFSurfaceSystem.BUILDING_CHUNK.remove();
	}

	public List<List<StrataRule.Layer>> reterraforged$RTFSurfaceSystem$getOrCreateStrata(Identifier name, Function<RandomSource, List<List<StrataRule.Layer>>> strata) {
		return this.strata.computeIfAbsent(name, (k) -> {
			PositionalRandomFactory factory = this.randomState.getOrCreateRandomFactory(GEOLOGY_RANDOM);
			return strata.apply(factory.fromHashOf(k));
		});
	}
}
