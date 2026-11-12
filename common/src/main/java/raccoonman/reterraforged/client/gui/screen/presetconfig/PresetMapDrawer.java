package raccoonman.reterraforged.client.gui.screen.presetconfig;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import raccoonman.reterraforged.config.PerformanceConfig;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;

/**
 * A whole preset as a small picture for the preset list: the settings pages' preview (PresetEditorPage) at a coarse
 * step, 128 cells of 100 blocks around the origin, by its heights (heightColor). The same area as the
 * preview at its default zoom, a quarter of its cells: the picture is there in a fraction of a second.
 */
public final class PresetMapDrawer {
	// 16 << 3 = 128 cells a side
	private static final int TILE_CHUNKS = 3;
	public static final int SIZE = 16 << TILE_CHUNKS;
	// blocks per cell: 12800 blocks a side, as the preview's default (256 cells of 50)
	private static final float ZOOM = 100.0F;

	private PresetMapDrawer() {
	}

	/** What a picture is made of, taken on the render thread: a copy of the preset, the screen's frozen registries */
	public record Input(Preset preset, RegistryAccess.Frozen registries, long seed) {
	}

	/** The picture's pixels, ABGR as NativeImage takes them, row by row */
	public static int[] draw(Input input, PresetMaps.Job job) {
		job.check();
		HolderLookup.Provider provider = input.preset().buildPatch(input.registries());
		Preset preset = provider.lookupOrThrow(RTFRegistries.PRESET).getOrThrow(Preset.KEY).value();
		WorldSettings.Properties properties = preset.world().properties;
		job.check();
		int seed = GeneratorContext.terrainSeed(input.seed(), preset);
		GeneratorContext context = GeneratorContext.makeUncached(preset, provider.lookupOrThrow(RTFRegistries.NOISE), seed, TILE_CHUNKS, 0, PerformanceConfig.makeDefault().batchCount());
		job.check();
		Tile tile = job.join(context.generator.generateZoomed(0, 0, ZOOM, false));
		job.check();
		Levels levels = new Levels(properties.terrainScaler(), properties.seaLevel);
		int width = tile.getBlockSize().size();
		int[] colors = new int[SIZE * SIZE];
		tile.iterate((cell, x, z) -> {
			if (x < SIZE && z < SIZE && x < width && z < width) {
				colors[z * SIZE + x] = heightColor(cell, levels);
			}
		});
		return colors;
	}

	// the heights, as "rtf previewmap" draws them: the sea darker with its depth, the land brighter with its height, green,
	// and the mountains red. What the presets differ in is their terrain, which the biome colours of the settings pages'
	// preview do not show. ABGR (NativeImage)
	static int heightColor(Cell cell, Levels levels) {
		float blocks = (cell.height - levels.water) * levels.worldHeight;
		int r, g, b;
		if (blocks <= 0.0F) {
			b = (int) Math.max(40.0F, 255.0F + blocks * 2.5F);
			g = b / 4;
			r = 0;
		} else {
			int shade = (int) Math.min(255.0F, 60.0F + blocks * 1.3F);
			boolean mountain = cell.terrain != null && cell.terrain.isMountain();
			r = mountain ? shade : shade / 2;
			g = mountain ? shade / 2 : shade;
			b = shade / 2;
		}
		return 0xFF000000 | b << 16 | g << 8 | r;
	}
}
