package raccoonman.reterraforged.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.data.worldgen.preset.settings.MiscellaneousSettings;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.feature.DecorateSnowFeature;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

/**
 * "rtf snowline" (with -Drtf.selftest=true): the snow line of the preset. Finds the highest ground within 1536 blocks
 * of the spawn, builds 8 x 8 chunks around it and looks at the top of every column: above the line and its variation
 * the ground (or the tree on it) has to be under snow, still water frozen. Prints "[RTF snowline] line=Y variation=V
 * top=Y columns above=A snowy=S (P %) below=B snowy=S2" (S2: the snow of cold biomes, not checked). A column where snow
 * cannot lie (a slab, a flower bed) or that the natural snow decorator cleared from steep rock does not count.
 * With a depth of 2 layers or more the snow of each column above the line is also checked against the layers the snow
 * decorator gives it ("depth checked=C right=R"; a column of one layer can be the smoothing's, not checked).
 */
public final class RTFSnowLineTest {
	private static final int CHUNKS = 8;

	private RTFSnowLineTest() {
	}

	public record Result(int line, int variation, int top, int above, int snowyAbove, int below, int snowyBelow, int bare, int depthChecked, int depthRight, String layers) {
		public double share() {
			return this.above == 0 ? 0.0 : 100.0 * this.snowyAbove / this.above;
		}

		public double depthShare() {
			return this.depthChecked == 0 ? 100.0 : 100.0 * this.depthRight / this.depthChecked;
		}

		public String describe() {
			return String.format(Locale.ROOT, "line=%d variation=%d top=%d columns above=%d snowy=%d (%.1f %%) below=%d snowy=%d bare rock=%d depth checked=%d right=%d (%.1f %%) layers %s",
				this.line, this.variation, this.top, this.above, this.snowyAbove, this.share(), this.below, this.snowyBelow, this.bare, this.depthChecked, this.depthRight, this.depthShare(), this.layers);
		}
	}

	public static String run(MinecraftServer server) {
		try {
			ServerLevel level = server.overworld();
			if (!((Object) level.getChunkSource().randomState() instanceof RTFRandomState s) || s.preset() == null) {
				return "[RTF snowline] not a ReTerraForged world";
			}
			String r = "[RTF snowline] " + (s.preset().miscellaneous().snowLine.enabled ? "" : "(off) ") + sample(level, s.preset().miscellaneous().snowLine).describe();
			RTFCommon.LOGGER.info(r);
			return r;
		} catch (Throwable t) {
			RTFCommon.LOGGER.error("[RTF snowline] exception", t);
			return "[RTF snowline] FAIL exception: " + t;
		}
	}

	public static Result sample(ServerLevel level, MiscellaneousSettings.SnowLine snowLine) {
		var gen = level.getChunkSource().getGenerator();
		var rs = level.getChunkSource().randomState();
		BlockPos spawn = level.getSharedSpawnPos();
		int bestX = spawn.getX(), bestZ = spawn.getZ(), bestY = Integer.MIN_VALUE;
		for (int x = -1536; x <= 1536; x += 64) {
			for (int z = -1536; z <= 1536; z += 64) {
				int y = gen.getBaseHeight(spawn.getX() + x, spawn.getZ() + z, Heightmap.Types.WORLD_SURFACE_WG, level, rs);
				if (y > bestY) {
					bestY = y;
					bestX = spawn.getX() + x;
					bestZ = spawn.getZ() + z;
				}
			}
		}
		int high = snowLine.height + snowLine.variation, low = snowLine.height - snowLine.variation;
		int above = 0, snowyAbove = 0, below = 0, snowyBelow = 0, bare = 0, depthChecked = 0, depthRight = 0;
		// columns above the line by their number of snow layers
		int[] layerCounts = new int[MiscellaneousSettings.SnowLine.MAX_DEPTH + 1];
		List<String> wrong = new ArrayList<>();
		Noise rand = (Object) rs instanceof RTFRandomState rtfState && rtfState.generatorContext() != null ? DecorateSnowFeature.randomNoise(rtfState.generatorContext()) : null;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int cx0 = (bestX >> 4) - CHUNKS / 2, cz0 = (bestZ >> 4) - CHUNKS / 2;
		for (int cx = cx0; cx < cx0 + CHUNKS; cx++) for (int cz = cz0; cz < cz0 + CHUNKS; cz++) {
			ChunkAccess c = level.getChunk(cx, cz, ChunkStatus.FULL, true);
			for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
				int top = c.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
				BlockState state = c.getBlockState(pos.set((cx << 4) + x, top, (cz << 4) + z));
				BlockState over = c.getBlockState(pos.set((cx << 4) + x, top + 1, (cz << 4) + z));
				boolean snowy = over.is(Blocks.SNOW) || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.ICE);
				if (top > high) {
					if (snowy) {
						above++;
						snowyAbove++;
						// the snow's layers and the ground under it (two layers and more are in the heightmap)
						BlockState snow = state.is(Blocks.SNOW) ? state : over.is(Blocks.SNOW) ? over : null;
						if (snow != null) {
							int layers = snow.getValue(SnowLayerBlock.LAYERS);
							layerCounts[layers]++;
							int ground = state.is(Blocks.SNOW) ? top - 1 : top;
							int expected = rand == null ? 0 : DecorateSnowFeature.snowLineLayers(ground, pos.getX(), pos.getZ(), snowLine.height, snowLine.variation, snowLine.depth, snowLine.peakDepth, snowLine.depthRange, rand);
							if (expected >= 2) {
								depthChecked++;
								if (layers == expected) depthRight++;
								else if (wrong.size() < 8) wrong.add(pos.toShortString() + " " + layers + "/" + expected + " " + state.getBlock().getDescriptionId());
							}
						}
					} else if (Blocks.SNOW.defaultBlockState().canSurvive(level, pos) && over.isAir()) {
						// snow could lie here: missing, unless the natural snow decorator cleared steep rock
						if (state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.GRAVEL)) bare++;
						else above++;
					}
				} else if (top < low && top > level.getSeaLevel()) {
					below++;
					if (snowy) snowyBelow++;
				}
			}
		}
		StringBuilder layers = new StringBuilder();
		for (int i = 1; i < layerCounts.length; i++) layers.append(i == 1 ? "" : " ").append(i).append(':').append(layerCounts[i]);
		if (!wrong.isEmpty()) layers.append("; wrong depth (layers/expected) at ").append(String.join(", ", wrong));
		return new Result(snowLine.height, snowLine.variation, bestY, above, snowyAbove, below, snowyBelow, bare, depthChecked, depthRight, layers.toString());
	}
}
