package raccoonman.reterraforged.server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.config.PerformanceConfig;
import raccoonman.reterraforged.config.WorldTileSize;
import raccoonman.reterraforged.data.worldgen.preset.settings.FilterSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.server.stress.RTFStressTest;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.cell.terrain.Terrain;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainCategory;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

/**
 * "rtf heightdump &lt;name&gt; &lt;x0&gt; &lt;z0&gt; &lt;size&gt; &lt;step&gt; &lt;seed&gt; &lt;spec&gt;" (with -Drtf.selftest=true,
 * tools/servertest.sh HEIGHTDUMPS=...): the heights of an area of size x size blocks from x0, z0, one sample every step
 * blocks, from the world's tiles as the generator makes them (the world's tile size and border, the erosion and
 * smoothing filters), for offline work on the erosion. Writes heightdump/&lt;name&gt;.npy (float32, shape (C, N, N), C
 * order, [channel][z][x], N = size / step) and heightdump/&lt;name&gt;.json (the area, the preset, the channel names).
 * Channels: raw (the tile's height before its filters), after_drops (raw + the droplets' erosion and deposit, before
 * the smoothing), final (the height the chunks get), all in blocks above the water level ((h - water) x worldHeight);
 * riverMask, continentEdge, terrainRegionEdge, erosionMask (0/1), flags (bits as a float, see FLAG_*), gradient (the
 * Steepness filter's). spec: default (the overworld's preset), set:... or preset:&lt;built-in name&gt; or file:...
 * (RTFStressTest.makePreset). With default and the world's seed it also compares the final height with the world's
 * ground at a few places. Runs on its own thread; the result is the line "[RTF heightdump] &lt;name&gt; ...".
 */
public final class RTFHeightDump {
	public static final String[] CHANNELS = { "raw", "after_drops", "final", "riverMask", "continentEdge", "terrainRegionEdge", "erosionMask", "flags", "gradient" };
	private static final int RAW = 0, AFTER_DROPS = 1, FINAL = 2, RIVER_MASK = 3, CONTINENT_EDGE = 4, REGION_EDGE = 5, EROSION_MASK = 6, FLAGS = 7, GRADIENT = 8;
	public static final int FLAG_MOUNTAIN = 1, FLAG_HILLS = 2, FLAG_RIVER = 4, FLAG_LAKE = 8, FLAG_WETLAND = 16, FLAG_BEACH = 32, FLAG_COAST = 64, FLAG_PLATEAU = 128;
	// 9 channels of 2048 x 2048 floats are 151 MB
	private static final int MAX_N = 2048;
	private static final AtomicBoolean RUNNING = new AtomicBoolean();

	private RTFHeightDump() {
	}

	public static String start(MinecraftServer server, String name, int x0, int z0, int size, int step, long seed, String spec) {
		if (size <= 0 || step <= 0 || size % step != 0) {
			return "[RTF heightdump] " + name + " FAIL size must be a positive multiple of step";
		}
		if (size / step > MAX_N) {
			return "[RTF heightdump] " + name + " FAIL size / step is at most " + MAX_N;
		}
		if (!RUNNING.compareAndSet(false, true)) {
			return "[RTF heightdump] " + name + " FAIL another dump is running";
		}
		Thread thread = new Thread(() -> {
			String r;
			try {
				r = run(server, name, x0, z0, size, step, seed, spec);
			} catch (Throwable t) {
				RTFCommon.LOGGER.error("[RTF heightdump] failed", t);
				r = "[RTF heightdump] " + name + " FAIL " + t;
			} finally {
				RUNNING.set(false);
			}
			RTFCommon.LOGGER.info(r);
		}, "RTF heightdump");
		thread.setDaemon(true);
		thread.start();
		return "[RTF heightdump] " + name + " started";
	}

	private static String run(MinecraftServer server, String name, int x0, int z0, int size, int step, long seed, String spec) throws IOException {
		long t0 = System.nanoTime();
		ServerLevel level = server.overworld();
		GeneratorContext world = (Object) level.getChunkSource().randomState() instanceof RTFRandomState state ? state.generatorContext() : null;
		Preset preset;
		HolderGetter<Noise> noises;
		if (spec.equals("default")) {
			if (world == null) {
				return "[RTF heightdump] " + name + " FAIL default: the overworld is not a ReTerraForged world";
			}
			preset = world.preset;
			noises = world.noiseLookup;
		} else {
			HolderLookup.Provider patch = RTFStressTest.makePreset(spec).buildPatch(server.registries().getAccessForLoading(RegistryLayer.DIMENSIONS));
			preset = patch.lookupOrThrow(RTFRegistries.PRESET).getOrThrow(Preset.KEY).value();
			noises = patch.lookupOrThrow(RTFRegistries.NOISE);
		}
		// the world's tile size: erosion runs per tile, another size erodes differently
		int tileSize = WorldTileSize.current(PerformanceConfig.makeDefault().tileSize());
		GeneratorContext ctx = GeneratorContext.makeCached(preset, noises, GeneratorContext.terrainSeed(seed, preset), tileSize, PerformanceConfig.makeDefault().batchCount(), false);
		try {
			return dump(server, level, world, ctx, preset, name, x0, z0, size, step, seed, spec, tileSize, t0);
		} finally {
			ctx.cache.close();
		}
	}

