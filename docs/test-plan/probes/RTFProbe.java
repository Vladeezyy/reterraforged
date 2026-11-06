package raccoonman.reterraforged.server;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Size;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.generation.TileGenerator;

/** Scratch-only probes for the test plan: "rtf probe far|seam|biome|spawn|regen|dim <id>". Never part of the mod. */
public final class RTFProbe {

	public static String run(MinecraftServer server, String args) {
		String[] a = args.trim().split("\\s+");
		Thread t = new Thread(() -> {
			try {
				String r = switch (a[0]) {
					case "far" -> far(server);
					case "seam" -> seam(server);
					case "biome" -> biome(server);
					case "spawn" -> spawn(server);
					case "regen" -> regen();
					case "dim" -> dim(server, a[1], a.length > 2 ? Integer.parseInt(a[2]) : 0, a.length > 3 ? Integer.parseInt(a[3]) : 0);
					case "swampfloor" -> swampfloor(server, Integer.parseInt(a[1]), Integer.parseInt(a[2]));
					case "islands" -> islands(server);
					case "rivers" -> rivers(server);
					case "swamplist" -> swamplist(server);
					default -> "unknown";
				};
				RTFCommon.LOGGER.info("[RTF probe] {} DONE {}", a[0], r);
			} catch (Throwable e) {
				RTFCommon.LOGGER.error("[RTF probe] " + a[0] + " DONE FAILED " + e, e);
			}
		}, "RTF probe");
		t.setDaemon(true);
		t.start();
		return "[RTF probe] started " + args;
	}

	private static GeneratorContext ctx(ServerLevel level) {
		return ((RTFRandomState) (Object) level.getChunkSource().randomState()).generatorContext();
	}

	// the terrain along lines at growing distances: float coordinates lose precision far out
	private static String far(MinecraftServer server) {
		GeneratorContext ctx = ctx(server.overworld());
		var hm = ctx.generator.getHeightmap();
		int wh = ctx.levels.worldHeight;
		int[] bases = { 0, 100000, 1000000, 4000000, 8388608, 16777216, 20000000, 29000000 };
		StringBuilder out = new StringBuilder();
		for (int base : bases) {
			int n = 2048;
			float[] h = new float[n];
			int zero = 0, nan = 0, land = 0;
			for (int i = 0; i < n; i++) {
				Cell c = new Cell();
				hm.apply(c, (float) (base + i), (float) base, false);
				h[i] = c.height * wh;
				if (Float.isNaN(c.height) || Float.isInfinite(c.height)) nan++;
				if (c.height > ctx.levels.water) land++;
			}
			double d1 = 0, max = 0, d2 = 0;
			for (int i = 1; i < n - 1; i++) {
				double d = Math.abs(h[i] - h[i - 1]);
				if (d == 0) zero++;
				d1 += d;
				max = Math.max(max, d);
				d2 += Math.abs(h[i + 1] - 2 * h[i] + h[i - 1]);
			}
			String line = String.format(Locale.ROOT, "base=%d meanStep=%.4f maxStep=%.3f roughness=%.4f identicalNeighbours=%.1f%% land=%.0f%% nonFinite=%d", base, d1 / (n - 2), max, d2 / (n - 2), 100.0 * zero / (n - 2), 100.0 * land / n, nan);
			RTFCommon.LOGGER.info("[RTF probe] far {}", line);
			out.append(line).append(" | ");
		}
		return out.toString();
	}

