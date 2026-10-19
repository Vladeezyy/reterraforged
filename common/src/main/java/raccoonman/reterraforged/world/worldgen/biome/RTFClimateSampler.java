package raccoonman.reterraforged.world.worldgen.biome;

import java.util.function.BiPredicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;

public interface RTFClimateSampler {
	void setSpawnSearchCenter(BlockPos center);
	
	BlockPos getSpawnSearchCenter();

	/** Where the world spawn must not be: the generator's water (the climate takes rivers and lakes for land). */
	void setSpawnWater(@Nullable BiPredicate<Integer, Integer> water);

	@Nullable
	BiPredicate<Integer, Integer> getSpawnWater();
}
