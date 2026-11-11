package raccoonman.reterraforged.world.worldgen.feature;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;

public class DiskFeature extends Feature<DiskConfiguration> {
    private static final Noise DOMAIN = Noises.simplex(1, 6, 3);

    public DiskFeature(Codec<DiskConfiguration> codec) {
		super(codec);
	}
    
	@Override
	public boolean place(FeaturePlaceContext<DiskConfiguration> ctx) {
		WorldGenLevel level = ctx.level();
		RandomSource random = ctx.random();
		BlockPos pos = ctx.origin();
		DiskConfiguration config = ctx.config();
		ChunkGenerator generator = ctx.chunkGenerator();
		
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
                        // within the world's bounds: the generator's depth is the world's height, not its top, and a
                        // block outside them is never the target (the bound on the depth stays, it is the same disk)
                        int minY = Math.max(pos.getY() - ySize, level.getMinY());
                        int maxY = Math.min(pos.getY() + ySize, level.getMaxY());
                        for(int y = minY; y <= maxY && y + 1 < generator.getGenDepth(); ++y) {
                            blockPos.set(x, y, z);
                            
                            if(config.target().test(level, blockPos)) {
                                level.setBlock(blockPos, config.stateProvider().getState(level, random, blockPos), 2);
                                ++i;
                            }
                        }
                    }
                }
            }
            return i > 0;
        }
	}
}
