package raccoonman.reterraforged.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.data.worldgen.PresetExport;
import raccoonman.reterraforged.data.worldgen.preset.settings.Presets;
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
		dispatcher.register(Commands.literal("rtf").requires(s -> s.hasPermission(4)).then(Commands.literal("selftest").executes(ctx -> {
			String result = new RTFSelfTest().run(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})).then(Commands.literal("exportpreset").executes(ctx -> {
			String result = exportPreset(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Component.literal(result), false);
			return 1;
		})));
	}

	// the "Default" preset as a datapack in the server folder, the same pack the preset screen writes: a dedicated
	// server only uses ReTerraForged with this pack in a new world's datapacks folder
	private static String exportPreset(MinecraftServer server) {
		Path out = server.getServerDirectory().resolve("reterraforged-preset.zip").toAbsolutePath();
		try {
			PresetExport.toZip(Presets.makeRTFDefault(), "Default", server.registries().getAccessForLoading(RegistryLayer.DIMENSIONS), out);
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
		check("terrain stays inside the world height", min > level.getMinBuildHeight() + 5 && max < level.getMaxBuildHeight() - 5);

		// bedrock floor of the overworld is whole
		ChunkAccess spawnChunk = full(level, 0, 0);
		int holes = 0;
		for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
			if (!spawnChunk.getBlockState(new BlockPos(x, level.getMinBuildHeight(), z)).is(Blocks.BEDROCK)) holes++;
		}
		check("overworld bedrock floor is whole (holes: " + holes + ")", holes == 0);

		// spawn on dry land
		BlockPos spawn = level.getSharedSpawnPos();
		ChunkAccess sc = full(level, spawn.getX() >> 4, spawn.getZ() >> 4);
		int sy = sc.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX() & 15, spawn.getZ() & 15);
		var below = level.getBlockState(new BlockPos(spawn.getX(), sy - 1, spawn.getZ()));
		check("spawn is on dry land (" + spawn.getX() + ", " + sy + ", " + spawn.getZ() + ": " + below.getBlock().getName().getString() + ")",
				below.getFluidState().isEmpty() && !below.isAir());

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
