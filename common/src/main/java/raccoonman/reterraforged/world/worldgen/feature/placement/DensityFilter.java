package raccoonman.reterraforged.world.worldgen.feature.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

// on average density times every position that comes in: 0.5 one in two, 2.5 two of each and a third for every second
public final class DensityFilter extends PlacementModifier {
	public static final MapCodec<DensityFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.FLOAT.fieldOf("density").forGetter((f) -> f.density)
	).apply(instance, DensityFilter::new));

	private final float density;

	public DensityFilter(float density) {
		this.density = density;
	}

	@Override
	public Stream<BlockPos> getPositions(PlacementContext ctx, RandomSource random, BlockPos pos) {
		int count = (int) this.density;
		if (random.nextFloat() < this.density - count) {
			count++;
		}
		return count == 0 ? Stream.empty() : Stream.generate(() -> pos).limit(count);
	}

	@Override
	public PlacementModifierType<DensityFilter> type() {
		return RTFPlacementModifiers.DENSITY;
	}
}