	private static String dump(MinecraftServer server, ServerLevel level, GeneratorContext world, GeneratorContext ctx, Preset preset, String name, int x0, int z0, int size, int step, long seed, String spec, int tileSize, long t0) throws IOException {
		Levels levels = ctx.levels;
		int n = size / step;
		int plane = n * n;
		float[][] data = new float[CHANNELS.length][plane];
		int tileBlocks = 16 << tileSize;
		int[] tileOfX = new int[n], tileOfZ = new int[n];
		for (int i = 0; i < n; i++) {
			tileOfX[i] = Math.floorDiv(x0 + i * step, tileBlocks);
			tileOfZ[i] = Math.floorDiv(z0 + i * step, tileBlocks);
		}
		int tx0 = tileOfX[0], tx1 = tileOfX[n - 1], tz0 = tileOfZ[0], tz1 = tileOfZ[n - 1];
		List<int[]> tiles = new ArrayList<>();
		for (int tz = tz0; tz <= tz1; tz++) for (int tx = tx0; tx <= tx1; tx++) tiles.add(new int[] { tx, tz });
		// a few tiles at a time: each one's batches use the generation threads, its filters one thread
		int window = Math.max(2, Runtime.getRuntime().availableProcessors());
		for (int start = 0; start < tiles.size(); start += window) {
			List<CompletableFuture<Tile>> futures = new ArrayList<>();
			int end = Math.min(tiles.size(), start + window);
			for (int k = start; k < end; k++) {
				// not through the cache: the tile is read once
				futures.add(ctx.generator.generate(tiles.get(k)[0], tiles.get(k)[1]));
			}
			for (int k = start; k < end; k++) {
				Tile tile = futures.get(k - start).join();
				int tx = tiles.get(k)[0], tz = tiles.get(k)[1];
				for (int iz = 0; iz < n; iz++) {
					if (tileOfZ[iz] != tz) continue;
					int bz = z0 + iz * step;
					for (int ix = 0; ix < n; ix++) {
						if (tileOfX[ix] != tx) continue;
						Cell cell = tile.lookup(x0 + ix * step, bz);
						int i = iz * n + ix;
						// the droplets' change for now, raw is added below
						data[AFTER_DROPS][i] = (cell.heightErosion + cell.sediment) * levels.worldHeight;
						data[FINAL][i] = (cell.height - levels.water) * levels.worldHeight;
						data[RIVER_MASK][i] = cell.riverMask;
						data[CONTINENT_EDGE][i] = cell.continentEdge;
						data[REGION_EDGE][i] = cell.terrainRegionEdge;
						data[EROSION_MASK][i] = cell.erosionMask ? 1.0F : 0.0F;
						data[FLAGS][i] = flags(cell.terrain);
						data[GRADIENT][i] = cell.gradient;
					}
				}
			}
		}
		long t1 = System.nanoTime();
		// raw: the column as a tile's cell before the filters (TileGenerator, Heightmap.applyColumn), rows in parallel
		IntStream.range(0, n).parallel().forEach((iz) -> {
			Cell cell = new Cell();
			int bz = z0 + iz * step;
			for (int ix = 0; ix < n; ix++) {
				ctx.generator.getHeightmap().applyColumn(cell.reset(), x0 + ix * step, bz, false);
				int i = iz * n + ix;
				float raw = (cell.height - levels.water) * levels.worldHeight;
				data[RAW][i] = raw;
				data[AFTER_DROPS][i] += raw;
			}
		});
		long t2 = System.nanoTime();

		Path dir = server.getServerDirectory().resolve("heightdump").toAbsolutePath();
		Files.createDirectories(dir);
		Path npy = dir.resolve(name + ".npy");
		writeNpy(npy, data, n);

		// checks: raw against a tile without its optional filters, the droplets' change, the world's ground
		String rawCheck = checkRaw(ctx, data[RAW], x0, z0, n, step, tileBlocks);
		long land = 0, eroded = 0;
		double dropSum = 0, smoothSum = 0, dropMax = 0;
		for (int i = 0; i < plane; i++) {
			if (data[RAW][i] <= 0.0F) continue;
			land++;
			double d = Math.abs(data[AFTER_DROPS][i] - data[RAW][i]);
			if (d > 0.01) eroded++;
			dropSum += d;
			dropMax = Math.max(dropMax, d);
			smoothSum += Math.abs(data[FINAL][i] - data[AFTER_DROPS][i]);
		}
		long[] bits = new long[8];
		for (int i = 0; i < plane; i++) for (int b = 0; b < 8; b++) if (((int) data[FLAGS][i] & (1 << b)) != 0) bits[b]++;
		String share = String.format(Locale.ROOT, "of all samples: land %.1f %%, mountain %.1f %%, hills %.1f %%, river %.1f %%, lake %.1f %%, wetland %.1f %%, beach %.1f %%, coast %.1f %%, plateau %.1f %%",
			100.0 * land / plane, 100.0 * bits[0] / plane, 100.0 * bits[1] / plane, 100.0 * bits[2] / plane, 100.0 * bits[3] / plane, 100.0 * bits[4] / plane, 100.0 * bits[5] / plane, 100.0 * bits[6] / plane, 100.0 * bits[7] / plane);
		String drops = share + ". " + String.format(Locale.ROOT, "Land %d samples: after_drops differs from raw by > 0.01 at %.1f %%, mean |d| %.3f, max %.2f; mean |final - after_drops| %.3f",
			land, 100.0 * eroded / Math.max(1, land), dropSum / Math.max(1, land), dropMax, smoothSum / Math.max(1, land));
		String worldCheck = world != null && spec.equals("default") && seed == level.getSeed()
			? checkWorld(server, level, data, x0, z0, n, step, levels)
			: "world check skipped (needs spec default and the world's seed " + level.getSeed() + ")";
		long t3 = System.nanoTime();

		JsonObject json = new JsonObject();
		json.addProperty("name", name);
		json.addProperty("x0", x0);
		json.addProperty("z0", z0);
		json.addProperty("size", size);
		json.addProperty("step", step);
		json.addProperty("n", n);
		json.addProperty("seed", seed);
		json.addProperty("terrainSeed", GeneratorContext.terrainSeed(seed, preset));
		json.addProperty("spec", spec);
		json.addProperty("preset", spec.equals("default") ? "world (the overworld's preset)" : spec);
		json.addProperty("seaLevel", preset.world().properties.seaLevel);
		json.addProperty("waterY", levels.waterY);
		// the scale of the heights (Levels: the preset's world height, at most 256)
		json.addProperty("worldHeight", levels.worldHeight);
		json.addProperty("presetWorldHeight", preset.world().properties.worldHeight);
		json.addProperty("worldDepth", preset.world().properties.worldDepth);
		json.addProperty("tileSize", tileSize);
		json.addProperty("tileBlocks", tileBlocks);
		json.addProperty("tileBorderChunks", Math.min(2, Math.max(1, preset.filters().erosion.dropletLifetime / 16)));
		json.addProperty("dtype", "<f4");
		json.addProperty("layout", "[channel][z][x], z = z0 + row * step, x = x0 + column * step");
		json.addProperty("heights", "blocks above the water level: (h - water) * worldHeight; the ground's top block is about at y = waterY + height");
		JsonArray channels = new JsonArray();
		for (String c : CHANNELS) channels.add(c);
		json.add("channels", channels);
		JsonObject flags = new JsonObject();
		flags.addProperty("mountain", FLAG_MOUNTAIN);
		flags.addProperty("hills", FLAG_HILLS);
		flags.addProperty("river", FLAG_RIVER);
		flags.addProperty("lake", FLAG_LAKE);
		flags.addProperty("wetland", FLAG_WETLAND);
		flags.addProperty("beach", FLAG_BEACH);
		flags.addProperty("coast", FLAG_COAST);
		flags.addProperty("plateau", FLAG_PLATEAU);
		json.add("flags", flags);
		FilterSettings.Erosion erosion = preset.filters().erosion;
		JsonObject e = new JsonObject();
		e.addProperty("dropletsPerChunk", erosion.dropletsPerChunk);
		e.addProperty("dropletLifetime", erosion.dropletLifetime);
		e.addProperty("dropletVolume", erosion.dropletVolume);
		e.addProperty("dropletVelocity", erosion.dropletVelocity);
		e.addProperty("erosionRate", erosion.erosionRate);
		e.addProperty("depositeRate", erosion.depositeRate);
		json.add("erosion", e);
		FilterSettings.Smoothing smoothing = preset.filters().smoothing;
		JsonObject s = new JsonObject();
		s.addProperty("iterations", smoothing.iterations);
		s.addProperty("smoothingRadius", smoothing.smoothingRadius);
		s.addProperty("smoothingRate", smoothing.smoothingRate);
		json.add("smoothing", s);
		json.addProperty("tilesMs", (t1 - t0) / 1_000_000);
		json.addProperty("rawMs", (t2 - t1) / 1_000_000);
		json.addProperty("totalMs", (t3 - t0) / 1_000_000);
		json.addProperty("dumpedAt", Instant.now().toString());
		json.addProperty("checkRaw", rawCheck);
		json.addProperty("checkDrops", drops);
		json.addProperty("checkWorld", worldCheck);
		Files.writeString(dir.resolve(name + ".json"), new GsonBuilder().setPrettyPrinting().create().toJson(json));

		return String.format(Locale.ROOT, "[RTF heightdump] %s done (%s, seed %d, %d %d size %d step %d, N %d, %d tiles of %d blocks): tiles %.1f s, raw %.1f s, total %.1f s. %s. %s. %s -> %s",
			name, spec, seed, x0, z0, size, step, n, tiles.size(), tileBlocks, (t1 - t0) / 1e9, (t2 - t1) / 1e9, (t3 - t0) / 1e9, rawCheck, drops, worldCheck, npy);
	}

