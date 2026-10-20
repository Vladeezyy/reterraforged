package raccoonman.reterraforged.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import raccoonman.reterraforged.data.worldgen.preset.PresetPlacedFeatures;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.feature.ErodeFeature;

// The erosion decorator runs as the surface is built, not as a feature (see ErodeFeature.erodeChunk)
@Mixin(NoiseBasedChunkGenerator.class)
class MixinNoiseBasedChunkGenerator {

	@Inject(
		at = @At("TAIL"),
		method = "buildSurface(Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/chunk/ChunkAccess;)V"
	)
	private void reterraforged$erode(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk, CallbackInfo callback) {
		GeneratorContext generatorContext;
		if(!((Object) randomState instanceof RTFRandomState rtfRandomState) || (generatorContext = rtfRandomState.generatorContext()) == null) {
			return;
		}
		// the preset's erosion decorator: the placed feature only exists when it is on
		Optional<Holder.Reference<PlacedFeature>> erode = region.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).getHolder(PresetPlacedFeatures.ERODE);
		if(erode.isEmpty() || !(erode.get().value().feature().value().config() instanceof ErodeFeature.Config config)) {
			return;
		}
		NoiseBasedChunkGenerator generator = (NoiseBasedChunkGenerator) (Object) this;
		// the biomes the surface rules see (NoiseBasedChunkGenerator.buildSurface): the same as the chunk's
		BiomeManager biomes = region.getBiomeManager().withDifferentSource((x, y, z) -> generator.getBiomeSource().getNoiseBiome(x, y, z, randomState.sampler()));
		ErodeFeature.erodeChunk(config, generatorContext, generator, chunk, biomes::getBiome, null);
		chunk.setInhabitedTime(Math.max(chunk.getInhabitedTime(), ErodeFeature.ERODED));
	}
}
