package raccoonman.reterraforged.server;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.RandomState;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.biome.SwampHeights;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainCategory;
import raccoonman.reterraforged.world.worldgen.cell.terrain.region.SwampBasins;
import raccoonman.reterraforged.world.worldgen.densityfunction.CellSampler;

/**
 * "rtf swamps" (with -Drtf.selftest=true, tools/servertest.sh SWAMPS=1, with MODS=... for other mods' swamps): where
 * the swamp biomes are (issue #28: the swamps of the biome mods along the rivers, too narrow for big swamp structures,
 * and swamps up on high ground). Samples the biome at the surface on a 16 x 16 km grid every 64 blocks from the
 * generator. For each swamp biome (SwampHeights.isSwamp: tagged c:is_swamp or named like one): its share of the land, how much of it lies in
 * river valleys (the river mask under the valley threshold) and on wetland terrain against the land as a whole, its
 * height above the sea, how much of it is wide (the 8 neighbours 64 blocks away the same biome: room for a big
 * structure) and the climate bands it comes from. Writes rtf-swamps.txt, rtf-swamps.png (one pixel a point: sea blue,
 * river valleys grey-green, land pale, swamps dark green, swamps more than 20 blocks above the sea orange) and prints
 * "[RTF swamps] ...".
 */
public final class RTFSwampTest {
	private static final int RADIUS = 8192, STEP = 64;
	// Heightmap.applyRivers: below this river mask the cell is in a river's valley (its erosion and weirdness set)
	private static final float VALLEY = 0.675F;

	private RTFSwampTest() {
	}

	private static final class Stat {
		int samples, valley, wetland, river, high, wide, wideAny;
		long height;
		final Map<String, Integer> bands = new HashMap<>();
	}

	public static String run(MinecraftServer server) {
		try {
			return test(server);
		} catch (Throwable t) {
			RTFCommon.LOGGER.error("[RTF swamps] exception", t);
			return "[RTF swamps] FAIL exception: " + t;
		}
	}

	// land: lighter higher up
	private static int grey(int aboveSea) {
		int v = Math.max(120, Math.min(245, 245 - aboveSea));
		return v << 16 | v << 8 | (v - 10);
	}

