package raccoonman.reterraforged.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.cell.Cell;

/**
 * "rtf structures" — with -Drtf.selftest=true (tools/servertest.sh runs it after the self test). Every vanilla
 * structure of the overworld is looked for near the spawn (3 rings of its placement grid) and built: it must have a start, its pieces
 * must stand on the ground (no air under their floor) and not be buried (no terrain over their top) where the
 * structure belongs on the surface, villages must have paths, and none may start on mountain terrain (0.0.6's rule).
 * Structures meant to be buried or sunk (ruined portals, trail ruins, buried treasure, shipwrecks, ocean ruins) and
 * the underground ones are only looked for. Some need biomes that may not be near: not finding those is no failure.
 */
public final class RTFStructureTest {
	// the search radius counts rings of each structure's placement grid (its spacing, 20 to 80 chunks), not chunks
	private static final int RADIUS = 3;

	private enum Kind {
		// stands on the terrain, nothing above it
		SURFACE,
		// a village: surface, with paths
		VILLAGE,
		// buried, sunk or underground by design: only looked for
		OTHER
	}

	private record Target(ResourceKey<Structure> key, Kind kind, boolean required, int rings) {

		Target(ResourceKey<Structure> key, Kind kind, boolean required) {
			this(key, kind, required, RADIUS);
		}
	}

	private static final List<Target> TARGETS = List.of(
		new Target(BuiltinStructures.VILLAGE_PLAINS, Kind.VILLAGE, false),
		new Target(BuiltinStructures.VILLAGE_DESERT, Kind.VILLAGE, false),
		new Target(BuiltinStructures.VILLAGE_SAVANNA, Kind.VILLAGE, false),
		new Target(BuiltinStructures.VILLAGE_SNOWY, Kind.VILLAGE, false),
		new Target(BuiltinStructures.VILLAGE_TAIGA, Kind.VILLAGE, false),
		new Target(BuiltinStructures.PILLAGER_OUTPOST, Kind.SURFACE, false),
		new Target(BuiltinStructures.DESERT_PYRAMID, Kind.OTHER, false),
		new Target(BuiltinStructures.JUNGLE_TEMPLE, Kind.SURFACE, false),
		new Target(BuiltinStructures.SWAMP_HUT, Kind.OTHER, false),
		new Target(BuiltinStructures.IGLOO, Kind.OTHER, false),
		new Target(BuiltinStructures.WOODLAND_MANSION, Kind.SURFACE, false),
		new Target(BuiltinStructures.OCEAN_MONUMENT, Kind.OTHER, false),
		new Target(BuiltinStructures.SHIPWRECK, Kind.OTHER, false),
		new Target(BuiltinStructures.SHIPWRECK_BEACHED, Kind.OTHER, false),
		new Target(BuiltinStructures.OCEAN_RUIN_COLD, Kind.OTHER, false),
		new Target(BuiltinStructures.OCEAN_RUIN_WARM, Kind.OTHER, false),
		new Target(BuiltinStructures.RUINED_PORTAL_STANDARD, Kind.OTHER, true),
		new Target(BuiltinStructures.RUINED_PORTAL_MOUNTAIN, Kind.OTHER, false),
		new Target(BuiltinStructures.RUINED_PORTAL_OCEAN, Kind.OTHER, false),
		// a grid of single chunks with a small chance each
		new Target(BuiltinStructures.BURIED_TREASURE, Kind.OTHER, false, 30),
		new Target(BuiltinStructures.TRAIL_RUINS, Kind.OTHER, false),
		new Target(BuiltinStructures.MINESHAFT, Kind.OTHER, true, 40),
		new Target(BuiltinStructures.MINESHAFT_MESA, Kind.OTHER, false, 40),
		new Target(BuiltinStructures.STRONGHOLD, Kind.OTHER, true),
		new Target(BuiltinStructures.ANCIENT_CITY, Kind.OTHER, false),
		new Target(BuiltinStructures.TRIAL_CHAMBERS, Kind.OTHER, false)
	);

	private RTFStructureTest() {
	}

