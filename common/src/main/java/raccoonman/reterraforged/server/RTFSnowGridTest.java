package raccoonman.reterraforged.server;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

import javax.imageio.ImageIO;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import raccoonman.reterraforged.RTFCommon;

/**
 * "rtf snowgrid <biome> [chunks]" (with -Drtf.selftest=true, tools/servertest.sh SNOWGRID="<biome> ..."): the snow layers
 * on the ground where the biome is (issue #28: an extra layer outlines the chunks of snowy biomes, a 16 x 16 grid).
 * Builds chunks x chunks chunks around the nearest place of the biome, counts the layers on top of each column (a snow
 * block is 8) and compares the columns on a chunk's edge with those inside: mean layers, the share with more than one
 * layer, and how often two neighbouring columns differ across a border and inside a chunk (seam = border / inner; about
 * 1 when nothing works chunk by chunk). Writes rtf-snowgrid-<biome>.png: white 1 layer, then darker blue per layer.
 */
public final class RTFSnowGridTest {

	private RTFSnowGridTest() {
	}

	public static String run(MinecraftServer server, String biome, int chunks) {
		try {
			return measure(server, biome, chunks);
		} catch (Throwable t) {
			RTFCommon.LOGGER.error("[RTF snowgrid] exception", t);
			return "[RTF snowgrid] " + biome + " FAIL exception: " + t;
		}
	}

	private static String measure(MinecraftServer server, String biome, int chunks) throws IOException {
		ServerLevel level = server.overworld();
		ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, Identifier.parse(biome));
		BlockPos centre = RTFSurfaceMap.find(level, key, 8000);
		if (centre == null) {
			return "[RTF snowgrid] " + biome + " FAIL not found";
		}
		int size = chunks * 16;
		int cx0 = (centre.getX() >> 4) - chunks / 2, cz0 = (centre.getZ() >> 4) - chunks / 2;
		int[][] layers = new int[size][size];
		int[][] ground = new int[size][size];
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int cx = 0; cx < chunks; cx++) for (int cz = 0; cz < chunks; cz++) {
			ChunkAccess c = level.getChunk(cx0 + cx, cz0 + cz, ChunkStatus.FULL, true);
			for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
				int y = c.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
				// the ground under the snow (snow blocks of deep snow are snow too)
				int g = y;
				while (g > level.getMinY() && (c.getBlockState(pos.set(x, g, z)).isAir() || c.getBlockState(pos).is(Blocks.SNOW) || c.getBlockState(pos).is(Blocks.SNOW_BLOCK))) g--;
				ground[cx * 16 + x][cz * 16 + z] = g;
				BlockState state = c.getBlockState(pos.set(x, y, z));
				layers[cx * 16 + x][cz * 16 + z] = state.is(Blocks.SNOW) ? state.getValue(SnowLayerBlock.LAYERS) : state.is(Blocks.SNOW_BLOCK) ? 8 : 0;
			}
		}
		// columns on a chunk's edge (x or z 0 or 15) and inside, of those with snow
		long edge = 0, edgeLayers = 0, edgeDeep = 0, inner = 0, innerLayers = 0, innerDeep = 0;
		for (int x = 0; x < size; x++) for (int z = 0; z < size; z++) {
			int l = layers[x][z];
			if (l == 0) continue;
			boolean onEdge = (x & 15) == 0 || (x & 15) == 15 || (z & 15) == 0 || (z & 15) == 15;
			if (onEdge) { edge++; edgeLayers += l; edgeDeep += l > 1 ? 1 : 0; } else { inner++; innerLayers += l; innerDeep += l > 1 ? 1 : 0; }
		}
		// the mean layers by the column's place in its chunk (0 .. 15 along x, then z): a ridge on a chunk border is a
		// peak at 0 or 15
		double[] byX = new double[16], byZ = new double[16];
		int[] nX = new int[16], nZ = new int[16];
		for (int x = 0; x < size; x++) for (int z = 0; z < size; z++) {
			int l = layers[x][z];
			if (l == 0) continue;
			byX[x & 15] += l; nX[x & 15]++;
			byZ[z & 15] += l; nZ[z & 15]++;
		}
		StringBuilder profile = new StringBuilder();
		for (double[][] row : new double[][][] { { byX }, { byZ } }) {
			double[] sum = row[0];
			int[] n = sum == byX ? nX : nZ;
			profile.append(sum == byX ? " x" : " z");
			for (int k = 0; k < 16; k++) profile.append(String.format(Locale.ROOT, " %.2f", n[k] == 0 ? 0 : sum[k] / n[k]));
		}
		// the ground's steps between neighbouring columns across a chunk border and inside a chunk (a ridge or step of
		// the terrain itself on the borders shows here, the snow only follows it)
		long gb = 0, gbSum = 0, gi = 0, giSum = 0;
		for (int a = 0; a < size; a++) for (int b = 0; b + 1 < size; b++) {
			boolean seam = (b & 15) == 15;
			int d = Math.abs(ground[b][a] - ground[b + 1][a]) + Math.abs(ground[a][b] - ground[a][b + 1]);
			if (seam) { gb += 2; gbSum += d; } else { gi += 2; giSum += d; }
		}
		// neighbour pairs of snowy columns across a chunk border and inside a chunk: how often their layers differ
		long border = 0, borderDiff = 0, in = 0, inDiff = 0;
		for (int a = 0; a < size; a++) for (int b = 0; b + 1 < size; b++) {
			boolean seam = (b & 15) == 15;
			for (int[] p : new int[][] { { layers[b][a], layers[b + 1][a] }, { layers[a][b], layers[a][b + 1] } }) {
				if (p[0] == 0 || p[1] == 0) continue;
				int d = p[0] != p[1] ? 1 : 0;
				if (seam) { border++; borderDiff += d; } else { in++; inDiff += d; }
			}
		}
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < size; x++) for (int z = 0; z < size; z++) {
			int l = layers[x][z];
			int rgb = l == 0 ? 0x5a6e3c : l == 1 ? 0xffffff : Math.max(0, 255 - l * 28) << 16 | Math.max(0, 255 - l * 20) << 8 | 255;
			image.setRGB(x, z, rgb);
		}
		Path out = RTFSurfaceMap.outputFile(server, "rtf-snowgrid-", biome, ".png");
		ImageIO.write(image, "png", out.toFile());
		double b = border == 0 ? 0 : (double) borderDiff / border, i = in == 0 ? 0 : (double) inDiff / in;
		String r = String.format(Locale.ROOT, "[RTF snowgrid] %s seam=%.2f (layers differ across borders %.3f, inside %.3f); mean layers edge %.2f inner %.2f; "
			+ "more than one layer edge %.1f%% inner %.1f%%; snowy columns %d of %d at %d %d; ground steps across borders %.3f inside %.3f; mean layers by place in the chunk%s",
			biome, i > 0 ? b / i : 0, b, i, edge == 0 ? 0 : (double) edgeLayers / edge, inner == 0 ? 0 : (double) innerLayers / inner,
			edge == 0 ? 0 : 100.0 * edgeDeep / edge, inner == 0 ? 0 : 100.0 * innerDeep / inner, edge + inner, size * size, centre.getX(), centre.getZ(), (double) gbSum / gb, (double) giSum / gi, profile);
		RTFCommon.LOGGER.info(r);
		return r;
	}
}
