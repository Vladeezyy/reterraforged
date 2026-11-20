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

import java.util.concurrent.ConcurrentHashMap;

import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.terrain.Terrain;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainCategory;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;

/**
 * -Drtf.erosionfilter=&lt;strength multiplier&gt; (0.5 = half the author's 0.22): the filter is added to the height of
 * Heightmap.applyTerrain (before the rivers, so the tiles, WorldLookup and the preview get it). Without the property
 * nothing changes. The gully directions come from central differences of the unfiltered height at +-STEP blocks (about
 * the gradient of a blurred height: the ripples of the raw height would scramble a per-block gradient). The strength
 * mask: the share of mountain or hills columns among the centre and the 4 gradient samples, times a ramp from 2 to 24
 * blocks above the water. Land is never taken below the water. More properties: rtf.erosionfilter.scale (160),
 * .step (28), .peak (140 blocks above the water, fade target +1; the valley is 6 blocks), .octaves (5).
 */
public final class ErosionFilterProto {
	public static final float STRENGTH = Float.parseFloat(System.getProperty("rtf.erosionfilter", "0"));
	public static final boolean ENABLED = STRENGTH > 0.0F;
	private static final double SCALE = Double.parseDouble(System.getProperty("rtf.erosionfilter.scale", "160"));
	private static final float STEP = Float.parseFloat(System.getProperty("rtf.erosionfilter.step", "28"));
	private static final double VALLEY = 6.0;
	private static final double PEAK = Double.parseDouble(System.getProperty("rtf.erosionfilter.peak", "140"));
	private static final int OCTAVES = Math.max(1, Math.min(Integer.getInteger("rtf.erosionfilter.octaves", 5), (int) Math.floor(1 + Math.log(SCALE / 8) / Math.log(2))));
	private static final double RAMP_LO = 2.0, RAMP_HI = 24.0;

	// author's defaults (ErosionProto.Params)
	private static final double GULLY_WEIGHT = 0.5, DETAIL = 1.5, RIDGE_ROUNDING = 0.1, CREASE_ROUNDING = 0.0, INPUT_ROUNDING = 0.1;
	private static final double OCTAVE_ROUNDING = 2.0, ONSET_INPUT = 1.25, ONSET_OCTAVE = 1.25, ASSUMED_SLOPE = 0.7, ASSUMED_SLOPE_WEIGHT = 1.0;
	private static final double CELL_SCALE = 0.7, NORMALIZATION = 0.5, LACUNARITY = 2.0, GAIN = 0.5;
	private static final int SEED = 1337;

	private static final ConcurrentHashMap<Terrain, Boolean> MASKED = new ConcurrentHashMap<>();
	private static final ThreadLocal<Work> WORK = ThreadLocal.withInitial(Work::new);

	private static final class Work {
		final Cell side = new Cell();
		final double[] out = new double[5];
		final double[] ph = new double[4];
	}

	private ErosionFilterProto() {
	}

	static {
		if (ENABLED) {
			raccoonman.reterraforged.RTFCommon.LOGGER.warn("[RTF] PROTOTYPE erosion filter on: strength x{} (author 0.22), scale {}, octaves {}, gradient step {}, fade 6..{} blocks", STRENGTH, SCALE, OCTAVES, STEP, PEAK);
		}
	}

	// mountains and hills (a mix of two terrains "a-b" counts when one part is hills)
	private static boolean masked(Terrain terrain) {
		if (terrain == null) return false;
		return MASKED.computeIfAbsent(terrain, (t) -> {
			if (t.isMountain() && !t.isVolcano()) return true;
			for (String part : t.getName().split("-")) {
				if (part.equals(TerrainType.HILLS.getName())) return true;
			}
			return false;
		});
	}

	private static boolean keptOut(Terrain terrain) {
		return terrain.isWetland() || terrain.isRiver() || terrain.isLake() || terrain.isVolcano() || terrain.getCategory() == TerrainCategory.BEACH || terrain.getCategory() == TerrainCategory.COAST || terrain == TerrainType.ISLAND_BEACH;
	}

