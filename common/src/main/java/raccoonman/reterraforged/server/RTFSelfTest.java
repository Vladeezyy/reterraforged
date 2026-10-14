package raccoonman.reterraforged.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.data.worldgen.PresetExport;
import raccoonman.reterraforged.data.worldgen.preset.PresetConfiguredFeatures;
import raccoonman.reterraforged.data.worldgen.preset.PresetStructureSets;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.Presets;
import raccoonman.reterraforged.data.worldgen.preset.settings.StructureSettings;
import raccoonman.reterraforged.server.stress.RTFEffectTest;
import raccoonman.reterraforged.server.stress.RTFStressTest;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;

/**
 * "rtf selftest" — only registered with -Drtf.selftest=true (tools/servertest.sh). Generates real chunks in the
 * overworld, the nether and the end and checks what players notice first: the overworld really uses the preset,
 * the terrain has relief and sea, the spawn is on dry land, bedrock layers are whole, villages are found and stand on
 * the ground. Prints "[RTF selftest] PASS all N" or the failed checks.
 */
public final class RTFSelfTest {
	private final List<String> failed = new ArrayList<>();
	private int checks;

	private RTFSelfTest() {
	}

	public static boolean enabled() {
		return Boolean.getBoolean("rtf.selftest");
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		if (!enabled()) return;
		dispatcher.register(Commands.literal("rtf").requires(Commands.hasPermission(Commands.LEVEL_OWNERS)).then(Commands.literal("selftest").executes(ctx -> {
			String result = new RTFSelfTest().run(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})).then(Commands.literal("bench").executes(ctx -> {
			String result = RTFBench.start(ctx.getSource().getServer(), 32);
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}).then(Commands.argument("size", IntegerArgumentType.integer(1, 128)).executes(ctx -> {
			String result = RTFBench.start(ctx.getSource().getServer(), IntegerArgumentType.getInteger(ctx, "size"));
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}))).then(Commands.literal("structures").executes(ctx -> {
			String result = RTFStructureTest.run(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})).then(Commands.literal("exportpreset").executes(ctx -> {
			String result = exportPreset(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}).then(Commands.argument("stress", StringArgumentType.greedyString()).executes(ctx -> {
			String result = exportStressPreset(ctx.getSource().getServer(), StringArgumentType.getString(ctx, "stress"));
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}))).then(Commands.literal("stress").executes(ctx -> {
			String result = RTFStressTest.run(ctx.getSource().getServer(), 40);
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}).then(Commands.literal("abuse").executes(ctx -> {
			String result = RTFStressTest.runAbuse(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})).then(Commands.literal("effects").executes(ctx -> {
			String result = RTFEffectTest.run(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})).then(Commands.argument("random", IntegerArgumentType.integer(0)).executes(ctx -> {
			String result = RTFStressTest.run(ctx.getSource().getServer(), IntegerArgumentType.getInteger(ctx, "random"));
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}))));
	}

	// the "Default" preset as a datapack in the server folder, the same pack the preset screen writes: a dedicated
	// server only uses ReTerraForged with this pack in a new world's datapacks folder
	private static String exportPreset(MinecraftServer server) {
		Path out = server.getServerDirectory().resolve("reterraforged-preset.zip").toAbsolutePath();
		try {
			PresetExport.toZip(Presets.makeDefault(), "Default", server.registries().getAccessForLoading(RegistryLayer.DIMENSIONS), out);
			return "[RTF] exported preset to " + out;
		} catch (Throwable t) {
			RTFCommon.LOGGER.error("[RTF] preset export failed", t);
			return "[RTF] preset export failed: " + t;
		}
	}

	// a stress test preset (min, max, random:N) as a datapack, for a real world with it (tools/stressworld.sh)
	private static String exportStressPreset(MinecraftServer server, String spec) {
		Path out = server.getServerDirectory().resolve("reterraforged-preset.zip").toAbsolutePath();
		try {
			PresetExport.toZip(RTFStressTest.makePreset(spec), "Stress " + spec, server.registries().getAccessForLoading(RegistryLayer.DIMENSIONS), out);
			return "[RTF] exported preset to " + out;
		} catch (Throwable t) {
			RTFCommon.LOGGER.error("[RTF] preset export failed", t);
			return "[RTF] preset export failed: " + t;
		}
	}

	private void check(String name, boolean ok) {
		this.checks++;
		if (!ok) this.failed.add(name);
		RTFCommon.LOGGER.info("[RTF selftest] {} {}", ok ? "ok  " : "FAIL", name);
	}

	private static ChunkAccess full(ServerLevel level, int cx, int cz) {
		return level.getChunk(cx, cz, ChunkStatus.FULL, true);
	}

	private String run(MinecraftServer server) {
		try {
			this.overworld(server.overworld());
			this.nether(server.getLevel(Level.NETHER));
			this.end(server.getLevel(Level.END));
		} catch (Throwable t) {
			this.failed.add("exception: " + t);
			RTFCommon.LOGGER.error("[RTF selftest] exception", t);
		}
		String r = this.failed.isEmpty() ? "[RTF selftest] PASS all " + this.checks : "[RTF selftest] FAIL " + this.failed.size() + "/" + this.checks + ": " + this.failed;
		RTFCommon.LOGGER.info(r);
		return r;
	}

	private void overworld(ServerLevel level) {
		boolean rtf = (Object) level.getChunkSource().randomState() instanceof RTFRandomState s && s.generatorContext() != null;
		check("overworld uses the ReTerraForged generator", rtf);
		// the preset's biome modifiers are applied: the forest has the mod's trees instead of vanilla's (on NeoForge
		// they were left out of the preset datapack, and every tree was vanilla)
		List<String> trees = level.registryAccess().lookupOrThrow(Registries.BIOME).getValue(Biomes.FOREST).getGenerationSettings().features().stream()
			.flatMap(HolderSet::stream).map(Holder::unwrapKey).flatMap(Optional::stream).map((key) -> key.identifier().toString())
			.filter((id) -> id.endsWith("trees") || id.contains("trees_")).toList();
		check("the forest grows ReTerraForged trees, not vanilla ones " + trees, trees.equals(List.of(RTFCommon.MOD_ID + ":forest_trees")));
		// and they stand in the world: trunks in the nearest forest (the feature list above holds even when every tree
		// fails to place, as on 26.1 with the soil tag and 26.3 with the template block names)
		Pair<BlockPos, Holder<Biome>> forest = level.findClosestBiome3d((biome) -> biome.is(Biomes.FOREST), BlockPos.ZERO.atY(level.getSeaLevel() + 8), 2400, 32, 64);
		if (forest == null) {
			RTFCommon.LOGGER.info("[RTF selftest] no forest within 2400 blocks, its trees not checked");
		} else {
			BlockPos center = forest.getFirst();
			for (int cx = (center.getX() >> 4) - 2; cx <= (center.getX() >> 4) + 2; cx++) for (int cz = (center.getZ() >> 4) - 2; cz <= (center.getZ() >> 4) + 2; cz++) full(level, cx, cz);
			int trunks = 0;
			for (int x = center.getX() - 24; x < center.getX() + 24; x++) {
				for (int z = center.getZ() - 24; z < center.getZ() + 24; z++) {
					int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
					if (level.getBlockState(new BlockPos(x, ground - 1, z)).is(BlockTags.LOGS)) trunks++;
				}
			}
			check("trees stand in the forest at " + center.getX() + ", " + center.getZ() + " (" + trunks + " trunk columns)", trunks >= 5);
		}
		// the structure page reaches the world: a preset with other village spacing, separation and salt makes a
		// datapack whose villages have them
		Preset spaced = Presets.makeDefault();
		StructureSettings.StructureSetEntry villages = new StructureSettings.StructureSetEntry(21, 9, -12345, false);
		spaced.structures().entries.put(BuiltinStructureSets.VILLAGES, villages);
		StructureSet set = spaced.buildPatch(level.getServer().registries().getAccessForLoading(RegistryLayer.DIMENSIONS)).lookupOrThrow(Registries.STRUCTURE_SET).getOrThrow(BuiltinStructureSets.VILLAGES).value();
		check("the structure page sets the villages' spacing, separation and salt", PresetStructureSets.matches(set, villages));

		// relief: surface heights over a 6 x 6 km area from the generator itself (no chunks are built for this)
		var gen = level.getChunkSource().getGenerator();
		var rs = level.getChunkSource().randomState();
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, water = 0, samples = 0;
		long t0 = System.nanoTime();
		for (int x = -3072; x <= 3072; x += 384) {
			for (int z = -3072; z <= 3072; z += 384) {
				int h = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
				int top = gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, rs);
				min = Math.min(min, h);
				max = Math.max(max, h);
				if (top > h) water++;
				samples++;
			}
		}
		long ms = (System.nanoTime() - t0) / 1_000_000;
		RTFCommon.LOGGER.info("[RTF selftest] {} height samples in {} ms, ground {} .. {}, water at {} of {} points", samples, ms, min, max, water, samples);
		// a 9 x 9 chunk area fully generated, timed
		t0 = System.nanoTime();
		for (int cx = 20; cx < 29; cx++) for (int cz = 20; cz < 29; cz++) full(level, cx, cz);
		RTFCommon.LOGGER.info("[RTF selftest] 81 chunks fully generated in {} ms", (System.nanoTime() - t0) / 1_000_000);
		check("terrain has relief (ground " + min + " .. " + max + ")", max - min >= 40);
		check("terrain reaches above sea level", max > level.getSeaLevel() + 20);
		check("there is water (" + water + "/" + samples + ")", water > 0 && water < samples);
		check("terrain stays inside the world height", min > level.getMinY() + 5 && max < level.getMaxY() - 4);

		// bedrock floor of the overworld is whole
		ChunkAccess spawnChunk = full(level, 0, 0);
		int holes = 0;
		for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
			if (!spawnChunk.getBlockState(new BlockPos(x, level.getMinY(), z)).is(Blocks.BEDROCK)) holes++;
		}
		check("overworld bedrock floor is whole (holes: " + holes + ")", holes == 0);

		// spawn on dry land
		BlockPos spawn = level.getRespawnData().pos();
		ChunkAccess sc = full(level, spawn.getX() >> 4, spawn.getZ() >> 4);
		int sy = sc.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX() & 15, spawn.getZ() & 15);
		var below = level.getBlockState(new BlockPos(spawn.getX(), sy - 1, spawn.getZ()));
		check("spawn is on dry land (" + spawn.getX() + ", " + sy + ", " + spawn.getZ() + ": " + below.getBlock().getName().getString() + ")",
				below.getFluidState().isEmpty() && !below.isAir());

		// bee nests grow on the mod's trees (their decorator was never used, those biomes had no bees): a meadow tree,
		// which always has one, grown on a grass block near the spawn (the spawn may be in a desert)
		Holder<ConfiguredFeature<?, ?>> meadowTree = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(PresetConfiguredFeatures.MEADOW_TREE);
		boolean nest = false;
		for (int i = 0; i < 12 && !nest; i++) {
			int x = spawn.getX() + 32 + i * 24, z = spawn.getZ() + 32;
			for (int cx = (x >> 4) - 1; cx <= (x >> 4) + 1; cx++) for (int cz = (z >> 4) - 1; cz <= (z >> 4) + 1; cz++) full(level, cx, cz);
			BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
			level.setBlock(ground.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
			if (meadowTree.value().place(level, level.getChunkSource().getGenerator(), RandomSource.create(i), ground)) {
				nest = BlockPos.betweenClosedStream(ground.offset(-8, 0, -8), ground.offset(8, 20, 8)).anyMatch((p) -> level.getBlockState(p).is(Blocks.BEE_NEST));
			}
		}
		check("bee nests grow on the mod's trees", nest);
		// and saplings grown with bone meal next to flowers are Minecraft's own trees, with its 5 % chance of a nest: 200
		// oak saplings, each with a poppy beside it, on grass blocks east of the spawn
		int grown = 0, nests = 0;
		for (int i = 0; i < 200; i++) {
			int x = spawn.getX() + 400 + (i % 20) * 7, z = spawn.getZ() + (i / 20) * 7;
			full(level, x >> 4, z >> 4);
			BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
			level.setBlock(ground.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
			level.setBlock(ground.east(), Blocks.POPPY.defaultBlockState(), 3);
			level.setBlock(ground.east().below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
			level.setBlock(ground, Blocks.OAK_SAPLING.defaultBlockState(), 3);
			if (TreeGrower.OAK.growTree(level, level.getChunkSource().getGenerator(), ground, level.getBlockState(ground), RandomSource.create(i))) {
				grown++;
				if (BlockPos.betweenClosedStream(ground.offset(-2, 0, -2), ground.offset(2, 12, 2)).anyMatch((p) -> level.getBlockState(p).is(Blocks.BEE_NEST))) nests++;
			}
		}
		check("saplings grown next to flowers get bee nests (" + nests + " of " + grown + ")", grown >= 50 && nests > 0);

		// a village exists and its start sits on the terrain, not floating or buried
		BlockPos village = level.findNearestMapStructure(StructureTags.VILLAGE, BlockPos.ZERO, 100, false);
		check("a village is found", village != null);
		if (village != null) {
			ChunkAccess vc = full(level, village.getX() >> 4, village.getZ() >> 4);
			int ground = vc.getHeight(Heightmap.Types.WORLD_SURFACE_WG, village.getX() & 15, village.getZ() & 15);
			boolean onLand = ground > level.getSeaLevel() - 2;
			RTFCommon.LOGGER.info("[RTF selftest] village at {} {} ground {}", village.getX(), village.getZ(), ground);
			check("the village stands on land above the sea", onLand);
		}
	}

	private void nether(ServerLevel level) {
		if (level == null) return;
		int floorHoles = 0, roofHoles = 0;
		int floorY = level.getMinY(), roofY = floorY + level.dimensionType().logicalHeight() - 1;
		for (int cx = -3; cx <= 3; cx++) for (int cz = -3; cz <= 3; cz++) {
			ChunkAccess c = full(level, cx, cz);
			for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
				if (!c.getBlockState(new BlockPos(x, floorY, z)).is(Blocks.BEDROCK)) floorHoles++;
				if (!c.getBlockState(new BlockPos(x, roofY, z)).is(Blocks.BEDROCK)) roofHoles++;
			}
		}
		check("nether bedrock floor is whole (holes: " + floorHoles + ")", floorHoles == 0);
		check("nether bedrock roof is whole (holes: " + roofHoles + ")", roofHoles == 0);
		ChunkAccess c = full(level, 0, 0);
		int netherrack = 0;
		for (int y = floorY + 10; y < floorY + 100; y++) if (c.getBlockState(new BlockPos(8, y, 8)).is(Blocks.NETHERRACK)) netherrack++;
		check("nether is made of netherrack as usual (" + netherrack + "/90)", netherrack > 10);
	}

	private void end(ServerLevel level) {
		if (level == null) return;
		ChunkAccess c = full(level, 0, 0);
		int stone = 0;
		for (int y = 40; y < 70; y++) if (c.getBlockState(new BlockPos(0, y, 0)).is(Blocks.END_STONE)) stone++;
		check("the end island is there", stone > 5);
	}
}
