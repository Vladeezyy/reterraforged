/*
 * RidgeTune: candidates of the in-game erosion filter (ErosionFilterKernel, the same file the mod uses) on an
 * "rtf heightdump" without the filter, as a hill-shaded sheet. Prototype only.
 *
 *   javac -d out common/src/main/java/raccoonman/reterraforged/world/worldgen/cell/heightmap/ErosionFilterKernel.java tools/erosionfilter/RidgeTune.java
 *   java -cp out RidgeTune <dump.npy> <out.png> <col0> <row0> <size> "<label>|mul=0.3;step=48;scale=240;..." ...
 *
 * Like ErosionFilterProto: the gradient from the raw height at +-step blocks, the mask from the share of mountain/hills
 * flags among the centre and the 4 samples times a ramp from 2 to 24 blocks. The droplets are not run: "Raw" and the
 * candidates are without them, "Current" is the dump's final height (droplets and smoothing). Keys other than mul and
 * step are ErosionFilterKernel's (rtf.erosionfilter.<key>).
 */

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import raccoonman.reterraforged.world.worldgen.cell.heightmap.ErosionFilterKernel;

public class RidgeTune {
    static int N;
    static double x0, z0, dstep;
    static float[][] ch;

    public static void main(String[] a) throws Exception {
        read(new File(a[0]));
        File out = new File(a[1]);
        int c0 = Integer.parseInt(a[2]), r0 = Integer.parseInt(a[3]), size = Integer.parseInt(a[4]);
        float[] raw = ch[0], fin = ch[2], flags = ch[7];
        List<BufferedImage> imgs = new ArrayList<>();
        imgs.add(shade(raw, c0, r0, size, "Raw (no erosion)"));
        imgs.add(shade(fin, c0, r0, size, "Current (droplets)"));
        for (int k = 5; k < a.length; k++) {
            String[] lp = a[k].split("\\|", 2);
            Map<String, String> p = new HashMap<>();
            for (String kv : lp[1].split(";")) if (kv.contains("=")) p.put(kv.split("=")[0].trim(), kv.split("=")[1].trim());
            double mul = Double.parseDouble(p.getOrDefault("mul", "0.5"));
            int st = (int) Math.round(Double.parseDouble(p.getOrDefault("step", "28")) / dstep);
            ErosionFilterKernel kern = new ErosionFilterKernel(mul, (key) -> p.get(key.substring("rtf.erosionfilter.".length())));
            float[] outH = raw.clone();
            double[] ph = new double[4];
            double sumAbs = 0, maxUp = -1e9, maxDown = 1e9; int cnt = 0;
            long t0 = System.nanoTime();
            for (int j = r0; j < r0 + size; j++) for (int i = c0; i < c0 + size; i++) {
                int idx = j * N + i;
                double h = raw[idx];
                int f = (int) flags[idx];
                if (h <= 2 || (f & (4 | 8 | 16 | 32 | 64)) != 0) continue;
                int ip = Math.min(N - 1, i + st), im = Math.max(0, i - st), jp = Math.min(N - 1, j + st), jm = Math.max(0, j - st);
                int in = m(flags[idx]) + m(flags[j * N + ip]) + m(flags[j * N + im]) + m(flags[jp * N + i]) + m(flags[jm * N + i]);
                if (in == 0) continue;
                double mask = in / 5.0 * ErosionFilterKernel.smooth(2, 24, h);
                double gx = (raw[j * N + ip] - raw[j * N + im]) / ((ip - im) * dstep);
                double gz = (raw[jp * N + i] - raw[jm * N + i]) / ((jp - jm) * dstep);
                double v = kern.apply(x0 + i * dstep, z0 + j * dstep, h, gx, gz, mask, ph);
                outH[idx] = (float) v;
                double d = v - h; sumAbs += Math.abs(d); maxUp = Math.max(maxUp, d); maxDown = Math.min(maxDown, d); cnt++;
            }
            double ns = (System.nanoTime() - t0) / (double) Math.max(1, cnt);
            System.out.printf(Locale.ROOT, "%-28s mean|d| %5.1f  up %+5.1f  down %+6.1f  %4.0f ns  [%s, step %d]%n", lp[0], sumAbs / Math.max(1, cnt), maxUp, maxDown, ns, kern.describe(), (int) (st * dstep));
            imgs.add(shade(outH, c0, r0, size, lp[0]));
            // -Dsave=<file>: the last candidate's heights, float32 little endian N x N (checks against an in-game dump)
            if (System.getProperty("save") != null) {
                ByteBuffer bb = ByteBuffer.allocate(outH.length * 4).order(ByteOrder.LITTLE_ENDIAN);
                bb.asFloatBuffer().put(outH);
                java.nio.file.Files.write(new File(System.getProperty("save")).toPath(), bb.array());
            }
        }
        int cols = Math.min(3, imgs.size()), rows = (imgs.size() + cols - 1) / cols, w = imgs.get(0).getWidth();
        BufferedImage s = new BufferedImage(cols * w + (cols - 1) * 6, rows * w + (rows - 1) * 6, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = s.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0, 0, s.getWidth(), s.getHeight());
        for (int k = 0; k < imgs.size(); k++) g.drawImage(imgs.get(k), (k % cols) * (w + 6), (k / cols) * (w + 6), null);
        g.dispose();
        ImageIO.write(s, "png", out);
        System.out.println("wrote " + out);
    }

