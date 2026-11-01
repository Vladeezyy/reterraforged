package raccoonman.reterraforged.world.worldgen.util;

public class PosUtil {

	public static long packMix(int left, float right) {
		return ((long) Float.floatToRawIntBits(right) & 0xFFFFFFFFL) | ((long) left & 0xFFFFFFFFL) << 32;
	}

	public static long pack(int left, int right) {
		return ((long) right & 0xFFFFFFFFL) | ((long) left & 0xFFFFFFFFL) << 32;
	}

	public static int unpackLeft(long packed) {
        return (int)(packed >>> 32 & 0xFFFFFFFFL);
	}

	public static int unpackRight(long packed) {
        return (int)(packed & 0xFFFFFFFFL);
	}

	public static long packf(float left, float right) {
		return ((long) Float.floatToRawIntBits(right) & 0xFFFFFFFFL) | ((long) Float.floatToRawIntBits(left) & 0xFFFFFFFFL) << 32;
	}

	public static float unpackLeftf(long packed) {
		return Float.intBitsToFloat((int) (packed >>> 32 & 0xFFFFFFFFL));
	}

	public static float unpackRightf(long packed) {
		return Float.intBitsToFloat((int) (packed & 0xFFFFFFFFL));
	}

}
