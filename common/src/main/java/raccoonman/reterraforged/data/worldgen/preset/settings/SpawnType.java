package raccoonman.reterraforged.data.worldgen.preset.settings;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.SpawnTargetPoint;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

public enum SpawnType implements StringRepresentable {
    CONTINENT_CENTER("CONTINENT_CENTER") {
        private static final Climate.Parameter FULL_RANGE = Climate.Parameter.span(-1.0F, 1.0F);
        private static final Climate.Parameter INLAND_CONTINENTALNESS = Climate.Parameter.span(-0.11F, 0.55F);

		@Override
    	public BlockPos getSearchCenter(GeneratorContext ctx) {
    		long center = ctx.generator.getHeightmap().continent().getNearestCenter(0.0F, 0.0F);
    		return new BlockPos(PosUtil.unpackLeft(center), 0, PosUtil.unpackRight(center));
    	}
    	
		@Override
		public List<SpawnTargetPoint> getSpawnTarget(Holder<DensityFunction> temperature, Holder<DensityFunction> vegetation, Holder<DensityFunction> continents, Holder<DensityFunction> erosion, Holder<DensityFunction> weirdness) {
			// the depth (surface) was never part of the fitness: the spawn finder sampled at depth 0
			return List.of(new SpawnTargetPoint(Map.of(temperature, FULL_RANGE, vegetation, FULL_RANGE, continents, Climate.Parameter.span(INLAND_CONTINENTALNESS, FULL_RANGE), erosion, FULL_RANGE, weirdness, FULL_RANGE)));
		}
	}, 
    ISLANDS("ISLANDS") {

		@Override
		public BlockPos getSearchCenter(GeneratorContext ctx) {
			return BlockPos.ZERO;
		}

		@Override
		public List<SpawnTargetPoint> getSpawnTarget(Holder<DensityFunction> temperature, Holder<DensityFunction> vegetation, Holder<DensityFunction> continents, Holder<DensityFunction> erosion, Holder<DensityFunction> weirdness) {
			return ImmutableList.of();
		}
	},
    WORLD_ORIGIN("WORLD_ORIGIN") {

		@Override
		public BlockPos getSearchCenter(GeneratorContext ctx) {
			return BlockPos.ZERO;
		}

		@Override
		public List<SpawnTargetPoint> getSpawnTarget(Holder<DensityFunction> temperature, Holder<DensityFunction> vegetation, Holder<DensityFunction> continents, Holder<DensityFunction> erosion, Holder<DensityFunction> weirdness) {
			return ImmutableList.of();
		}
	};
	
	public static final Codec<SpawnType> CODEC = StringRepresentable.fromEnum(SpawnType::values);

	private String name;
	
	private SpawnType(String name) {
		this.name = name;
	}
	
	@Override
	public String getSerializedName() {
		return this.name;
	}
	
	public abstract BlockPos getSearchCenter(GeneratorContext ctx);
	
	public abstract List<SpawnTargetPoint> getSpawnTarget(Holder<DensityFunction> temperature, Holder<DensityFunction> vegetation, Holder<DensityFunction> continents, Holder<DensityFunction> erosion, Holder<DensityFunction> weirdness);
}