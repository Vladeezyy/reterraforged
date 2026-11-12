package raccoonman.reterraforged.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
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
import raccoonman.reterraforged.world.worldgen.feature.template.SaplingTrees;
import raccoonman.reterraforged.world.worldgen.feature.template.TemplateFeature;
import raccoonman.reterraforged.world.worldgen.feature.template.decorator.TreeContext;
import raccoonman.reterraforged.world.worldgen.feature.template.paste.PasteConfig;
import raccoonman.reterraforged.world.worldgen.feature.template.paste.PasteType;
import raccoonman.reterraforged.world.worldgen.feature.template.placement.TemplatePlacement;
import raccoonman.reterraforged.world.worldgen.feature.template.placement.TemplatePlacements;
import raccoonman.reterraforged.world.worldgen.feature.template.template.BlockInfo;
import raccoonman.reterraforged.world.worldgen.feature.template.template.FeatureTemplate;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

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
		dispatcher.register(Commands.literal("rtf").requires(s -> s.hasPermission(4)).then(Commands.literal("selftest").executes(ctx -> {
			String result = new RTFSelfTest().run(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})).then(Commands.literal("previewmap").then(Commands.argument("name", StringArgumentType.word()).then(Commands.argument("zoom", IntegerArgumentType.integer(1, 256)).then(Commands.argument("spec", StringArgumentType.greedyString()).executes(ctx -> {
			String result = RTFPreviewMap.run(ctx.getSource().getServer(), StringArgumentType.getString(ctx, "name"), IntegerArgumentType.getInteger(ctx, "zoom"), StringArgumentType.getString(ctx, "spec"));
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}))))).then(Commands.literal("valleys").then(Commands.argument("name", StringArgumentType.word()).then(Commands.argument("x", IntegerArgumentType.integer()).then(Commands.argument("z", IntegerArgumentType.integer()).then(Commands.argument("seed", LongArgumentType.longArg()).then(Commands.argument("spec", StringArgumentType.greedyString()).executes(ctx -> {
			String result = RTFValleyTest.run(ctx.getSource().getServer(), StringArgumentType.getString(ctx, "name"), IntegerArgumentType.getInteger(ctx, "x"), IntegerArgumentType.getInteger(ctx, "z"), LongArgumentType.getLong(ctx, "seed"), StringArgumentType.getString(ctx, "spec"));
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}))))))).then(Commands.literal("bench").executes(ctx -> {
			String result = RTFBench.start(ctx.getSource().getServer(), 32);
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}).then(Commands.argument("size", IntegerArgumentType.integer(1, 128)).executes(ctx -> {
			String result = RTFBench.start(ctx.getSource().getServer(), IntegerArgumentType.getInteger(ctx, "size"));
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}).then(Commands.argument("chunkX", IntegerArgumentType.integer()).then(Commands.argument("chunkZ", IntegerArgumentType.integer()).executes(ctx -> {
			String result = RTFBench.start(ctx.getSource().getServer(), IntegerArgumentType.getInteger(ctx, "size"), IntegerArgumentType.getInteger(ctx, "chunkX"), IntegerArgumentType.getInteger(ctx, "chunkZ"));
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}))))).then(Commands.literal("tilebench").executes(ctx -> {
			String result = RTFTileBench.start(ctx.getSource().getServer(), 60);
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}).then(Commands.argument("tiles", IntegerArgumentType.integer(1, 1000)).executes(ctx -> {
			String result = RTFTileBench.start(ctx.getSource().getServer(), IntegerArgumentType.getInteger(ctx, "tiles"));
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}))).then(Commands.literal("fault").executes(ctx -> {
			String result = RTFFaultTest.run(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}).then(Commands.literal("ticket").executes(ctx -> {
			String result = RTFFaultTest.chunk(ctx.getSource().getServer(), false);
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})).then(Commands.literal("block").executes(ctx -> {
			String result = RTFFaultTest.chunk(ctx.getSource().getServer(), true);
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}))).then(Commands.literal("tilecache").executes(ctx -> {
			String result = RTFTileCacheTest.start(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})).then(Commands.literal("swamps").executes(ctx -> {
			String result = RTFSwampTest.run(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})).then(Commands.literal("islands").executes(ctx -> {
			String result = RTFIslandTest.run(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})).then(Commands.literal("coasts").executes(ctx -> {
			String result = RTFCoastTest.run(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})).then(Commands.literal("structures").executes(ctx -> {
			String result = RTFStructureTest.run(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})).then(Commands.literal("holes").executes(ctx -> {
			String result = RTFHoleTest.run(ctx.getSource().getServer(), 16);
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}).then(Commands.argument("chunks", IntegerArgumentType.integer(4, 64)).executes(ctx -> {
			String result = RTFHoleTest.run(ctx.getSource().getServer(), IntegerArgumentType.getInteger(ctx, "chunks"));
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}))).then(Commands.literal("locate").then(Commands.argument("structure and biome", StringArgumentType.greedyString()).executes(ctx -> {
			String[] args = StringArgumentType.getString(ctx, "structure and biome").trim().split("\\s+");
			String result = args.length == 2 ? RTFLocateTest.run(ctx.getSource().getServer(), args[0], args[1]) : "[RTF locate] FAIL usage: rtf locate <structure> <biome>";
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}))).then(Commands.literal("floating").executes(ctx -> {
			String result = RTFFloatingTest.run(ctx.getSource().getServer(), 16);
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}).then(Commands.argument("chunks", IntegerArgumentType.integer(4, 64)).executes(ctx -> {
			String result = RTFFloatingTest.run(ctx.getSource().getServer(), IntegerArgumentType.getInteger(ctx, "chunks"));
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}))).then(Commands.literal("heights").executes(ctx -> {
			String result = RTFWorldChecks.heights(ctx.getSource().getServer(), "");
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}).then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
			String result = RTFWorldChecks.heights(ctx.getSource().getServer(), StringArgumentType.getString(ctx, "name"));
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}))).then(Commands.literal("fingerprint").executes(ctx -> {
			String result = RTFFingerprint.run(ctx.getSource().getServer(), "");
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}).then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
			String result = RTFFingerprint.run(ctx.getSource().getServer(), StringArgumentType.getString(ctx, "name"));
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}))).then(Commands.literal("weather").executes(ctx -> {
			String result = RTFWorldChecks.weather(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})).then(Commands.literal("extremes").executes(ctx -> {
			String result = RTFWorldChecks.extremes(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})).then(Commands.literal("dungeons").then(Commands.argument("chunks", IntegerArgumentType.integer(1, 64)).executes(ctx -> {
			String result = RTFWorldChecks.countDungeons(ctx.getSource().getServer(), IntegerArgumentType.getInteger(ctx, "chunks"));
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}))).then(Commands.literal("biomemods").executes(ctx -> {
			String result = RTFBiomeModTest.run(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}).then(Commands.literal("probe").executes(ctx -> {
			String result = RTFBiomeModTest.probe(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})).then(Commands.literal("match").executes(ctx -> {
			String result = RTFBiomeModTest.match(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})).then(Commands.literal("dump").executes(ctx -> {
			String result = RTFBiomeModTest.dump(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}))).then(Commands.literal("surfacemap").then(Commands.argument("biome", net.minecraft.commands.arguments.ResourceLocationArgument.id()).executes(ctx -> {
			String result = RTFSurfaceMap.run(ctx.getSource().getServer(), net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "biome").toString(), 12);
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}).then(Commands.argument("chunks", IntegerArgumentType.integer(2, 64)).executes(ctx -> {
			String result = RTFSurfaceMap.run(ctx.getSource().getServer(), net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "biome").toString(), IntegerArgumentType.getInteger(ctx, "chunks"));
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}).then(Commands.argument("x", IntegerArgumentType.integer()).then(Commands.argument("z", IntegerArgumentType.integer()).executes(ctx -> {
			String result = RTFSurfaceMap.run(ctx.getSource().getServer(), net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "biome").toString(), IntegerArgumentType.getInteger(ctx, "chunks"), new net.minecraft.core.BlockPos(IntegerArgumentType.getInteger(ctx, "x"), 0, IntegerArgumentType.getInteger(ctx, "z")));
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})))))).then(Commands.literal("snowgrid").then(Commands.argument("biome", net.minecraft.commands.arguments.ResourceLocationArgument.id()).executes(ctx -> {
			String result = RTFSnowGridTest.run(ctx.getSource().getServer(), net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "biome").toString(), 12);
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}).then(Commands.argument("chunks", IntegerArgumentType.integer(2, 64)).executes(ctx -> {
			String result = RTFSnowGridTest.run(ctx.getSource().getServer(), net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "biome").toString(), IntegerArgumentType.getInteger(ctx, "chunks"));
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})))).then(Commands.literal("vegetation").executes(ctx -> {
			String result = RTFVegetationTest.run(ctx.getSource().getServer(), null, 16);
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}).then(Commands.argument("biome", net.minecraft.commands.arguments.ResourceLocationArgument.id()).then(Commands.argument("chunks", IntegerArgumentType.integer(2, 64)).executes(ctx -> {
			String result = RTFVegetationTest.run(ctx.getSource().getServer(), net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "biome").toString(), IntegerArgumentType.getInteger(ctx, "chunks"));
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})))).then(Commands.literal("strata").executes(ctx -> {
			String result = RTFStrataTest.run(ctx.getSource().getServer(), 8);
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}).then(Commands.argument("chunks", IntegerArgumentType.integer(1, 64)).executes(ctx -> {
			String result = RTFStrataTest.run(ctx.getSource().getServer(), IntegerArgumentType.getInteger(ctx, "chunks"));
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		}))).then(Commands.literal("snowline").executes(ctx -> {
			String result = RTFSnowLineTest.run(ctx.getSource().getServer());
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
		})).then(Commands.literal("curves").executes(ctx -> {
			String result = RTFEffectTest.runCurves(ctx.getSource().getServer());
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

	// a size x size square of grass blocks at the surface at x, z, cleared above, for saplings
	private BlockPos saplingGround(ServerLevel level, int x, int z, int size) {
		for (int cx = (x >> 4) - 1; cx <= ((x + size) >> 4) + 1; cx++) for (int cz = (z >> 4) - 1; cz <= ((z + size) >> 4) + 1; cz++) full(level, cx, cz);
		BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
		for (int dx = 0; dx < size; dx++) for (int dz = 0; dz < size; dz++) {
			level.setBlock(ground.offset(dx, -1, dz), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
			for (int dy = 0; dy < 3; dy++) level.setBlock(ground.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
		}
		return ground;
	}

	private static ChunkAccess full(ServerLevel level, int cx, int cz) {
		return level.getChunk(cx, cz, ChunkStatus.FULL, true);
	}

	private String run(MinecraftServer server) {
		// the list of a preset's differences from Default (the log, crash reports): every built-in preset, read back through the datapack codec
		List<String> presetDiff = PresetDiffCheck.run();
		this.check("the differences of the built-in presets from Default are listed " + presetDiff, presetDiff.isEmpty());
		try {
			this.overworld(server.overworld());
			this.nether(server.getLevel(Level.NETHER));
			this.end(server.getLevel(Level.END));
		} catch (Throwable t) {
			this.failed.add("exception: " + t);
			RTFCommon.LOGGER.error("[RTF selftest] exception", t);
		}
		// the checks from the FreeTerraForged test plan run even when the ones above stopped (a vanilla reference world)
		try {
			RTFWorldChecks.run(server, this::check);
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
		check("the density sliders act in the overworld", raccoonman.reterraforged.world.worldgen.feature.placement.DensityFilter.scalesIn(level.getChunkSource().randomState(), level.registryAccess()));
		// the preset's biome modifiers are applied: the forest has the mod's trees instead of vanilla's (on NeoForge
		// they were left out of the preset datapack, and every tree was vanilla)
		List<String> trees = level.registryAccess().registryOrThrow(Registries.BIOME).get(Biomes.FOREST).getGenerationSettings().features().stream()
			.flatMap(HolderSet::stream).map(Holder::unwrapKey).flatMap(Optional::stream).map((key) -> key.location().toString())
			.filter((id) -> id.endsWith("trees") || id.contains("trees_")).toList();
		check("the forest grows ReTerraForged trees, not vanilla ones " + trees, trees.equals(List.of(RTFCommon.MOD_ID + ":forest_trees")));
		// the datapack's biome modifiers reach this loader whichever loader exported it: NeoForge reads
		// neoforge/biome_modifier, Fabric forge/biome_modifier, and a pack with only the other one's folder gave a world
		// without them (no swamp surface, erosion, trees of the preset). PRESET_ZIP=<the other loader's export>
		List<String> modifierFolders = new ArrayList<>();
		for (String folder : List.of("neoforge/biome_modifier", "forge/biome_modifier")) {
			if (!level.getServer().getResourceManager().listResources(folder, (id) -> id.getNamespace().equals(RTFCommon.MOD_ID) && id.getPath().endsWith("/add_swamp_surface.json")).isEmpty()) modifierFolders.add(folder);
		}
		if (!modifierFolders.isEmpty()) {
			boolean swampSurface = level.registryAccess().registryOrThrow(Registries.BIOME).get(Biomes.SWAMP).getGenerationSettings().features().stream()
				.flatMap(HolderSet::stream).map(Holder::unwrapKey).flatMap(Optional::stream).anyMatch(raccoonman.reterraforged.data.worldgen.preset.PresetPlacedFeatures.SWAMP_SURFACE::equals);
			check("the preset datapack's biome modifiers are applied (the swamp has the swamp surface, the pack has " + modifierFolders + ")", swampSurface);
		}
		// and they stand in the world: trunks in the nearest forest (the feature list above holds even when every tree
		// fails to place, as on 26.1 with the soil tag and 26.3 with the template block names)
		// a forest on the surface (biomes are 3D: at a fixed height a "forest" can be inside a mountain or in the air)
		Pair<BlockPos, Holder<Biome>> forest = RTFWorldChecks.surfaceBiome(level, Biomes.FOREST, 2400);
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
			if (trunks < 5) {
				// what the forest stands on, to tell rock slopes and water from trees that failed
				java.util.Map<String, Integer> surface = new java.util.TreeMap<>();
				for (int x = center.getX() - 24; x < center.getX() + 24; x += 4) for (int z = center.getZ() - 24; z < center.getZ() + 24; z += 4) {
					int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
					surface.merge(level.getBlockState(new BlockPos(x, top, z)).getBlock().getDescriptionId() + " (" + level.getBiome(new BlockPos(x, top, z)).getRegisteredName() + ")", 1, Integer::sum);
				}
				RTFCommon.LOGGER.info("[RTF selftest] the forest at {} {}: surface {}", center.getX(), center.getZ(), surface);
			}
			check("trees stand in the forest at " + center.getX() + ", " + center.getZ() + " (" + trunks + " trunk columns)", trunks >= 5);
		}
		// the structure page reaches the world: a preset with other village spacing, separation and salt gives the
		// loaded villages those (PresetStructureSets.apply when the registries load), and its datapack has no structure
		// set (one names the set's structures: with their mod removed the world did not load)
		Preset spaced = Presets.makeDefault();
		StructureSettings.StructureSetEntry villages = new StructureSettings.StructureSetEntry(21, 9, -12345, false);
		spaced.structures().entries.put(BuiltinStructureSets.VILLAGES, villages);
		StructureSet set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).getOrThrow(BuiltinStructureSets.VILLAGES);
		check("the structure page sets the villages' spacing, separation and salt", PresetStructureSets.matches(PresetStructureSets.placement(set.placement(), villages), villages));
		check("the preset datapack writes no structure set", spaced.buildPatch(level.getServer().registries().getAccessForLoading(RegistryLayer.DIMENSIONS)).lookup(Registries.STRUCTURE_SET).map((sets) -> sets.listElements().findAny().isEmpty()).orElse(true));
		// the world's own preset: every entry on a set of this world is its placement now
		level.registryAccess().registry(raccoonman.reterraforged.registries.RTFRegistries.PRESET).flatMap((presets) -> presets.getOptional(Preset.KEY)).ifPresent((worldPreset) -> worldPreset.structures().entries.forEach((key, entry) -> {
			StructureSet loaded = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(key);
			if (loaded != null && loaded.placement() instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement && PresetStructureSets.valid(entry)) {
				check("the preset's structure settings are on " + key.location(), PresetStructureSets.matches(loaded.placement(), entry) && (!entry.disabled || ((raccoonman.reterraforged.mixin.StructurePlacementAccessor) (Object) loaded.placement()).reterraforged$frequency() == 0.0F));
			}
		}));
		// a new world's preset (rtf exportpreset, the preset screen) has the fixes of this version, also after the
		// datapack's codec, and its strata noise reads the world's seed
		net.minecraft.core.HolderLookup.Provider fresh = Presets.makeDefault().buildPatch(level.getServer().registries().getAccessForLoading(RegistryLayer.DIMENSIONS));
		int generationVersion = fresh.lookupOrThrow(raccoonman.reterraforged.registries.RTFRegistries.PRESET).getOrThrow(Preset.KEY).value().world().properties.generationVersion;
		String strataDepth = raccoonman.reterraforged.world.worldgen.noise.module.Noise.DIRECT_CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, fresh.lookupOrThrow(raccoonman.reterraforged.registries.RTFRegistries.NOISE).getOrThrow(raccoonman.reterraforged.data.worldgen.preset.PresetStrataNoise.STRATA_DEPTH).value()).getOrThrow().toString();
		check("a new preset has generation version " + generationVersion + " of " + raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings.Properties.CURRENT_GENERATION_VERSION + ", its strata noise of the world's seed", generationVersion == raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings.Properties.CURRENT_GENERATION_VERSION && strataDepth.contains("\"seeded\":true"));

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
		// the generator's sea level: Level.getSeaLevel() is 63 on 1.21.1 whatever the preset sets
		check("terrain reaches above sea level (" + gen.getSeaLevel() + ")", max > gen.getSeaLevel() + 20);
		check("there is water (" + water + "/" + samples + ")", water > 0 && water < samples);
		check("terrain stays inside the world height", min > level.getMinBuildHeight() + 5 && max < level.getMaxBuildHeight() - 5);

		// bedrock floor of the overworld is whole
		ChunkAccess spawnChunk = full(level, 0, 0);
		int holes = 0;
		for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
			if (!spawnChunk.getBlockState(new BlockPos(x, level.getMinBuildHeight(), z)).is(Blocks.BEDROCK)) holes++;
		}
		check("overworld bedrock floor is whole (holes: " + holes + ")", holes == 0);

		// spawn on dry land
		// (a player appears at a random safe spot within the spawn radius: the spawn block itself and at least a quarter
		// of the 21 x 21 blocks around it, a beach, must be dry land)
		BlockPos spawn = level.getSharedSpawnPos();
		for (int cx = (spawn.getX() - 10) >> 4; cx <= (spawn.getX() + 10) >> 4; cx++) for (int cz = (spawn.getZ() - 10) >> 4; cz <= (spawn.getZ() + 10) >> 4; cz++) full(level, cx, cz);
		int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ());
		var below = level.getBlockState(new BlockPos(spawn.getX(), sy - 1, spawn.getZ()));
		int dry = 0;
		for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX() + x, spawn.getZ() + z);
			if (level.getBlockState(new BlockPos(spawn.getX() + x, y - 1, spawn.getZ() + z)).getFluidState().isEmpty()) dry++;
		}
		check("spawn is on dry land (" + spawn.getX() + ", " + sy + ", " + spawn.getZ() + ": " + below.getBlock().getName().getString() + ", dry " + dry + "/441)",
				below.getFluidState().isEmpty() && !below.isAir() && dry >= 441 / 4);

		// bee nests grow on the mod's trees (their decorator was never used, those biomes had no bees): a meadow tree,
		// which always has one, grown on a grass block near the spawn (the spawn may be in a desert)
		// (a preset of an older version, tools/servertest.sh LEGACY=..., has no meadow tree)
		Holder<ConfiguredFeature<?, ?>> meadowTree = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(PresetConfiguredFeatures.MEADOW_TREE).orElse(null);
		boolean nest = false;
		for (int i = 0; i < 12 && !nest && meadowTree != null; i++) {
			int x = spawn.getX() + 32 + i * 24, z = spawn.getZ() + 32;
			for (int cx = (x >> 4) - 1; cx <= (x >> 4) + 1; cx++) for (int cz = (z >> 4) - 1; cz <= (z >> 4) + 1; cz++) full(level, cx, cz);
			BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
			level.setBlock(ground.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
			if (meadowTree.value().place(level, level.getChunkSource().getGenerator(), RandomSource.create(i), ground)) {
				nest = BlockPos.betweenClosedStream(ground.offset(-8, 0, -8), ground.offset(8, 20, 8)).anyMatch((p) -> level.getBlockState(p).is(Blocks.BEE_NEST));
			}
		}
		check("bee nests grow on the mod's trees" + (meadowTree == null ? " (the preset has no meadow tree)" : ""), nest);
		// saplings grow the mod's trees where the preset puts them in the biomes (#41), Minecraft's otherwise: 200 oak
		// saplings, each with a poppy beside it, on grass blocks east of the spawn, get bee nests as Minecraft's 5 %
		boolean templates = (Object) level.getChunkSource().randomState() instanceof RTFRandomState srs && srs.preset() != null && srs.preset().miscellaneous().customBiomeFeatures;
		int grown = 0, nests = 0, before = SaplingTrees.grown;
		for (int i = 0; i < 200; i++) {
			int x = spawn.getX() + 400 + (i % 20) * 7, z = spawn.getZ() + (i / 20) * 7;
			BlockPos ground = this.saplingGround(level, x, z, 1);
			level.setBlock(ground.east(), Blocks.POPPY.defaultBlockState(), 3);
			level.setBlock(ground.east().below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
			level.setBlock(ground, Blocks.OAK_SAPLING.defaultBlockState(), 3);
			if (TreeGrower.OAK.growTree(level, level.getChunkSource().getGenerator(), ground, level.getBlockState(ground), RandomSource.create(i))) {
				grown++;
				if (BlockPos.betweenClosedStream(ground.offset(-4, 0, -4), ground.offset(4, 16, 4)).anyMatch((p) -> level.getBlockState(p).is(Blocks.BEE_NEST))) nests++;
			}
		}
		int mod = SaplingTrees.grown - before;
		check("saplings grown next to flowers get bee nests (" + nests + " of " + grown + ", the mod's " + mod + ")", grown >= 50 && nests > 0 && nests < grown / 4 && mod == (templates ? grown : 0));
		// each of Minecraft's saplings, with bone meal (its last stage), a 2 x 2 one with its trunk on the four saplings,
		// the trees' leaves not persistent and not about to decay: the mod's trees with its features, Minecraft's own
		// for a cherry sapling and without them
		Object[][] saplings = {
			{ Blocks.OAK_SAPLING, 1 }, { Blocks.BIRCH_SAPLING, 1 }, { Blocks.SPRUCE_SAPLING, 1 }, { Blocks.JUNGLE_SAPLING, 1 }, { Blocks.ACACIA_SAPLING, 1 },
			{ Blocks.SPRUCE_SAPLING, 2 }, { Blocks.JUNGLE_SAPLING, 2 }, { Blocks.DARK_OAK_SAPLING, 2 }, { Blocks.CHERRY_SAPLING, 1 }
		};
		for (int k = 0; k < saplings.length; k++) {
			Block block = (Block) saplings[k][0];
			int size = (Integer) saplings[k][1];
			boolean modTree = templates && block != Blocks.CHERRY_SAPLING;
			int grownTrees = 0, onSaplings = 0, badLeaves = 0;
			before = SaplingTrees.grown;
			for (int i = 0; i < 12; i++) {
				int x = spawn.getX() + 600 + k * 40, z = spawn.getZ() + i * 40;
				BlockPos ground = this.saplingGround(level, x, z, size);
				for (int dx = 0; dx < size; dx++) for (int dz = 0; dz < size; dz++) level.setBlock(ground.offset(dx, 0, dz), block.defaultBlockState().setValue(SaplingBlock.STAGE, 1), 3);
				// the leaves that were there: with the trees of other mods around (their bushes have persistent leaves) only
				// what this tree adds is counted
				int leavesBefore = badLeaves(level, ground);
				((SaplingBlock) block).advanceTree(level, ground, level.getBlockState(ground), RandomSource.create(k * 100 + i));
				if (level.getBlockState(ground).is(block)) continue;
				grownTrees++;
				boolean trunk = true;
				for (int dx = 0; dx < size; dx++) for (int dz = 0; dz < size; dz++) trunk &= level.getBlockState(ground.offset(dx, 0, dz)).is(BlockTags.LOGS);
				if (trunk) onSaplings++;
				badLeaves += Math.max(0, badLeaves(level, ground) - leavesBefore);
			}
			mod = SaplingTrees.grown - before;
			String name = size + "x" + size + " " + block.getName().getString();
			RTFCommon.LOGGER.info("[RTF selftest] saplings {}: {} of 12 grown, {} the mod's, {} on their saplings, {} leaves persistent or decaying", name, grownTrees, mod, onSaplings, badLeaves);
			check("a " + name + " grows " + (modTree ? "the mod's tree" : "Minecraft's tree") + " on its sapling (" + grownTrees + " of 12, the mod's " + mod + ")", grownTrees >= 6 && onSaplings == grownTrees && mod == (modTree ? grownTrees : 0) && (!modTree || badLeaves == 0));
		}
		// no tree grows where it does not fit, under a roof 4 blocks above (the mod's spruces are 8 to 14 high, and
		// saplings grow none of its bushes): the saplings stay
		int roofed = 0;
		before = SaplingTrees.grown;
		for (int i = 0; i < 12; i++) {
			int x = spawn.getX() + 1000, z = spawn.getZ() + i * 40;
			BlockPos ground = this.saplingGround(level, x, z, 1);
			for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) level.setBlock(ground.offset(dx, 4, dz), Blocks.STONE.defaultBlockState(), 3);
			level.setBlock(ground, Blocks.SPRUCE_SAPLING.defaultBlockState().setValue(SaplingBlock.STAGE, 1), 3);
			((SaplingBlock) Blocks.SPRUCE_SAPLING).advanceTree(level, ground, level.getBlockState(ground), RandomSource.create(i));
			if (level.getBlockState(ground).is(Blocks.SPRUCE_SAPLING)) roofed++;
		}
		check("no mod's tree grows under a low roof (" + roofed + " of 12 saplings stay)", !templates || (SaplingTrees.grown == before && roofed == 12));
		// a grown tree leaves the player's saplings and flowers around it, as Minecraft's: our generation rules replaced
		// them, and a tree farm lost its saplings to the first tree. On dirt pillars 2 to 5 high around each oak sapling,
		// in its crown; "in the crown" counts the plants next to the tree's leaves, so the check is not empty. Flowers as
		// Minecraft's trees: from 1.21.5 on its #replaceable_by_trees has #small_flowers, its trees take them too
		boolean flowersReplaced = Blocks.POPPY.defaultBlockState().is(BlockTags.REPLACEABLE_BY_TREES);
		int plants = 0, kept = 0, inCrown = 0, farmGrown = 0, saplingsPlaced = 0, saplingsKept = 0;
		before = SaplingTrees.grown;
		for (int i = 0; i < 12; i++) {
			int x = spawn.getX() + 1100, z = spawn.getZ() + i * 40;
			BlockPos ground = this.saplingGround(level, x, z, 1);
			List<BlockPos> placed = new ArrayList<>();
			java.util.Set<BlockPos> placedSaplings = new java.util.HashSet<>();
			int n = 0;
			for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) < 2 || (dx + dz) % 2 != 0) continue;
				int h = 2 + (n++ % 4);
				for (int dy = 0; dy < h; dy++) level.setBlock(ground.offset(dx, dy, dz), Blocks.DIRT.defaultBlockState(), 2);
				BlockPos top = ground.offset(dx, h, dz);
				level.setBlock(top, (n % 2 == 0 ? Blocks.OAK_SAPLING : Blocks.POPPY).defaultBlockState(), 2);
				placed.add(top);
				if (n % 2 == 0) placedSaplings.add(top);
			}
			level.setBlock(ground, Blocks.OAK_SAPLING.defaultBlockState().setValue(SaplingBlock.STAGE, 1), 3);
			((SaplingBlock) Blocks.OAK_SAPLING).advanceTree(level, ground, level.getBlockState(ground), RandomSource.create(5000 + i));
			if (level.getBlockState(ground).is(Blocks.OAK_SAPLING)) continue;
			farmGrown++;
			for (BlockPos p : placed) {
				plants++;
				BlockState state = level.getBlockState(p);
				boolean sapling = placedSaplings.contains(p);
				if (sapling) saplingsPlaced++;
				if (sapling ? state.is(Blocks.OAK_SAPLING) : state.is(Blocks.POPPY)) {
					kept++;
					if (sapling) saplingsKept++;
				}
				for (Direction d : Direction.values()) {
					if (level.getBlockState(p.relative(d)).getBlock() instanceof LeavesBlock) {
						inCrown++;
						break;
					}
				}
			}
		}
		int farmMod = SaplingTrees.grown - before;
		check("a grown tree leaves the saplings and flowers around it (" + kept + " of " + plants + " stay, saplings " + saplingsKept + " of " + saplingsPlaced + ", " + inCrown + " in its crown, " + farmGrown + " trees, the mod's " + farmMod + (flowersReplaced ? "; flowers replaceable as in Minecraft" : "") + ")",
			farmGrown >= 6 && saplingsKept == saplingsPlaced && saplingsPlaced > 0 && (flowersReplaced || kept == plants) && (!templates || (farmMod == farmGrown && inCrown > 0)));
		// a tree grown from a sapling keeps the blocks below its roots: the base pasted down from a template's lowest logs
		// (3 deep for all the preset's trees) took glass, flowers and water there. A trunk with a root beside it over
		// each of them, both pastes; generation still fills them, as the worlds were made
		FeatureTemplate rooted = new FeatureTemplate(List.of(new BlockInfo(BlockPos.ZERO, Blocks.OAK_LOG.defaultBlockState()), new BlockInfo(new BlockPos(1, 0, 0), Blocks.OAK_LOG.defaultBlockState()), new BlockInfo(new BlockPos(0, 1, 0), Blocks.OAK_LOG.defaultBlockState())));
		BlockState[] under = { Blocks.GLASS.defaultBlockState(), Blocks.POPPY.defaultBlockState(), Blocks.WATER.defaultBlockState() };
		PasteType[] pastes = { FeatureTemplate.UNCHECKED, FeatureTemplate.CHECKED };
		PasteConfig baseConfig = new PasteConfig(3, false, false, false, false);
		int cases = under.length * pastes.length, keptUnder = 0, filledUnder = 0, roots = 0;
		for (int i = 0; i < cases * 2; i++) {
			boolean sapling = i % 2 == 0;
			BlockState underRoot = under[(i / 2) % under.length];
			PasteType paste = pastes[i / (2 * under.length)];
			BlockPos ground = this.saplingGround(level, spawn.getX() + 1200, spawn.getZ() + i * 40, 2);
			BlockPos root = ground.east();
			level.setBlock(root.below(2), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
			level.setBlock(root.below(), underRoot, 2);
			TemplatePlacement<TreeContext> generation = TemplatePlacements.tree();
			TemplatePlacement<TreeContext> placement = sapling ? TemplateFeature.saplingPlacement(generation) : generation;
			paste.get(rooted).apply(level, placement.createContext(), ground, Mirror.NONE, Rotation.NONE, placement, baseConfig);
			if (!level.getBlockState(root).is(Blocks.OAK_LOG)) continue;
			roots++;
			BlockState after = level.getBlockState(root.below());
			if (sapling && after == underRoot) keptUnder++;
			if (!sapling && after.is(Blocks.OAK_LOG)) filledUnder++;
		}
		check("a sapling's tree keeps the glass, flowers and water below its roots (" + keptUnder + " of " + cases + ", generation fills " + filledUnder + " of " + cases + ", roots " + roots + ")", roots == cases * 2 && keptUnder == cases && filledUnder == cases);
		// Fabric: the preset's biome modifiers are applied from a copy of Fabric's list, not added to it. Added, they stayed
		// for every world loaded after this one in the same game (a vanilla world got the preset's trees)
		check("the preset's biome modifiers are not left in Fabric's list (" + fabricModifiersOfThisMod() + ")", fabricModifiersOfThisMod() <= 0);

		// a village exists and its start sits on the terrain, not floating or buried
		BlockPos village = level.findNearestMapStructure(StructureTags.VILLAGE, BlockPos.ZERO, 100, false);
		check("a village is found", village != null);
		if (village != null) {
			ChunkAccess vc = full(level, village.getX() >> 4, village.getZ() >> 4);
			int ground = vc.getHeight(Heightmap.Types.WORLD_SURFACE_WG, village.getX() & 15, village.getZ() & 15);
			boolean onLand = ground > level.getChunkSource().getGenerator().getSeaLevel() - 2;
			RTFCommon.LOGGER.info("[RTF selftest] village at {} {} ground {}", village.getX(), village.getZ(), ground);
			check("the village stands on land above the sea", onLand);
		}
	}

	// -1 without Fabric API
	private static int fabricModifiersOfThisMod() {
		try {
			Class<?> impl = Class.forName("net.fabricmc.fabric.impl.biome.modification.BiomeModificationImpl");
			Object instance = impl.getField("INSTANCE").get(null);
			java.lang.reflect.Field listField = impl.getDeclaredField("modifiers");
			listField.setAccessible(true);
			int count = 0;
			for (Object record : (List<?>) listField.get(instance)) {
				java.lang.reflect.Field id = record.getClass().getDeclaredField("id");
				id.setAccessible(true);
				if (String.valueOf(id.get(record)).startsWith(RTFCommon.MOD_ID + ":")) count++;
			}
			return count;
		} catch (ClassNotFoundException e) {
			return -1;
		} catch (ReflectiveOperationException e) {
			RTFCommon.LOGGER.warn("[RTF selftest] Fabric's biome modifier list could not be read", e);
			return -1;
		}
	}

	private void nether(ServerLevel level) {
		if (level == null) return;
		// the mod's trees in a dimension without its terrain: the preset's biome modifiers replace a biome's trees in every
		// dimension (a mod's or a datapack's with a birch forest), and their chance modifiers threw there ("Feature
		// placement" crash). Placed here in the Nether, whose random state has no ReTerraForged context
		Holder<ConfiguredFeature<?, ?>> birchTrees = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(PresetConfiguredFeatures.BIRCH_TREES).orElse(null);
		if (birchTrees != null) {
			String error = null;
			try {
				for (int i = 0; i < 16; i++) birchTrees.value().place(level, level.getChunkSource().getGenerator(), RandomSource.create(i), new BlockPos(i * 16, 70, 0));
			} catch (RuntimeException e) {
				error = e.toString();
				RTFCommon.LOGGER.error("[RTF selftest] the mod's trees in the Nether", e);
			}
			check("the mod's trees place in a dimension without its terrain" + (error != null ? " (" + error + ")" : ""), error == null);
		}
		// the density sliders only where the preset builds the world: a biome of the overworld is also in other mods'
		// dimensions. Here a filter of density 0 lets the position through (in a world of generation version 0 it did not)
		{
			raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings.Properties properties = level.registryAccess().registry(raccoonman.reterraforged.registries.RTFRegistries.PRESET).flatMap((presets) -> presets.getOptional(Preset.KEY)).map((preset) -> preset.world().properties).orElse(null);
			long through = new raccoonman.reterraforged.world.worldgen.feature.placement.DensityFilter(0.0F).getPositions(new net.minecraft.world.level.levelgen.placement.PlacementContext(level, level.getChunkSource().getGenerator(), Optional.empty()), RandomSource.create(0), new BlockPos(0, 70, 0)).count();
			boolean scales = raccoonman.reterraforged.world.worldgen.feature.placement.DensityFilter.scales(false, properties);
			check("the density sliders " + (scales ? "act" : "do not act") + " in the Nether (" + through + " of 1 position)", through == (scales ? 0 : 1));
		}
		int floorHoles = 0, roofHoles = 0;
		int floorY = level.getMinBuildHeight(), roofY = floorY + level.dimensionType().logicalHeight() - 1;
		for (int cx = -3; cx <= 3; cx++) for (int cz = -3; cz <= 3; cz++) {
			ChunkAccess c = full(level, cx, cz);
			for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
				if (!c.getBlockState(new BlockPos(x, floorY, z)).is(Blocks.BEDROCK)) floorHoles++;
				if (!c.getBlockState(new BlockPos(x, roofY, z)).is(Blocks.BEDROCK)) roofHoles++;
			}
		}
		check("nether bedrock floor is whole (holes: " + floorHoles + ")", floorHoles == 0);
		check("nether bedrock roof is whole (holes: " + roofHoles + ")", roofHoles == 0);
		// one column of each of the 7 x 7 chunks (one may stand in the lava sea), any nether stone (a seed may have a
		// soul sand valley or basalt deltas there)
		int stone = 0;
		for (int cx = -3; cx <= 3; cx++) for (int cz = -3; cz <= 3; cz++) {
			ChunkAccess c = full(level, cx, cz);
			for (int y = floorY + 10; y < floorY + 100; y++) {
				var state = c.getBlockState(new BlockPos(8, y, 8));
				if (state.is(BlockTags.BASE_STONE_NETHER) || state.is(BlockTags.SOUL_FIRE_BASE_BLOCKS) || state.is(BlockTags.NYLIUM)) stone++;
			}
		}
		check("nether is made of nether stone as usual (" + stone + "/4410)", stone > 490);
	}

	private void end(ServerLevel level) {
		if (level == null) return;
		ChunkAccess c = full(level, 0, 0);
		int stone = 0;
		for (int y = 40; y < 70; y++) if (c.getBlockState(new BlockPos(0, y, 0)).is(Blocks.END_STONE)) stone++;
		check("the end island is there", stone > 5);
	}

	// the leaves around a sapling that are persistent or about to decay
	private static int badLeaves(ServerLevel level, BlockPos ground) {
		return (int) BlockPos.betweenClosedStream(ground.offset(-12, 0, -12), ground.offset(12, 40, 12)).map(level::getBlockState)
			.filter((state) -> state.getBlock() instanceof LeavesBlock && (state.getValue(LeavesBlock.PERSISTENT) || state.getValue(LeavesBlock.DISTANCE) >= LeavesBlock.DECAY_DISTANCE)).count();
	}
}
