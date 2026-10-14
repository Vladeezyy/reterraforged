package raccoonman.reterraforged.world.worldgen.feature.template.decorator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * A bee nest on the trunk of a template tree, as Minecraft's BeehiveDecorator puts one on its trees: facing south, next
 * to a log, with air where it goes and in front of it, two or three bees inside. Minecraft's decorator only tries the
 * height just under the lowest leaves, which the mod's tree templates fill with leaves around the trunk, so it found
 * no place and the mod's trees never had nests; this one tries that height first, then the trunk from the bottom up.
 */
record BeeNestDecorator(float probability) implements TemplateDecorator<TreeContext> {
	public static final MapCodec<BeeNestDecorator> CODEC = Codec.floatRange(0.0F, 1.0F).fieldOf("probability").xmap(BeeNestDecorator::new, BeeNestDecorator::probability);
	private static final Direction FACING = Direction.SOUTH;
	private static final Direction[] SIDES = { Direction.EAST, Direction.SOUTH, Direction.WEST };

	@Override
	public void apply(LevelAccessor level, TreeContext buffer, RandomSource random, boolean modified) {
		if (buffer.logs().isEmpty() || random.nextFloat() >= this.probability) {
			return;
		}
		List<BlockPos> logs = new ArrayList<>(buffer.logs());
		logs.sort(Comparator.comparingInt(BlockPos::getY));
		int bottom = logs.get(0).getY() + 1;
		int top = logs.get(logs.size() - 1).getY();
		int preferred = buffer.leaves().isEmpty() ? Math.min(bottom + random.nextInt(3), top) : Math.max(buffer.leaves().stream().mapToInt(BlockPos::getY).min().getAsInt() - 1, bottom);
		if (this.place(level, logs, preferred, random)) {
			return;
		}
		for (int y = bottom; y <= top; y++) {
			if (y != preferred && this.place(level, logs, y, random)) {
				return;
			}
		}
	}

	private boolean place(LevelAccessor level, List<BlockPos> logs, int y, RandomSource random) {
		List<BlockPos> spots = new ArrayList<>();
		for (BlockPos log : logs) {
			if (log.getY() == y) {
				for (Direction side : SIDES) {
					spots.add(log.relative(side));
				}
			}
		}
		Util.shuffle(spots, random);
		for (BlockPos spot : spots) {
			if (level.isEmptyBlock(spot) && level.isEmptyBlock(spot.relative(FACING))) {
				level.setBlock(spot, Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.FACING, FACING), 19);
				level.getBlockEntity(spot, BlockEntityType.BEEHIVE).ifPresent((nest) -> {
					int bees = 2 + random.nextInt(2);
					for (int i = 0; i < bees; i++) {
						nest.storeBee(BeehiveBlockEntity.Occupant.create(random.nextInt(599)));
					}
				});
				return true;
			}
		}
		return false;
	}

	@Override
	public MapCodec<BeeNestDecorator> codec() {
		return CODEC;
	}
}
