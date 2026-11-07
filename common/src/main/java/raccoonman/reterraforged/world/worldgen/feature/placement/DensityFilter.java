package raccoonman.reterraforged.world.worldgen.feature.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

// on average density times every position that comes in: 0.5 one in two, 2.5 two of each and a third for every second
public record DensityFilter(float density) implements PlacementModifier {
	public static final MapCodec<DensityFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.FLOAT.fieldOf("density").forGetter(DensityFilter::density)
	).apply(instance, DensityFilter::new));

	@Override
	public void modify(PlacementContext ctx, RandomSource random, BlockPos pos, Consumer<BlockPos> output) {
		int count = (int) this.density;
		if (random.nextFloat() < this.density - count) {
			count++;
		}
		for (int i = 0; i < count; i++) {
			output.accept(pos);
		}
	}

	@Override
	public MapCodec<DensityFilter> codec() {
		return CODEC;
	}
}
