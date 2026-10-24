package raccoonman.reterraforged.server;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

import javax.imageio.ImageIO;

import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.client.gui.screen.presetconfig.RenderMode;
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
 * "rtf previewmap &lt;name&gt; &lt;blocks per pixel&gt; &lt;default|set:...&gt;": the terrain of a preset as the preset screen
 * makes it (no world needed), 768 x 768 pixels around 0, 0, written to rtf-previewmap-&lt;name&gt;.png. Land is shaded
 * by height (mountain terrains in red), water by depth; the line says the share of mountain cells, the land height
 * and the depth of the sea floor below sea level (blocks). rtf-previewmap-&lt;name&gt;-screen.png has the colours of the
 * preset screen's preview (RenderMode.BIOME_TYPE, the screen's default).
 */
public final class RTFPreviewMap {
	private static final int TILES = 3;

	private RTFPreviewMap() {
	}

	public static String run(MinecraftServer server, String name, int zoom, String spec) {
		try {
			CacheManager.clear();
		} catch (Exception e) {
			RTFCommon.LOGGER.warn("[RTF previewmap] cache clear failed", e);
		}
		Preset preset = spec.equals("default") ? Presets.makeDefault() : RTFStressTest.makePreset(spec);
		HolderLookup.Provider patch = preset.buildPatch(server.registries().getAccessForLoading(RegistryLayer.DIMENSIONS));
		Preset patched = patch.lookupOrThrow(RTFRegistries.PRESET).getOrThrow(Preset.KEY).value();
		GeneratorContext ctx = GeneratorContext.makeUncached(patched, patch.lookupOrThrow(RTFRegistries.NOISE), (int) server.overworld().getSeed(), 4, 0, PerformanceConfig.makeDefault().batchCount());
		Levels levels = ctx.levels;
		int worldHeight = levels.worldHeight;
		int tile = 256, size = tile * TILES;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		BufferedImage screen = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		long mountains = 0, land = 0, ocean = 0;
		double landSum = 0, depthSum = 0, depthMax = 0, landMax = 0;
		for (int tz = 0; tz < TILES; tz++) for (int tx = 0; tx < TILES; tx++) {
			float cx = (tx - (TILES - 1) / 2.0F) * tile * zoom, cz = (tz - (TILES - 1) / 2.0F) * tile * zoom;
			try (Tile t = ctx.generator.generateZoomed(cx, cz, zoom, false).join()) {
				int width = t.getBlockSize().size();
				Cell[] cells = t.getBacking();
				for (int z = 0; z < tile; z++) for (int x = 0; x < tile; x++) {
					Cell cell = cells[z * width + x];
					float blocks = (cell.height - levels.water) * worldHeight;
					int rgb;
					if (blocks <= 0) {
						ocean++;
						depthSum -= blocks;
						depthMax = Math.max(depthMax, -blocks);
						int b = (int) Math.max(40, 255 + blocks * 2.5F);
						rgb = (b / 4) << 8 | b;
					} else {
						land++;
						landSum += blocks;
						landMax = Math.max(landMax, blocks);
						int g = (int) Math.min(255, 60 + blocks * 1.3F);
						if (cell.terrain.isMountain()) {
							mountains++;
							rgb = g << 16 | (g / 2) << 8 | (g / 2);
						} else {
							rgb = (g / 2) << 16 | g << 8 | (g / 2);
						}
					}
					image.setRGB(tx * tile + x, tz * tile + z, rgb);
					// the screen's colours are ABGR (NativeImage)
					int abgr = RenderMode.BIOME_TYPE.getColor(cell, levels, patched.world().properties.worldHeight - 1);
					screen.setRGB(tx * tile + x, tz * tile + z, (abgr & 0xFF) << 16 | (abgr & 0xFF00) | (abgr >> 16 & 0xFF));
				}
			}
		}
		Path out = server.getServerDirectory().resolve("rtf-previewmap-" + name + ".png").toAbsolutePath();
		try {
			ImageIO.write(image, "png", out.toFile());
			ImageIO.write(screen, "png", server.getServerDirectory().resolve("rtf-previewmap-" + name + "-screen.png").toFile());
		} catch (IOException e) {
			return "[RTF previewmap] FAIL writing " + out + ": " + e;
		}
		long total = (long) size * size;
		String r = String.format(Locale.ROOT, "[RTF previewmap] %s (%s, %d blocks/px): mountains %.1f%% of land, land %.1f%% mean %.1f max %.0f, water %.1f%% mean depth %.1f max %.0f -> %s",
			name, spec, zoom, 100.0 * mountains / Math.max(1, land), 100.0 * land / total, landSum / Math.max(1, land), landMax, 100.0 * ocean / total, depthSum / Math.max(1, ocean), depthMax, out);
		RTFCommon.LOGGER.info(r);
		return r;
	}
}
