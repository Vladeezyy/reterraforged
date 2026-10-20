package raccoonman.reterraforged.mixin;

import java.util.List;
import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SpawnTargetPoint;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.cell.biome.spawn.SpawnFinderFix;
import raccoonman.reterraforged.world.worldgen.feature.ErodeFeature;

@Mixin(NoiseBasedChunkGenerator.class)
class MixinNoiseBasedChunkGenerator {
	@Shadow
	@Final
	private Holder<NoiseGeneratorSettings> settings;

	// the world spawn is searched around the preset's spawn search center (vanilla: around 0, 0)
	@Inject(at = @At("HEAD"), method = "getOrigin", cancellable = true)
	private void getOrigin(RandomState randomState, CallbackInfoReturnable<ChunkPos> callback) {
		List<SpawnTargetPoint> spawnTarget = this.settings.value().spawnTarget();
		if((Object) randomState instanceof RTFRandomState rtfRandomState && !spawnTarget.isEmpty()) {
			BlockPos center = rtfRandomState.spawnSearchCenter();
			if(center != null) {
				SamplerContext samplerContext = SamplerContext.builder().enableCaches().build();
				callback.setReturnValue(ChunkPos.containing(new SpawnFinderFix(spawnTarget, randomState.samplersWithContext(samplerContext), center, rtfRandomState.spawnWater()).result.location()));
			}
		}
	}

	// The erosion decorator runs as the surface is built (in 26.3 inside buildTerrain, before the carvers), not as a
	// feature (see ErodeFeature.erodeChunk)
	@Inject(at = @At("TAIL"), method = "buildSurface")
	private void reterraforged$erode(ChunkAccess chunk, NoiseChunk noiseChunk, RandomState randomState, BiomeManager biomeManager, Set<Holder<Biome>> biomes, MaterialRule rule, CallbackInfo callback) {
		GeneratorContext generatorContext;
		Preset preset;
		if(!((Object) randomState instanceof RTFRandomState rtfRandomState) || (generatorContext = rtfRandomState.generatorContext()) == null || (preset = rtfRandomState.preset()) == null) {
			return;
		}
		// the preset's erosion decorator (no registry access here: the same settings PresetConfiguredFeatures registers)
		if(!preset.miscellaneous().erosionDecorator) {
			return;
		}
		ErodeFeature.erodeChunk(ErodeFeature.Config.of(preset), generatorContext, (NoiseBasedChunkGenerator) (Object) this, chunk, biomeManager::getBiome, null);
		chunk.setInhabitedTime(Math.max(chunk.getInhabitedTime(), ErodeFeature.ERODED));
	}
}
