/*
 * ErosionProto: standalone CPU prototype of the "Advanced Terrain Erosion Filter"
 * (fade mode, stacked fading, normalized gullies, straight gullies, gully weight, rounding)
 * for timing and pictures only. Not for the mod.
 *
 * Algorithm: Rune Skovbo Johansen, "Fast and Gorgeous Erosion Filter" (2026)
 *   https://blog.runevision.com/2026/03/fast-and-gorgeous-erosion-filter.html
 *   Phacelle Noise: https://blog.runevision.com/2026/01/phacelle-cheap-directional-noise.html
 *   Original shader: https://www.shadertoy.com/view/wXcfWn
 *   Advanced Terrain Erosion Filter and Phacelle Noise copyright (c) 2025 Rune Skovbo Johansen.
 * Transcribed from the C# Burst port by Luke Mitchell
 *   https://github.com/lpmitchell/AdvancedTerrainErosion (AdvancedTerrainErosion.cs, MPL-2.0 parts)
 * and cross-checked with the Java port in oovaa8/catto (ErosionFilter.java, MPL-2.0)
 * and ItsVerday/Hytale_Worldgen_Additions (ErosionDensity.java).
 *
 * The ErosionFilter / PhacelleNoise parts of this file are subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file, you can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Differences from the shader:
 *  - coordinates and heights are in blocks (the filter is scale invariant: gully depth = strength*scale,
 *    gully slope ~ 2*pi*strength*gullyWeight, both independent of units if h and x share units);
 *  - integer cell hash instead of the float fract() hash (the float hash degrades far from the origin);
 *  - optional exact early-out for cells with zero weight (d^2 >= 2.24996), optional sine table, float variant.
 *
 * Usage:  java ErosionProto bench | images | check
 */

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

public class ErosionProto {

    // ------------------------------------------------------------------------------------------------
    // Parameters (defaults = author's Shadertoy defaults, as in lpmitchell README; scale in blocks)
    // ------------------------------------------------------------------------------------------------
    static final class Params {
        double scale = 200;           // period of the widest gullies in blocks (shader: 0.15 of a 0.33-wide hill)
        double strength = 0.22;       // erosion strength; depth of octave 1 = strength*scale*gullyWeight
        double gullyWeight = 0.5;     // 0 = sharp peaks, no gullies; 1 = full gullies, rounded peaks
        double detail = 1.5;          // how strongly smaller octaves are allowed on gentle slopes
        double ridgeRounding = 0.1;   // rounding.x
        double creaseRounding = 0.0;  // rounding.y
        double inputRounding = 0.1;   // rounding.z, multiplier for the input terrain
        double octaveRounding = 2.0;  // rounding.w, multiplier per octave (= lacunarity -> same look)
        double onsetInput = 1.25;     // onset.x
        double onsetOctave = 1.25;    // onset.y
        double ridgeOnsetInput = 2.8; // onset.z (ridge map only)
        double ridgeOnsetOctave = 1.5;// onset.w (ridge map only)
        double assumedSlope = 0.7;    // assumedSlope.x
        double assumedSlopeWeight = 1.0; // assumedSlope.y
        double cellScale = 0.7;
        double normalization = 0.5;   // k = 1/(1-0.5) = 2
        int octaves = 5;
        double lacunarity = 2.0;
        double gain = 0.5;
        // fade target from altitude: -1 at valleyAlt, +1 at peakAlt (article: inverse_lerp*2-1)
        double valleyAlt = 70, peakAlt = 260;
        // shader TerrainHeightOffset (x, y): offset = lerp(x, -fadeTarget, y) * magnitude
        double offsetX = -0.65, offsetY = 0.0;
        int seed = 1337;

        Params copy() {
            Params p = new Params();
            p.scale = scale; p.strength = strength; p.gullyWeight = gullyWeight; p.detail = detail;
            p.ridgeRounding = ridgeRounding; p.creaseRounding = creaseRounding; p.inputRounding = inputRounding;
            p.octaveRounding = octaveRounding; p.onsetInput = onsetInput; p.onsetOctave = onsetOctave;
            p.ridgeOnsetInput = ridgeOnsetInput; p.ridgeOnsetOctave = ridgeOnsetOctave; p.assumedSlope = assumedSlope;
            p.assumedSlopeWeight = assumedSlopeWeight; p.cellScale = cellScale; p.normalization = normalization;
            p.octaves = octaves; p.lacunarity = lacunarity; p.gain = gain; p.valleyAlt = valleyAlt; p.peakAlt = peakAlt;
            p.offsetX = offsetX; p.offsetY = offsetY; p.seed = seed;
            return p;
        }
    }

    // Phacelle variants
    static final int V_REF = 0;        // faithful: 16 cells, Math.cos/sin/exp, max(0, w)
    static final int V_SKIP = 1;       // exact early-out for zero-weight cells
    static final int V_TAB = 2;        // 16 cells, sine table
    static final int V_SKIP_TAB = 3;   // early-out + sine table
    static final int V_FAST = 6;       // early-out + sine table + polynomial bell (1-d2/CUT2)^4 instead of exp (NOT exact)

    // weight = exp(-2 d^2) - 0.01111 <= 0  <=>  d^2 >= -ln(0.01111)/2
    static final double CUT2 = -Math.log(0.01111) / 2.0; // 2.24996...
    static final float CUT2F = (float) CUT2;
    static final double TAU = Math.PI * 2;

    // counters for the cost model
    static long cntCells, cntActive, cntCalls;
    static boolean counting = false;