	public static String run(MinecraftServer server) {
		ServerLevel level = server.overworld();
		BlockPos spawn = level.getSharedSpawnPos();
		List<String> failures = new ArrayList<>();
		List<String> missing = new ArrayList<>();
		int found = 0, villages = 0;
		long start = System.nanoTime();
		for (Target target : TARGETS) {
			String name = target.key().location().getPath();
			Optional<Holder.Reference<Structure>> holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(target.key());
			if (holder.isEmpty()) {
				missing.add(name);
				continue;
			}
			Pair<BlockPos, Holder<Structure>> result = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder.get()), spawn, target.rings(), false);
			if (result == null) {
				missing.add(name);
				if (target.required()) failures.add(name + " not found within " + target.rings() + " placement rings");
				RTFCommon.LOGGER.info("[RTF structures] {}: none within {} placement rings", name, target.rings());
				continue;
			}
			found++;
			String problem = check(level, holder.get().value(), result.getFirst(), target, name);
			if (target.kind() == Kind.VILLAGE) villages++;
			if (problem != null) failures.add(problem);
		}
		if (villages == 0) failures.add("no village within " + RADIUS + " placement rings");
		String summary = String.format("[RTF structures] %s: %d of %d structures found in %d s, not near: %s%s",
			failures.isEmpty() ? "PASS" : "FAIL " + failures.size(), found, TARGETS.size(), (System.nanoTime() - start) / 1_000_000_000L, missing, failures.isEmpty() ? "" : "; " + failures);
		RTFCommon.LOGGER.info(summary);
		return summary;
	}

	private static boolean isStreet(BlockState state) {
		return state.is(Blocks.DIRT_PATH) || state.is(Blocks.SMOOTH_SANDSTONE);
	}

	// null when it is fine, else what is wrong
	private static String check(ServerLevel level, Structure structure, BlockPos pos, Target target, String name) {
		ChunkAccess startChunk = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS, true);
		StructureStart start = startChunk.getStartForStructure(structure);
		if (start == null || !start.isValid()) {
			return name + " at " + pos.getX() + ", " + pos.getZ() + " has no start";
		}
		BoundingBox box = start.getBoundingBox();
		// the terrain where it starts: 0.0.6 keeps structures that start at the surface off mountains
		String terrain = "?";
		if ((Object) level.getChunkSource().randomState() instanceof RTFRandomState state && state.generatorContext() != null) {
			Cell cell = new Cell();
			state.generatorContext().lookup.applyCell(cell.reset(), box.getCenter().getX(), box.getCenter().getZ(), false);
			terrain = cell.terrain.getName();
			if (target.kind() != Kind.OTHER && cell.terrain.isMountain() && state.preset() != null && state.preset().miscellaneous().originalGeneration) {
				return name + " at " + box.getCenter().getX() + ", " + box.getCenter().getZ() + " starts on " + terrain;
			}
		}
		if (target.kind() == Kind.OTHER) {
			RTFCommon.LOGGER.info("[RTF structures] {} at {} {} {}: {} pieces, terrain {}", name, box.getCenter().getX(), box.minY(), box.getCenter().getZ(), start.getPieces().size(), terrain);
			return null;
		}
		// built, with a chunk around it
		for (int cx = (box.minX() >> 4) - 1; cx <= (box.maxX() >> 4) + 1; cx++) {
			for (int cz = (box.minZ() >> 4) - 1; cz <= (box.maxZ() >> 4) + 1; cz++) {
				level.getChunk(cx, cz, ChunkStatus.FULL, true);
			}
		}
		int floating = 0, buried = 0;
		List<StructurePiece> pieces = start.getPieces();
		for (StructurePiece piece : pieces) {
			BoundingBox b = piece.getBoundingBox();
			// four points inside the piece: air below its floor at most of them is floating
			int air = 0, covered = 0;
			int[][] points = { { b.minX() + 1, b.minZ() + 1 }, { b.maxX() - 1, b.minZ() + 1 }, { b.minX() + 1, b.maxZ() - 1 }, { b.maxX() - 1, b.maxZ() - 1 } };
			for (int[] p : points) {
				int gap = 0;
				for (int y = b.minY() - 1; y > b.minY() - 6 && level.getBlockState(new BlockPos(p[0], y, p[1])).isAir(); y--) gap++;
				if (gap >= 4) air++;
				// terrain (not leaves or another piece) over the piece's top
				BlockState above = level.getBlockState(new BlockPos(p[0], b.maxY() + 3, p[1]));
				if (above.is(BlockTags.DIRT) || above.is(BlockTags.BASE_STONE_OVERWORLD) || above.is(BlockTags.SAND) || above.is(Blocks.GRAVEL)) covered++;
			}
			// only pieces on the structure's lowest level stand on the ground: under the rooms of a mansion's upper floors
			// are the rooms below
			if (air >= 3 && b.minY() <= box.minY() + 1) floating++;
			if (covered >= 3) buried++;
		}
		int paths = 0;
		if (target.kind() == Kind.VILLAGE) {
			for (int x = box.minX(); x <= box.maxX(); x++) {
				for (int z = box.minZ(); z <= box.maxZ(); z++) {
					int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
					// streets: dirt paths, smooth sandstone in desert villages
					if (isStreet(level.getBlockState(new BlockPos(x, y, z))) || isStreet(level.getBlockState(new BlockPos(x, y - 1, z)))) paths++;
				}
			}
		}
		RTFCommon.LOGGER.info("[RTF structures] {} at {} {} {}: {} pieces, {} floating, {} buried{}, terrain {}", name, box.getCenter().getX(), box.minY(), box.getCenter().getZ(), pieces.size(), floating, buried, target.kind() == Kind.VILLAGE ? ", " + paths + " path blocks" : "", terrain);
		// a piece or two over a ravine or a cave can float or sit in a slope; most of them may not
		int limit = Math.max(1, pieces.size() / 5);
		if (floating > limit) return name + " at " + box.getCenter().getX() + ", " + box.getCenter().getZ() + ": " + floating + " of " + pieces.size() + " pieces float";
		if (buried > limit) return name + " at " + box.getCenter().getX() + ", " + box.getCenter().getZ() + ": " + buried + " of " + pieces.size() + " pieces are buried";
		if (target.kind() == Kind.VILLAGE && paths == 0) return name + " at " + box.getCenter().getX() + ", " + box.getCenter().getZ() + " has no paths";
		return null;
	}
}