	private static String test(MinecraftServer server) throws IOException {
		ServerLevel level = server.overworld();
		RandomState randomState = level.getChunkSource().randomState();
		if (!((Object) randomState instanceof RTFRandomState state) || state.generatorContext() == null) {
			return "[RTF swamps] FAIL not a ReTerraForged world";
		}
		GeneratorContext ctx = state.generatorContext();
		int n = RADIUS * 2 / STEP + 1;
		String[][] grid = new String[n][n];
		boolean[][] isSwamp = new boolean[n][n];
		BufferedImage image = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
		// the same map with a colour per swamp biome: rtf-swamps-biomes.png
		BufferedImage biomes = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
		Cell[][] cells = new Cell[n][n];
		Climate.TargetPoint[][] points = new Climate.TargetPoint[n][n];
		int land = 0, landValley = 0, landWetland = 0, lowland = 0, lowlandValley = 0;
		// Swamp Basins (ClimateSettings.swampBasins): the low land of the basins, how much of it is in valleys, is a swamp, is wide
		SwampBasins basins = ctx.lookup.getHeightmap().swampBasins();
		int basin = 0, basinValley = 0, basinSwamp = 0, basinWide = 0, basinWideAny = 0;
		for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
			int x = -RADIUS + i * STEP, z = -RADIUS + j * STEP;
			Cell cell = new Cell();
			ctx.lookup.applyCell(cell, x, z, false);
			cells[i][j] = cell;
			// the map: sea blue, river valleys grey-green, other land pale by height, swamps dark green
			int h = ctx.levels.scale(cell.height) - ctx.levels.waterLevel;
			image.setRGB(i, j, cell.height <= ctx.levels.water ? 0x3d6fb8 : cell.riverMask < VALLEY ? 0x9fb39a : grey(h));
			biomes.setRGB(i, j, image.getRGB(i, j));
			if (cell.height <= ctx.levels.water) continue;
			int y = ctx.levels.scale(cell.height);
			CellSampler.clearThreadLocalCache();
			Holder<Biome> biome = level.getUncachedNoiseBiome(x >> 2, y >> 2, z >> 2);
			grid[i][j] = biome.unwrapKey().map((k) -> k.identifier().toString()).orElse("?");
			isSwamp[i][j] = SwampHeights.isSwamp(biome);
			if (isSwamp[i][j]) {
				image.setRGB(i, j, h > 20 ? 0xc2410c : 0x2f5d1e);
				biomes.setRGB(i, j, java.awt.Color.HSBtoRGB((grid[i][j].hashCode() & 0xFFF) * 0.618034F % 1.0F, 0.9F, 0.75F));
			}
			points[i][j] = randomState.sampler().sample(x >> 2, y >> 2, z >> 2);
			land++;
			if (cell.riverMask < VALLEY) landValley++;
			if (cell.terrain.isWetland()) landWetland++;
			if (cell.terrain.getCategory() == TerrainCategory.FLATLAND || cell.terrain.getCategory() == TerrainCategory.LOWLAND) {
				lowland++;
				if (cell.riverMask < VALLEY) lowlandValley++;
			}
			if (basins != null && basins.contains(cell)) {
				basin++;
				if (cell.riverMask < VALLEY) basinValley++;
				if (isSwamp[i][j]) basinSwamp++;
			}
		}
		Map<String, Stat> stats = new HashMap<>();
		for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
			if (!isSwamp[i][j]) continue;
			Cell cell = cells[i][j];
			Stat s = stats.computeIfAbsent(grid[i][j], (k) -> new Stat());
			s.samples++;
			int y = ctx.levels.scale(cell.height);
			s.height += y - ctx.levels.waterLevel;
			if (y - ctx.levels.waterLevel > 20) s.high++;
			if (cell.riverMask < VALLEY) s.valley++;
			if (cell.terrain.isWetland()) s.wetland++;
			if (cell.terrain.isRiver()) s.river++;
			boolean wide = i > 0 && j > 0 && i + 1 < n && j + 1 < n;
			for (int di = -1; wide && di <= 1; di++) for (int dj = -1; dj <= 1; dj++) {
				if (!grid[i][j].equals(grid[i + di][j + dj])) { wide = false; break; }
			}
			if (wide) s.wide++;
			// swamps all around, of any kinds: the biome mods' swamps side by side look like one to a player
			boolean wideAny = i > 0 && j > 0 && i + 1 < n && j + 1 < n;
			for (int di = -1; wideAny && di <= 1; di++) for (int dj = -1; dj <= 1; dj++) {
				if (!isSwamp[i + di][j + dj]) { wideAny = false; break; }
			}
			if (wideAny) s.wideAny++;
			if (basins != null && basins.contains(cell)) {
				if (wide) basinWide++;
				if (wideAny) basinWideAny++;
			}
			int[] b = RTFBiomeModTest.bands(points[i][j]);
			s.bands.merge("erosion " + b[3] + " weirdness " + b[4], 1, Integer::sum);
		}
		List<Map.Entry<String, Stat>> sorted = new ArrayList<>(stats.entrySet());
		sorted.sort((a, b) -> b.getValue().samples - a.getValue().samples);
		List<String> lines = new ArrayList<>();
		lines.add(String.format(Locale.ROOT, "# land %d points every %d blocks: in river valleys %.1f%%, wetland terrain %.1f%%", land, STEP, 100.0 * landValley / land, 100.0 * landWetland / land));
		lines.add(String.format(Locale.ROOT, "# flat or rolling land %.1f%% of the land: in river valleys %.1f%%", 100.0 * lowland / land, 100.0 * lowlandValley / Math.max(1, lowland)));
		if (basins != null) {
			lines.add(String.format(Locale.ROOT, "# swamp basins %.1f%% of the land: in river valleys %.1f%%, swamps %.1f%%; of those wide %.1f%%, swamps of any kind all around %.1f%%", 100.0 * basin / land, 100.0 * basinValley / Math.max(1, basin), 100.0 * basinSwamp / Math.max(1, basin), 100.0 * basinWide / Math.max(1, basinSwamp), 100.0 * basinWideAny / Math.max(1, basinSwamp)));
		}
		lines.add("# biome: share of the land, in river valleys, on wetland terrain, on river terrain, mean blocks above the sea, more than 20 above, wide; climate bands (vanilla table: erosion 0-6, weirdness 0-7, valley 4 and 5)");
		StringBuilder top = new StringBuilder();
		int swampTotal = 0, swampValley = 0, swampWide = 0, swampWideAny = 0;
		for (Map.Entry<String, Stat> e : sorted) {
			Stat s = e.getValue();
			swampTotal += s.samples;
			swampValley += s.valley;
			swampWide += s.wide;
			swampWideAny += s.wideAny;
			List<Map.Entry<String, Integer>> bands = new ArrayList<>(s.bands.entrySet());
			bands.sort((a, b) -> b.getValue() - a.getValue());
			StringBuilder bs = new StringBuilder();
			for (int k = 0; k < Math.min(3, bands.size()); k++) bs.append(k > 0 ? ", " : "").append(bands.get(k).getKey()).append(String.format(Locale.ROOT, " %.0f%%", 100.0 * bands.get(k).getValue() / s.samples));
			String line = String.format(Locale.ROOT, "%s: %.2f%% of the land, valleys %.0f%%, wetland %.0f%%, river %.0f%%, +%.0f blocks, high %.0f%%, wide %.0f%%; %s",
				e.getKey(), 100.0 * s.samples / land, 100.0 * s.valley / s.samples, 100.0 * s.wetland / s.samples, 100.0 * s.river / s.samples,
				(double) s.height / s.samples, 100.0 * s.high / s.samples, 100.0 * s.wide / s.samples, bs);
			lines.add(line);
			if (top.length() < 600) top.append(" | ").append(line);
		}
		java.nio.file.Files.write(server.getServerDirectory().resolve("rtf-swamps.txt"), lines);
		ImageIO.write(image, "png", server.getServerDirectory().resolve("rtf-swamps.png").toFile());
		ImageIO.write(biomes, "png", server.getServerDirectory().resolve("rtf-swamps-biomes.png").toFile());
		String r = String.format(Locale.ROOT, "[RTF swamps] %d swamp biomes, %.1f%% of the land; in river valleys %.0f%% (the land %.0f%%), wide %.0f%%, swamps of any kind all around %.0f%%%s",
			stats.size(), 100.0 * swampTotal / land, swampTotal == 0 ? 0 : 100.0 * swampValley / swampTotal, 100.0 * landValley / land,
			swampTotal == 0 ? 0 : 100.0 * swampWide / swampTotal, swampTotal == 0 ? 0 : 100.0 * swampWideAny / swampTotal, top);
		RTFCommon.LOGGER.info(r);
		return r;
	}
}