	// the terrain type's bits; a mix of two terrains (CompositeTerrain "a-b") has the bits of both
	private static float flags(Terrain terrain) {
		int f = 0;
		if (terrain.isMountain()) f |= FLAG_MOUNTAIN;
		if (terrain.isRiver()) f |= FLAG_RIVER;
		if (terrain.isLake()) f |= FLAG_LAKE;
		if (terrain.isWetland()) f |= FLAG_WETLAND;
		if (terrain.getCategory() == TerrainCategory.BEACH || terrain == TerrainType.ISLAND_BEACH) f |= FLAG_BEACH;
		if (terrain.getCategory() == TerrainCategory.COAST) f |= FLAG_COAST;
		for (String part : terrain.getName().split("-")) {
			if (part.equals(TerrainType.HILLS.getName())) f |= FLAG_HILLS;
			if (part.equals(TerrainType.PLATEAU.getName())) f |= FLAG_PLATEAU;
		}
		return f;
	}

	// numpy's .npy format 1.0: magic, version, little-endian header length, a dict padded with spaces to a multiple of
	// 64 bytes ending in a newline, then the data
	private static void writeNpy(Path path, float[][] data, int n) throws IOException {
		String dict = "{'descr': '<f4', 'fortran_order': False, 'shape': (" + data.length + ", " + n + ", " + n + "), }";
		int unpadded = 10 + dict.length() + 1;
		int padding = (64 - unpadded % 64) % 64;
		String header = dict + " ".repeat(padding) + "\n";
		ByteBuffer head = ByteBuffer.allocate(10 + header.length()).order(ByteOrder.LITTLE_ENDIAN);
		head.put((byte) 0x93).put("NUMPY".getBytes(StandardCharsets.US_ASCII)).put((byte) 1).put((byte) 0);
		head.putShort((short) header.length());
		head.put(header.getBytes(StandardCharsets.US_ASCII));
		try (OutputStream out = Files.newOutputStream(path)) {
			out.write(head.array());
			ByteBuffer buf = ByteBuffer.allocate(4 * n).order(ByteOrder.LITTLE_ENDIAN);
			for (float[] channel : data) {
				for (int row = 0; row < n; row++) {
					buf.clear();
					for (int i = row * n, e = i + n; i < e; i++) buf.putFloat(channel[i]);
					out.write(buf.array(), 0, buf.position());
				}
			}
		}
	}

