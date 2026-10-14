package raccoonman.reterraforged.world.worldgen.feature.template.template;

import net.minecraft.core.BlockPos;

public record Dimensions(BlockPos min, BlockPos max) {

    public int getSizeY() {
        return this.max.getY() - this.min.getY();
    }
}