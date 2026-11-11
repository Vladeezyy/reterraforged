package raccoonman.reterraforged.world.worldgen.feature.template.placement;

import java.util.function.Function;

import com.mojang.serialization.Codec;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import raccoonman.reterraforged.registries.RTFBuiltInRegistries;
import raccoonman.reterraforged.world.worldgen.feature.template.template.Dimensions;
import raccoonman.reterraforged.world.worldgen.feature.template.template.TemplateContext;

public interface TemplatePlacement<T extends TemplateContext> {
    public static final Codec<TemplatePlacement<?>> CODEC = RTFBuiltInRegistries.TEMPLATE_PLACEMENT_TYPE.byNameCodec().dispatch(TemplatePlacement::codec, Function.identity());
    
    boolean canPlaceAt(LevelAccessor world, BlockPos pos, Dimensions dimensions);

    boolean canReplaceAt(LevelAccessor world, BlockPos pos);

    // whether the base pasted down from a template's lowest full blocks goes on through pos: in generation through
    // all that does not occlude, as it always did (worlds stay the same)
    default boolean canExtendBase(LevelAccessor world, BlockPos pos) {
        return !world.getBlockState(pos).canOcclude();
    }
    
    T createContext();
    
    MapCodec<? extends TemplatePlacement<T>> codec();
}