	// raw against the cells of one tile made without the optional filters (erosion, smoothing): the other filters do
	// not change the height, so the two are the same to the bit
	private static String checkRaw(GeneratorContext ctx, float[] raw, int x0, int z0, int n, int step, int tileBlocks) {
		// generateZoomed's tile starts at center - size / 2 (whole blocks at zoom 1: x0, z0), its backing array has the
		// border around it
		Tile tile = ctx.generator.generateZoomed(x0 + tileBlocks / 2.0F, z0 + tileBlocks / 2.0F, 1.0F, false).join();
		int width = tile.getBlockSize().total(), border = tile.getBlockSize().border();
		Cell[] cells = tile.getBacking();
		Levels levels = ctx.levels;
		int points = 0;
		float max = 0.0F;
		for (int iz = 0; iz < n && iz * step < tileBlocks; iz++) {
			for (int ix = 0; ix < n && ix * step < tileBlocks; ix++) {
				Cell cell = cells[(iz * step + border) * width + ix * step + border];
				float h = (cell.height - levels.water) * levels.worldHeight;
				max = Math.max(max, Math.abs(h - raw[iz * n + ix]));
				points++;
			}
		}
		return String.format(Locale.ROOT, "raw vs an unfiltered tile: max |d| %.4f over %d samples", max, points);
	}

