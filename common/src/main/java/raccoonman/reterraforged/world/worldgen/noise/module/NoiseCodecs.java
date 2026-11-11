package raccoonman.reterraforged.world.worldgen.noise.module;

import com.mojang.serialization.Codec;

/**
 * The counts a datapack may give the noises: a huge one made every sample loop for minutes or ran out of memory, a
 * negative one failed in the middle of the generation. Every count that worked before still reads the same. Not in
 * Noises: its static block registers the codecs that read these.
 */
final class NoiseCodecs {
	// the presets write up to 200 (PresetTerrainNoise's erosion and ridges), the editor up to 8
	static final int MAX_OCTAVES = 256;
	// from 0: below it the noises' bounds failed (an index or array size below 0)
	static final Codec<Integer> OCTAVES = Codec.intRange(0, MAX_OCTAVES);
	// the noises that took a count below 1 as one octave or none
	static final Codec<Integer> OCTAVES_ANY_LOW = Codec.intRange(Integer.MIN_VALUE, MAX_OCTAVES);
	// a terrace of 0 steps failed on its first sample, the array of a huge one ran out of memory
	static final Codec<Integer> TERRACE_STEPS = Codec.intRange(1, 1 << 16);

	private NoiseCodecs() {
	}
}