    static final double EXAG = Double.parseDouble(System.getProperty("exag", "0.5"));

    static int m(float flag) { return ((int) flag & 3) != 0 ? 1 : 0; }

    // light from the north west, 40 degrees up; a little height tint
    static BufferedImage shade(float[] h, int c0, int r0, int size, String label) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        double alt = Math.toRadians(40), lx = -Math.cos(alt) * Math.sqrt(0.5), lz = -Math.cos(alt) * Math.sqrt(0.5), ly = Math.sin(alt);
        for (int j = r0; j < r0 + size; j++) for (int i = c0; i < c0 + size; i++) {
            int i0 = Math.max(i - 1, 0), i1 = Math.min(i + 1, N - 1), j0 = Math.max(j - 1, 0), j1 = Math.min(j + 1, N - 1);
            double hx = EXAG * (h[j * N + i1] - h[j * N + i0]) / ((i1 - i0) * dstep);
            double hz = EXAG * (h[j1 * N + i] - h[j0 * N + i]) / ((j1 - j0) * dstep);
            double s = Math.max(0, (-hx * lx - hz * lz + ly) / Math.sqrt(hx * hx + hz * hz + 1));
            double t = Math.max(0, Math.min(1, h[j * N + i] / 220.0));
            double v = 0.3 + 0.7 * s;
            int r = (int) (255 * v * (0.85 + 0.15 * t)), gg = (int) (255 * v * (0.92 + 0.08 * (1 - t))), b = (int) (255 * v * (0.8 + 0.2 * t));
            img.setRGB(i - c0, j - r0, Math.min(255, r) << 16 | Math.min(255, gg) << 8 | Math.min(255, b));
        }
        Graphics2D g = img.createGraphics();
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.setColor(new Color(0, 0, 0, 160)); g.fillRect(0, 0, g.getFontMetrics().stringWidth(label) + 12, 26);
        g.setColor(Color.WHITE); g.drawString(label, 6, 19);
        g.dispose();
        return img;
    }

    static void read(File npy) throws IOException {
        try (FileChannel fc = FileChannel.open(npy.toPath())) {
            ByteBuffer head = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN);
            fc.read(head, 0); head.flip(); head.position(8);
            int hlen = head.getShort() & 0xFFFF;
            ByteBuffer hb = ByteBuffer.allocate(hlen); fc.read(hb, 10);
            String header = new String(hb.array(), StandardCharsets.ISO_8859_1);
            Matcher m = Pattern.compile("'shape':\\s*\\((\\d+),\\s*(\\d+),\\s*(\\d+)").matcher(header);
            if (!m.find()) throw new IOException(header);
            int C = Integer.parseInt(m.group(1)); N = Integer.parseInt(m.group(2));
            long plane = (long) N * N; ch = new float[C][];
            for (int c = 0; c < C; c++) {
                ByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 10 + hlen + c * plane * 4, plane * 4).order(ByteOrder.LITTLE_ENDIAN);
                ch[c] = new float[(int) plane]; bb.asFloatBuffer().get(ch[c]);
            }
        }
        String js = new String(java.nio.file.Files.readAllBytes(new File(npy.getPath().replaceAll("\\.npy$", ".json")).toPath()));
        x0 = num(js, "x0"); z0 = num(js, "z0"); dstep = num(js, "step");
    }

    static double num(String s, String k) {
        Matcher m = Pattern.compile("\"" + k + "\"\\s*:\\s*(-?[0-9.]+)").matcher(s);
        return m.find() ? Double.parseDouble(m.group(1)) : 0;
    }
}