	// the final height against the world's ground at the area's 3 x 3 centres, its highest and its steepest sample:
	// the generator's ground without features (getBaseHeight, OCEAN_FLOOR_WG: the first block above the ground) and a
	// built chunk's (the first block from the top that blocks movement and is not a leaf, a log or a fluid)
	private static String checkWorld(MinecraftServer server, ServerLevel level, float[][] data, int x0, int z0, int n, int step, Levels levels) {
		List<int[]> points = new ArrayList<>();
		for (int a = 0; a < 3; a++) for (int b = 0; b < 3; b++) points.add(new int[] { (2 * b + 1) * n / 6, (2 * a + 1) * n / 6 });
		int highest = 0, steepest = 0;
		for (int i = 0; i < n * n; i++) {
			if (data[FINAL][i] > data[FINAL][highest]) highest = i;
			if (data[GRADIENT][i] > data[GRADIENT][steepest]) steepest = i;
		}
		points.add(new int[] { highest % n, highest / n });
		points.add(new int[] { steepest % n, steepest / n });
		var gen = level.getChunkSource().getGenerator();
		var rs = level.getChunkSource().randomState();
		StringBuilder sb = new StringBuilder("world check (x z: final, expected top y, generator top y, chunk top y, relief +-2 samples)");
		int within = 0, chunkWithin = 0;
		for (int[] p : points) {
			int x = x0 + p[0] * step, z = z0 + p[1] * step;
			float h = data[FINAL][p[1] * n + p[0]];
			// the top block of the ground: the noise is 0 at y = 256 h - 0.48 (PresetNoiseRouterData), RTFCoastTest's scale(h)
			int expected = (int) ((h / levels.worldHeight + levels.water) * levels.worldHeight);
			int genTop = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs) - 1;
			// the built chunk's top and, where it is off, the block above it and the top block of the column (a lake, a tree)
			String[] built = server.submit(() -> chunkTop(level, x, z)).join();
			int chunkTop = Integer.parseInt(built[0]);
			float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
			for (int dz = -2; dz <= 2; dz++) for (int dx = -2; dx <= 2; dx++) {
				int qx = p[0] + dx, qz = p[1] + dz;
				if (qx < 0 || qz < 0 || qx >= n || qz >= n) continue;
				lo = Math.min(lo, data[FINAL][qz * n + qx]);
				hi = Math.max(hi, data[FINAL][qz * n + qx]);
			}
			if (Math.abs(genTop - expected) <= 1) within++;
			if (Math.abs(chunkTop - expected) <= 1) chunkWithin++;
			sb.append(String.format(Locale.ROOT, "; %d %d: %.2f, %d, %d (%+d), %d (%+d)%s, %.1f", x, z, h, expected, genTop, genTop - expected, chunkTop, chunkTop - expected,
				Math.abs(chunkTop - expected) > 1 ? " [" + built[1] + "]" : "", hi - lo));
		}
		return String.format(Locale.ROOT, "world check: generator within 1 block at %d/%d, built chunk at %d/%d. ", within, points.size(), chunkWithin, points.size()) + sb;
	}

	private static String[] chunkTop(ServerLevel level, int x, int z) {
		ChunkAccess chunk = level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
		int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
		String surface = name(chunk.getBlockState(pos.below()));
		for (; pos.getY() >= level.getMinBuildHeight(); pos.move(0, -1, 0)) {
			BlockState state = chunk.getBlockState(pos);
			if (state.blocksMotion() && state.getFluidState().isEmpty() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)) {
				return new String[] { String.valueOf(pos.getY()), name(state) + " under " + name(chunk.getBlockState(pos.above())) + ", column top " + surface + " at " + (top - 1) };
			}
		}
		return new String[] { String.valueOf(level.getMinBuildHeight() - 1), "no ground" };
	}

	private static String name(BlockState state) {
		return state.getBlock().builtInRegistryHolder().key().location().getPath();
	}
}
