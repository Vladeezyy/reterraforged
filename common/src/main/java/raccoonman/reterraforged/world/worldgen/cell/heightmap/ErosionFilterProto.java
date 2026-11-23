/*
 * TEST FEATURE (branch proto/erosion-filter): the preset's Ridge strength and Ridge style (FilterSettings.Ridges).
 * The filter itself is ErosionFilterKernel (MPL-2.0, see its header).
 */
package raccoonman.reterraforged.world.worldgen.cell.heightmap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import raccoonman.reterraforged.data.worldgen.preset.settings.FilterSettings.RidgeStyle;
import raccoonman.reterraforged.data.worldgen.preset.settings.FilterSettings.Ridges;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.terrain.Terrain;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainCategory;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.util.Seed;

/**
 * The erosion filter of the preset's Ridges (Heightmap.make makes it, none at strength 0): added to the height of
 * Heightmap.applyTerrain (before the rivers, so the tiles, WorldLookup and the preview get it). The gully directions
 * come from central differences of the unfiltered height at +-step blocks (28 Sharp, 56 Smooth; about the gradient of
 * a blurred height: the ripples of the raw height would scramble a per-block gradient). The strength mask: the share
 * of mountain or hills columns among the centre and the 4 gradient samples, times a ramp from 2 to 24 blocks above
 * the water.
 * <p>
 * Strength 1 is the author's 0.22. For the screenshots of development the system properties go over the preset:
 * -Drtf.erosionfilter=&lt;strength&gt;, -Drtf.erosionfilter.preset=subtle|bold (Smooth at 0.45 / 0.75) and every
 * setting of ErosionFilterKernel as rtf.erosionfilter.&lt;name&gt; (rtf.erosionfilter.seed=1337 gives the seed of the
 * prototype's shots back; otherwise it comes from the world's seed).
 */
public final class ErosionFilterProto {
	// Smooth ("Ridges: subtle" 0.45, "Ridges: bold" 0.75), picked with tools/erosionfilter/RidgeTune.java on Towering
	// Peaks: wide ridges (scale 300, 3 octaves: no needles of the small octaves), rounded ridge tops, the fade target up
	// to 200 blocks (with 140 the whole upper mountain was carved to its full depth), anchor 1 (the ridges of the upper
	// slopes stay at the input height: with the author's offset the shoulders sank about twice as much as the summit,
	// which stood up as a horn), the strength down to 0.15 from 110 to 170 blocks (the gullies around a summit run in
	// all directions and still cut it into horns), rises kept at 0.3 and at most 8 blocks
	private static final String[] SMOOTH = { "step", "56", "scale", "300", "octaves", "3", "ridgeRounding", "0.5", "detail", "1.0", "peak", "200", "anchor", "1", "topLo", "110", "topHi", "170", "topCut", "0.85", "raise", "0.3", "maxRaise", "8" };
	private static final Map<String, String> SMOOTH_SETTINGS = settings(SMOOTH);
	// the filter's seed: an offset of the world's, so the other noises keep theirs
	private static final int SEED_OFFSET = 48611;
	private static final double RAMP_LO = 2.0, RAMP_HI = 24.0;
	private static final AtomicReference<String> LOGGED = new AtomicReference<>("");

	private static Map<String, String> settings(String[] pairs) {
		Map<String, String> map = new java.util.HashMap<>();
		for (int i = 0; i < pairs.length; i += 2) map.put(pairs[i], pairs[i + 1]);
		return Map.copyOf(map);
	}

	private final float step;
	private final ErosionFilterKernel kernel;

	private ErosionFilterProto(float step, ErosionFilterKernel kernel) {
		this.step = step;
		this.kernel = kernel;
	}

	@Nullable
	public static ErosionFilterProto make(Ridges ridges, Seed seed) {
		String dev = System.getProperty("rtf.erosionfilter.preset", "");
		float strength = ridges.strength;
		RidgeStyle style = ridges.style;
		if (dev.equals("subtle") || dev.equals("bold")) {
			style = RidgeStyle.SMOOTH;
			strength = dev.equals("subtle") ? 0.45F : 0.75F;
		}
		String strengthProperty = System.getProperty("rtf.erosionfilter");
		if (strengthProperty != null) {
			strength = Float.parseFloat(strengthProperty);
		}
		if (!(strength > 0.0F)) {
			return null;
		}
		Map<String, String> styleSettings = style == RidgeStyle.SMOOTH ? SMOOTH_SETTINGS : Map.of();
		String filterSeed = String.valueOf(seed.offset(SEED_OFFSET).root());
		Function<String, String> lookup = (key) -> {
			String v = System.getProperty(key);
			if (v != null) return v;
			String name = key.substring("rtf.erosionfilter.".length());
			return name.equals("seed") ? filterSeed : styleSettings.get(name);
		};
		String step = lookup.apply("rtf.erosionfilter.step");
		ErosionFilterProto filter = new ErosionFilterProto(Float.parseFloat(step != null ? step : "28"), new ErosionFilterKernel(strength, lookup));
		// once per setting, not on every preview of the preset screen
		String description = style + " x" + strength + ", gradient step " + filter.step + ", " + filter.kernel.describe();
		if (!LOGGED.getAndSet(description).equals(description)) {
			raccoonman.reterraforged.RTFCommon.LOGGER.info("[RTF] Ridges (test feature) on: {}", description);
		}
		return filter;
	}

	private static final ConcurrentHashMap<Terrain, Boolean> MASKED = new ConcurrentHashMap<>();
	private static final ThreadLocal<Work> WORK = ThreadLocal.withInitial(Work::new);

	private static final class Work {
		final Cell side = new Cell();
		final double[] ph = new double[4];
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
	void apply(Heightmap heightmap, Cell cell, float x, float z) {
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
		heightmap.applyTerrainRaw(side.reset(), x + this.step, z);
		double hxp = side.height;
		inMask += masked(side.terrain) ? 1 : 0;
		heightmap.applyTerrainRaw(side.reset(), x - this.step, z);
		double hxm = side.height;
		inMask += masked(side.terrain) ? 1 : 0;
		heightmap.applyTerrainRaw(side.reset(), x, z + this.step);
		double hzp = side.height;
		inMask += masked(side.terrain) ? 1 : 0;
		heightmap.applyTerrainRaw(side.reset(), x, z - this.step);
		double hzm = side.height;
		inMask += masked(side.terrain) ? 1 : 0;
		if (inMask == 0) {
			return;
		}
		double mask = inMask / 5.0 * ErosionFilterKernel.smooth(RAMP_LO, RAMP_HI, h);
		double gx = (hxp - hxm) * worldHeight / (2.0 * this.step);
		double gz = (hzp - hzm) * worldHeight / (2.0 * this.step);
		double v = this.kernel.apply(x, z, h, gx, gz, mask, work.ph);
		cell.height = (float) (water + v / worldHeight);
	}
}
