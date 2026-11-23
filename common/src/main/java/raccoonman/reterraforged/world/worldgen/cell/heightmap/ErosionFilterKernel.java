/*
 * PROTOTYPE, branch proto/erosion-filter only: for in-game comparison screenshots, not for a release.
 *
 * The erosion filter of Rune Skovbo Johansen, "Fast and Gorgeous Erosion Filter" (2026)
 *   https://blog.runevision.com/2026/03/fast-and-gorgeous-erosion-filter.html
 *   Phacelle Noise: https://blog.runevision.com/2026/01/phacelle-cheap-directional-noise.html
 *   Original shader: https://www.shadertoy.com/view/wXcfWn
 *   Advanced Terrain Erosion Filter and Phacelle Noise copyright (c) 2025 Rune Skovbo Johansen.
 * Transcribed from the C# Burst port by Luke Mitchell (https://github.com/lpmitchell/AdvancedTerrainErosion,
 * AdvancedTerrainErosion.cs, MPL-2.0 parts) via tools/erosionfilter/ErosionProto.java of this branch.
 *
 * This file is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not distributed
 * with this file, you can obtain one at https://mozilla.org/MPL/2.0/.
 */
package raccoonman.reterraforged.world.worldgen.cell.heightmap;

import java.util.Locale;
import java.util.function.Function;

/**
 * The filter alone, plain Java (no game classes): ErosionFilterProto uses it in the generator, the offline tuner
 * tools/erosionfilter/RidgeTune.java compiles this same file. Heights and coordinates in blocks above the water.
 * Every setting is read through a lookup of "rtf.erosionfilter.&lt;name&gt;" (System::getProperty in the game).
 */
public final class ErosionFilterKernel {
	public final double strength;     // the author's strength (0.22 at x1); depth of octave 1 = strength * scale * gullyWeight
	public final double scale;        // period of the widest gullies in blocks
	public final int octaves;
	public final double gullyWeight, detail, ridgeRounding, creaseRounding, inputRounding, octaveRounding;
	public final double onsetInput, onsetOctave, assumedSlope, assumedSlopeWeight, cellScale, normalization, lacunarity, gain;
	public final double valley, peak; // fade target -1 at valley, +1 at peak (blocks above the water)
	public final double raise;        // the part of a rise that is kept (1 = as the author; 0.3: ridges barely above the input)
	public final double maxRaise;     // the most a column is raised, blocks
	public final double deepen;       // a lowering is multiplied by this
	public final double anchor;       // 0: the author's offset (ridges of the upper slopes sink by (1 - gullyWeight) of the magnitude); 1: they stay at the input height, only the gullies are cut
	public final double topLo, topHi, topCut; // the strength falls by topCut from topLo to topHi blocks: summits stay whole (the gullies of a summit run in all directions and cut it into horns)
	public final int seed;

	public ErosionFilterKernel(double multiplier, Function<String, String> lookup) {
		this.strength = 0.22 * multiplier;
		this.scale = num(lookup, "scale", 160);
		int maxOctaves = (int) Math.floor(1 + Math.log(this.scale / 8) / Math.log(2));
		this.octaves = Math.max(1, Math.min((int) num(lookup, "octaves", 5), maxOctaves));
		this.gullyWeight = num(lookup, "gullyWeight", 0.5);
		this.detail = num(lookup, "detail", 1.5);
		this.ridgeRounding = num(lookup, "ridgeRounding", 0.1);
		this.creaseRounding = num(lookup, "creaseRounding", 0.0);
		this.inputRounding = num(lookup, "inputRounding", 0.1);
		this.octaveRounding = num(lookup, "octaveRounding", 2.0);
		this.onsetInput = num(lookup, "onsetInput", 1.25);
		this.onsetOctave = num(lookup, "onsetOctave", 1.25);
		this.assumedSlope = num(lookup, "assumedSlope", 0.7);
		this.assumedSlopeWeight = num(lookup, "assumedSlopeWeight", 1.0);
		this.cellScale = num(lookup, "cellScale", 0.7);
		this.normalization = num(lookup, "normalization", 0.5);
		this.lacunarity = num(lookup, "lacunarity", 2.0);
		this.gain = num(lookup, "gain", 0.5);
		this.valley = num(lookup, "valley", 6);
		this.peak = num(lookup, "peak", 140);
		this.raise = num(lookup, "raise", 1.0);
		this.maxRaise = num(lookup, "maxRaise", 1000);
		this.deepen = num(lookup, "deepen", 1.0);
		this.anchor = num(lookup, "anchor", 0.0);
		this.topLo = num(lookup, "topLo", 1000);
		this.topHi = num(lookup, "topHi", 1100);
		this.topCut = num(lookup, "topCut", 0);
		this.seed = (int) num(lookup, "seed", 1337);
	}

