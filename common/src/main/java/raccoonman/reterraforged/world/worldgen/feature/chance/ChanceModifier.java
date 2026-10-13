package raccoonman.reterraforged.world.worldgen.feature.chance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import raccoonman.reterraforged.registries.RTFBuiltInRegistries;
import raccoonman.reterraforged.world.worldgen.feature.util.PlaceContext;

public interface ChanceModifier {
	public static final Codec<ChanceModifier> CODEC = RTFBuiltInRegistries.CHANCE_MODIFIER_TYPE.byNameCodec().dispatch(ChanceModifier::codec, Function.identity());
	
	float getChance(ChanceContext chanceCtx, PlaceContext<?> placeCtx);
	
	MapCodec<? extends ChanceModifier> codec();
}
