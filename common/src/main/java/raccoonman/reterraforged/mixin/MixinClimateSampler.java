package raccoonman.reterraforged.mixin;

import java.util.function.BiPredicate;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Climate;
import raccoonman.reterraforged.world.worldgen.biome.RTFClimateSampler;

@Mixin(Climate.Sampler.class)
@Implements(@Interface(iface = RTFClimateSampler.class, prefix = "reterraforged$RTFClimateSampler$"))
class MixinClimateSampler {
	private BlockPos spawnSearchCenter = BlockPos.ZERO;
	@Nullable
	private BiPredicate<Integer, Integer> spawnWater;
	
	public void reterraforged$RTFClimateSampler$setSpawnSearchCenter(BlockPos spawnSearchCenter) {
		this.spawnSearchCenter = spawnSearchCenter;
	}
	
	public BlockPos reterraforged$RTFClimateSampler$getSpawnSearchCenter() {
		return this.spawnSearchCenter;
	}

	public void reterraforged$RTFClimateSampler$setSpawnWater(@Nullable BiPredicate<Integer, Integer> spawnWater) {
		this.spawnWater = spawnWater;
	}

	@Nullable
	public BiPredicate<Integer, Integer> reterraforged$RTFClimateSampler$getSpawnWater() {
		return this.spawnWater;
	}
}