	private static double num(Function<String, String> lookup, String name, double def) {
		String v = lookup.apply("rtf.erosionfilter." + name);
		return v == null || v.isBlank() ? def : Double.parseDouble(v.trim());
	}

	public String describe() {
		return String.format(Locale.ROOT, "strength %.3f, scale %.0f, octaves %d, gullyWeight %.2f, rounding ridge %.2f crease %.2f input %.2f octave %.2f, detail %.2f, fade %.0f..%.0f, raise %.2f max %.0f, deepen %.2f, anchor %.2f, top cut %.2f over %.0f..%.0f",
			this.strength, this.scale, this.octaves, this.gullyWeight, this.ridgeRounding, this.creaseRounding, this.inputRounding, this.octaveRounding, this.detail, this.valley, this.peak, this.raise, this.maxRaise, this.deepen, this.anchor, this.topCut, this.topLo, this.topHi);
	}

	/** the filtered height at x, z of the height h with the slope (dhdx, dhdz), strength scaled by mask (0..1) */
	public double apply(double x, double z, double h, double dhdx, double dhdz, double mask, double[] ph) {
		if (this.topCut > 0) mask *= 1 - this.topCut * smooth(this.topLo, this.topHi, h);
		double v = this.erode(x, z, h, dhdx, dhdz, this.strength * mask, ph);
		double d = v - h;
		d = d > 0 ? Math.min(d * this.raise, this.maxRaise) : d * this.deepen;
		v = h + d;
		// land stays land
		if (h > 0) v = Math.max(v, Math.min(h, 1.0));
		return v;
	}

	private double erode(double x, double z, double h, double dhdx, double dhdz, double strengthParam, double[] ph) {
		double fadeTarget = clamp((h - this.valley) / (this.peak - this.valley) * 2.0 - 1.0, -1, 1);
		double startFade = fadeTarget;
		double strength = strengthParam * this.scale;
		double freq = 1.0 / (this.scale * this.cellScale);
		double sx = dhdx, sz = dhdz;
		double slopeLength = Math.max(Math.sqrt(sx * sx + sz * sz), 1e-10);
		double hh = h;
		double magnitude = 0;
		double roundingMult = 1;

		double roundingForInput = mix(this.creaseRounding, this.ridgeRounding, clamp01(fadeTarget + 0.5)) * this.inputRounding;
		double combiMask = easeOut(smoothStart(slopeLength * this.onsetInput, roundingForInput * this.onsetInput));

		double gx = mix(sx, sx / slopeLength * this.assumedSlope, this.assumedSlopeWeight);
		double gz = mix(sz, sz / slopeLength * this.assumedSlope, this.assumedSlopeWeight);

		for (int i = 0; i < this.octaves; i++) {
			double gl = Math.sqrt(gx * gx + gz * gz);
			double nx = gl > 1e-10 ? gx / gl : 0, nz = gl > 1e-10 ? gz / gl : 0;
			phacelle(x * freq, z * freq, nx, nz, this.cellScale, 0.25, this.normalization, this.seed, ph);
			double pc = ph[0], ps = ph[1];
			double pdx = -ph[2] * freq, pdz = -ph[3] * freq;
			double sloping = Math.abs(ps);
			double sg = Math.signum(ps) * strength * this.gullyWeight;
			gx += sg * pdx;
			gz += sg * pdz;
			double gh = pc;
			double fh = mix(fadeTarget, gh * this.gullyWeight, combiMask);
			hh += fh * strength;
			magnitude += strength;
			fadeTarget = fh;
			double roundingForOctave = mix(this.creaseRounding, this.ridgeRounding, clamp01(pc + 0.5)) * roundingMult;
			double newMask = easeOut(smoothStart(sloping * this.onsetOctave, roundingForOctave * this.onsetOctave));
			combiMask = powInv(combiMask, this.detail) * newMask;
			strength *= this.gain;
			freq *= this.lacunarity;
			roundingMult *= this.octaveRounding;
		}
		// anchored height offset (0, 1): the mean height stays where it was
		return hh - startFade * magnitude * (1 - this.anchor * this.gullyWeight);
	}

