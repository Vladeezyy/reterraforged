package raccoonman.reterraforged.server;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.data.worldgen.PresetExport;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.Presets;

/**
 * Checks from the FreeTerraForged release test plan that the self test did not have: the Nether and the End far from
 * the centre, dungeon spawners, the biomes' mob lists, presets saved by older versions of the mod, and "rtf heights"
 * (the world's shape as a file, compared by tools/servertest.sh and tools/legacytest.sh between loaders, mod sets, old
 * and new versions of the mod).
 */
public final class RTFWorldChecks {
	// the chunks the self test generates fully ("81 chunks fully generated")
	static final int AREA_MIN = 20, AREA_MAX = 28;

	private RTFWorldChecks() {
	}

	private static ChunkAccess full(ServerLevel level, int cx, int cz) {
		return level.getChunk(cx, cz, ChunkStatus.FULL, true);
	}

	private static int minY(ServerLevel level) {
		return level.dimensionType().minY();
	}

	private static int maxY(ServerLevel level) {
		return level.dimensionType().minY() + level.dimensionType().height() - 1;
	}

	// blocks the overworld's generation places and the Nether's and the End's never do
	private static boolean overworldOnly(BlockState state) {
		// (not the logs and leaves tags: crimson and warped stems are logs)
		return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.WATER) || state.is(Blocks.SAND)
			|| state.is(Blocks.OAK_LOG) || state.is(Blocks.SPRUCE_LOG) || state.is(Blocks.BIRCH_LOG) || state.is(Blocks.OAK_LEAVES) || state.is(Blocks.SPRUCE_LEAVES) || state.is(Blocks.SNOW);
	}

	/**
	 * The nearest dry place where the biome is on the surface: a grid of the generator's ground heights around the centre,
	 * nearest ring first (findClosestBiome3d at a fixed height finds biomes inside mountains or in the air).
	 */
	public static com.mojang.datafixers.util.Pair<BlockPos, Holder<Biome>> surfaceBiome(ServerLevel level, net.minecraft.resources.ResourceKey<Biome> key, int radius) {
		var gen = level.getChunkSource().getGenerator();
		var rs = level.getChunkSource().randomState();
		for (int r = 0; r <= radius; r += 64) {
			for (int x = -r; x <= r; x += 64) for (int z = -r; z <= r; z += 64) {
				if (Math.max(Math.abs(x), Math.abs(z)) != r) continue;
				// inside the biome, not at its edge (a forest's trees thin out towards the plains): the point and at least
				// 6 of the 9 points around it at 24 blocks dry land of that biome
				Holder<Biome> biome = null;
				int h = 0, inside = 0;
				for (int i = 0; i < 9; i++) {
					int px = x + (i % 3 - 1) * 24, pz = z + (i / 3 - 1) * 24;
					int ph = gen.getBaseHeight(px, pz, Heightmap.Types.WORLD_SURFACE_WG, level, rs);
					// dry land only: a lake or a river keeps the biome of its banks (a "forest" under water has no trees)
					Holder<Biome> b = level.getUncachedNoiseBiome(px >> 2, ph >> 2, pz >> 2);
					boolean in = gen.getBaseHeight(px, pz, Heightmap.Types.OCEAN_FLOOR_WG, level, rs) == ph && b.is(key);
					if (i == 4) {
						if (!in) break;
						biome = b;
						h = ph;
					}
					if (in) inside++;
				}
				if (biome != null && inside >= 6) return com.mojang.datafixers.util.Pair.of(new BlockPos(x, h, z), biome);
			}
		}
		return null;
	}

	/** Self test checks, added to its list through {@code check}. */
	static void run(MinecraftServer server, BiConsumer<String, Boolean> check) {
		ServerLevel overworld = server.overworld();
		farDimension(server.getLevel(Level.NETHER), "nether", 5000, true, check);
		farDimension(server.getLevel(Level.END), "end", 6000, false, check);
		dungeons(overworld, check);
		mobs(overworld, check);
		legacyPreset(check);
		reexport(server, check);
	}

	// a preset exported again into the same file (the preset screen's "Export As Datapack" writes exports/<name>.zip)
	// holds only what its settings make now: the erosion decorator switched off leaves no erosion feature behind (#5)
	private static void reexport(MinecraftServer server, BiConsumer<String, Boolean> check) {
		boolean ok;
		String why;
		Path zip = server.getServerDirectory().resolve("rtf-reexport-test.zip");
		try {
			RegistryAccess registries = server.registries().getAccessForLoading(RegistryLayer.DIMENSIONS);
			Files.deleteIfExists(zip);
			PresetExport.toZip(Presets.makeDefault(), "Default", registries, zip);
			Preset noErosion = Presets.makeDefault();
			noErosion.miscellaneous().erosionDecorator = false;
			PresetExport.toZip(noErosion, "Default", registries, zip);
			try (FileSystem fs = FileSystems.newFileSystem(zip)) {
				boolean feature = Files.exists(fs.getPath("data/reterraforged/worldgen/placed_feature/erode.json"));
				boolean preset = Files.readString(fs.getPath("data/reterraforged/reterraforged/worldgen/preset/preset.json")).replace(" ", "").contains("\"erosionDecorator\":false");
				ok = !feature && preset;
				why = "erosion feature " + (feature ? "still there" : "gone") + ", preset says erosion " + (preset ? "off" : "on");
			}
			Files.deleteIfExists(zip);
		} catch (Throwable t) {
			ok = false;
			why = t.toString();
		}
		check.accept("a preset exported again into the same file drops what its settings switched off (" + why + ")", ok);
	}

	// far from the centre (players fly 5 - 10 thousand blocks there): bedrock as usual and nothing of the overworld's
	// generation, such as the mod's erosion or snow features in the wrong dimension
	private static void farDimension(ServerLevel level, String name, int distance, boolean bedrock, BiConsumer<String, Boolean> check) {
		if (level == null) return;
		int cx = distance >> 4, cz = distance >> 4;
		int floorHoles = 0, roofHoles = 0, foreign = 0, solid = 0;
		int floorY = minY(level), roofY = floorY + level.dimensionType().logicalHeight() - 1;
		for (int dx = 0; dx < 2; dx++) for (int dz = 0; dz < 2; dz++) {
			ChunkAccess c = full(level, cx + dx, cz + dz);
			for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
				if (bedrock) {
					if (!c.getBlockState(new BlockPos(x, floorY, z)).is(Blocks.BEDROCK)) floorHoles++;
					if (!c.getBlockState(new BlockPos(x, roofY, z)).is(Blocks.BEDROCK)) roofHoles++;
				}
				for (int y = floorY; y <= Math.min(roofY, maxY(level)); y += 2) {
					BlockState state = c.getBlockState(new BlockPos(x, y, z));
					if (!state.isAir()) solid++;
					if (overworldOnly(state)) {
						if (foreign++ < 3) RTFCommon.LOGGER.info("[RTF selftest] {}: {} at {}, {}, {}", name, state, (cx + dx) * 16 + x, y, (cz + dz) * 16 + z);
					}
				}
			}
		}
		RTFCommon.LOGGER.info("[RTF selftest] {} at {}, {}: {} solid samples, {} of the overworld's blocks, bedrock holes floor {} roof {}", name, distance, distance, solid, foreign, floorHoles, roofHoles);
		if (bedrock) {
			check.accept(name + " bedrock is whole " + distance + " blocks out (holes: floor " + floorHoles + ", roof " + roofHoles + ")", floorHoles == 0 && roofHoles == 0);
		}
		check.accept(name + " has no overworld blocks " + distance + " blocks out (" + foreign + ")", foreign == 0);
	}

	// dungeons with their spawners are a vanilla feature the preset's biomes must keep: every overworld biome lists both
	// of vanilla's dungeon features ("rtf dungeons N" counts the spawners over N x N chunks; they are rare, about one in
	// a hundred chunks)
	private static void dungeons(ServerLevel level, BiConsumer<String, Boolean> check) {
		List<String> missing = new ArrayList<>();
		for (Holder<Biome> biome : level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes()) {
			List<String> features = biome.value().getGenerationSettings().features().stream().flatMap((set) -> set.stream()).map(Holder::getRegisteredName).toList();
			if (!features.contains("minecraft:monster_room") || !features.contains("minecraft:monster_room_deep")) missing.add(biome.getRegisteredName());
		}
		check.accept("every biome keeps vanilla's dungeons " + (missing.size() > 6 ? missing.subList(0, 6) + " and " + (missing.size() - 6) + " more" : missing), missing.isEmpty());
	}

	public static String countDungeons(MinecraftServer server, int size) {
		ServerLevel level = server.overworld();
		int spawners = 0;
		int bottom = minY(level), top = maxY(level);
		long t0 = System.nanoTime();
		for (int cx = AREA_MIN; cx < AREA_MIN + size; cx++) for (int cz = AREA_MIN; cz < AREA_MIN + size; cz++) {
			ChunkAccess c = full(level, cx, cz);
			for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
				int surface = c.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
				for (int y = bottom; y <= Math.min(top, surface); y++) {
					if (c.getBlockState(new BlockPos(x, y, z)).is(Blocks.SPAWNER)) spawners++;
				}
			}
		}
		String r = "[RTF dungeons] " + spawners + " spawners in " + (size * size) + " chunks (" + (System.nanoTime() - t0) / 1_000_000_000L + " s)";
		RTFCommon.LOGGER.info(r);
		return r;
	}

	// the preset rewrites the vanilla biomes: their mobs must stay
	private static void mobs(ServerLevel level, BiConsumer<String, Boolean> check) {
		List<String> empty = new ArrayList<>();
		var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
		for (var key : List.of(Biomes.PLAINS, Biomes.FOREST, Biomes.TAIGA, Biomes.DESERT)) {
			Biome biome = biomes.getOrThrow(key).value();
			if (biome.getMobSettings().getMobs(MobCategory.MONSTER).isEmpty()) empty.add(key.toString() + " monsters");
			if (key != Biomes.DESERT && biome.getMobSettings().getMobs(MobCategory.CREATURE).isEmpty()) empty.add(key.toString() + " animals");
		}
		check.accept("the preset's biomes keep their mobs " + empty, empty.isEmpty());
	}

	// a preset file saved by an older version of the mod: settings added later are missing and get their defaults, a
	// setting that no longer exists (originalGeneration, 0.0.11 to 0.0.16) is ignored
	private static void legacyPreset(BiConsumer<String, Boolean> check) {
		boolean ok;
		String why;
		try {
			JsonObject json = Preset.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, Presets.makeDefault()).getOrThrow().getAsJsonObject();
			for (String later : List.of("surface", "caves", "island", "fakeWaterBiomes")) json.remove(later);
			json.getAsJsonObject("miscellaneous").addProperty("originalGeneration", false);
			json.getAsJsonObject("world").getAsJsonObject("properties").remove("lavaLevel");
			Preset old = Preset.DIRECT_CODEC.parse(JsonOps.INSTANCE, (JsonElement) json).getOrThrow();
			ok = old.world().properties.lavaLevel == -54;
			why = "lavaLevel " + old.world().properties.lavaLevel;
		} catch (Throwable t) {
			ok = false;
			why = t.toString();
		}
		check.accept("a preset saved by an older version loads with the defaults (" + why + ")", ok);
	}

	/**
	 * "rtf heights [name]": the world's shape in run/.../rtf-heights[-name].txt: the generator's ground height and the
	 * biome at a 33 x 33 grid over 4 x 4 km (no chunks needed), then the top of the ground (no plants, trees or water) of
	 * the 81 chunks the self test generates, at 4 columns each. Chunks already saved, as by an older version of the mod,
	 * are read as they are.
	 */
	public static String heights(MinecraftServer server, String name) {
		ServerLevel level = server.overworld();
		var gen = level.getChunkSource().getGenerator();
		var rs = level.getChunkSource().randomState();
		List<String> lines = new ArrayList<>();
		lines.add("# seed " + level.getSeed());
		for (int x = -2048; x <= 2048; x += 128) {
			for (int z = -2048; z <= 2048; z += 128) {
				int h = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
				Holder<Biome> biome = level.getUncachedNoiseBiome(x >> 2, h >> 2, z >> 2);
				lines.add("base " + x + " " + z + " " + h + " " + biome.getRegisteredName());
			}
		}
		for (int cx = AREA_MIN; cx <= AREA_MAX; cx++) for (int cz = AREA_MIN; cz <= AREA_MAX; cz++) {
			ChunkAccess c = full(level, cx, cz);
			for (int[] p : new int[][] { { 2, 2 }, { 13, 2 }, { 2, 13 }, { 13, 13 } }) {
				int y = c.getHeight(Heightmap.Types.WORLD_SURFACE, p[0], p[1]);
				while (y > minY(level)) {
					BlockState state = c.getBlockState(new BlockPos(p[0], y, p[1]));
					if (!state.isAir() && state.getFluidState().isEmpty() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS) && !state.canBeReplaced()) break;
					y--;
				}
				lines.add("ground " + (cx * 16 + p[0]) + " " + (cz * 16 + p[1]) + " " + y);
			}
		}
		Path out = server.getServerDirectory().resolve("rtf-heights" + (name.isEmpty() ? "" : "-" + name) + ".txt").toAbsolutePath();
		try {
			Files.write(out, lines);
		} catch (IOException e) {
			return "[RTF heights] FAIL writing " + out + ": " + e;
		}
		String r = "[RTF heights] " + (lines.size() - 1) + " samples in " + out;
		RTFCommon.LOGGER.info(r);
		return r;
	}

	/**
	 * "rtf extremes": what a preset with extreme world levels (tools/servertest.sh PRESET=set:...seaLevel=...) must still
	 * give: whole bedrock and the world generated; reports how much lava and water there is.
	 */
	public static String extremes(MinecraftServer server) {
		ServerLevel level = server.overworld();
		// the generator's sea level: Level.getSeaLevel() is 63 on 1.21.1 whatever the world's settings
		int seaLevel = level.getChunkSource().getGenerator().getSeaLevel();
		int holes = 0, lava = 0, water = 0, top = 0;
		for (int cx = -2; cx <= 2; cx++) for (int cz = -2; cz <= 2; cz++) {
			ChunkAccess c = full(level, cx, cz);
			for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
				if (!c.getBlockState(new BlockPos(x, minY(level), z)).is(Blocks.BEDROCK)) holes++;
				for (int y = minY(level); y <= maxY(level); y += 4) {
					BlockState state = c.getBlockState(new BlockPos(x, y, z));
					if (state.is(Blocks.LAVA)) lava++;
					else if (state.is(Blocks.WATER)) water++;
				}
				if (!c.getBlockState(new BlockPos(x, maxY(level), z)).isAir()) top++;
			}
		}
		boolean ok = holes == 0;
		String r = String.format("[RTF extremes] %s: sea level %d, bedrock holes %d, lava %d, water %d samples, blocks at the world top %d (25 chunks)", ok ? "PASS" : "FAIL", seaLevel, holes, lava, water, top);
		RTFCommon.LOGGER.info(r);
		return r;
	}
}
