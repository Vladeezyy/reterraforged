package raccoonman.reterraforged.data.worldgen.preset.settings;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Climate.ParameterPoint;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

public enum SpawnType implements StringRepresentable {
    CONTINENT_CENTER("CONTINENT_CENTER") {
        private static final Climate.Parameter FULL_RANGE = Climate.Parameter.span(-1.0F, 1.0F);
        private static final Climate.Parameter SURFACE_DEPTH = Climate.Parameter.point(0.0F);
        private static final Climate.Parameter INLAND_CONTINENTALNESS = Climate.Parameter.span(-0.11F, 0.55F);

		@Override
    	public BlockPos getSearchCenter(GeneratorContext ctx) {
    		long center = ctx.generator.getHeightmap().continent().getNearestCenter(0.0F, 0.0F);
    		return new BlockPos(PosUtil.unpackLeft(center), 0, PosUtil.unpackRight(center));
    	}
    	
		@Override
		public List<ParameterPoint> getParameterPoints() {
			return List.of(new Climate.ParameterPoint(FULL_RANGE, FULL_RANGE, Climate.Parameter.span(INLAND_CONTINENTALNESS, FULL_RANGE), FULL_RANGE, SURFACE_DEPTH, FULL_RANGE, 0L), new Climate.ParameterPoint(FULL_RANGE, FULL_RANGE, Climate.Parameter.span(INLAND_CONTINENTALNESS, FULL_RANGE), FULL_RANGE, SURFACE_DEPTH, FULL_RANGE, 0L));
		}
	}, 
    // the dry land nearest 0, 0. With no points the game skips the search and spawns at 0, 0, half of the worlds in the
    // ocean; any climate fits this point, and what counts is the generator's water (SpawnFinderFix)
    ISLANDS("ISLANDS") {
        private static final Climate.Parameter FULL_RANGE = Climate.Parameter.span(-1.0F, 1.0F);

		@Override
		public BlockPos getSearchCenter(GeneratorContext ctx) {
			return BlockPos.ZERO;
		}

		@Override
		public List<ParameterPoint> getParameterPoints() {
			return List.of(new Climate.ParameterPoint(FULL_RANGE, FULL_RANGE, FULL_RANGE, FULL_RANGE, Climate.Parameter.point(0.0F), FULL_RANGE, 0L));
		}
	},
    WORLD_ORIGIN("WORLD_ORIGIN") {

		@Override
		public BlockPos getSearchCenter(GeneratorContext ctx) {
			return BlockPos.ZERO;
		}

		@Override
		public List<ParameterPoint> getParameterPoints() {
			return ImmutableList.of();
		}
	};
	
	public static final Codec<SpawnType> CODEC = StringRepresentable.fromEnum(SpawnType::values);

	private final String name;
	
	private SpawnType(String name) {
		this.name = name;
	}
	
	@Override
	public String getSerializedName() {
		return this.name;
	}
	
	public abstract BlockPos getSearchCenter(GeneratorContext ctx);	
	public abstract List<ParameterPoint> getParameterPoints();
}