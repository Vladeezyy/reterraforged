package raccoonman.reterraforged.world.worldgen.noise.domain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import raccoonman.reterraforged.world.worldgen.noise.module.Noise.Visitor;

public record CompoundWarp(Domain input1, Domain input2) implements Domain {
	public static final MapCodec<CompoundWarp> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Domain.CODEC.fieldOf("input1").forGetter(CompoundWarp::input1),
		Domain.CODEC.fieldOf("input2").forGetter(CompoundWarp::input2)
	).apply(instance, CompoundWarp::new));
	
	@Override
	public float getOffsetX(float x, float z, int seed) {
        float ax = this.input1.getX(x, z, seed);
        float ay = this.input1.getZ(x, z, seed);
        return this.input2.getOffsetX(ax, ay, seed);
	}

	@Override
	public float getOffsetZ(float x, float z, int seed) {
        float ax = this.input1.getX(x, z, seed);
        float ay = this.input1.getZ(x, z, seed);
        return this.input2.getOffsetZ(ax, ay, seed);
	}

	// the inner warp once for both (it was worked out twice, and twice again for every level of nesting)
	@Override
	public long getOffsets(float x, float z, int seed) {
		long warped = this.input1.warp(x, z, seed);
		return this.input2.getOffsets(Domain.offsetX(warped), Domain.offsetZ(warped), seed);
	}

	@Override
	public Domain mapAll(Visitor visitor) {
		return new CompoundWarp(this.input1.mapAll(visitor), this.input2.mapAll(visitor));
	}

	@Override
	public MapCodec<CompoundWarp> codec() {
		return CODEC;
	}
}
