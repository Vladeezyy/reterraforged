package raccoonman.reterraforged.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import raccoonman.reterraforged.world.worldgen.RTFBiomeSource;
import raccoonman.reterraforged.world.worldgen.biome.SwampHeights;

// 26.3: the biomes are asked through resolvers (createResolver, createResolverForChunk), no longer through
// getNoiseBiome(x, y, z, sampler); the resolvers they return are wrapped
@Mixin(MultiNoiseBiomeSource.class)
@Implements(@Interface(iface = RTFBiomeSource.class, prefix = "reterraforged$RTFBiomeSource$"))
abstract class MixinMultiNoiseBiomeSource {
	@Unique
	@Nullable
	private volatile SwampHeights swampHeights;

	@Shadow
	public abstract Holder<Biome> getNoiseBiome(Climate.TargetPoint target);

	public void reterraforged$RTFBiomeSource$setSwampHeights(@Nullable SwampHeights swampHeights) {
		this.swampHeights = swampHeights;
	}

	@ModifyReturnValue(method = "createResolver", at = @At("RETURN"))
	private BiomeResolver createResolver(BiomeResolver resolver, Climate.Sampler sampler) {
		return this.limitSwamps(resolver, sampler);
	}

	@ModifyReturnValue(method = "createResolverForChunk", at = @At("RETURN"))
	private BiomeResolver createResolverForChunk(BiomeResolver resolver, Climate.Sampler sampler, int minQuartX, int minQuartY, int minQuartZ, int maxQuartX, int maxQuartY, int maxQuartZ) {
		return this.limitSwamps(resolver, sampler);
	}

	// Highest Swamps: a swamp too high above the sea is picked again (SwampHeights) from the climate at that place with
	// the erosion offset (MixinClimateSampler); getNoiseBiome keeps TerraBlender's and the other mods' own changes
	@Unique
	private BiomeResolver limitSwamps(BiomeResolver resolver, Climate.Sampler sampler) {
		SwampHeights heights = this.swampHeights;
		if (heights == null) {
			return resolver;
		}
		return (x, y, z) -> heights.limit(x, z, resolver.getNoiseBiome(x, y, z), () -> this.getNoiseBiome(sampler.sample(x, y, z)));
	}
}
