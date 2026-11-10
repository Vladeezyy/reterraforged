package raccoonman.reterraforged.world.worldgen.feature.chance;

import com.mojang.serialization.MapCodec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;

class ElevationChanceModifier extends RangeChanceModifier {
	public static final MapCodec<ElevationChanceModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.FLOAT.fieldOf("from").forGetter((o) -> o.from),
		Codec.FLOAT.fieldOf("to").forGetter((o) -> o.to),
		Codec.BOOL.fieldOf("exclusive").forGetter((o) -> o.exclusive)
	).apply(instance, ElevationChanceModifier::new));
	
	public ElevationChanceModifier(float from, float to, boolean exclusive) {
		super(from, to, exclusive);
	}

	@Override
	public MapCodec<ElevationChanceModifier> codec() {
		return CODEC;
	}

	@Override
	protected float getValue(GeneratorContext generatorContext, ChanceContext chanceCtx, FeaturePlaceContext<?> placeCtx) {
		return generatorContext.generator.getHeightmap().levels().elevation(cellAt(generatorContext, placeCtx.origin()).height);
	}
}