	// the tile edges: height steps across them against inside a tile, and how far two neighbouring tiles disagree about
	// the same place (the left tile's border cells against the right tile's own)
	private static String seam(MinecraftServer server) {
		GeneratorContext ctx = ctx(server.overworld());
		int wh = ctx.levels.worldHeight;
		float water = ctx.levels.water;
		double seamSum = 0, innerSum = 0, seamMax = 0, innerMax = 0, disagreeSum = 0, disagreeMax = 0;
		long seamN = 0, innerN = 0, seam2 = 0, inner2 = 0, disagreeN = 0, disagree1 = 0;
		int tiles = 40;
		for (int k = 0; k < tiles; k++) {
			int tx = 40 + k * 9, tz = -35 + k * 5;
			Tile a = ctx.cache.provide(tx, tz), b = ctx.cache.provide(tx + 1, tz);
			Size bs = a.getBlockSize();
			int size = bs.size();
			int bx = (tx + 1) * size, baseZ = tz * size;
			for (int dz = 0; dz < size; dz++) {
				int z = baseZ + dz;
				Cell l = a.lookup(bx - 1, z), r = b.lookup(bx, z);
				if (l.height > water && r.height > water) {
					double d = Math.abs(l.height - r.height) * wh;
					seamSum += d; seamMax = Math.max(seamMax, d); seamN++;
					if (d >= 2) seam2++;
				}
				for (int inner : new int[] { 16, 48, 80, 112 }) {
					Cell p = a.lookup(bx - inner - 1, z), q = a.lookup(bx - inner, z);
					if (p.height > water && q.height > water) {
						double d = Math.abs(p.height - q.height) * wh;
						innerSum += d; innerMax = Math.max(innerMax, d); innerN++;
						if (d >= 2) inner2++;
					}
				}
				Cell overlap = a.getCellRaw(bs.border() + size, bs.border() + dz);
				if (r.height > water) {
					double d = Math.abs(overlap.height - r.height) * wh;
					disagreeSum += d; disagreeMax = Math.max(disagreeMax, d); disagreeN++;
					if (d >= 1) disagree1++;
				}
			}
		}
		return String.format(Locale.ROOT, "tileBlocks=%d border=%d seamMean=%.4f seamMax=%.2f seamSteps>=2=%.3f%% innerMean=%.4f innerMax=%.2f innerSteps>=2=%.3f%% overlapDisagreeMean=%.4f overlapDisagreeMax=%.2f overlapDisagree>=1block=%.2f%% (n seam %d inner %d overlap %d)",
			ctx.cache == null ? 0 : ctx.cache.provide(40, -35).getBlockSize().size(), ctx.cache.provide(40, -35).getBlockSize().border(), seamSum / Math.max(1, seamN), seamMax, 100.0 * seam2 / Math.max(1, seamN), innerSum / Math.max(1, innerN), innerMax, 100.0 * inner2 / Math.max(1, innerN), disagreeSum / Math.max(1, disagreeN), disagreeMax, 100.0 * disagree1 / Math.max(1, disagreeN), seamN, innerN, disagreeN);
	}

