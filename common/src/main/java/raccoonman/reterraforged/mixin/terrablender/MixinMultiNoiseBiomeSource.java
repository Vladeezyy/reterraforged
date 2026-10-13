package raccoonman.reterraforged.mixin.terrablender;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import raccoonman.reterraforged.world.worldgen.terrablender.TBClimateSampler;
import raccoonman.reterraforged.world.worldgen.terrablender.TBCompat;

// 26.3 fills a chunk's biomes from climate volumes: the points are made by Climate.target, not Climate.Sampler.sample
// (MixinClimateSampler), so they had no preset biome region and TerraBlender chose the regions in chunks by its own
// noise, other than /locate, structures and the spawn (which sample point by point): with Biomes O' Plenty 45 % of
// the places had another biome in the chunk than the biome source said. The chunk's resolver now gives each lookup
// the region of its sampler (MixinParameterList reads it when the point has none).
@Mixin(MultiNoiseBiomeSource.class)
class MixinMultiNoiseBiomeSource {

	@Inject(
		at = @At("RETURN"),
		method = "createResolverForChunk",
		cancellable = true
	)
	private void createResolverForChunk(Climate.Sampler sampler, int minQuartX, int minQuartY, int minQuartZ, int sizeX, int sizeY, int sizeZ, CallbackInfoReturnable<BiomeResolver> callback) {
		if(!((Object) sampler instanceof TBClimateSampler tbClimateSampler) || tbClimateSampler.getUniqueness() == null) {
			return;
		}
		DensitySampler.Bound uniqueness = tbClimateSampler.getUniqueness();
		BiomeResolver resolver = callback.getReturnValue();
		callback.setReturnValue((quartX, quartY, quartZ) -> {
			TBCompat.CHUNK_UNIQUENESS.set(uniqueness.sampleValue(QuartPos.toBlock(quartX), QuartPos.toBlock(quartY), QuartPos.toBlock(quartZ)));
			try {
				return resolver.getNoiseBiome(quartX, quartY, quartZ);
			} finally {
				TBCompat.CHUNK_UNIQUENESS.remove();
			}
		});
	}
}