	// Phacelle with the cells of zero weight skipped and a sine table (error about 0.012 blocks)
	private static void phacelle(double px, double pz, double nx, double nz, double cellFreq, double offset, double normalization, int seed, double[] out) {
		double sdx = -nz * cellFreq * TAU, sdz = nx * cellFreq * TAU;
		offset *= TAU;
		double fx0 = Math.floor(px), fz0 = Math.floor(pz);
		int ix = (int) fx0, iz = (int) fz0;
		double fx = px - fx0, fz = pz - fz0;
		double cs = 0, sn = 0, ws = 0;
		for (int i = -1; i <= 2; i++) {
			for (int j = -1; j <= 2; j++) {
				int hs = hash(ix + i, iz + j, seed);
				double vx = fx - i - ((hs & 0xFFFF) * (1.0 / 65535.0) - 0.5);
				double vz = fz - j - ((hs >>> 16) * (1.0 / 65535.0) - 0.5);
				double d2 = vx * vx + vz * vz;
				if (d2 >= CUT2) continue;
				double w = Math.exp(-2 * d2) - 0.01111;
				ws += w;
				int k = (int) ((vx * sdx + vz * sdz + offset) * TAB_K);
				cs += SIN[(k + TAB_QUARTER) & TAB_MASK] * w;
				sn += SIN[k & TAB_MASK] * w;
			}
		}
		double cx = cs / ws, cy = sn / ws;
		double m = Math.max(1 - normalization, Math.sqrt(cx * cx + cy * cy));
		out[0] = cx / m;
		out[1] = cy / m;
		out[2] = sdx;
		out[3] = sdz;
	}

	private static final double TAU = Math.PI * 2;
	private static final double CUT2 = -Math.log(0.01111) / 2.0;
	private static final int TAB_BITS = 16, TAB_SIZE = 1 << TAB_BITS, TAB_MASK = TAB_SIZE - 1, TAB_QUARTER = TAB_SIZE / 4;
	private static final double TAB_K = TAB_SIZE / TAU;
	private static final float[] SIN = new float[TAB_SIZE];
	static {
		for (int i = 0; i < TAB_SIZE; i++) SIN[i] = (float) Math.sin(i * TAU / TAB_SIZE);
	}

	private static int hash(int x, int z, int seed) {
		int h = seed * 0x9E3779B9 ^ x * 0x27D4EB2D ^ z * 0x165667B1;
		h ^= h >>> 15;
		h *= 0x85EBCA6B;
		h ^= h >>> 13;
		h *= 0xC2B2AE35;
		h ^= h >>> 16;
		return h;
	}

	private static double clamp(double v, double a, double b) { return v < a ? a : (v > b ? b : v); }
	private static double clamp01(double v) { return clamp(v, 0, 1); }
	private static double mix(double a, double b, double t) { return a + (b - a) * t; }
	private static double easeOut(double t) { double v = 1 - clamp01(t); return 1 - v * v; }
	private static double smoothStart(double t, double s) { return t >= s ? t - 0.5 * s : 0.5 * t * t / s; }
	private static double powInv(double t, double p) { return 1 - Math.pow(1 - clamp01(t), p); }
	public static double smooth(double a, double b, double v) { double t = clamp01((v - a) / (b - a)); return t * t * (3 - 2 * t); }
}
