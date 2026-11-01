package raccoonman.reterraforged.server;

import java.util.Optional;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;

/**
 * "rtf locate &lt;structure&gt; &lt;biome&gt;" (with -Drtf.selftest=true, #34): the nearest place of the biome to the spawn,
 * then the nearest structure from there within RINGS rings of its placement grid, timed. A structure of a biome far
 * from the spawn ("rtf structures" only looks around the spawn), such as the swamp hut, is found or not this way.
 * Prints "[RTF locate] &lt;rtf|vanilla&gt; &lt;structure&gt; found at X Z, D blocks from &lt;biome&gt; at X Z (N ms)" or
 * "... not found within RINGS rings of &lt;biome&gt; at X Z (N ms)".
 */
public final class RTFLocateTest {
	private static final int RINGS = 10;

	private RTFLocateTest() {
	}

	public static String run(MinecraftServer server, String structureId, String biomeId) {
		try {
			return test(server, structureId, biomeId);
		} catch (Throwable t) {
			RTFCommon.LOGGER.error("[RTF locate] exception", t);
			return "[RTF locate] FAIL exception: " + t;
		}
	}

	private static String test(MinecraftServer server, String structureId, String biomeId) {
		ServerLevel level = server.overworld();
		String kind = (Object) level.getChunkSource().randomState() instanceof RTFRandomState s && s.generatorContext() != null ? "rtf" : "vanilla";
		Optional<Holder.Reference<Structure>> structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(ResourceKey.create(Registries.STRUCTURE, Identifier.parse(structureId)));
		if (structure.isEmpty()) {
			return "[RTF locate] FAIL no structure " + structureId;
		}
		ResourceKey<Biome> biome = ResourceKey.create(Registries.BIOME, Identifier.parse(biomeId));
		long start = System.nanoTime();
		BlockPos spawn = level.getRespawnData().pos();
		Pair<BlockPos, Holder<Biome>> place = level.findClosestBiome3d((b) -> b.is(biome), spawn.atY(level.getSeaLevel()), 6400, 32, 64);
		if (place == null) {
			return "[RTF locate] " + kind + " " + structureId + ": no " + biomeId + " within 6400 blocks";
		}
		BlockPos from = place.getFirst();
		Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(structure.get()), from, RINGS, false);
		long ms = (System.nanoTime() - start) / 1_000_000;
		String r;
		if (found == null) {
			r = String.format("[RTF locate] %s %s not found within %d rings of %s at %d %d (%d ms)", kind, structureId, RINGS, biomeId, from.getX(), from.getZ(), ms);
		} else {
			BlockPos at = found.getFirst();
			int d = (int) Math.sqrt(at.distSqr(from.atY(at.getY())));
			r = String.format("[RTF locate] %s %s found at %d %d, %d blocks from %s at %d %d (%d ms)", kind, structureId, at.getX(), at.getZ(), d, biomeId, from.getX(), from.getZ(), ms);
		}
		RTFCommon.LOGGER.info(r);
		return r;
	}
}
