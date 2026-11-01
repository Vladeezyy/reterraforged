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
import raccoonman.reterraforged.world.worldgen.cell.heightmap.WorldLookup;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.data.worldgen.PresetExport;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
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
		strata(overworld, check);
		snowLine(overworld, check);
		deepslate(overworld, check);
		swampTrees(overworld, check);
		warmRainforestOceans(overworld, check);
	}

	// a preset with Warm Oceans by Rainforests (#35, tools/servertest.sh PRESET=set:climate().warmRainforestOceans=true):
	// the sea of the tropical rainforest regions is warm ocean; over 32 x 32 km every 256 blocks, no chunks generated
	private static void warmRainforestOceans(ServerLevel level, BiConsumer<String, Boolean> check) {
		if (!((Object) level.getChunkSource().randomState() instanceof RTFRandomState s) || s.preset() == null || s.generatorContext() == null || !s.preset().climate().warmRainforestOceans) return;
		var lookup = s.generatorContext().lookup;
		int sea = level.getSeaLevel(), ocean = 0, warm = 0, rainforest = 0, seas = 0;
		java.util.Map<String, Integer> other = new java.util.TreeMap<>();
		for (int x = -16384; x <= 16384; x += 256) for (int z = -16384; z <= 16384; z += 256) {
			raccoonman.reterraforged.world.worldgen.cell.Cell cell = new raccoonman.reterraforged.world.worldgen.cell.Cell();
			lookup.applyCell(cell, x, z, true);
			boolean isSea = cell.terrain.isDeepOcean() || cell.terrain.isShallowOcean();
			boolean isRainforest = cell.biome == raccoonman.reterraforged.world.worldgen.cell.biome.type.BiomeType.TROPICAL_RAINFOREST;
			if (isSea) seas++;
			if (isRainforest) rainforest++;
			if (!isRainforest || !isSea) continue;
			ocean++;
			Holder<Biome> biome = level.getUncachedNoiseBiome(x >> 2, (sea - 8) >> 2, z >> 2);
			if (biome.is(Biomes.WARM_OCEAN)) warm++; else other.merge(biome.getRegisteredName(), 1, Integer::sum);
		}
		check.accept("the sea of the rainforest regions is warm ocean (" + warm + "/" + ocean + (other.isEmpty() ? "" : ", else " + other) + "; " + rainforest + " rainforest and " + seas + " sea points)", ocean < 10 || warm >= ocean * 0.9);
	}

	// the trees of a swamp stand all over their chunks, not on a 16 block grid (all on the chunk's corner,
	// ReTerraForged 0.0.6, #34): over 9 x 9 chunks of it, the trunks on the ground by their place in the chunk
	private static void swampTrees(ServerLevel level, BiConsumer<String, Boolean> check) {
		if (!((Object) level.getChunkSource().randomState() instanceof RTFRandomState s) || s.preset() == null || !s.preset().miscellaneous().customBiomeFeatures) return;
		// searched from 8 km east of the spawn: the chunks it generates must stay away from the shape "rtf heights" samples
		// around the spawn (generated chunks leave eroded tiles in the cache, and a sample there can come out a block
		// off, so NeoForge and Fabric did not compare)
		var swamp = level.findClosestBiome3d((b) -> b.is(Biomes.SWAMP), level.getRespawnData().pos().offset(8192, 0, 0).atY(level.getSeaLevel()), 6400, 32, 64);
		if (swamp == null) return;
		int cx0 = swamp.getFirst().getX() >> 4, cz0 = swamp.getFirst().getZ() >> 4;
		int trunks = 0, corner = 0;
		java.util.Set<Integer> places = new java.util.HashSet<>();
		for (int cx = cx0 - 4; cx <= cx0 + 4; cx++) for (int cz = cz0 - 4; cz <= cz0 + 4; cz++) {
			ChunkAccess c = full(level, cx, cz);
			for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
				int top = c.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
				for (int y = top; y > top - 24; y--) {
					BlockPos pos = new BlockPos(cx * 16 + x, y, cz * 16 + z);
					if (!c.getBlockState(pos).is(BlockTags.LOGS)) continue;
					BlockState below = c.getBlockState(pos.below());
					if (below.is(BlockTags.LOGS) || below.isAir() || !below.getFluidState().isEmpty() && !Heightmap.Types.MOTION_BLOCKING.isOpaque().test(below)) continue;
					if (!level.getBiome(pos).is(Biomes.SWAMP)) break;
					trunks++;
					places.add(x * 16 + z);
					if (x == 0 && z == 0) corner++;
					break;
				}
			}
		}
		check.accept("swamp trees are spread over their chunks (" + trunks + " trunks at " + places.size() + " places in the chunk, " + corner + " on its corner; swamp at "
			+ swamp.getFirst().getX() + " " + swamp.getFirst().getZ() + ")", trunks < 5 || (places.size() >= 8 && corner * 2 < trunks));
	}

	// a preset with its own deepslate level (#32, tools/servertest.sh PRESET=set:world().properties.deepslateLevel=40):
	// in the self test's chunks the stone 4 blocks below the level is deepslate, the rock 12 blocks above it is not
	private static void deepslate(ServerLevel level, BiConsumer<String, Boolean> check) {
		if (!((Object) level.getChunkSource().randomState() instanceof RTFRandomState s) || s.preset() == null || s.preset().world().properties.deepslateLevel == 0) return;
		int y = s.preset().world().properties.deepslateLevel;
		int below = 0, belowDeep = 0, above = 0, aboveDeep = 0;
		for (int cx = AREA_MIN; cx <= AREA_MAX; cx++) for (int cz = AREA_MIN; cz <= AREA_MAX; cz++) {
			ChunkAccess c = full(level, cx, cz);
			for (int x = 0; x < 16; x += 3) for (int z = 0; z < 16; z += 3) {
				BlockState low = c.getBlockState(new BlockPos(cx * 16 + x, y - 4, cz * 16 + z));
				if (low.is(Blocks.STONE) || low.is(Blocks.DEEPSLATE)) { below++; if (low.is(Blocks.DEEPSLATE)) belowDeep++; }
				BlockState high = c.getBlockState(new BlockPos(cx * 16 + x, y + 12, cz * 16 + z));
				// above: any rock (the strata lay granite, diorite and andesite there), where the ground is higher
				boolean underGround = c.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) > y + 16;
				if (underGround && !high.isAir() && high.getFluidState().isEmpty()) { above++; if (high.is(Blocks.DEEPSLATE)) aboveDeep++; }
			}
		}
		check.accept("deepslate starts at the preset's level " + y + " (deepslate: " + belowDeep + "/" + below + " of the stone at y " + (y - 4) + ", " + aboveDeep + "/" + above + " of the rock at y " + (y + 12) + ")",
			below > 50 && above > 50 && belowDeep >= below * 0.95 && aboveDeep <= above * 0.05);
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

	// a preset with the snow line on (tools/servertest.sh PRESET=set:miscellaneous().snowLine.enabled=true;...): the
	// ground of the highest mountain above the line is under snow whatever its biome (rtf snowline)
	private static void snowLine(ServerLevel level, BiConsumer<String, Boolean> check) {
		if (!((Object) level.getChunkSource().randomState() instanceof RTFRandomState s) || s.preset() == null || !s.preset().miscellaneous().snowLine.enabled) return;
		RTFSnowLineTest.Result result = RTFSnowLineTest.sample(level, s.preset().miscellaneous().snowLine);
		check.accept("above the snow line the ground is under snow (" + result.describe() + ")", result.above() >= 50 && result.share() >= 95.0);
		// with a depth of 2 layers or more: the snow as deep as the preset gives each height
		if (result.depthChecked() > 0) check.accept("the snow above the line is as deep as the preset says (" + result.describe() + ")", result.depthShare() >= 95.0);
	}

	// the strata decorator of "Default" lays bands of granite, diorite, andesite and stone under the ground (rtf strata)
	private static void strata(ServerLevel level, BiConsumer<String, Boolean> check) {
		if (!((Object) level.getChunkSource().randomState() instanceof RTFRandomState s) || s.preset() == null || !s.preset().miscellaneous().strataDecorator) return;
		BlockPos spawn = level.getRespawnData().pos();
		RTFStrataTest.Result result = RTFStrataTest.sample(level, ((spawn.getX() + 2000) >> 4) - 2, (spawn.getZ() >> 4) - 2, 4);
		check.accept("the strata lay rock bands under the ground (" + result.describe() + ")", result.rock().size() >= 3 && result.layers() >= 2);
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
			// 26.3: a biome's mobs are an environment attribute
			var mobs = biomes.getOrThrow(key).value().getAttributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS, net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS.defaultValue());
			if (mobs.getMobsInCategory(MobCategory.MONSTER).isEmpty()) empty.add(key.toString() + " monsters");
			if (key != Biomes.DESERT && mobs.getMobsInCategory(MobCategory.CREATURE).isEmpty()) empty.add(key.toString() + " animals");
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
			// before 0.0.20: no settings version, the settings that did nothing hold the values of 0.0.6's Default
			JsonObject misc = json.getAsJsonObject("miscellaneous");
			misc.remove("settingsVersion");
			misc.addProperty("plainStoneErosion", true);
			misc.addProperty("mountainBiomeUsage", 0.4F);
			misc.addProperty("volcanoBiomeUsage", 0.4F);
			json.getAsJsonObject("rivers").remove("branchSizeFromMain");
			json.getAsJsonObject("rivers").getAsJsonObject("lakes").addProperty("maxStartDistance", 0.03F);
			json.getAsJsonObject("world").getAsJsonObject("controlPoints").addProperty("islandCoast", 0.1F);
			Preset old = Preset.DIRECT_CODEC.parse(JsonOps.INSTANCE, (JsonElement) json).getOrThrow();
			var cp = old.world().controlPoints;
			// PresetMigration: the values that keep the world of such a preset as it was
			boolean migrated = !old.miscellaneous().plainStoneErosion && old.miscellaneous().mountainBiomeUsage == 1.0F && old.miscellaneous().volcanoBiomeUsage == 0.0F
				&& old.rivers().branchSizeFromMain && old.rivers().lakes.maxStartDistance == 0.0F && cp.islandCoast == cp.coast && cp.islandInland == cp.inland;
			ok = old.world().properties.lavaLevel == -54 && migrated;
			why = "lavaLevel " + old.world().properties.lavaLevel + ", settings that did nothing " + (migrated ? "migrated" : "NOT migrated");
		} catch (Throwable t) {
			ok = false;
			why = t.toString();
		}
		check.accept("a preset saved by an older version loads with the defaults (" + why + ")", ok);
	}

	/**
	 * "rtf heights [name]": the world's shape in run/.../rtf-heights[-name].txt: the generator's ground height and the
	 * biome at a 33 x 33 grid over 4 x 4 km (no chunks needed; the raw estimate, not the eroded cell of a cached tile,
	 * so the same whichever chunks were generated before), then the top of the ground (no plants, trees or water) of
	 * the 81 chunks the self test generates, at 4 columns each. Chunks already saved, as by an older version of the mod,
	 * are read as they are.
	 */
	/**
	 * "rtf weather": on land points of an 8 x 8 km grid (every 64 blocks, base heights, no chunks generated), how often
	 * the ground's biome rains at sea level but snows at the ground's height, where vanilla cools a biome with altitude
	 * (rain is heard only where it rains on the ground near the player; snow has no sound)
	 */
	public static String weather(MinecraftServer server) {
		ServerLevel level = server.overworld();
		var gen = level.getChunkSource().getGenerator();
		var rs = level.getChunkSource().randomState();
		int sea = gen.getSeaLevel();
		int land = 0, rainLow = 0, snowHigh = 0;
		List<Integer> heights = new ArrayList<>();
		for (int x = -4096; x <= 4096; x += 64) {
			for (int z = -4096; z <= 4096; z += 64) {
				int h = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
				if (h <= sea) continue;
				land++;
				heights.add(h);
				Biome biome = level.getUncachedNoiseBiome(x >> 2, h >> 2, z >> 2).value();
				if (biome.getPrecipitationAt(new BlockPos(x, sea + 1, z), sea) == Biome.Precipitation.RAIN) {
					rainLow++;
					if (biome.getPrecipitationAt(new BlockPos(x, h, z), sea) == Biome.Precipitation.SNOW) snowHigh++;
				}
			}
		}
		heights.sort(null);
		int median = heights.isEmpty() ? 0 : heights.get(heights.size() / 2), p90 = heights.isEmpty() ? 0 : heights.get(heights.size() * 9 / 10);
		return String.format("[RTF weather] land %d, ground y median %d, 90%% %d; rainy biomes %d, of them snowing at the ground's height %d (%.1f%%)",
			land, median, p90, rainLow, snowHigh, rainLow == 0 ? 0.0 : 100.0 * snowHigh / rainLow);
	}

	public static String heights(MinecraftServer server, String name) {
		ServerLevel level = server.overworld();
		var gen = level.getChunkSource().getGenerator();
		var rs = level.getChunkSource().randomState();
		List<String> lines = new ArrayList<>();
		lines.add("# seed " + level.getSeed());
		WorldLookup.beginRaw();
		try {
			for (int x = -2048; x <= 2048; x += 128) {
				for (int z = -2048; z <= 2048; z += 128) {
					int h = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
					Holder<Biome> biome = level.getUncachedNoiseBiome(x >> 2, h >> 2, z >> 2);
					lines.add("base " + x + " " + z + " " + h + " " + biome.getRegisteredName());
				}
			}
		} finally {
			WorldLookup.endRaw();
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
		int holes = 0, lava = 0, water = 0, top = 0, lavaTop = Integer.MIN_VALUE;
		for (int cx = -2; cx <= 2; cx++) for (int cz = -2; cz <= 2; cz++) {
			ChunkAccess c = full(level, cx, cz);
			for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
				if (!c.getBlockState(new BlockPos(x, minY(level), z)).is(Blocks.BEDROCK)) holes++;
				for (int y = minY(level); y <= maxY(level); y += 4) {
					BlockState state = c.getBlockState(new BlockPos(x, y, z));
					if (state.is(Blocks.LAVA)) {
						lava++;
						lavaTop = Math.max(lavaTop, y);
					}
					else if (state.is(Blocks.WATER)) water++;
				}
				if (!c.getBlockState(new BlockPos(x, maxY(level), z)).isAir()) top++;
			}
		}
		boolean ok = holes == 0;
		// the highest lava sample: the lava level of the preset (#13: it stayed at -54 whatever the slider)
		String r = String.format("[RTF extremes] %s: sea level %d, bedrock holes %d, lava %d (highest y %s), water %d samples, blocks at the world top %d (25 chunks)", ok ? "PASS" : "FAIL", seaLevel, holes, lava, lava > 0 ? String.valueOf(lavaTop) : "-", water, top);
		RTFCommon.LOGGER.info(r);
		return r;
	}
}