	// the biome a chunk stored against what the biome source says there (surface rules, /locate, structures)
	private static String biome(MinecraftServer server) {
		ServerLevel level = server.overworld();
		var source = level.getChunkSource().getGenerator().getBiomeSource();
		Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
		int[][] areas = { { 20, 20 }, { 300, -200 }, { -500, 700 }, { 900, 900 } };
		long total = 0, differ = 0;
		Map<String, Integer> pairs = new HashMap<>();
		for (int[] area : areas) {
			for (int cx = area[0]; cx < area[0] + 12; cx++) for (int cz = area[1]; cz < area[1] + 12; cz++) {
				ChunkAccess c = level.getChunk(cx, cz, ChunkStatus.FULL, true);
				for (int qx = 0; qx < 4; qx++) for (int qz = 0; qz < 4; qz++) {
					int x = (cx << 4) + qx * 4, z = (cz << 4) + qz * 4;
					int top = c.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x & 15, z & 15);
					int qy = QuartPos.fromBlock(top);
					Holder<Biome> stored = c.getNoiseBiome(QuartPos.fromBlock(x), qy, QuartPos.fromBlock(z));
					Holder<Biome> asked = source.getNoiseBiome(QuartPos.fromBlock(x), qy, QuartPos.fromBlock(z), sampler);
					total++;
					if (!stored.equals(asked)) {
						differ++;
						pairs.merge(stored.getRegisteredName() + " != " + asked.getRegisteredName(), 1, Integer::sum);
					}
				}
			}
		}
		List<Map.Entry<String, Integer>> top = pairs.entrySet().stream().sorted((p, q) -> q.getValue() - p.getValue()).limit(8).toList();
		return String.format(Locale.ROOT, "stored vs source: %d of %d differ (%.2f%%), top %s", differ, total, 100.0 * differ / Math.max(1, total), top);
	}

	private static String spawn(MinecraftServer server) {
		ServerLevel level = server.overworld();
		BlockPos spawn = level.getSharedSpawnPos();
		level.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4, ChunkStatus.FULL, true);
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ());
		var below = level.getBlockState(new BlockPos(spawn.getX(), top - 1, spawn.getZ()));
		GeneratorContext ctx = ctx(level);
		String terrain = "-";
		if (ctx != null) {
			Cell c = new Cell();
			ctx.generator.getHeightmap().apply(c, spawn.getX(), spawn.getZ(), false);
			terrain = c.terrain.getName() + " continentEdge=" + c.continentEdge;
		}
		return String.format(Locale.ROOT, "spawn %d %d %d top block %s water=%s terrain %s biome %s", spawn.getX(), top, spawn.getZ(), below.getBlock().getDescriptionId(), !below.getFluidState().isEmpty(), terrain, level.getBiome(new BlockPos(spawn.getX(), top, spawn.getZ())).getRegisteredName());
	}

	private static String regen() {
		long g = TileGenerator.GENERATED.get();
		int u = TileGenerator.UNIQUE.size();
		Runtime rt = Runtime.getRuntime();
		return String.format(Locale.ROOT, "tiles generated %d, unique places %d, regenerated %d (%.1f%%), heap %d MB", g, u, g - u, 100.0 * (g - u) / Math.max(1, u), (rt.totalMemory() - rt.freeMemory()) >> 20);
	}

	// the floor of swamps: the same block at the same place in every chunk means local coordinates in the material noise
	private static String swampfloor(MinecraftServer server, int centerX, int centerZ) {
		ServerLevel level = server.overworld();
		BlockPos c = new BlockPos(centerX, 0, centerZ);
		int cx0 = (c.getX() >> 4) - 8, cz0 = (c.getZ() >> 4) - 8;
		Map<Integer, Map<String, Integer>> perLocal = new HashMap<>();
		Map<String, Integer> overall = new TreeMap<>();
		long columns = 0;
		for (int cx = cx0; cx < cx0 + 16; cx++) for (int cz = cz0; cz < cz0 + 16; cz++) {
			ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, true);
			for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
				int y = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
				BlockPos pos = new BlockPos((cx << 4) + x, y, (cz << 4) + z);
				if (!chunk.getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(y), QuartPos.fromBlock(pos.getZ())).is(net.minecraft.world.level.biome.Biomes.SWAMP)) continue;
				if (chunk.getBlockState(pos.above()).getFluidState().isEmpty()) continue;
				String block = chunk.getBlockState(pos).getBlock().getDescriptionId().replace("block.minecraft.", "");
				perLocal.computeIfAbsent(x * 16 + z, (k) -> new HashMap<>()).merge(block, 1, Integer::sum);
				overall.merge(block, 1, Integer::sum);
				columns++;
			}
		}
		double sumShare = 0;
		int positions = 0;
		for (Map<String, Integer> m : perLocal.values()) {
			int total = m.values().stream().mapToInt(Integer::intValue).sum();
			if (total < 4) continue;
			sumShare += (double) m.values().stream().mapToInt(Integer::intValue).max().orElse(0) / total;
			positions++;
		}
		int top = overall.values().stream().mapToInt(Integer::intValue).max().orElse(0);
		return String.format(Locale.ROOT, "swamp at %d %d: %d flooded swamp columns, blocks %s; most common block overall %.1f%%, most common block at the same place in a chunk (mean over %d places) %.1f%%", c.getX(), c.getZ(), columns, overall, 100.0 * top / Math.max(1, columns), positions, 100.0 * sumShare / Math.max(1, positions));
	}

	// how long one river map takes: the generation holds the river cache's only lock
	private static String rivers(MinecraftServer server) {
		var continent = ctx(server.overworld()).generator.getHeightmap().continent();
		double sum = 0, max = 0;
		int n = 40;
		for (int i = 1; i <= n; i++) {
			long t0 = System.nanoTime();
			continent.getRivermap(12000 * i, -9000 * i);
			double ms = (System.nanoTime() - t0) / 1.0E6;
			sum += ms;
			max = Math.max(max, ms);
		}
		return String.format(Locale.ROOT, "%d river maps: mean %.1f ms, max %.1f ms", n, sum / n, max);
	}

	// which biomes Highest Swamps treats as swamps: by the tag or by a part of the name
	private static String swamplist(MinecraftServer server) {
		var tag = net.minecraft.tags.TagKey.create(Registries.BIOME, ResourceLocation.parse("c:is_swamp"));
		var registry = server.overworld().registryAccess().registryOrThrow(Registries.BIOME);
		StringBuilder byTag = new StringBuilder(), byName = new StringBuilder();
		registry.holders().forEach((h) -> {
			if (!raccoonman.reterraforged.world.worldgen.biome.SwampHeights.isSwamp(h)) return;
			(h.is(tag) ? byTag : byName).append(h.unwrapKey().get().location()).append(' ');
		});
		return "by tag c:is_swamp: " + byTag + "| by name only: " + byName;
	}

	private static GeneratorContext uncached(MinecraftServer server, raccoonman.reterraforged.data.worldgen.preset.settings.Preset preset, int seed) {
		var patch = preset.buildPatch(server.registries().getAccessForLoading(net.minecraft.server.RegistryLayer.DIMENSIONS));
		var patched = patch.lookupOrThrow(raccoonman.reterraforged.registries.RTFRegistries.PRESET).getOrThrow(raccoonman.reterraforged.data.worldgen.preset.settings.Preset.KEY).value();
		return GeneratorContext.makeUncached(patched, patch.lookupOrThrow(raccoonman.reterraforged.registries.RTFRegistries.NOISE), seed, 4, 0, 4);
	}

	private static boolean island(Cell c) {
		return c.terrain == TerrainType.ISLAND || c.terrain == TerrainType.ISLAND_BEACH || c.terrain == TerrainType.ISLAND_MOUNTAINS;
	}

	// #38: the islands of two seeds, of two horizontal scales, and how much of their beach is above the water
	private static String islands(MinecraftServer server) {
		var base = raccoonman.reterraforged.data.worldgen.preset.settings.Presets.makeDefault();
		base.island().enableArchipelago = true;
		var wide = base.copy();
		wide.island().islandHorizontalScale = 3.0F;
		var points = base.copy();
		points.world().controlPoints.islandCoast = 0.1F;
		points.world().controlPoints.islandInland = 0.05F;
		GeneratorContext a = uncached(server, base, 1), b = uncached(server, base, 2), c = uncached(server, wide, 1), d = uncached(server, points, 1);
		int step = 16, half = 8000;
		long ia = 0, ib = 0, ic = 0, both = 0, either = 0, oceanBoth = 0, sameAC = 0, eitherAC = 0;
		long beach = 0, beachAbove = 0, heightsDiffer = 0, compared = 0;
		var off = base.copy();
		off.world().properties.generationFixes = false;
		GeneratorContext e = uncached(server, off, 1), f = uncached(server, off, 2);
		long offBoth = 0, offEither = 0, offA = 0, offScale = 0, biomesDiffer = 0, offBeach = 0, offBeachAbove = 0;
		var wideOff = off.copy();
		wideOff.island().islandHorizontalScale = 3.0F;
		GeneratorContext g = uncached(server, wideOff, 1);
		for (int x = -half; x < half; x += step) for (int z = -half; z < half; z += step) {
			Cell ca = new Cell(), cb = new Cell(), cc = new Cell(), cd = new Cell();
			a.generator.getHeightmap().applyTerrain(ca, x, z);
			b.generator.getHeightmap().applyTerrain(cb, x, z);
			c.generator.getHeightmap().applyTerrain(cc, x, z);
			d.generator.getHeightmap().applyTerrain(cd, x, z);
			boolean la = island(ca), lb = island(cb), lc = island(cc);
			if (la) ia++;
			if (lb) ib++;
			if (lc) ic++;
			boolean oa = la || ca.terrain.isDeepOcean() || ca.terrain.isShallowOcean();
			boolean ob = lb || cb.terrain.isDeepOcean() || cb.terrain.isShallowOcean();
			if (oa && ob) {
				oceanBoth++;
				if (la && lb) both++;
				if (la || lb) either++;
			}
			if (la && lc) sameAC++;
			if (la || lc) eitherAC++;
			if (ca.terrain == TerrainType.ISLAND_BEACH) {
				beach++;
				if (ca.height > a.levels.water) beachAbove++;
			}
			Cell ce = new Cell(), cf = new Cell(), cg = new Cell();
			e.generator.getHeightmap().applyTerrain(ce, x, z);
			f.generator.getHeightmap().applyTerrain(cf, x, z);
			g.generator.getHeightmap().applyTerrain(cg, x, z);
			if (island(ce)) offA++;
			if (island(cg)) offScale++;
			if (island(ce) && island(cf)) offBoth++;
			if (island(ce) || island(cf)) offEither++;
			if (ce.terrain == TerrainType.ISLAND_BEACH) { offBeach++; if (ce.height > e.levels.water) offBeachAbove++; }
			if (la) {
				compared++;
				if (ca.height != cd.height || ca.terrain != cd.terrain) heightsDiffer++;
				if (raccoonman.reterraforged.world.worldgen.densityfunction.CellSampler.Field.CONTINENT.read(ca, a.generator.getHeightmap()) != raccoonman.reterraforged.world.worldgen.densityfunction.CellSampler.Field.CONTINENT.read(cd, d.generator.getHeightmap())) biomesDiffer++;
			}
		}
		String offs = String.format(Locale.ROOT, " | OFF: island cells %d scale3 %d; seeds overlap %.1f%% (%d/%d); beach above water %.1f%% (%d/%d) | island cells with another continentalness when Island Coast 0.1, Inland 0.05: %d/%d", offA, offScale, offEither == 0 ? 0 : 100.0 * offBoth / offEither, offBoth, offEither, offBeach == 0 ? 0 : 100.0 * offBeachAbove / offBeach, offBeachAbove, offBeach, biomesDiffer, compared);
		return offs + String.format(Locale.ROOT, "island cells seed1 %d seed2 %d scale3 %d; where both seeds have open water or an island the islands of the two seeds overlap %.1f%% (%d/%d); seed1 scale1 vs scale3 overlap %.1f%%; ISLAND_BEACH above the water %.1f%% of %d; island cells changed by islandCoast/islandInland %d of %d", ia, ib, ic, 100.0 * both / Math.max(1, either), both, either, 100.0 * sameAC / Math.max(1, eitherAC), 100.0 * beachAbove / Math.max(1, beach), beach, heightsDiffer, compared);
		// followed by what generationFixes=false still gives, and the continent field of Island Coast and Inland
	}

	private static String dim(MinecraftServer server, String id, int cx0, int cz0) {
		ServerLevel l = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(id)));
		if (l == null) return "no dimension " + id;
		RTFRandomState rs = (RTFRandomState) (Object) l.getChunkSource().randomState();
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		long sum = 0, n = 0;
		Map<String, Integer> tops = new TreeMap<>();
		for (int cx = cx0; cx < cx0 + 6; cx++) for (int cz = cz0; cz < cz0 + 6; cz++) {
			ChunkAccess c = l.getChunk(cx, cz, ChunkStatus.FULL, true);
			for (int x = 0; x < 16; x += 4) for (int z = 0; z < 16; z += 4) {
				int h = c.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
				min = Math.min(min, h); max = Math.max(max, h); sum += h; n++;
				tops.merge(c.getBlockState(new BlockPos(x, h, z)).getBlock().getDescriptionId(), 1, Integer::sum);
			}
		}
		return String.format(Locale.ROOT, "%s rtfFunctions=%s context=%s heights %d..%d mean %.1f tops %s", id, rs.hasReTerraForgedFunctions(), rs.generatorContext() != null, min, max, (double) sum / Math.max(1, n), tops);
	}
}
