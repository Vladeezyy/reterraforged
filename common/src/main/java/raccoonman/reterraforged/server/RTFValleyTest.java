package raccoonman.reterraforged.server;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import javax.imageio.ImageIO;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.concurrent.cache.CacheManager;
import raccoonman.reterraforged.config.PerformanceConfig;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.Presets;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.server.stress.RTFStressTest;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;

/**
 * "rtf valleys &lt;name&gt; &lt;x&gt; &lt;z&gt; &lt;seed&gt; &lt;spec&gt;" (#29): the terrain of a preset around x, z on a seed as
 * the preset screen makes it (no world, no erosion filter), 2 blocks per pixel over 1536 x 1536 blocks, hill-shaded,
 * written to rtf-valleys-&lt;name&gt;.png. The line says how much of the land, and of the land in river and lake valleys,
 * is steeper than 45, 63 and 72 degrees (1, 2 and 3 blocks of height per block). spec: default, set:... (Default with
 * settings changed, see RTFStressTest.makePreset), or file:&lt;preset.json&gt; with optional |set:... after it (a preset
 * file, such as the preset.json of an exported datapack, with settings changed).
 */
public final class RTFValleyTest {
	private static final int TILES = 3;
	private static final int ZOOM = 2;

	private RTFValleyTest() {
	}

	public static String run(MinecraftServer server, String name, int x, int z, long seed, String spec) {
		try {
			CacheManager.clear();
		} catch (Exception e) {
			RTFCommon.LOGGER.warn("[RTF valleys] cache clear failed", e);
		}
		Preset preset;
		try {
			preset = load(spec);
		} catch (Exception e) {
			return "[RTF valleys] FAIL reading " + spec + ": " + e;
		}
		HolderLookup.Provider patch = preset.buildPatch(server.registries().getAccessForLoading(RegistryLayer.DIMENSIONS));
		Preset patched = patch.lookupOrThrow(RTFRegistries.PRESET).getOrThrow(Preset.KEY).value();
		GeneratorContext ctx = GeneratorContext.makeUncached(patched, patch.lookupOrThrow(RTFRegistries.NOISE), (int) seed, 4, 0, PerformanceConfig.makeDefault().batchCount());
		Levels levels = ctx.levels;
		int tile = 256, size = tile * TILES;
		// height above sea level in blocks, and whether the cell is in a valley (river mask below 1)
		float[] height = new float[size * size];
		boolean[] valley = new boolean[size * size];
		for (int tz = 0; tz < TILES; tz++) for (int tx = 0; tx < TILES; tx++) {
			float cx = x + (tx - (TILES - 1) / 2.0F) * tile * ZOOM, cz = z + (tz - (TILES - 1) / 2.0F) * tile * ZOOM;
			try (Tile t = ctx.generator.generateZoomed(cx, cz, ZOOM, false).join()) {
				int width = t.getBlockSize().size();
				Cell[] cells = t.getBacking();
				for (int pz = 0; pz < tile; pz++) for (int px = 0; px < tile; px++) {
					Cell cell = cells[pz * width + px];
					int i = (tz * tile + pz) * size + tx * tile + px;
					height[i] = (cell.height - levels.water) * levels.worldHeight;
					valley[i] = cell.riverMask < 1.0F;
				}
			}
		}
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		long land = 0, valleyLand = 0;
		long[] steep = new long[3], steepValley = new long[3];
		float maxSlope = 0.0F, maxValleySlope = 0.0F;
		for (int pz = 0; pz < size; pz++) for (int px = 0; px < size; px++) {
			int i = pz * size + px;
			float h = height[i];
			// the steepest drop to a neighbour pixel, as height per block
			float slope = 0.0F;
			float dx = 0.0F, dz = 0.0F;
			if (px > 0 && px < size - 1 && pz > 0 && pz < size - 1) {
				for (int n : new int[] { i - 1, i + 1, i - size, i + size }) {
					slope = Math.max(slope, Math.abs(h - height[n]) / ZOOM);
				}
				dx = (height[i + 1] - height[i - 1]) / (2.0F * ZOOM);
				dz = (height[i + size] - height[i - size]) / (2.0F * ZOOM);
			}
			int rgb;
			if (h <= 0.0F) {
				int b = (int) Math.max(40, 255 + h * 2.5F);
				rgb = (b / 4) << 8 | b;
			} else {
				land++;
				maxSlope = Math.max(maxSlope, slope);
				for (int k = 0; k < 3; k++) if (slope > k + 1) steep[k]++;
				if (valley[i]) {
					valleyLand++;
					maxValleySlope = Math.max(maxValleySlope, slope);
					for (int k = 0; k < 3; k++) if (slope > k + 1) steepValley[k]++;
				}
				// light from the north west, so slopes and cliffs show up as shading
				float nx = -dx, nz = -dz, len = (float) Math.sqrt(nx * nx + nz * nz + 1.0F);
				float light = Math.max(0.15F, (-0.577F * nx - 0.577F * nz + 0.577F) / len);
				int g = (int) Math.min(255, 70 + h * 0.6F);
				int r = valley[i] ? g * 3 / 4 : g, gr = g, bl = valley[i] ? g : g * 3 / 4;
				rgb = (int) (r * light) << 16 | (int) (gr * light) << 8 | (int) (bl * light);
			}
			image.setRGB(px, pz, rgb);
		}
		Path out = server.getServerDirectory().resolve("rtf-valleys-" + name + ".png").toAbsolutePath();
		try {
			ImageIO.write(image, "png", out.toFile());
		} catch (IOException e) {
			return "[RTF valleys] FAIL writing " + out + ": " + e;
		}
		String r = String.format(Locale.ROOT, "[RTF valleys] %s (%s, seed %d, at %d %d): land %d px, steeper than 45/63/72 deg %.2f/%.2f/%.2f %% (max %.1f); in valleys %d px, %.2f/%.2f/%.2f %% (max %.1f) -> %s",
			name, spec, seed, x, z, land, pct(steep[0], land), pct(steep[1], land), pct(steep[2], land), maxSlope,
			valleyLand, pct(steepValley[0], valleyLand), pct(steepValley[1], valleyLand), pct(steepValley[2], valleyLand), maxValleySlope, out);
		RTFCommon.LOGGER.info(r);
		return r;
	}

	private static double pct(long part, long total) {
		return 100.0 * part / Math.max(1, total);
	}

	private static Preset load(String spec) throws IOException {
		if (!spec.startsWith("file:")) {
			return spec.equals("default") ? Presets.makeDefault() : RTFStressTest.makePreset(spec);
		}
		String[] parts = spec.substring(5).split("\\|", 2);
		JsonElement json = JsonParser.parseString(Files.readString(Path.of(parts[0].trim())));
		Preset preset = Preset.DIRECT_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
		if (parts.length > 1) {
			RTFStressTest.applySettings(preset, parts[1].trim());
		}
		return preset;
	}
}
