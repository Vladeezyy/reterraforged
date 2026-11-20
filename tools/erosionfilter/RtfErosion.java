/*
 * RtfErosion: applies the erosion filter prototype (ErosionProto.erode, see that file's header for sources:
 * Rune Skovbo Johansen's Advanced Terrain Erosion Filter, MPL-2.0, via lpmitchell/AdvancedTerrainErosion)
 * to ReTerraForged height dumps and renders comparison hillshades. Prototype only, not for the mod.
 *
 * Dump format: <name>.npy float32 (C, N, N), C order, [channel][z][x]; <name>.json with x0, z0, size, step, ...
 * Channels: 0 raw, 1 after_drops, 2 final, 3 riverMask, 4 continentEdge, 5 terrainRegionEdge, 6 erosionMask,
 *           7 flags (mountain 1, hills 2, river 4, lake 8, wetland 16, beach 32, coast 64, plateau 128), 8 gradient.
 * Heights in blocks above sea level (sea = 0).
 *
 * Usage:
 *   java RtfErosion synth <dir>                         write a synthetic dump in the same format
 *   java RtfErosion rtf <path.npy> [outDir] [scale] [strengthMul]   render a/b/c/d/e, close-up, numbers
 *   java RtfErosion tune <path.npy> [outDir]            scale x strength sheet on the mountain close-up window
 */

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RtfErosion {

    static final int RAW = 0, DROPS = 1, FINAL = 2, RIVER = 3, CONT_EDGE = 4, REGION_EDGE = 5, EMASK = 6, FLAGS = 7, GRAD = 8;
    static final int F_MOUNTAIN = 1, F_HILLS = 2, F_RIVER = 4, F_LAKE = 8, F_WETLAND = 16, F_BEACH = 32, F_COAST = 64, F_PLATEAU = 128;

    static final class Dump {
        String name;
        int C, N;
        float[][] ch;
        double x0, z0, step = 1, seaLevel = 63;
        String preset = "?";
        long seed;
    }

    // ------------------------------------------------------------------------------------------------
    // NPY / JSON IO
    // ------------------------------------------------------------------------------------------------
    static Dump read(File npy) throws IOException {
        Dump d = new Dump();
        String base = npy.getName().replaceAll("\\.npy$", "");
        d.name = base;
        try (FileChannel fc = FileChannel.open(npy.toPath())) {
            ByteBuffer head = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
            fc.read(head, 0);
            head.flip();
            byte[] magic = new byte[6];
            head.get(magic);
            if (magic[0] != (byte) 0x93 || magic[1] != 'N') throw new IOException("not an npy file: " + npy);
            int major = head.get() & 0xFF;
            head.get();
            int hlen, hoff;
            if (major == 1) { hlen = head.getShort() & 0xFFFF; hoff = 10; }
            else { hlen = head.getInt(); hoff = 12; }
            ByteBuffer hb = ByteBuffer.allocate(hlen);
            fc.read(hb, hoff);
            String header = new String(hb.array(), StandardCharsets.ISO_8859_1);
            if (!header.contains("'<f4'")) throw new IOException("expected float32 little endian, header: " + header);
            if (header.contains("'fortran_order': True")) throw new IOException("fortran order not supported");
            Matcher m = Pattern.compile("'shape':\\s*\\((\\d+),\\s*(\\d+),\\s*(\\d+)").matcher(header);
            if (!m.find()) throw new IOException("bad shape in header: " + header);
            d.C = Integer.parseInt(m.group(1));
            int n1 = Integer.parseInt(m.group(2)), n2 = Integer.parseInt(m.group(3));
            if (n1 != n2) throw new IOException("expected square tiles, got " + n1 + "x" + n2);
            d.N = n1;
            long dataOff = hoff + hlen;
            long plane = (long) d.N * d.N;
            d.ch = new float[d.C][];
            for (int c = 0; c < d.C; c++) {
                ByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, dataOff + c * plane * 4, plane * 4).order(ByteOrder.LITTLE_ENDIAN);
                float[] a = new float[(int) plane];
                bb.asFloatBuffer().get(a);
                d.ch[c] = a;
            }
        }
        File js = new File(npy.getParentFile(), base + ".json");
        if (js.exists()) {
            String s = Files.readString(js.toPath());
            d.x0 = num(s, "x0", 0); d.z0 = num(s, "z0", 0); d.step = num(s, "step", 1);
            d.seaLevel = num(s, "seaLevel", 63); d.seed = (long) num(s, "seed", 0);
            Matcher pm = Pattern.compile("\"preset\"\\s*:\\s*\"([^\"]*)\"").matcher(s);
            if (pm.find()) d.preset = pm.group(1);
        } else System.out.println("WARN: no json next to " + npy + ", assuming step 1, origin 0,0");
        return d;
    }

    static double num(String json, String key, double def) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?[0-9.eE+-]+)").matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : def;
    }

    static void writeNpy(File f, float[][] ch, int N) throws IOException {
        String dict = "{'descr': '<f4', 'fortran_order': False, 'shape': (" + ch.length + ", " + N + ", " + N + "), }";
        int total = 10 + dict.length() + 1;
        int pad = (64 - total % 64) % 64;
        String header = dict + " ".repeat(pad) + "\n";
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(f), 1 << 20)) {
            os.write(new byte[]{(byte) 0x93, 'N', 'U', 'M', 'P', 'Y', 1, 0});
            os.write(header.length() & 0xFF); os.write(header.length() >> 8);
            os.write(header.getBytes(StandardCharsets.ISO_8859_1));
            ByteBuffer bb = ByteBuffer.allocate(N * 4).order(ByteOrder.LITTLE_ENDIAN);
            for (float[] plane : ch) for (int r = 0; r < N; r++) {
                bb.clear();
                for (int i = 0; i < N; i++) bb.putFloat(plane[r * N + i]);
                os.write(bb.array());
            }
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Synthetic dump (ErosionProto terrain, sea at 0, a fake river, flags), same format
    // ------------------------------------------------------------------------------------------------
    static void synth(File dir) throws IOException {
        dir.mkdirs();
        int N = 1024; double step = 2, x0 = -3600, z0 = 4400;
        float[][] ch = new float[9][N * N];
        for (int j = 0; j < N; j++) for (int i = 0; i < N; i++) {
            double x = x0 + i * step, z = z0 + j * step;
            double h = ErosionProto.terrain(x, z) - 63;
            double rn = ErosionProto.noise(x / 1800.0, z / 1800.0, 777) + 0.3 * ErosionProto.noise(x / 400.0, z / 400.0, 778);
            double riv = 1 - ErosionProto.smooth(0.0, 0.035, Math.abs(rn));          // 1 at river core
            double valley = ErosionProto.smooth(0.0, 0.12, Math.abs(rn));
            if (h > 0) h = h * (0.25 + 0.75 * valley) - 3 * riv;                       // river valley, bed below sea
            int k = j * N + i;
            ch[RAW][k] = (float) h;
            ch[RIVER][k] = (float) (1 - riv);                                          // RTF style: 1 = far from river
            int fl = 0;
            double mnt = ErosionProto.smooth(-0.15, 0.35, ErosionProto.noise(x / 2600.0, z / 2600.0, ErosionProto.TSEED + 2));
            if (h > 0 && mnt > 0.45 && h > 40) fl |= F_MOUNTAIN;
            else if (h > 0 && mnt > 0.2 && h > 15) fl |= F_HILLS;
            if (riv > 0.5) fl |= F_RIVER;
            if (h > -1 && h < 2.5) fl |= F_BEACH;
            if (h > -6 && h <= -1) fl |= F_COAST;
            ch[FLAGS][k] = fl;
            ch[EMASK][k] = h > 0 ? 1 : 0;
        }
        // fake "drops": small blur + slight lowering in concave spots, so b and e differ from a
        float[] bl = boxBlur(toD(ch[RAW]), N, 2, 2);
        for (int k = 0; k < N * N; k++) {
            float r = ch[RAW][k];
            ch[DROPS][k] = r > 0 ? (float) Math.min(r, bl[k] + 0.3 * (r - bl[k])) : r;
            ch[FINAL][k] = r > 0 ? (float) (0.5 * ch[DROPS][k] + 0.5 * bl[k]) : r;
        }
        writeNpy(new File(dir, "synth_proto.npy"), ch, N);
        try (Writer w = new FileWriter(new File(dir, "synth_proto.json"))) {
            w.write(String.format(Locale.ROOT, "{\"x0\": %.0f, \"z0\": %.0f, \"size\": %d, \"step\": %.0f, \"seed\": 0, \"preset\": \"synthetic\", \"seaLevel\": 63, \"worldHeight\": 384}%n",
                    x0, z0, (int) (N * step), step));
        }
        System.out.println("wrote " + new File(dir, "synth_proto.npy"));
    }

    static double[] toD(float[] a) { double[] d = new double[a.length]; for (int i = 0; i < a.length; i++) d[i] = a[i]; return d; }

    // separable box blur, radius r pixels, `passes` passes (3 ~ gaussian)
    static float[] boxBlur(double[] src, int N, int r, int passes) {
        double[] a = src.clone(), b = new double[a.length];
        for (int p = 0; p < passes; p++) {
            for (int j = 0; j < N; j++) {          // horizontal
                double s = 0; int cnt = 0;
                for (int i = -r; i <= r; i++) { int ii = clampI(i, N); s += a[j * N + ii]; cnt++; }
                for (int i = 0; i < N; i++) {
                    b[j * N + i] = s / cnt;
                    s += a[j * N + clampI(i + r + 1, N)] - a[j * N + clampI(i - r, N)];
                }
            }
            for (int i = 0; i < N; i++) {          // vertical
                double s = 0; int cnt = 0;
                for (int j = -r; j <= r; j++) { int jj = clampI(j, N); s += b[jj * N + i]; cnt++; }
                for (int j = 0; j < N; j++) {
                    a[j * N + i] = s / cnt;
                    s += b[clampI(j + r + 1, N) * N + i] - b[clampI(j - r, N) * N + i];
                }
            }
        }
        float[] out = new float[a.length];
        for (int k = 0; k < a.length; k++) out[k] = (float) a[k];
        return out;
    }

    static int clampI(int v, int N) { return v < 0 ? 0 : (v >= N ? N - 1 : v); }

    // chamfer distance (in pixels) to the nearest pixel where src is true
    static float[] distance(boolean[] src, int N) {
        final float INF = 1e9f, A = 1f, D = 1.41421f;
        float[] d = new float[N * N];
        for (int k = 0; k < N * N; k++) d[k] = src[k] ? 0 : INF;
        for (int j = 0; j < N; j++) for (int i = 0; i < N; i++) {
            int k = j * N + i; float v = d[k];
            if (i > 0) v = Math.min(v, d[k - 1] + A);
            if (j > 0) {
                v = Math.min(v, d[k - N] + A);
                if (i > 0) v = Math.min(v, d[k - N - 1] + D);
                if (i < N - 1) v = Math.min(v, d[k - N + 1] + D);
            }
            d[k] = v;
        }
        for (int j = N - 1; j >= 0; j--) for (int i = N - 1; i >= 0; i--) {
            int k = j * N + i; float v = d[k];
            if (i < N - 1) v = Math.min(v, d[k + 1] + A);
            if (j < N - 1) {
                v = Math.min(v, d[k + N] + A);
                if (i < N - 1) v = Math.min(v, d[k + N + 1] + D);
                if (i > 0) v = Math.min(v, d[k + N - 1] + D);
            }
            d[k] = v;
        }
        return d;
    }

    // ------------------------------------------------------------------------------------------------
    // Strength mask
    // ------------------------------------------------------------------------------------------------
    static double hillsWeight = 1.0;      // full strength on hills too (task)
    static double terrainBlurBlocks = 48; // soft transition mountains/hills -> plains
    static double keepOutFadeBlocks = 64; // distance over which strength ramps up from rivers/lakes/coast/sea
    static double heightRampLo = 2, heightRampHi = 24; // blocks above sea

    static float[] buildMask(Dump d, StringBuilder log) {
        int N = d.N, n = N * N;
        float[] raw = d.ch[RAW], flags = d.ch[FLAGS], riv = d.ch[RIVER];
        // riverMask semantics: auto-detect "1 = far from river" (TerraForged Cell default) vs "1 = river"
        int ones = 0;
        for (int k = 0; k < n; k++) if (riv[k] >= 0.99f) ones++;
        // ReTerraForged: riverMask is 1 away from rivers and falls towards 0 at them (tile Modifier weakens erosion below 0.1);
        // the share of 1s depends on the area (river-rich Towering Peaks: 27 %), so no auto-detection
        boolean invert = true;
        log.append(String.format(Locale.ROOT, "riverMask: %.1f%% of pixels >= 0.99 -> treating %s%n", 100.0 * ones / n,
                invert ? "1 = far from river (riverness = 1 - riverMask)" : "1 = river"));
        double[] t = new double[n];
        boolean[] keep = new boolean[n];
        for (int k = 0; k < n; k++) {
            int f = (int) flags[k];
            t[k] = (f & F_MOUNTAIN) != 0 ? 1 : ((f & F_HILLS) != 0 ? hillsWeight : 0);
            double r = invert ? 1 - riv[k] : riv[k];
            keep[k] = (f & (F_RIVER | F_LAKE | F_WETLAND | F_BEACH | F_COAST)) != 0 || r > 0.05 || raw[k] <= 0.5f;
        }
        int rad = Math.max(1, (int) Math.round(terrainBlurBlocks / d.step / 2)); // 3 passes of r ~ gaussian sigma ~ r
        float[] tb = boxBlur(t, N, rad, 3);
        float[] dist = distance(keep, N);
        float[] m = new float[n];
        double sum = 0; int land = 0;
        for (int k = 0; k < n; k++) {
            double fk = ErosionProto.smooth(0, keepOutFadeBlocks, dist[k] * d.step);
            double fh = ErosionProto.smooth(heightRampLo, heightRampHi, raw[k]);
            m[k] = (float) (tb[k] * fk * fh);
            if (raw[k] > 0) { land++; sum += m[k]; }
        }
        // final light blur against chamfer "teeth"; keep exact zeros on keep-out pixels
        float[] mb = boxBlur(toD(m), N, Math.max(1, (int) Math.round(8 / d.step)), 2);
        sum = 0;
        for (int k = 0; k < n; k++) { m[k] = keep[k] ? 0 : mb[k]; if (raw[k] > 0) sum += m[k]; }
        log.append(String.format(Locale.ROOT, "mask: mean over land %.3f%n", land > 0 ? sum / land : 0));
        return m;
    }

    // ------------------------------------------------------------------------------------------------
    // Filter
    // ------------------------------------------------------------------------------------------------
    static int capOctaves(double scale, int want) {
        int cap = (int) Math.floor(1 + Math.log(scale / 8) / Math.log(2));
        return Math.max(1, Math.min(want, cap));
    }

    static ErosionProto.Params params(double scale, double strengthMul, double valley, double peak) {
        ErosionProto.Params P = new ErosionProto.Params();
        P.scale = scale;
        P.strength = 0.22 * strengthMul;
        P.octaves = capOctaves(scale, 5);
        P.offsetX = 0; P.offsetY = 1;      // anchored
        P.valleyAlt = valley; P.peakAlt = peak;
        return P;
    }

    static float[] blurCacheSrc, blurCacheOut;
    static int blurCacheR;

    /** three box-blur passes of radius r (about a Gaussian), edges clamped; cached for the last source */
    static float[] blurred(float[] src, int N, int r) {
        if (src == blurCacheSrc && r == blurCacheR) return blurCacheOut;
        float[] a = src.clone(), b = new float[a.length];
        for (int pass = 0; pass < 3; pass++) {
            for (int j = 0; j < N; j++) for (int i = 0; i < N; i++) {
                double s = 0;
                for (int d = -r; d <= r; d++) s += a[j * N + Math.max(0, Math.min(N - 1, i + d))];
                b[j * N + i] = (float) (s / (2 * r + 1));
            }
            for (int j = 0; j < N; j++) for (int i = 0; i < N; i++) {
                double s = 0;
                for (int d = -r; d <= r; d++) s += b[Math.max(0, Math.min(N - 1, j + d)) * N + i];
                a[j * N + i] = (float) (s / (2 * r + 1));
            }
        }
        blurCacheSrc = src; blurCacheR = r; blurCacheOut = a;
        return a;
    }

    static double lastNsPerFiltered, lastNsPerPoint;
    static int lastFiltered;

    /** returns filtered heights; only pixels in [i0,i1)x[j0,j1) are filtered, others copy raw */
    static float[] filter(Dump d, float[] mask, ErosionProto.Params P0, int i0, int j0, int i1, int j1, int variant) {
        int N = d.N;
        float[] raw = d.ch[RAW];
        float[] out = raw.clone();
        // -Dgradblur=<blocks>: take the gully directions from a blurred copy (ReTerraForged's raw height carries small
        // ripples that scramble a per-pixel gradient)
        double gb = Double.parseDouble(System.getProperty("gradblur", "0"));
        float[] graw = gb > 0 ? blurred(raw, N, Math.max(1, (int) Math.round(gb / d.step))) : raw;
        double[] o = new double[5], ph = new double[4];
        ErosionProto.Params P = P0.copy();
        double inv2s = 1.0 / (2 * d.step);
        long t0 = System.nanoTime();
        int cnt = 0;
        for (int j = j0; j < j1; j++) for (int i = i0; i < i1; i++) {
            int k = j * N + i;
            float m = mask[k];
            if (m <= 0) continue;
            double h = raw[k];
            int ia = Math.max(i - 1, 0), ib = Math.min(i + 1, N - 1), ja = Math.max(j - 1, 0), jb = Math.min(j + 1, N - 1);
            double gx = (graw[j * N + ib] - graw[j * N + ia]) / ((ib - ia) * d.step);
            double gz = (graw[jb * N + i] - graw[ja * N + i]) / ((jb - ja) * d.step);
            P.strength = P0.strength * m;
            ErosionProto.erode(d.x0 + i * d.step, d.z0 + j * d.step, h, gx, gz, P, variant, o, ph);
            double v = o[0];
            if (h > 0) v = Math.max(v, Math.min(h, 1.0)); // never dig land below sea level
            out[k] = (float) v;
            cnt++;
        }
        long t = System.nanoTime() - t0;
        lastFiltered = cnt;
        lastNsPerFiltered = cnt > 0 ? (double) t / cnt : 0;
        lastNsPerPoint = (double) t / ((long) (i1 - i0) * (j1 - j0));
        return out;
    }

    // ------------------------------------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------------------------------------
    static BufferedImage hillshade(float[] g, int N, double step, String label) {
        BufferedImage img = new BufferedImage(N, N, BufferedImage.TYPE_INT_RGB);
        double alt = Math.toRadians(45);
        double lx = -Math.cos(alt) * Math.sqrt(0.5), ly = -Math.cos(alt) * Math.sqrt(0.5), lz = Math.sin(alt);
        for (int j = 0; j < N; j++) for (int i = 0; i < N; i++) {
            int i0 = Math.max(i - 1, 0), i1 = Math.min(i + 1, N - 1), j0 = Math.max(j - 1, 0), j1 = Math.min(j + 1, N - 1);
            double hx = (g[j * N + i1] - g[j * N + i0]) / ((i1 - i0) * step);
            double hy = (g[j1 * N + i] - g[j0 * N + i]) / ((j1 - j0) * step);
            double s = ErosionProto.clamp01((-hx * lx - hy * ly + lz) / Math.sqrt(hx * hx + hy * hy + 1));
            double h = g[j * N + i];
            int c;
            if (h < 0) {
                double dd = ErosionProto.clamp01(-h / 40.0);
                int sh = (int) (40 * (0.6 + 0.4 * s));
                c = (int) (40 * (1 - dd)) << 16 | (int) (90 + 40 * (1 - dd) + sh * 0) << 8 | (int) (200 - 90 * dd);
            } else {
                int v = (int) (255 * (0.12 + 0.88 * s));
                c = v << 16 | v << 8 | v;
            }
            img.setRGB(i, j, c);
        }
        if (label != null) ErosionProto.label(img, label);
        return img;
    }

    static BufferedImage maskImage(float[] m, int N, String label) {
        BufferedImage img = new BufferedImage(N, N, BufferedImage.TYPE_INT_RGB);
        for (int k = 0; k < N * N; k++) { int v = (int) (255 * ErosionProto.clamp01(m[k])); img.setRGB(k % N, k / N, v << 16 | (v / 2 + 40) << 8 | 40); }
        ErosionProto.label(img, label);
        return img;
    }

    static BufferedImage crop(BufferedImage src, int x, int y, int w, String label) {
        BufferedImage c = new BufferedImage(w, w, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = c.createGraphics();
        g.drawImage(src.getSubimage(x, y, w, w), 0, 0, null);
        g.dispose();
        if (label != null) ErosionProto.label(c, label);
        return c;
    }

    static void sheet(BufferedImage[] imgs, int cols, File f) throws IOException {
        int w = imgs[0].getWidth(), rows = (imgs.length + cols - 1) / cols;
        BufferedImage s = new BufferedImage(cols * w + (cols - 1) * 4, rows * w + (rows - 1) * 4, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = s.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0, 0, s.getWidth(), s.getHeight());
        for (int k = 0; k < imgs.length; k++) g.drawImage(imgs[k], (k % cols) * (w + 4), (k / cols) * (w + 4), null);
        g.dispose();
        ImageIO.write(s, "png", f);
    }

    // window of `w` pixels with the most mountain pixels (ties -> highest relief)
    static int[] mountainWindow(Dump d, int w) {
        int N = d.N;
        if (w >= N) return new int[]{0, 0};
        long[] I = new long[(N + 1) * (N + 1)];
        for (int j = 0; j < N; j++) {
            long row = 0;
            for (int i = 0; i < N; i++) {
                int f = (int) d.ch[FLAGS][j * N + i];
                row += ((f & F_MOUNTAIN) != 0 ? 1000 : 0) + (long) Math.max(0, d.ch[RAW][j * N + i]);
                I[(j + 1) * (N + 1) + i + 1] = I[j * (N + 1) + i + 1] + row;
            }
        }
        long best = -1; int bi = 0, bj = 0;
        int st = Math.max(1, w / 16);
        for (int j = 0; j + w <= N; j += st) for (int i = 0; i + w <= N; i += st) {
            long s = I[(j + w) * (N + 1) + i + w] - I[j * (N + 1) + i + w] - I[(j + w) * (N + 1) + i] + I[j * (N + 1) + i];
            if (s > best) { best = s; bi = i; bj = j; }
        }
        return new int[]{bi, bj};
    }

    static double percentileLand(float[] raw, double p) {
        float[] a = new float[raw.length]; int n = 0;
        for (float v : raw) if (v > 0) a[n++] = v;
        if (n == 0) return 100;
        Arrays.sort(a, 0, n);
        return a[Math.min(n - 1, (int) (p * n))];
    }

    // stats of |b - a| over land (a > 0)
    static String diffStats(String name, float[] a, float[] b) {
        int N = (int) Math.round(Math.sqrt(a.length));
        return diffStats(name, a, b, 0, 0, N, N);
    }

    static String diffStats(String name, float[] a, float[] b, int i0, int j0, int w, int N) {
        double sum = 0, mx = 0; int land = 0, sunk = 0;
        for (int jj = j0; jj < j0 + w; jj++) for (int ii = i0; ii < i0 + w; ii++) {
            int k = jj * N + ii;
            if (a[k] <= 0) continue;
            land++;
            double dd = Math.abs(b[k] - a[k]);
            sum += dd; mx = Math.max(mx, dd);
            if (b[k] <= 0) sunk++;
        }
        return String.format(Locale.ROOT, "%-34s mean|dh| %6.2f  max %7.2f  land->water %6.3f%%", name, sum / land, mx, 100.0 * sunk / land);
    }

    // ------------------------------------------------------------------------------------------------
    static void runRtf(File npy, File outDir, double scale, double mulFull) throws IOException {
        outDir.mkdirs();
        Dump d = read(npy);
        StringBuilder log = new StringBuilder();
        int N = d.N;
        float[] raw = d.ch[RAW];
        double peak = percentileLand(raw, 0.99), valley = 6;
        log.append(String.format(Locale.ROOT, "%s: N=%d step=%.1f (%.0f blocks), origin %.0f,%.0f, preset %s, seed %d%n",
                d.name, N, d.step, N * d.step, d.x0, d.z0, d.preset, d.seed));
        int fl = 0, mnt = 0, hil = 0, landPx = 0;
        double mx = -1e9;
        for (int k = 0; k < N * N; k++) {
            int f = (int) d.ch[FLAGS][k];
            if (raw[k] > 0) landPx++;
            if ((f & F_MOUNTAIN) != 0) mnt++;
            if ((f & F_HILLS) != 0) hil++;
            mx = Math.max(mx, raw[k]);
        }
        log.append(String.format(Locale.ROOT, "land %.1f%%, mountain %.1f%%, hills %.1f%%, raw max %.1f, p99 land %.1f -> fade %.0f..%.0f%n",
                100.0 * landPx / (N * N), 100.0 * mnt / (N * N), 100.0 * hil / (N * N), mx, peak, valley, peak));
        float[] mask = buildMask(d, log);
        ErosionProto.Params Ph = params(scale, 0.5 * mulFull, valley, peak);
        ErosionProto.Params Pf = params(scale, mulFull, valley, peak);
        log.append(String.format(Locale.ROOT, "filter: scale %.0f blocks, octaves %d, strength %.3f / %.3f, anchored offset (0,1)%n",
                scale, Pf.octaves, Ph.strength, Pf.strength));
        filter(d, mask, Pf, 0, 0, Math.min(N, 64), Math.min(N, 64), ErosionProto.V_SKIP_TAB); // JIT warm-up
        filter(d, mask, Pf, 0, 0, N, N, ErosionProto.V_SKIP_TAB);
        float[] fh = filter(d, mask, Ph, 0, 0, N, N, ErosionProto.V_SKIP_TAB);
        float[] ff = filter(d, mask, Pf, 0, 0, N, N, ErosionProto.V_SKIP_TAB);
        log.append(String.format(Locale.ROOT, "time: %.0f ns per filtered point (%d of %d points have mask > 0), %.0f ns per tile point, %.0f ms per tile%n",
                lastNsPerFiltered, lastFiltered, N * N, lastNsPerPoint, lastNsPerPoint * N * N / 1e6));
        float[] e = new float[N * N];
        float[] fin = d.ch[FINAL];
        for (int k = 0; k < N * N; k++) e[k] = fin[k] + (ff[k] - raw[k]);
        log.append(diffStats("filter x0.5 vs raw", raw, fh)).append('\n');
        log.append(diffStats("filter x1 vs raw", raw, ff)).append('\n');
        log.append(diffStats("drops (after_drops) vs raw", raw, d.ch[DROPS])).append('\n');
        log.append(diffStats("final vs raw", raw, fin)).append('\n');
        log.append(diffStats("final + filter vs raw", raw, e)).append('\n');

        String sc = String.format(Locale.ROOT, "scale %.0f", scale);
        BufferedImage a = hillshade(raw, N, d.step, "(a) raw, no erosion");
        BufferedImage b = hillshade(fin, N, d.step, "(b) final: drops + smoothing (world now)");
        BufferedImage c = hillshade(fh, N, d.step, String.format(Locale.ROOT, "(c) raw + filter x%.2f, %s", 0.5 * mulFull, sc));
        BufferedImage dd = hillshade(ff, N, d.step, String.format(Locale.ROOT, "(d) raw + filter x%.2f, %s", mulFull, sc));
        BufferedImage ee = hillshade(e, N, d.step, "(e) final + (filtered - raw)");
        BufferedImage mi = maskImage(mask, N, "strength mask");
        String p = d.name + "_";
        ImageIO.write(a, "png", new File(outDir, p + "a_raw.png"));
        ImageIO.write(b, "png", new File(outDir, p + "b_final.png"));
        ImageIO.write(c, "png", new File(outDir, p + "c_filter050.png"));
        ImageIO.write(dd, "png", new File(outDir, p + "d_filter100.png"));
        ImageIO.write(ee, "png", new File(outDir, p + "e_final_plus_filter.png"));
        ImageIO.write(mi, "png", new File(outDir, p + "mask.png"));
        sheet(new BufferedImage[]{a, b, c, dd}, 2, new File(outDir, p + "sheet_abcd.png"));
        sheet(new BufferedImage[]{b, ee, mi, dd}, 2, new File(outDir, p + "sheet_e_mask.png"));

        // close-up: 512 blocks of the most mountainous window, no upscaling
        int w = (int) Math.min(N, Math.round(512 / d.step));
        int[] win = mountainWindow(d, w);
        String cl = String.format(Locale.ROOT, " [%d blocks @ %.0f b/px]", (int) (w * d.step), d.step);
        sheet(new BufferedImage[]{crop(a, win[0], win[1], w, "(a) raw" + cl), crop(b, win[0], win[1], w, "(b) final"),
                crop(c, win[0], win[1], w, "(c) filter x0.5"), crop(dd, win[0], win[1], w, "(d) filter x1")}, 2, new File(outDir, p + "sheet_closeup.png"));
        log.append(String.format(Locale.ROOT, "close-up window: px %d,%d size %d (world %.0f,%.0f)%n", win[0], win[1], w,
                d.x0 + win[0] * d.step, d.z0 + win[1] * d.step));
        String s = log.toString();
        System.out.print(s);
        Files.writeString(new File(outDir, p + "stats.txt").toPath(), s);
    }

    static void runTune(File npy, File outDir) throws IOException {
        outDir.mkdirs();
        Dump d = read(npy);
        StringBuilder log = new StringBuilder();
        int N = d.N;
        float[] mask = buildMask(d, log);
        double peak = percentileLand(d.ch[RAW], 0.99);
        int w = (int) Math.min(N, Math.round(1024 / d.step));
        int[] win = mountainWindow(d, w);
        double[] scales = {64, 96, 128, 192, 256};
        double[] muls = {0.5, 1.0};
        java.util.List<BufferedImage> imgs = new ArrayList<>();
        BufferedImage rawImg = hillshade(d.ch[RAW], N, d.step, null);
        BufferedImage finImg = hillshade(d.ch[FINAL], N, d.step, null);
        imgs.add(crop(rawImg, win[0], win[1], w, "raw"));
        imgs.add(crop(finImg, win[0], win[1], w, "final (drops)"));
        for (double m : muls) for (double sc : scales) {
            ErosionProto.Params P = params(sc, m, 6, peak);
            int pad = 2;
            float[] f = filter(d, mask, P, Math.max(0, win[0] - pad), Math.max(0, win[1] - pad), Math.min(N, win[0] + w + pad), Math.min(N, win[1] + w + pad), ErosionProto.V_SKIP_TAB);
            BufferedImage im = hillshade(f, N, d.step, null);
            imgs.add(crop(im, win[0], win[1], w, String.format(Locale.ROOT, "scale %.0f, x%.1f, %d oct", sc, m, P.octaves)));
            log.append(String.format(Locale.ROOT, "%s%n", diffStats(String.format(Locale.ROOT, "scale %.0f x%.1f (window)", sc, m), d.ch[RAW], f, win[0], win[1], w, N)));
        }
        log.append(diffStats("final vs raw (window)", d.ch[RAW], d.ch[FINAL], win[0], win[1], w, N)).append(System.lineSeparator());
        sheet(imgs.toArray(new BufferedImage[0]), 4, new File(outDir, d.name + "_sheet_tune.png"));
        System.out.print(log);
        Files.writeString(new File(outDir, d.name + "_tune.txt").toPath(), log.toString());
    }

    public static void main(String[] args) throws Exception {
        String cmd = args.length > 0 ? args[0] : "";
        switch (cmd) {
            case "synth" -> synth(new File(args[1]));
            case "rtf" -> runRtf(new File(args[1]), new File(args.length > 2 ? args[2] : "render"),
                    args.length > 3 ? Double.parseDouble(args[3]) : 128, args.length > 4 ? Double.parseDouble(args[4]) : 1.0);
            case "tune" -> runTune(new File(args[1]), new File(args.length > 2 ? args[2] : "render"));
            default -> System.out.println("usage: synth <dir> | rtf <npy> [outDir] [scale] [strengthMul] | tune <npy> [outDir]");
        }
    }
}
