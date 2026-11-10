package raccoonman.reterraforged.world.worldgen.feature.chance;

import com.mojang.serialization.MapCodec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import raccoonman.reterraforged.world.worldgen.feature.util.PlaceContext;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;

class BiomeEdgeChanceModifier extends RangeChanceModifier {
	public static final MapCodec<BiomeEdgeChanceModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.FLOAT.fieldOf("from").forGetter((o) -> o.from),
		Codec.FLOAT.fieldOf("to").forGetter((o) -> o.to),
		Codec.BOOL.fieldOf("exclusive").forGetter((o) -> o.exclusive)
	).apply(instance, BiomeEdgeChanceModifier::new));
	
	public BiomeEdgeChanceModifier(float from, float to, boolean exclusive) {
		super(from, to, exclusive);
	}

	@Override
	public MapCodec<BiomeEdgeChanceModifier> codec() {
		return CODEC;
	}

	@Override
	protected float getValue(GeneratorContext generatorContext, ChanceContext chanceCtx, PlaceContext<?> placeCtx) {
		return cellAt(generatorContext, placeCtx.origin()).biomeRegionEdge;
	}
}
