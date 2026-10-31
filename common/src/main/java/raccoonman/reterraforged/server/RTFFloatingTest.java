package raccoonman.reterraforged.server;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;

/**
 * "rtf floating [chunks]" (with -Drtf.selftest=true, #28): blocks hanging over water, as a player reported for swamp
 * biomes of biome mods in rivers ("trees and dirt floating on the water"). Builds 16 squares of chunks x chunks chunks
 * spread over 9 x 9 km and counts, near the sea level, the solid blocks (no leaves, no ice) at the top of a column
 * that stand on water: a log, dirt or grass open to the sky with a water block right under it. Counted by the biome at the block; rtf-floating-<rtf|vanilla>.txt lists
 * the places. Prints "[RTF floating] <rtf|vanilla> blocks=N columns=C top: biome n, ...".
 */
public final class RTFFloatingTest {
	private static final int SQUARES = 4, SPACING = 3000, BAND = 4;

	private RTFFloatingTest() {
	}

	public static String run(MinecraftServer server, int chunks) {
		try {
			return test(server, chunks);
		} catch (Throwable t) {
			RTFCommon.LOGGER.error("[RTF floating] exception", t);
			return "[RTF floating] FAIL exception: " + t;
		}
	}

	private static String test(MinecraftServer server, int chunks) throws IOException {
		ServerLevel level = server.overworld();
		boolean rtf = (Object) level.getChunkSource().randomState() instanceof RTFRandomState s && s.generatorContext() != null;
		String kind = rtf ? "rtf" : "vanilla";
		BlockPos spawn = level.getRespawnData().pos();
		int sea = level.getSeaLevel();
		Map<String, Integer> byBiome = new TreeMap<>();
		Map<String, Integer> byBlock = new TreeMap<>();
		List<String> lines = new ArrayList<>();
		lines.add("# " + kind + " world, seed " + level.getSeed() + ": x y z block biome (a solid block with water right under it, y within "
			+ BAND + " of the sea level " + sea + ")");
		int blocks = 0, columns = 0;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int i = 0; i < SQUARES; i++) {
			for (int j = 0; j < SQUARES; j++) {
				int cx0 = ((spawn.getX() + (int) ((i - (SQUARES - 1) / 2.0) * SPACING)) >> 4) - chunks / 2;
				int cz0 = ((spawn.getZ() + (int) ((j - (SQUARES - 1) / 2.0) * SPACING)) >> 4) - chunks / 2;
				for (int cx = cx0; cx < cx0 + chunks; cx++) {
					for (int cz = cz0; cz < cz0 + chunks; cz++) {
						ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, true);
						for (int x = 0; x < 16; x++) {
							for (int z = 0; z < 16; z++) {
								boolean found = false;
								for (int y = sea - BAND; y <= sea + BAND; y++) {
									pos.set(cx * 16 + x, y, cz * 16 + z);
									BlockState state = chunk.getBlockState(pos);
									if (state.isAir() || state.is(BlockTags.LEAVES) || state.is(BlockTags.ICE) || !state.blocksMotion() || !state.getFluidState().isEmpty()) continue;
									// the top of the column: open to the sky above (plants and leaves do not count), not a cave roof
									BlockState above = chunk.getBlockState(pos.move(0, 1, 0));
									pos.move(0, -1, 0);
									if (above.blocksMotion() && !above.is(BlockTags.LEAVES) && !above.is(BlockTags.LOGS)) continue;
									BlockState below = chunk.getBlockState(pos.move(0, -1, 0));
									pos.move(0, 1, 0);
									if (!below.getFluidState().is(FluidTags.WATER) || below.blocksMotion()) continue;
									byBlock.merge(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), 1, Integer::sum);
									String biome = level.getBiome(pos).getRegisteredName();
									byBiome.merge(biome, 1, Integer::sum);
									blocks++;
									found = true;
									if (lines.size() < 2000) {
										lines.add(pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + BuiltInRegistries.BLOCK.getKey(state.getBlock()) + " " + biome);
									}
								}
								if (found) columns++;
							}
						}
					}
				}
			}
		}
		Files.write(server.getServerDirectory().resolve("rtf-floating-" + kind + ".txt").toAbsolutePath(), lines);
		StringBuilder top = new StringBuilder();
		byBiome.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(8)
			.forEach((e) -> top.append(top.length() == 0 ? "" : ", ").append(e.getKey()).append(' ').append(e.getValue()));
		StringBuilder kinds = new StringBuilder();
		byBlock.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(6)
			.forEach((e) -> kinds.append(kinds.length() == 0 ? "" : ", ").append(e.getKey()).append(' ').append(e.getValue()));
		String r = "[RTF floating] " + kind + " blocks=" + blocks + " columns=" + columns + " top: " + top + "; blocks: " + kinds;
		RTFCommon.LOGGER.info(r);
		return r;
	}
}
