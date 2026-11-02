package raccoonman.reterraforged.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import raccoonman.reterraforged.world.worldgen.RTFBiomeSource;
import raccoonman.reterraforged.world.worldgen.biome.SwampHeights;

@Mixin(MultiNoiseBiomeSource.class)
@Implements(@Interface(iface = RTFBiomeSource.class, prefix = "reterraforged$RTFBiomeSource$"))
class MixinMultiNoiseBiomeSource {
	@Unique
	@Nullable
	private volatile SwampHeights swampHeights;

	public void reterraforged$RTFBiomeSource$setSwampHeights(@Nullable SwampHeights swampHeights) {
		this.swampHeights = swampHeights;
	}

	// Highest Swamps: a swamp too high above the sea is picked again (SwampHeights); the original keeps TerraBlender's
	// and the other mods' own changes to this method
	@WrapMethod(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;")
	private Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler, Operation<Holder<Biome>> original) {
		Holder<Biome> biome = original.call(x, y, z, sampler);
		SwampHeights heights = this.swampHeights;
		return heights == null ? biome : heights.limit(x, z, biome, () -> original.call(x, y, z, sampler));
	}
}
