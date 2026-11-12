package raccoonman.reterraforged.client.gui.screen.presetconfig;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import raccoonman.reterraforged.config.PerformanceConfig;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.biome.type.BiomeType;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;

/**
 * A whole preset as a small picture for the preset list: 512 cells of 25 blocks around the origin (the area the settings
 * pages' preview shows at its default zoom), drawn as a relief map. The land has the colour of its climate (the biome
 * type: snow, forest, steppe, desert), from 70 blocks above the sea the colour of its height (rock, then snow), and is
 * lit from the north-west, which is what shows the mountains and valleys the presets differ in; the sea is darker with
 * its depth, rivers and lakes are drawn, sea shores are sand. About 0.15 s on a background thread.
 */
public final class PresetMapDrawer {
	// 16 << 5 = 512 cells a side
	private static final int TILE_CHUNKS = 5;
	public static final int SIZE = 16 << TILE_CHUNKS;
	// blocks per cell: 12800 blocks a side, as the preview's default (256 cells of 50)
	private static final float ZOOM = 25.0F;

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
		Cell[] cells = tile.getBacking();
		int n = SIZE;
		float[] height = new float[n * n];
		int[] climate = new int[n * n];
		boolean[] mountain = new boolean[n * n];
		boolean[] river = new boolean[n * n];
		for (int z = 0; z < n; z++) {
			for (int x = 0; x < n; x++) {
				Cell cell = cells[Math.min(z, width - 1) * width + Math.min(x, width - 1)];
				int i = z * n + x;
				height[i] = (cell.height - levels.water) * levels.worldHeight;
				climate[i] = climateColor(cell.biome);
				mountain[i] = cell.terrain != null && cell.terrain.isMountain();
				river[i] = cell.terrain != null && (cell.terrain.isRiver() || cell.terrain.isLake());
			}
		}
		job.check();
		return relief(n, ZOOM, height, climate, mountain, river);
	}

	private static final int SNOWY = 0xE6ECF2;

	// the colour a biome type has on the map, RGB
	static int climateColor(BiomeType type) {
		if (type == null) {
			return 0x548C42;
		}
		return switch (type) {
			case TROPICAL_RAINFOREST -> 0x2E7036;
			case SAVANNA -> 0xACA654;
			case DESERT -> 0xD8BE80;
			case TEMPERATE_RAINFOREST -> 0x3A7842;
			case TEMPERATE_FOREST -> 0x548C42;
			case GRASSLAND -> 0x7EA652;
			case COLD_STEPPE -> 0xA4A88C;
			case STEPPE -> 0xB2B06C;
			case TAIGA -> 0x48765C;
			case TUNDRA -> SNOWY;
			case ALPINE -> 0xA8A0AC;
		};
	}

	// the colours of the heights where they take over from the climate (blocks above the sea), and of the sea's depths
	private static final float[] HIGH_AT = {110.0F, 150.0F, 190.0F, 230.0F};
	private static final int[] HIGH = {0xAA9668, 0x967C60, 0x96908C, 0xECEEF2};
	private static final float[] SEA_AT = {-110.0F, -60.0F, -25.0F, -8.0F, 0.0F};
	private static final int[] SEA = {0x0E1C54, 0x163484, 0x265CB2, 0x4084D0, 0x68A8E2};
	private static final int SAND = 0xD8CC96;
	private static final int FRESH_WATER = 0x4E8CD6;

	/**
	 * The relief map of n x n cells of "step" blocks: heights in blocks above the sea (the sea 0 and below), the
	 * climate's colour (RGB), mountain terrain, rivers and lakes. ABGR
	 */
	static int[] relief(int n, float step, float[] height, int[] climate, boolean[] mountain, boolean[] river) {
		// the climates run into each other over a few cells: their borders in the world are not lines either
		float[][] tint = new float[3][];
		for (int c = 0; c < 3; c++) {
			float[] channel = new float[n * n];
			int shift = 16 - 8 * c;
			for (int i = 0; i < channel.length; i++) {
				channel[i] = climate[i] >> shift & 0xFF;
			}
			tint[c] = blur(n, channel, 2);
		}
		boolean[] nearSea = nearSea(n, height, 2);
		int[] colors = new int[n * n];
		float[] rgb = new float[3];
		for (int z = 0; z < n; z++) {
			for (int x = 0; x < n; x++) {
				int i = z * n + x;
				float h = height[i];
				if (h <= 0.0F) {
					colors[i] = abgr(ramp(SEA_AT, SEA, h, 0), ramp(SEA_AT, SEA, h, 1), ramp(SEA_AT, SEA, h, 2));
					continue;
				}
				if (river[i]) {
					color(rgb, FRESH_WATER);
				} else if (h < 3.0F && nearSea[i] && !mountain[i] && climate[i] != SNOWY) {
					color(rgb, SAND);
				} else {
					// lowlands a little brighter with their height; from 70 blocks the colour of the height comes in,
					// but snowy land stays white on its mountains
					float low = 0.9F + 0.2F * clamp(h / 70.0F);
					float brightness = (tint[0][i] + tint[1][i] + tint[2][i]) / 3.0F;
					float high = clamp((h - 70.0F) / 50.0F) * (1.0F - clamp((brightness - 200.0F) / 30.0F));
					for (int c = 0; c < 3; c++) {
						rgb[c] = tint[c][i] * low * (1.0F - high) + ramp(HIGH_AT, HIGH, h, c) * high;
					}
				}
				// lit from the north-west; the sea is flat
				float west = Math.max(0.0F, height[z * n + Math.max(x - 1, 0)]), east = Math.max(0.0F, height[z * n + Math.min(x + 1, n - 1)]);
				float north = Math.max(0.0F, height[Math.max(z - 1, 0) * n + x]), south = Math.max(0.0F, height[Math.min(z + 1, n - 1) * n + x]);
				float nx = -(east - west) / (2.0F * step) * 2.2F, nz = -(south - north) / (2.0F * step) * 2.2F;
				float light = (nx * -0.5F + nz * -0.5F + 0.7071F) / (float) Math.sqrt(nx * nx + nz * nz + 1.0F);
				float shade = Math.max(0.45F, Math.min(1.35F, 1.0F + (light - 0.7071F) * 1.9F));
				colors[i] = abgr(rgb[0] * shade, rgb[1] * shade, rgb[2] * shade);
			}
		}
		return colors;
	}

	private static void color(float[] rgb, int color) {
		rgb[0] = color >> 16 & 0xFF;
		rgb[1] = color >> 8 & 0xFF;
		rgb[2] = color & 0xFF;
	}

	private static float clamp(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	private static int abgr(float r, float g, float b) {
		return 0xFF000000 | channel(b) << 16 | channel(g) << 8 | channel(r);
	}

	private static int channel(float value) {
		return Math.max(0, Math.min(255, Math.round(value)));
	}

	// channel c (0 red, 1 green, 2 blue) of the colour at "value" on a scale of colours at rising values
	static float ramp(float[] at, int[] colors, float value, int c) {
		int shift = 16 - 8 * c;
		if (value <= at[0]) {
			return colors[0] >> shift & 0xFF;
		}
		for (int i = 1; i < at.length; i++) {
			if (value <= at[i]) {
				float t = (value - at[i - 1]) / (at[i] - at[i - 1]);
				return (colors[i - 1] >> shift & 0xFF) * (1.0F - t) + (colors[i] >> shift & 0xFF) * t;
			}
		}
		return colors[colors.length - 1] >> shift & 0xFF;
	}

	// the mean of the (2 * radius + 1) cells around each, along the rows and then the columns; the edge cells repeat
	static float[] blur(int n, float[] values, int radius) {
		float[] rows = new float[n * n];
		float[] out = new float[n * n];
		int count = 2 * radius + 1;
		for (int z = 0; z < n; z++) {
			for (int x = 0; x < n; x++) {
				float sum = 0.0F;
				for (int d = -radius; d <= radius; d++) {
					sum += values[z * n + Math.max(0, Math.min(n - 1, x + d))];
				}
				rows[z * n + x] = sum / count;
			}
		}
		for (int z = 0; z < n; z++) {
			for (int x = 0; x < n; x++) {
				float sum = 0.0F;
				for (int d = -radius; d <= radius; d++) {
					sum += rows[Math.max(0, Math.min(n - 1, z + d)) * n + x];
				}
				out[z * n + x] = sum / count;
			}
		}
		return out;
	}

	// the cells with the sea within "radius" cells (a square)
	static boolean[] nearSea(int n, float[] height, int radius) {
		boolean[] rows = new boolean[n * n];
		boolean[] out = new boolean[n * n];
		for (int z = 0; z < n; z++) {
			for (int x = 0; x < n; x++) {
				for (int d = -radius; d <= radius && !rows[z * n + x]; d++) {
					rows[z * n + x] = height[z * n + Math.max(0, Math.min(n - 1, x + d))] <= 0.0F;
				}
			}
		}
		for (int z = 0; z < n; z++) {
			for (int x = 0; x < n; x++) {
				for (int d = -radius; d <= radius && !out[z * n + x]; d++) {
					out[z * n + x] = rows[Math.max(0, Math.min(n - 1, z + d)) * n + x];
				}
			}
		}
		return out;
	}
}