    // ------------------------------------------------------------------------------------------------
    // Erosion filter. out = {height, dHdx, dHdz, ridgeMap, magnitude}
    // ------------------------------------------------------------------------------------------------
    static void erode(double x, double z, double h, double dhdx, double dhdz, Params P, int variant, double[] out, double[] ph) {
        double fadeTarget = clamp((h - P.valleyAlt) / (P.peakAlt - P.valleyAlt) * 2.0 - 1.0, -1, 1);

        double strength = P.strength * P.scale;
        double freq = 1.0 / (P.scale * P.cellScale);
        double sx = dhdx, sz = dhdz;
        double slopeLength = Math.max(Math.sqrt(sx * sx + sz * sz), 1e-10);
        double hh = h;
        double magnitude = 0;
        double roundingMult = 1;

        double roundingForInput = mix(P.creaseRounding, P.ridgeRounding, clamp01(fadeTarget + 0.5)) * P.inputRounding;
        double combiMask = easeOut(smoothStart(slopeLength * P.onsetInput, roundingForInput * P.onsetInput));
        double ridgeMask = easeOut(slopeLength * P.ridgeOnsetInput);
        double ridgeFade = fadeTarget;

        double gx = mix(sx, sx / slopeLength * P.assumedSlope, P.assumedSlopeWeight);
        double gz = mix(sz, sz / slopeLength * P.assumedSlope, P.assumedSlopeWeight);

        for (int i = 0; i < P.octaves; i++) {
            double gl = Math.sqrt(gx * gx + gz * gz);
            double nx = gl > 1e-10 ? gx / gl : 0, nz = gl > 1e-10 ? gz / gl : 0; // normalizesafe
            switch (variant) {
                case V_REF -> phacelleRef(x * freq, z * freq, nx, nz, P.cellScale, 0.25, P.normalization, P.seed, ph);
                case V_SKIP -> phacelleSkip(x * freq, z * freq, nx, nz, P.cellScale, 0.25, P.normalization, P.seed, ph);
                case V_TAB -> phacelleTab(x * freq, z * freq, nx, nz, P.cellScale, 0.25, P.normalization, P.seed, ph);
                case V_FAST -> phacelleFast(x * freq, z * freq, nx, nz, P.cellScale, 0.25, P.normalization, P.seed, ph);
                default -> phacelleSkipTab(x * freq, z * freq, nx, nz, P.cellScale, 0.25, P.normalization, P.seed, ph);
            }
            double pc = ph[0], ps = ph[1];
            double pdx = -ph[2] * freq, pdz = -ph[3] * freq;   // slope directions point down
            double sloping = Math.abs(ps);
            // straight gullies: sign(sine) instead of the sine for the direction of the next octave
            double sg = Math.signum(ps) * strength * P.gullyWeight;
            gx += sg * pdx;
            gz += sg * pdz;
            // gullies: height cos, derivative sin*dir
            double gh = pc, gdx = ps * pdx, gdz = ps * pdz;
            // fade towards fadeTarget by combiMask (fade mode)
            double fh = mix(fadeTarget, gh * P.gullyWeight, combiMask);
            double fdx = gdx * P.gullyWeight * combiMask;
            double fdz = gdz * P.gullyWeight * combiMask;
            hh += fh * strength;
            sx += fdx * strength;
            sz += fdz * strength;
            magnitude += strength;
            // stacked fading
            fadeTarget = fh;
            double roundingForOctave = mix(P.creaseRounding, P.ridgeRounding, clamp01(pc + 0.5)) * roundingMult;
            double newMask = easeOut(smoothStart(sloping * P.onsetOctave, roundingForOctave * P.onsetOctave));
            combiMask = powInv(combiMask, P.detail) * newMask;
            // ridge map
            ridgeFade = mix(ridgeFade, gh, ridgeMask);
            ridgeMask *= easeOut(sloping * P.ridgeOnsetOctave);

            strength *= P.gain;
            freq *= P.lacunarity;
            roundingMult *= P.octaveRounding;
        }
        double offset = mix(P.offsetX, -clamp(fadeTargetOf(h, P), -1, 1), P.offsetY) * magnitude;
        out[0] = hh + offset;
        out[1] = sx;
        out[2] = sz;
        out[3] = ridgeFade * (1 - ridgeMask);
        out[4] = magnitude;
    }

    static double fadeTargetOf(double h, Params P) {
        return (h - P.valleyAlt) / (P.peakAlt - P.valleyAlt) * 2.0 - 1.0;
    }

