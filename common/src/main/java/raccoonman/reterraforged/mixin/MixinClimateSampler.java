package raccoonman.reterraforged.mixin;

import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import raccoonman.reterraforged.world.worldgen.biome.SwampHeights;

import java.util.function.BiPredicate;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Climate;
import raccoonman.reterraforged.world.worldgen.biome.RTFClimateSampler;

@Mixin(Climate.Sampler.class)
@Implements(@Interface(iface = RTFClimateSampler.class, prefix = "reterraforged$RTFClimateSampler$"))
class MixinClimateSampler {
	@Unique
	private BlockPos spawnSearchCenter = BlockPos.ZERO;
	@Unique
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

	// Highest Swamps: the erosion offset while a swamp is picked again (SwampHeights), the point as it is otherwise
	@ModifyReturnValue(method = "sample", at = @At("RETURN"))
	private Climate.TargetPoint sample(Climate.TargetPoint point) {
		return SwampHeights.offset(point);
	}
}
