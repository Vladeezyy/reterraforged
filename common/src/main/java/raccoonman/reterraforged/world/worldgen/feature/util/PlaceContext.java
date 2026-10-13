package raccoonman.reterraforged.world.worldgen.feature.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

// what FeaturePlaceContext held before 26.3 (a feature is its own configuration since then)
public record PlaceContext<C>(C config, WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
}