    // ---------------- Phacelle (double) ----------------
    static void phacelleRef(double px, double pz, double nx, double nz, double cellFreq, double offset,
                            double normalization, int seed, double[] out) {
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
                double w = Math.max(0, Math.exp(-2 * d2) - 0.01111);
                ws += w;
                double a = vx * sdx + vz * sdz + offset;
                cs += Math.cos(a) * w;
                sn += Math.sin(a) * w;
            }
        }
        finish(cs, sn, ws, normalization, sdx, sdz, out);
    }

    static void phacelleSkip(double px, double pz, double nx, double nz, double cellFreq, double offset,
                             double normalization, int seed, double[] out) {
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
                if (counting) { cntCells++; if (d2 < CUT2) cntActive++; }
                if (d2 >= CUT2) continue;
                double w = Math.exp(-2 * d2) - 0.01111;
                ws += w;
                double a = vx * sdx + vz * sdz + offset;
                cs += Math.cos(a) * w;
                sn += Math.sin(a) * w;
            }
        }
        if (counting) cntCalls++;
        finish(cs, sn, ws, normalization, sdx, sdz, out);
    }

    static void phacelleTab(double px, double pz, double nx, double nz, double cellFreq, double offset,
                            double normalization, int seed, double[] out) {
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
                double w = Math.max(0, Math.exp(-2 * d2) - 0.01111);
                ws += w;
                int k = (int) ((vx * sdx + vz * sdz + offset) * TAB_K);
                cs += SIN[(k + TAB_QUARTER) & TAB_MASK] * w;
                sn += SIN[k & TAB_MASK] * w;
            }
        }
        finish(cs, sn, ws, normalization, sdx, sdz, out);
    }

    static void phacelleSkipTab(double px, double pz, double nx, double nz, double cellFreq, double offset,
                                double normalization, int seed, double[] out) {
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
        finish(cs, sn, ws, normalization, sdx, sdz, out);
    }

    static void phacelleFast(double px, double pz, double nx, double nz, double cellFreq, double offset,
                             double normalization, int seed, double[] out) {
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
                double t = 1 - d2 * (1.0 / CUT2);
                t *= t;
                double w = t * t;
                ws += w;
                int k = (int) ((vx * sdx + vz * sdz + offset) * TAB_K);
                cs += SIN[(k + TAB_QUARTER) & TAB_MASK] * w;
                sn += SIN[k & TAB_MASK] * w;
            }
        }
        finish(cs, sn, ws, normalization, sdx, sdz, out);
    }

    static void finish(double cs, double sn, double ws, double normalization, double sdx, double sdz, double[] out) {
        double ix = cs / ws, iy = sn / ws;
        double m = Math.max(1 - normalization, Math.sqrt(ix * ix + iy * iy));
        out[0] = ix / m;
        out[1] = iy / m;
        out[2] = sdx;
        out[3] = sdz;
    }

    // Sine table like net.minecraft.util.Mth (65536 entries, nearest)
    static final int TAB_BITS = 16, TAB_SIZE = 1 << TAB_BITS, TAB_MASK = TAB_SIZE - 1, TAB_QUARTER = TAB_SIZE / 4;
    static final double TAB_K = TAB_SIZE / TAU;
    static final float TAB_KF = (float) TAB_K;
    static final float[] SIN = new float[TAB_SIZE];
    static { for (int i = 0; i < TAB_SIZE; i++) SIN[i] = (float) Math.sin(i * TAU / TAB_SIZE); }

    static int hash(int x, int z, int seed) {
        int h = seed * 0x9E3779B9 ^ x * 0x27D4EB2D ^ z * 0x165667B1;
        h ^= h >>> 15; h *= 0x85EBCA6B;
        h ^= h >>> 13; h *= 0xC2B2AE35;
        h ^= h >>> 16;
        return h;
    }

    // ------------------------------------------------------------------------------------------------
    // Float variant (whole filter in float; cell index/fraction from double coordinates for precision)
    // ------------------------------------------------------------------------------------------------
    static void erodeF(double x, double z, float h, float dhdx, float dhdz, Params P, boolean table, float[] out, float[] ph) {
        float fadeTarget = clampf((float) fadeTargetOf(h, P), -1, 1);
        float gullyWeight = (float) P.gullyWeight, detail = (float) P.detail;
        float ridgeR = (float) P.ridgeRounding, creaseR = (float) P.creaseRounding;
        float onY = (float) P.onsetOctave, onW = (float) P.ridgeOnsetOctave;
        float gain = (float) P.gain, octR = (float) P.octaveRounding;
        float strength = (float) (P.strength * P.scale);
        double freq = 1.0 / (P.scale * P.cellScale);
        float sx = dhdx, sz = dhdz;
        float slopeLength = Math.max((float) Math.sqrt(sx * sx + sz * sz), 1e-10f);
        float hh = h, magnitude = 0, roundingMult = 1;
        float roundingForInput = mixf(creaseR, ridgeR, clamp01f(fadeTarget + 0.5f)) * (float) P.inputRounding;
        float combiMask = easeOutf(smoothStartf(slopeLength * (float) P.onsetInput, roundingForInput * (float) P.onsetInput));
        float ridgeMask = easeOutf(slopeLength * (float) P.ridgeOnsetInput);
        float ridgeFade = fadeTarget;
        float as = (float) P.assumedSlope, aw = (float) P.assumedSlopeWeight;
        float gx = mixf(sx, sx / slopeLength * as, aw), gz = mixf(sz, sz / slopeLength * as, aw);
        float cellF = (float) P.cellScale, norm = (float) P.normalization;
        for (int i = 0; i < P.octaves; i++) {
            float gl = (float) Math.sqrt(gx * gx + gz * gz);
            float nx = gl > 1e-10f ? gx / gl : 0, nz = gl > 1e-10f ? gz / gl : 0;
            phacelleF(x * freq, z * freq, nx, nz, cellF, norm, P.seed, table, ph);
            float pc = ph[0], ps = ph[1];
            float ff = (float) freq;
            float pdx = -ph[2] * ff, pdz = -ph[3] * ff;
            float sloping = Math.abs(ps);
            float sg = Math.signum(ps) * strength * gullyWeight;
            gx += sg * pdx; gz += sg * pdz;
            float fh = mixf(fadeTarget, pc * gullyWeight, combiMask);
            float k = gullyWeight * combiMask * strength * ps;
            hh += fh * strength;
            sx += k * pdx; sz += k * pdz;
            magnitude += strength;
            fadeTarget = fh;
            float rfo = mixf(creaseR, ridgeR, clamp01f(pc + 0.5f)) * roundingMult;
            float newMask = easeOutf(smoothStartf(sloping * onY, rfo * onY));
            combiMask = powInvf(combiMask, detail) * newMask;
            ridgeFade = mixf(ridgeFade, pc, ridgeMask);
            ridgeMask *= easeOutf(sloping * onW);
            strength *= gain;
            freq *= P.lacunarity;
            roundingMult *= octR;
        }
        float offset = mixf((float) P.offsetX, -clampf((float) fadeTargetOf(h, P), -1, 1), (float) P.offsetY) * magnitude;
        out[0] = hh + offset; out[1] = sx; out[2] = sz; out[3] = ridgeFade * (1 - ridgeMask); out[4] = magnitude;
    }

    static void phacelleF(double px, double pz, float nx, float nz, float cellFreq, float normalization, int seed, boolean table, float[] out) {
        final float tau = (float) TAU;
        float sdx = -nz * cellFreq * tau, sdz = nx * cellFreq * tau;
        float offset = 0.25f * tau;
        double fx0 = Math.floor(px), fz0 = Math.floor(pz);
        int ix = (int) fx0, iz = (int) fz0;
        float fx = (float) (px - fx0), fz = (float) (pz - fz0);
        float cs = 0, sn = 0, ws = 0;
        for (int i = -1; i <= 2; i++) {
            for (int j = -1; j <= 2; j++) {
                int hs = hash(ix + i, iz + j, seed);
                float vx = fx - i - ((hs & 0xFFFF) * (1f / 65535f) - 0.5f);
                float vz = fz - j - ((hs >>> 16) * (1f / 65535f) - 0.5f);
                float d2 = vx * vx + vz * vz;
                if (d2 >= CUT2F) continue;
                float w = (float) Math.exp(-2 * d2) - 0.01111f;
                ws += w;
                float a = vx * sdx + vz * sdz + offset;
                if (table) {
                    int k = (int) (a * TAB_KF);
                    cs += SIN[(k + TAB_QUARTER) & TAB_MASK] * w;
                    sn += SIN[k & TAB_MASK] * w;
                } else {
                    cs += (float) Math.cos(a) * w;
                    sn += (float) Math.sin(a) * w;
                }
            }
        }
        float ixv = cs / ws, iyv = sn / ws;
        float m = Math.max(1 - normalization, (float) Math.sqrt(ixv * ixv + iyv * iyv));
        out[0] = ixv / m; out[1] = iyv / m; out[2] = sdx; out[3] = sdz;
    }

    // ---------------- helpers ----------------
    static double clamp(double v, double a, double b) { return v < a ? a : (v > b ? b : v); }
    static double clamp01(double v) { return clamp(v, 0, 1); }
    static double mix(double a, double b, double t) { return a + (b - a) * t; }
    static double easeOut(double t) { double v = 1 - clamp01(t); return 1 - v * v; }
    static double smoothStart(double t, double s) { return t >= s ? t - 0.5 * s : 0.5 * t * t / s; }
    static double powInv(double t, double p) { return 1 - Math.pow(1 - clamp01(t), p); }
    static float clampf(float v, float a, float b) { return v < a ? a : (v > b ? b : v); }
    static float clamp01f(float v) { return clampf(v, 0, 1); }
    static float mixf(float a, float b, float t) { return a + (b - a) * t; }
    static float easeOutf(float t) { float v = 1 - clamp01f(t); return 1 - v * v; }
    static float smoothStartf(float t, float s) { return t >= s ? t - 0.5f * s : 0.5f * t * t / s; }
    static float powInvf(float t, float p) { return 1 - (float) Math.pow(1 - clamp01f(t), p); }

    // ------------------------------------------------------------------------------------------------
    // Synthetic terrain in blocks: plains around sea level + ridged-multifractal mountain ranges
    // ------------------------------------------------------------------------------------------------
    static final int TSEED = 4242;
    static final double SEA = 63;
    static double terrainMode = 0; // 0 normal, 1 gentle plains + coast only

    static double terrain(double x, double z) {
        double base = 66 + 14 * fbm(x / 700.0, z / 700.0, 3, TSEED + 1);   // ~50..80: sea, coast, plains
        if (terrainMode == 1) return base;
        double mask = smooth(-0.15, 0.35, noise(x / 2600.0, z / 2600.0, TSEED + 2));
        double r = ridged(x / 1400.0, z / 1400.0, terrainMode == 2 ? 6 : 3, TSEED + 3); // 0..~1; 3 oct = smooth input
        return base + mask * 200 * r;
    }

    static double ridged(double x, double z, int oct, int seed) {
        double sum = 0, amp = 1, weight = 1, norm = 0;
        for (int i = 0; i < oct; i++) {
            double n = noise(x, z, seed + i * 31);
            // mode 2: classic sharp |n| crests; otherwise softened crests (C1), which the filter needs
            double s = terrainMode == 2 ? 1 - Math.abs(n) : 1 - Math.sqrt(n * n + 0.015);
            s *= s;
            s *= weight;
            weight = clamp01(s * 2.0);
            sum += s * amp;
            norm += amp;
            amp *= 0.5;
            x = x * 2.03 + 17.1;
            z = z * 2.03 - 9.3;
        }
        return sum / norm * 1.15;
    }

    static double fbm(double x, double z, int oct, int seed) {
        double sum = 0, amp = 1, norm = 0;
        for (int i = 0; i < oct; i++) {
            sum += noise(x, z, seed + i * 17) * amp;
            norm += amp; amp *= 0.5; x *= 2.01; z *= 2.01;
        }
        return sum / norm;
    }

    static double smooth(double a, double b, double v) { double t = clamp01((v - a) / (b - a)); return t * t * (3 - 2 * t); }

    // 2D gradient noise, ~[-1,1]
    static double noise(double x, double z, int seed) {
        double x0 = Math.floor(x), z0 = Math.floor(z);
        int ix = (int) x0, iz = (int) z0;
        double fx = x - x0, fz = z - z0;
        double ux = fx * fx * fx * (fx * (fx * 6 - 15) + 10), uz = fz * fz * fz * (fz * (fz * 6 - 15) + 10);
        double a = grad(hash(ix, iz, seed), fx, fz), b = grad(hash(ix + 1, iz, seed), fx - 1, fz);
        double c = grad(hash(ix, iz + 1, seed), fx, fz - 1), d = grad(hash(ix + 1, iz + 1, seed), fx - 1, fz - 1);
        return 1.6 * mix(mix(a, b, ux), mix(c, d, ux), uz);
    }

    static double grad(int h, double x, double z) {
        double ang = (h & 0xFFFF) * (TAU / 65536.0);
        return Math.cos(ang) * x + Math.sin(ang) * z;
    }

    // ------------------------------------------------------------------------------------------------
    // Benchmark
    // ------------------------------------------------------------------------------------------------
    static volatile double sink;

    interface Kernel { double run(); }

    static double timeNs(Kernel k, int points, double warmSec, double measSec, int reps) {
        long end = System.nanoTime() + (long) (warmSec * 1e9);
        double s = 0;
        while (System.nanoTime() < end) s += k.run();
        double best = Double.MAX_VALUE;
        double[] all = new double[reps];
        for (int r = 0; r < reps; r++) {
            long t0 = System.nanoTime(), n = 0;
            long stop = t0 + (long) (measSec * 1e9);
            long t1;
            do { s += k.run(); n++; t1 = System.nanoTime(); } while (t1 < stop);
            all[r] = (t1 - t0) / (double) (n * points);
            best = Math.min(best, all[r]);
        }
        sink = s;
        Arrays.sort(all);
        return all[reps / 2]; // median
    }

    static void bench(String only) {
        final int N = 128 * 128;
        final double[] xs = new double[N], zs = new double[N], hs = new double[N], dx = new double[N], dz = new double[N];
        final float[] hsf = new float[N], dxf = new float[N], dzf = new float[N];
        // sample points scattered in a mountain area, block coordinates, 3 blocks apart
        for (int i = 0; i < N; i++) {
            double x = X0 + 400 + (i % 128) * 11.0 + 0.5, z = Z0 + 400 + (i / 128) * 11.0 + 0.5;
            xs[i] = x; zs[i] = z;
            double h = terrain(x, z);
            hs[i] = h;
            dx[i] = (terrain(x + 1, z) - h);
            dz[i] = (terrain(x, z + 1) - h);
            hsf[i] = (float) h; dxf[i] = (float) dx[i]; dzf[i] = (float) dz[i];
        }
        double meanSlope = 0;
        for (int i = 0; i < N; i++) meanSlope += Math.hypot(dx[i], dz[i]);
        System.out.printf("bench points: %d, mean input slope %.3f%n", N, meanSlope / N);

        final double W = 1.0, M = 1.0; final int R = 3;
        String[] names = {"ref double Math.sin/cos (16 cells)", "double + early-out", "double + sin table (16 cells)", "double + early-out + table",
                "float + early-out + Math.sin", "float + early-out + table", "double early-out+table+poly bell"};
        int[] octs = {3, 5, 8};
        System.out.printf("%-38s %10s %10s %10s%n", "variant", "3 oct", "5 oct", "8 oct");
        for (int v = 0; v < 7; v++) {
            if (!only.equals("all") && !only.equals(String.valueOf(v))) continue;
            StringBuilder sb = new StringBuilder(String.format("%-38s", names[v]));
            for (int o : octs) {
                final Params P = new Params();
                P.octaves = o;
                final int var = v;
                Kernel k;
                if (v < 4 || v == 6) {
                    k = () -> {
                        double[] out = new double[5], ph = new double[4];
                        double acc = 0;
                        for (int i = 0; i < N; i++) { erode(xs[i], zs[i], hs[i], dx[i], dz[i], P, var, out, ph); acc += out[0]; }
                        return acc;
                    };
                } else {
                    final boolean tab = v == 5;
                    k = () -> {
                        float[] out = new float[5], ph = new float[4];
                        double acc = 0;
                        for (int i = 0; i < N; i++) { erodeF(xs[i], zs[i], hsf[i], dxf[i], dzf[i], P, tab, out, ph); acc += out[0]; }
                        return acc;
                    };
                }
                sb.append(String.format(" %10.1f", timeNs(k, N, W, M, R)));
            }
            System.out.println(sb);
        }

        if (!only.equals("all") && !only.equals("fd")) return;
        // terrain cost and finite-difference overhead
        Kernel tOnly = () -> { double a = 0; for (int i = 0; i < N; i++) a += terrain(xs[i], zs[i]); return a; };
        double tNs = timeNs(tOnly, N, W, M, R);
        final Params P5 = new Params();
        Kernel fwd = () -> {
            double[] out = new double[5], ph = new double[4]; double a = 0;
            for (int i = 0; i < N; i++) {
                double x = xs[i], z = zs[i], h = terrain(x, z);
                double gx = terrain(x + 1, z) - h, gz = terrain(x, z + 1) - h;
                erode(x, z, h, gx, gz, P5, V_SKIP, out, ph); a += out[0];
            }
            return a;
        };
        Kernel cen = () -> {
            double[] out = new double[5], ph = new double[4]; double a = 0;
            for (int i = 0; i < N; i++) {
                double x = xs[i], z = zs[i], h = terrain(x, z);
                double gx = (terrain(x + 1, z) - terrain(x - 1, z)) * 0.5, gz = (terrain(x, z + 1) - terrain(x, z - 1)) * 0.5;
                erode(x, z, h, gx, gz, P5, V_SKIP, out, ph); a += out[0];
            }
            return a;
        };
        Kernel none = () -> {
            double[] out = new double[5], ph = new double[4]; double a = 0;
            for (int i = 0; i < N; i++) {
                double x = xs[i], z = zs[i], h = terrain(x, z);
                erode(x, z, h, dx[i], dz[i], P5, V_SKIP, out, ph); a += out[0];
            }
            return a;
        };
        System.out.printf("terrain() alone (3-oct ridged + 3-oct fbm + mask = 7 gradient-noise calls): %.1f ns%n", tNs);
        System.out.printf("terrain + erode(5 oct, early-out), gradient given:    %.1f ns%n", timeNs(none, N, W, M, R));
        System.out.printf("terrain + 2 fwd diff + erode(5 oct, early-out):       %.1f ns%n", timeNs(fwd, N, W, M, R));
        System.out.printf("terrain + 4 central diff + erode(5 oct, early-out):   %.1f ns%n", timeNs(cen, N, W, M, R));

        // raw primitive costs (dependent chain so they are not hoisted)
        Kernel mSin = () -> { double a = 0.1; for (int i = 0; i < N; i++) { a = Math.sin(a + i * 1e-3) + Math.cos(a) ; } return a; };
        Kernel tSin = () -> { double a = 0.1; for (int i = 0; i < N; i++) { int k = (int) ((a + i * 1e-3) * TAB_K); a = SIN[k & TAB_MASK] + SIN[(k + TAB_QUARTER) & TAB_MASK]; } return a; };
        Kernel mExp = () -> { double a = 0.1; for (int i = 0; i < N; i++) { a = Math.exp(-2 * (a * a + i * 1e-5)) ; } return a; };
        System.out.printf("primitive: Math.sin+Math.cos %.1f ns, table sin+cos %.1f ns, Math.exp %.1f ns (latency chain)%n",
                timeNs(mSin, N, 0.5, 0.5, R), timeNs(tSin, N, 0.5, 0.5, R), timeNs(mExp, N, 0.5, 0.5, R));
    }

    // accuracy and cost-model checks
    static void check() {
        Params P = new Params();
        double[] out = new double[5], o2 = new double[5], ph = new double[4];
        float[] of = new float[5], phf = new float[4];
        double[] maxd = new double[6];
        double maxF = 0, sumF = 0;
        counting = true;
        int n = 0;
        for (int i = 0; i < 200; i++) for (int j = 0; j < 200; j++) {
            double x = X0 + i * 9.3, z = Z0 + j * 9.3;
            double h = terrain(x, z), gx = terrain(x + 1, z) - h, gz = terrain(x, z + 1) - h;
            erode(x, z, h, gx, gz, P, V_REF, out, ph);
            for (int v = 1; v < 4; v++) { erode(x, z, h, gx, gz, P, v, o2, ph); maxd[v] = Math.max(maxd[v], Math.abs(o2[0] - out[0])); }
            erode(x, z, h, gx, gz, P, V_FAST, o2, ph); maxF = Math.max(maxF, Math.abs(o2[0] - out[0])); sumF += Math.abs(o2[0] - out[0]);
            erodeF(x, z, (float) h, (float) gx, (float) gz, P, false, of, phf); maxd[4] = Math.max(maxd[4], Math.abs(of[0] - out[0]));
            erodeF(x, z, (float) h, (float) gx, (float) gz, P, true, of, phf); maxd[5] = Math.max(maxd[5], Math.abs(of[0] - out[0]));
            n++;
        }
        counting = false;
        System.out.printf("max |height - ref| in blocks: early-out %.2e, table %.2e, early-out+table %.2e, float %.2e, float+table %.2e%n",
                maxd[1], maxd[2], maxd[3], maxd[4], maxd[5]);
        System.out.printf("poly bell instead of exp: max |dh| %.2f blocks, mean %.3f blocks%n", maxF, sumF / n);
        System.out.printf("cells with nonzero weight: %.2f of 16 per octave (%d phacelle calls)%n", 16.0 * cntActive / cntCells, cntCalls);
        // far from origin: float hash in the original would break; here int hash. Check float coordinate path
        double X = 29_000_000.5, Z = -29_000_000.25;
        erode(X, Z, 150, 0.4, 0.2, P, V_REF, out, ph);
        erodeF(X, Z, 150f, 0.4f, 0.2f, P, true, of, phf);
        System.out.printf("at x=29M: double %.3f, float %.3f blocks%n", out[0], of[0]);
    }

    // ------------------------------------------------------------------------------------------------
    // Images
    // ------------------------------------------------------------------------------------------------
    static final int IMG = 512;
    static double STEP = 4; // blocks per pixel
    static double X0 = Double.parseDouble(System.getProperty("x0", "-3072")), Z0 = Double.parseDouble(System.getProperty("z0", "5120"));

    static double[] heightGrid(int mode) {
        terrainMode = mode;
        double[] g = new double[IMG * IMG];
        for (int j = 0; j < IMG; j++) for (int i = 0; i < IMG; i++) g[j * IMG + i] = terrain(X0 + i * STEP, Z0 + j * STEP);
        terrainMode = 0;
        return g;
    }

    static int imgVariant = V_SKIP;

    static double[][] erodedGrid(double[] base, int mode, Params P) {
        terrainMode = mode;
        double[] g = new double[IMG * IMG], r = new double[IMG * IMG];
        double[] out = new double[5], ph = new double[4];
        for (int j = 0; j < IMG; j++) for (int i = 0; i < IMG; i++) {
            double x = X0 + i * STEP, z = Z0 + j * STEP, h = base[j * IMG + i];
            double gx = terrain(x + 1, z) - h, gz = terrain(x, z + 1) - h; // forward differences, 1 block
            if (P.strength == 0) { g[j * IMG + i] = h; continue; }
            erode(x, z, h, gx, gz, P, imgVariant, out, ph);
            g[j * IMG + i] = out[0];
            r[j * IMG + i] = out[3];
        }
        terrainMode = 0;
        return new double[][]{g, r};
    }

    static BufferedImage hillshade(double[] g, String label) {
        BufferedImage img = new BufferedImage(IMG, IMG, BufferedImage.TYPE_INT_RGB);
        double alt = Math.toRadians(45), az = Math.toRadians(315); // NW
        // x right = east, y down = south; light from north-west -> towards (-x, -y)
        double lx = -Math.cos(alt) * Math.sqrt(0.5), ly = -Math.cos(alt) * Math.sqrt(0.5), lz = Math.sin(alt);
        for (int j = 0; j < IMG; j++) for (int i = 0; i < IMG; i++) {
            int i0 = Math.max(i - 1, 0), i1 = Math.min(i + 1, IMG - 1), j0 = Math.max(j - 1, 0), j1 = Math.min(j + 1, IMG - 1);
            double hx = (g[j * IMG + i1] - g[j * IMG + i0]) / ((i1 - i0) * STEP);
            double hy = (g[j1 * IMG + i] - g[j0 * IMG + i]) / ((j1 - j0) * STEP);
            double nl = Math.sqrt(hx * hx + hy * hy + 1);
            double s = (-hx * lx - hy * ly + lz) / nl;
            s = clamp01(s);
            double h = g[j * IMG + i];
            int c;
            if (h < SEA) { // water tint
                double d = clamp01((SEA - h) / 40.0);
                int b = (int) (200 - 90 * d);
                c = (int) (40 * (1 - d)) << 16 | (int) (90 + 40 * (1 - d)) << 8 | b;
            } else {
                int v = (int) (255 * (0.12 + 0.88 * s));
                c = v << 16 | v << 8 | v;
            }
            img.setRGB(i, j, c);
        }
        if (label != null) label(img, label);
        return img;
    }

    static BufferedImage ridgeImage(double[] r, String label) {
        BufferedImage img = new BufferedImage(IMG, IMG, BufferedImage.TYPE_INT_RGB);
        for (int k = 0; k < IMG * IMG; k++) {
            double v = clamp(r[k], -1, 1);
            int R = (int) (128 + 127 * Math.max(v, 0)), B = (int) (128 + 127 * Math.max(-v, 0)), G = (int) (128 - 60 * Math.abs(v));
            img.setRGB(k % IMG, k / IMG, R << 16 | G << 8 | B);
        }
        label(img, label);
        return img;
    }

    static void label(BufferedImage img, String text) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(4, 4, fm.stringWidth(text) + 10, fm.getHeight() + 4);
        g.setColor(Color.WHITE);
        g.drawString(text, 9, 4 + fm.getAscent() + 2);
        g.dispose();
    }

    static void sheet(BufferedImage[] imgs, int cols, File f) throws Exception {
        int rows = (imgs.length + cols - 1) / cols;
        BufferedImage s = new BufferedImage(cols * IMG + (cols - 1) * 4, rows * IMG + (rows - 1) * 4, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = s.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0, 0, s.getWidth(), s.getHeight());
        for (int k = 0; k < imgs.length; k++) g.drawImage(imgs[k], (k % cols) * (IMG + 4), (k / cols) * (IMG + 4), null);
        g.dispose();
        ImageIO.write(s, "png", f);
    }

    static void stats(String name, double[] g) {
        double mn = 1e9, mx = -1e9, sea = 0;
        for (double v : g) { mn = Math.min(mn, v); mx = Math.max(mx, v); if (v < SEA) sea++; }
        System.out.printf("%-34s min %6.1f max %6.1f  below sea %4.1f%%%n", name, mn, mx, 100 * sea / g.length);
    }

    static void images(File dir) throws Exception {
        dir.mkdirs();
        double[] base = heightGrid(0);
        stats("base", base);
        double[] mults = {0, 0.5, 1.0};
        BufferedImage[] strengthImgs = new BufferedImage[4];
        for (int k = 0; k < 3; k++) {
            Params P = new Params();
            P.strength *= mults[k];
            P.offsetX = 0; P.offsetY = 1; // anchored: flat ground keeps its height
            double[][] e = erodedGrid(base, 0, P);
            stats("strength x" + mults[k] + " anchored", e[0]);
            String lab = String.format("strength %.2f (x%.1f), scale %.0f, 5 oct", P.strength, mults[k], P.scale);
            BufferedImage im = hillshade(e[0], lab);
            ImageIO.write(im, "png", new File(dir, String.format("strength_%03d.png", (int) (mults[k] * 100))));
            strengthImgs[k] = im;
            if (k == 2) {
                BufferedImage rim = ridgeImage(e[1], "ridge map (red ridges, blue creases)");
                ImageIO.write(rim, "png", new File(dir, "ridgemap_100.png"));
                strengthImgs[3] = rim;
            }
        }
        sheet(strengthImgs, 2, new File(dir, "sheet_strength.png"));

        // gully width (scale), default strength
        double[] scales = {100, 200, 400};
        BufferedImage[] scaleImgs = new BufferedImage[4];
        scaleImgs[0] = strengthImgs[0];
        for (int k = 0; k < 3; k++) {
            Params P = new Params();
            P.scale = scales[k];
            P.offsetX = 0; P.offsetY = 1;
            double[][] e = erodedGrid(base, 0, P);
            stats("scale " + scales[k], e[0]);
            BufferedImage im = hillshade(e[0], String.format("scale %.0f blocks, strength %.2f", P.scale, P.strength));
            ImageIO.write(im, "png", new File(dir, String.format("scale_%03d.png", (int) scales[k])));
            scaleImgs[k + 1] = im;
        }
        sheet(scaleImgs, 2, new File(dir, "sheet_scale.png"));

        // offset mode: author's default offset (-0.65, 0) vs anchored (0, 1), shows the sea level / plains shift
        {
            Params A = new Params();
            double[][] ea = erodedGrid(base, 0, A);
            stats("author offset (-0.65,0)", ea[0]);
            BufferedImage ia = hillshade(ea[0], "author offset (-0.65, 0): plains sink");
            ImageIO.write(ia, "png", new File(dir, "offset_author.png"));
            // octaves 8
            Params O = new Params(); O.octaves = 8; O.offsetX = 0; O.offsetY = 1;
            double[][] eo = erodedGrid(base, 0, O);
            BufferedImage io = hillshade(eo[0], "8 octaves (last period 1.6 blocks)");
            ImageIO.write(io, "png", new File(dir, "octaves_8.png"));
            Params O3 = new Params(); O3.octaves = 3; O3.offsetX = 0; O3.offsetY = 1;
            double[][] e3 = erodedGrid(base, 0, O3);
            BufferedImage i3 = hillshade(e3[0], "3 octaves");
            ImageIO.write(i3, "png", new File(dir, "octaves_3.png"));
            sheet(new BufferedImage[]{ia, strengthImgs[2], i3, io}, 2, new File(dir, "sheet_offset_octaves.png"));
        }

        // exact vs fast (table + polynomial bell)
        {
            Params B = new Params(); B.offsetX = 0; B.offsetY = 1;
            imgVariant = V_FAST;
            double[][] ef = erodedGrid(base, 0, B);
            imgVariant = V_SKIP;
            BufferedImage im = hillshade(ef[0], "fast: sine table + (1-d2/2.25)^4 bell");
            ImageIO.write(im, "png", new File(dir, "fast_variant.png"));
            sheet(new BufferedImage[]{strengthImgs[2], im}, 2, new File(dir, "sheet_exact_vs_fast.png"));
        }

        // sharp ridged multifractal input (6 oct, |n| crests) vs smooth input
        {
            double[] sharp = heightGrid(2);
            stats("sharp base", sharp);
            Params B = new Params(); B.offsetX = 0; B.offsetY = 1;
            double[][] es = erodedGrid(sharp, 2, B);
            double[][] e0 = erodedGrid(base, 0, B);
            sheet(new BufferedImage[]{hillshade(sharp, "classic ridged: 6 oct, sharp |n| crests"),
                    hillshade(es[0], "eroded: seams on crests, noisy mask"),
                    hillshade(base, "smooth input: 3 oct, softened crests"),
                    hillshade(e0[0], "eroded (default params)")}, 2, new File(dir, "sheet_input_sharp_vs_smooth.png"));
        }

        // flat + coast only: what happens on gentle terrain near the sea
        {
            double[] flat = heightGrid(1);
            stats("flat base", flat);
            Params A = new Params();
            double[][] ea = erodedGrid(flat, 1, A);
            stats("flat, author offset", ea[0]);
            Params B = new Params(); B.offsetX = 0; B.offsetY = 1;
            double[][] eb = erodedGrid(flat, 1, B);
            stats("flat, anchored", eb[0]);
            Params C = new Params(); C.offsetX = 0; C.offsetY = 1; C.valleyAlt = 40; C.peakAlt = 90; C.scale = 60;
            double[][] ec = erodedGrid(flat, 1, C);
            stats("flat, anchored, scale 60, fade 40..90", ec[0]);
            sheet(new BufferedImage[]{hillshade(flat, "plains + coast, no erosion"),
                    hillshade(ea[0], "author offset: whole area sinks"),
                    hillshade(eb[0], "anchored, scale 200"),
                    hillshade(ec[0], "anchored, scale 60, fade 40..90")}, 2, new File(dir, "sheet_flat_coast.png"));
        }
    }

    public static void main(String[] args) throws Exception {
        String cmd = args.length > 0 ? args[0] : "bench";
        switch (cmd) {
            case "bench" -> bench(args.length > 1 ? args[1] : "all");
            case "check" -> check();
            case "preview" -> { STEP = 32; X0 = -8192; Z0 = -8192; double[] g = heightGrid(0); stats("preview", g);
                ImageIO.write(hillshade(g, "16 km preview, 32 blocks/px, origin -8192,-8192"), "png", new File("png/preview.png"));
                // best 2048-block window (64 px): most area above 110 with some sea
                double best = -1; int bi = 0, bj = 0;
                for (int j = 0; j + 64 <= IMG; j += 4) for (int i = 0; i + 64 <= IMG; i += 4) {
                    int hi = 0, lo = 0;
                    for (int b = 0; b < 64; b += 2) for (int a = 0; a < 64; a += 2) { double v = g[(j + b) * IMG + i + a]; if (v > 110) hi++; if (v < SEA) lo++; }
                    double sc = hi + (lo > 10 ? 150 : 0);
                    if (sc > best) { best = sc; bi = i; bj = j; }
                }
                System.out.printf("best window x0=%.0f z0=%.0f score %.0f%n", -8192 + bi * 32.0, -8192 + bj * 32.0, best); }
            case "images" -> images(new File(args.length > 1 ? args[1] : "png"));
            default -> System.out.println("usage: bench | check | images [dir]");
        }
    }
}
