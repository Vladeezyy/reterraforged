package raccoonman.reterraforged.world.worldgen.noise.domain;

import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import raccoonman.reterraforged.registries.RTFBuiltInRegistries;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

public interface Domain {
    public static final Codec<Domain> CODEC = RTFBuiltInRegistries.DOMAIN_TYPE.byNameCodec().dispatch(Domain::codec, Function.identity());
	
    float getOffsetX(float x, float z, int seed);
    
    float getOffsetZ(float x, float z, int seed);
    
    Domain mapAll(Noise.Visitor visitor);
    
    MapCodec<? extends Domain> codec();

    /**
     * Both offsets at once, packed (offsetX / offsetZ read them): a warp whose X and Z share work (the same distance,
     * angle or inner warp) does it once instead of twice. The same values as getOffsetX and getOffsetZ.
     */
    default long getOffsets(float x, float z, int seed) {
        return pack(this.getOffsetX(x, z, seed), this.getOffsetZ(x, z, seed));
    }

    /** The warped place, packed as getOffsets: x + its offset X, z + its offset Z */
    default long warp(float x, float z, int seed) {
        long offsets = this.getOffsets(x, z, seed);
        return pack(x + offsetX(offsets), z + offsetZ(offsets));
    }

    static long pack(float x, float z) {
        return ((long) Float.floatToRawIntBits(x) << 32) | (Float.floatToRawIntBits(z) & 0xFFFFFFFFL);
    }

    static float offsetX(long packed) {
        return Float.intBitsToFloat((int) (packed >>> 32));
    }

    static float offsetZ(long packed) {
        return Float.intBitsToFloat((int) packed);
    }

    default float getX(float x, float z, int seed) {
        return x + this.getOffsetX(x, z, seed);
    }
    
    default float getZ(float x, float z, int seed) {
        return z + this.getOffsetZ(x, z, seed);
    }
}