	// cell: after Heightmap.applyTerrain at x, z
	static void apply(Heightmap heightmap, Cell cell, float x, float z) {
		Levels levels = heightmap.levels();
		float water = levels.water;
		int worldHeight = levels.worldHeight;
		double h = (cell.height - water) * worldHeight;
		if (h <= RAMP_LO || cell.terrain == null || keptOut(cell.terrain)) {
			return;
		}
		Work work = WORK.get();
		Cell side = work.side;
		int inMask = masked(cell.terrain) ? 1 : 0;
		heightmap.applyTerrainRaw(side.reset(), x + STEP, z);
		double hxp = side.height;
		inMask += masked(side.terrain) ? 1 : 0;
		heightmap.applyTerrainRaw(side.reset(), x - STEP, z);
		double hxm = side.height;
		inMask += masked(side.terrain) ? 1 : 0;
		heightmap.applyTerrainRaw(side.reset(), x, z + STEP);
		double hzp = side.height;
		inMask += masked(side.terrain) ? 1 : 0;
		heightmap.applyTerrainRaw(side.reset(), x, z - STEP);
		double hzm = side.height;
		inMask += masked(side.terrain) ? 1 : 0;
		if (inMask == 0) {
			return;
		}
		double mask = inMask / 5.0 * smooth(RAMP_LO, RAMP_HI, h);
		double gx = (hxp - hxm) * worldHeight / (2.0 * STEP);
		double gz = (hzp - hzm) * worldHeight / (2.0 * STEP);
		erode(x, z, h, gx, gz, 0.22 * STRENGTH * mask, work.out, work.ph);
		double v = work.out[0];
		// land stays land
		v = Math.max(v, Math.min(h, 1.0));
		cell.height = (float) (water + v / worldHeight);
	}

	// out = {height, dHdx, dHdz, ridgeMap, magnitude}; heights and coordinates in blocks
	private static void erode(double x, double z, double h, double dhdx, double dhdz, double strengthParam, double[] out, double[] ph) {
		double fadeTarget = clamp((h - VALLEY) / (PEAK - VALLEY) * 2.0 - 1.0, -1, 1);
		double strength = strengthParam * SCALE;
		double freq = 1.0 / (SCALE * CELL_SCALE);
		double sx = dhdx, sz = dhdz;
		double slopeLength = Math.max(Math.sqrt(sx * sx + sz * sz), 1e-10);
		double hh = h;
		double magnitude = 0;
		double roundingMult = 1;

		double roundingForInput = mix(CREASE_ROUNDING, RIDGE_ROUNDING, clamp01(fadeTarget + 0.5)) * INPUT_ROUNDING;
		double combiMask = easeOut(smoothStart(slopeLength * ONSET_INPUT, roundingForInput * ONSET_INPUT));

		double gx = mix(sx, sx / slopeLength * ASSUMED_SLOPE, ASSUMED_SLOPE_WEIGHT);
		double gz = mix(sz, sz / slopeLength * ASSUMED_SLOPE, ASSUMED_SLOPE_WEIGHT);

		for (int i = 0; i < OCTAVES; i++) {
			double gl = Math.sqrt(gx * gx + gz * gz);
			double nx = gl > 1e-10 ? gx / gl : 0, nz = gl > 1e-10 ? gz / gl : 0;
			phacelle(x * freq, z * freq, nx, nz, CELL_SCALE, 0.25, NORMALIZATION, SEED, ph);
			double pc = ph[0], ps = ph[1];
			double pdx = -ph[2] * freq, pdz = -ph[3] * freq;
			double sloping = Math.abs(ps);
			double sg = Math.signum(ps) * strength * GULLY_WEIGHT;
			gx += sg * pdx;
			gz += sg * pdz;
			double gh = pc, gdx = ps * pdx, gdz = ps * pdz;
			double fh = mix(fadeTarget, gh * GULLY_WEIGHT, combiMask);
			double fdx = gdx * GULLY_WEIGHT * combiMask;
			double fdz = gdz * GULLY_WEIGHT * combiMask;
			hh += fh * strength;
			sx += fdx * strength;
			sz += fdz * strength;
			magnitude += strength;
			fadeTarget = fh;
			double roundingForOctave = mix(CREASE_ROUNDING, RIDGE_ROUNDING, clamp01(pc + 0.5)) * roundingMult;
			double newMask = easeOut(smoothStart(sloping * ONSET_OCTAVE, roundingForOctave * ONSET_OCTAVE));
			combiMask = powInv(combiMask, DETAIL) * newMask;
			strength *= GAIN;
			freq *= LACUNARITY;
			roundingMult *= OCTAVE_ROUNDING;
		}
		// anchored height offset (0, 1): the mean height stays where it was
		double offset = -clamp((h - VALLEY) / (PEAK - VALLEY) * 2.0 - 1.0, -1, 1) * magnitude;
		out[0] = hh + offset;
		out[1] = sx;
		out[2] = sz;
		out[4] = magnitude;
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
	private static double smooth(double a, double b, double v) { double t = clamp01((v - a) / (b - a)); return t * t * (3 - 2 * t); }
}
