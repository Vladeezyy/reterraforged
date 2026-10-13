package raccoonman.reterraforged.world.worldgen.feature.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

@Deprecated
class LegacyCountExtraModifier implements PlacementModifier {
	public static final MapCodec<LegacyCountExtraModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.INT.fieldOf("count").forGetter((p) -> p.count),
		Codec.FLOAT.fieldOf("extra_chance").forGetter((p) -> p.extraChance),
		Codec.INT.fieldOf("extra_count").forGetter((p) -> p.extraCount)
	).apply(instance, LegacyCountExtraModifier::new));

	private int count;
	private float extraChance;
	private int extraCount;
	
	public LegacyCountExtraModifier(int count, float extraChance, int extraCount) {
		this.count = count;
		this.extraChance = extraChance;
		this.extraCount = extraCount;
	}
	
	@Override
	public void modify(PlacementContext ctx, RandomSource random, BlockPos pos, Consumer<BlockPos> output) {
		this.getPositions(ctx, random, pos).forEach(output);
	}

	private Stream<BlockPos> getPositions(PlacementContext ctx, RandomSource random, BlockPos pos) {
	      int i = this.count + (random.nextFloat() < this.extraChance ? this.extraCount : 0);
	      return IntStream.range(0, i).mapToObj((o) -> {
	         return pos;
	      });
	}

	@Override
	public MapCodec<LegacyCountExtraModifier> codec() {
		return CODEC;
	}
}
