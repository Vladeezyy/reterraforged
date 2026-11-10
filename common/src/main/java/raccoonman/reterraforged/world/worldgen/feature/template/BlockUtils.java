package raccoonman.reterraforged.world.worldgen.feature.template;

import java.util.function.BiPredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.pathfinder.PathComputationType;

public class BlockUtils {

    public static boolean isSoil(LevelAccessor world, BlockPos pos) {
        // snow: the trees that may stand there say so in their placement (PresetPlacedFeatures), the ground of groves is snow
        BlockState state = world.getBlockState(pos);
        return TreeFeature.isDirt(state) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW);
    }

    public static boolean isVegetation(LevelAccessor world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.is(BlockTags.SAPLINGS) || state.is(BlockTags.FLOWERS) || state.is(Blocks.VINE);
    }

    public static boolean canTreeReplace(LevelAccessor world, BlockPos pos) {
        if (TreeFeature.isAirOrLeaves(world, pos) || isVegetation(world, pos)) {
            return true;
        }
        // grass and ferns too, as for vanilla's trees (not water, which is in the same tag): vanilla's taiga puts its large
        // ferns before its trees, and the mod's trees left a clearing wherever such a patch grew (#37)
        BlockState state = world.getBlockState(pos);
        return state.is(BlockTags.REPLACEABLE_BY_TREES) && state.getFluidState().isEmpty();
    }

    // a tree grown from a sapling in the world, as vanilla's (TreeFeature.validTreePos): saplings and flowers stay. Our
    // generation rules above took them, and a grown tree cleared the saplings and the flowers of a tree farm around it
    public static boolean canSaplingTreeReplace(LevelAccessor world, BlockPos pos) {
        if (TreeFeature.isAirOrLeaves(world, pos)) {
            return true;
        }
        BlockState state = world.getBlockState(pos);
        return state.is(BlockTags.REPLACEABLE_BY_TREES) && state.getFluidState().isEmpty();
    }

    public static boolean isSolid(BlockGetter reader, BlockPos pos) {
        BlockState state = reader.getBlockState(pos);
        return isSolid(state);
    }

    public static boolean isSolid(BlockState state) {
        return state.canOcclude() || !state.isPathfindable(PathComputationType.LAND);
    }

    public static boolean isSoilOrRock(LevelAccessor world, BlockPos pos) {
        BlockState block = world.getBlockState(pos);
        return TreeFeature.isDirt(block) || block.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    public static boolean isClearOverhead(LevelAccessor world, BlockPos pos, int height, BiPredicate<LevelAccessor, BlockPos> predicate) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        // world.getMaxHeight ?
        int max = Math.min(world.getMaxBuildHeight() - 1, pos.getY() + height);
        for (int y = pos.getY(); y < max; y++) {
            mutable.set(pos.getX(), y, pos.getZ());
            if (!predicate.test(world, mutable)) {
                return false;
            }
        }
        return true;
    }
}
