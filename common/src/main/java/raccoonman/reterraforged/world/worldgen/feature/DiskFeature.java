package raccoonman.reterraforged.world.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;

// a disk with a noisy edge; the fields are vanilla's disk configuration (the radius is the mod's own)
public record DiskFeature(Holder<BlockStateProvider> stateProvider, BlockPredicate target, IntProvider radius, int halfHeight) implements Feature {
	public static final MapCodec<DiskFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		BlockStateProvider.CODEC.fieldOf("state_provider").forGetter(DiskFeature::stateProvider),
		BlockPredicate.CODEC.fieldOf("target").forGetter(DiskFeature::target),
		IntProviders.codec(0, 8).fieldOf("radius").forGetter(DiskFeature::radius),
		Codec.intRange(0, 4).fieldOf("half_height").forGetter(DiskFeature::halfHeight)
	).apply(instance, DiskFeature::new));
    private static final Noise DOMAIN = Noises.simplex(1, 6, 3);

	@Override
	public MapCodec<DiskFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos pos) {
        if (!level.getFluidState(pos).is(FluidTags.WATER)) {
            return false;
        } else {
            int cRadius = 6;
            int ySize = 5;

            int i = 0;
            int radius = 4 + random.nextInt(cRadius);
            float radius2 = (radius * radius)  * 0.65F;
            BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

            for(int x = pos.getX() - radius; x <= pos.getX() + radius; ++x) {
                for(int z = pos.getZ() - radius; z <= pos.getZ() + radius; ++z) {
                    int dx = x - pos.getX();
                    int dz = z - pos.getZ();
                    float rad2 = DOMAIN.compute(x, z, 0) * radius2;
                    if (dx * dx + dz * dz <= rad2) {
                        for(int y = pos.getY() - ySize; y <= pos.getY() + ySize && y + 1 < generator.getGenDepth(); ++y) {
                            blockPos.set(x, y, z);
                            
                            if(this.target.test(level, blockPos)) {
                                BlockState state = this.stateProvider.value().getOptionalState(level, random, blockPos);
                                if(state != null) {
                                    level.setBlock(blockPos, state, 2);
                                    ++i;
                                }
                            }
                        }
                    }
                }
            }
            return i > 0;
        }
	}
}
