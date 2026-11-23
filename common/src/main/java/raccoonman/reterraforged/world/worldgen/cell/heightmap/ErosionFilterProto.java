/*
 * PROTOTYPE, branch proto/erosion-filter only: for in-game comparison screenshots, not for a release.
 * The filter itself is ErosionFilterKernel (MPL-2.0, see its header).
 */
package raccoonman.reterraforged.world.worldgen.cell.heightmap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.terrain.Terrain;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainCategory;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;

/**
 * -Drtf.erosionfilter=&lt;strength multiplier&gt; (1 = the author's 0.22) or -Drtf.erosionfilter.preset=subtle|bold:
 * the filter is added to the height of Heightmap.applyTerrain (before the rivers, so the tiles, WorldLookup and the
 * preview get it). Without them nothing changes. The gully directions come from central differences of the unfiltered
 * height at +-step blocks (rtf.erosionfilter.step, 28; about the gradient of a blurred height: the ripples of the raw
 * height would scramble a per-block gradient). The strength mask: the share of mountain or hills columns among the
 * centre and the 4 gradient samples, times a ramp from 2 to 24 blocks above the water. The filter's own settings are
 * ErosionFilterKernel's (rtf.erosionfilter.&lt;name&gt;, over the preset's).
 */
public final class ErosionFilterProto {
	// "Ridges: subtle" and "Ridges: bold", picked with tools/erosionfilter/RidgeTune.java on Towering Peaks: wide ridges
	// (scale 300, 3 octaves: no needles of the small octaves), rounded ridge tops, the fade target up to 200 blocks (with
	// 140 the whole upper mountain was carved to its full depth), anchor 1 (the ridges of the upper slopes stay at the
	// input height: with the author's offset the shoulders sank about twice as much as the summit, which stood up as a
	// horn), the strength down to 0.15 from 110 to 170 blocks (the gullies around a summit run in all directions and
	// still cut it into horns), rises kept at 0.3 and at most 8 blocks; the two differ only in the strength, as two
	// positions of one slider
	private static final String[] RIDGES = { "step", "56", "scale", "300", "octaves", "3", "ridgeRounding", "0.5", "detail", "1.0", "peak", "200", "anchor", "1", "topLo", "110", "topHi", "170", "topCut", "0.85", "raise", "0.3", "maxRaise", "8" };
	public static final Map<String, String> SUBTLE = ridges("0.45");
	public static final Map<String, String> BOLD = ridges("0.75");

	private static Map<String, String> ridges(String strength) {
		Map<String, String> map = new java.util.HashMap<>();
		for (int i = 0; i < RIDGES.length; i += 2) map.put(RIDGES[i], RIDGES[i + 1]);
		map.put("strength", strength);
		return Map.copyOf(map);
	}

	public static final float STRENGTH;
	public static final boolean ENABLED;
	private static final float STEP;
	private static final ErosionFilterKernel KERNEL;
	private static final double RAMP_LO = 2.0, RAMP_HI = 24.0;

	static {
		String name = System.getProperty("rtf.erosionfilter.preset", "");
		Map<String, String> preset = name.equals("subtle") ? SUBTLE : name.equals("bold") ? BOLD : Map.of();
		Function<String, String> lookup = (key) -> {
			String v = System.getProperty(key);
			return v != null ? v : preset.get(key.substring("rtf.erosionfilter.".length()));
		};
		STRENGTH = Float.parseFloat(System.getProperty("rtf.erosionfilter", preset.getOrDefault("strength", "0")));
		ENABLED = STRENGTH > 0.0F;
		String step = lookup.apply("rtf.erosionfilter.step");
		STEP = Float.parseFloat(step != null ? step : "28");
		KERNEL = new ErosionFilterKernel(STRENGTH, lookup);
		if (ENABLED) {
			raccoonman.reterraforged.RTFCommon.LOGGER.warn("[RTF] PROTOTYPE erosion filter on ({}): x{}, gradient step {}, {}", name.isEmpty() ? "properties" : name, STRENGTH, STEP, KERNEL.describe());
		}
	}

	private static final ConcurrentHashMap<Terrain, Boolean> MASKED = new ConcurrentHashMap<>();
	private static final ThreadLocal<Work> WORK = ThreadLocal.withInitial(Work::new);

	private static final class Work {
		final Cell side = new Cell();
		final double[] ph = new double[4];
	}

	private ErosionFilterProto() {
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
		double mask = inMask / 5.0 * ErosionFilterKernel.smooth(RAMP_LO, RAMP_HI, h);
		double gx = (hxp - hxm) * worldHeight / (2.0 * STEP);
		double gz = (hzp - hzm) * worldHeight / (2.0 * STEP);
		double v = KERNEL.apply(x, z, h, gx, gz, mask, work.ph);
		cell.height = (float) (water + v / worldHeight);
	}
}
