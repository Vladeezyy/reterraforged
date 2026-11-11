package raccoonman.reterraforged.world.worldgen.noise.module;

import java.util.Arrays;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.StringRepresentable;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil.Vec2f;

public record Erosion(Noise input, int seed, int octaves, float strength, float gridSize, float amplitude, float lacunarity, float distanceFallOff, BlendMode blendMode, ThreadLocal<Scratch> cache) implements Noise {
	public static final MapCodec<Erosion> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Noise.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(Erosion::input),
		Codec.INT.fieldOf("seed").forGetter(Erosion::seed),
		NoiseCodecs.OCTAVES_ANY_LOW.fieldOf("octaves").forGetter(Erosion::octaves),
		Codec.FLOAT.fieldOf("strength").forGetter(Erosion::strength),
		Codec.FLOAT.fieldOf("grid_size").forGetter(Erosion::gridSize),
		Codec.FLOAT.fieldOf("amplitude").forGetter(Erosion::amplitude),
		Codec.FLOAT.fieldOf("lacunarity").forGetter(Erosion::lacunarity),
		Codec.FLOAT.fieldOf("distance_falloff").forGetter(Erosion::distanceFallOff),
		BlendMode.CODEC.fieldOf("blend_mode").forGetter(Erosion::blendMode)
	).apply(instance, Erosion::new));

	public Erosion(Noise input, int seed, int octaves, float strength, float gridSize, float amplitude, float lacunarity, float distanceFallOff, BlendMode blendMode) {
		this(input, seed, octaves, strength, gridSize, amplitude, lacunarity, distanceFallOff, blendMode, ThreadLocal.withInitial(() -> new Scratch(octaves)));
	}

	@Override
	public float compute(float x, float z, int seed) {
		float value = this.input.compute(x, z, seed);
		float erosion = this.getErosionValue(x, z, this.cache.get());
		return NoiseUtil.lerp(erosion, value, this.blendMode.blend(value, erosion, this.strength));
	}

	@Override
	public float minValue() {
		return 0.0F;
	}

	@Override
	public float maxValue() {
		return 1.0F;
	}

	@Override
	public Noise mapAll(Visitor visitor) {
		return new Erosion(this.input.mapAll(visitor), this.seed, this.octaves, this.strength, this.gridSize, this.amplitude, this.lacunarity, this.distanceFallOff, this.blendMode);
	}

	@Override
	public MapCodec<Erosion> codec() {
		return CODEC;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Erosion other && other.input.equals(this.input) && other.seed == this.seed && other.octaves == this.octaves && other.strength == this.strength && other.gridSize == this.gridSize && other.amplitude == this.amplitude && other.lacunarity == this.lacunarity && other.distanceFallOff == this.distanceFallOff && other.blendMode.equals(this.blendMode);
	}
	
	private float getErosionValue(float x, float y, Scratch cache) {
		float sum = 0.0F;
		float max = 0.0F;
		float gain = 1.0F;
		float distance = this.gridSize;
		for (int i = 0; i < this.octaves; ++i) {
			float value = this.getSingleErosionValue(x, y, distance, cache.cells, cache.heights[i]);
			value *= gain;
			sum += value;
			max += gain;
			gain *= this.amplitude;
			distance *= this.distanceFallOff;
			x *= this.lacunarity;
			y *= this.lacunarity;
		}
		return sum / max;
	}

	// cache: the heights of the 5 x 5 cells around (0 - 24, -1 until sampled), then their points' x (25 - 49) and z
	// (50 - 74), worked out once (each cell was looked up for every one of its up to 9 neighbours)
	private float getSingleErosionValue(float x, float y, float gridSize, float[] cache, Heights heights) {
		Arrays.fill(cache, 0, 25, -1.0F);
		int pix = NoiseUtil.floor(x / gridSize);
		int piy = NoiseUtil.floor(y / gridSize);
		for (int dy = -2; dy <= 2; ++dy) {
			for (int dx = -2; dx <= 2; ++dx) {
				int px = pix + dx;
				int py = piy + dy;
				Vec2f vec = NoiseUtil.cell(this.seed, px, py);
				int index = (dy + 2) * 5 + (dx + 2);
				cache[25 + index] = (px + vec.x()) * gridSize;
				cache[50 + index] = (py + vec.y()) * gridSize;
			}
		}
		float minHeight2 = Float.MAX_VALUE;
		for (int dy1 = -1; dy1 <= 1; ++dy1) {
			for (int dx1 = -1; dx1 <= 1; ++dx1) {
				int a = (dy1 + 2) * 5 + (dx1 + 2);
				float ax = cache[25 + a];
				float ay = cache[50 + a];
				float bx = ax;
				float by = ay;
				float lowestNeighbour = Float.MAX_VALUE;
				for (int dy2 = -1; dy2 <= 1; ++dy2) {
					for (int dx2 = -1; dx2 <= 1; ++dx2) {
						int b = a + dy2 * 5 + dx2;
						float candidateX = cache[25 + b];
						float candidateY = cache[50 + b];
						float height = getNoiseValue(dx1 + dx2, dy1 + dy2, pix, piy, candidateX, candidateY, this.input, cache, heights);
						if (height < lowestNeighbour) {
							lowestNeighbour = height;
							bx = candidateX;
							by = candidateY;
						}
					}
				}
				float height2 = sd(x, y, ax, ay, bx, by);
				if (height2 < minHeight2) {
					minHeight2 = height2;
				}
			}
		}
		return NoiseUtil.clamp(sqrt(minHeight2) / gridSize, 0.0F, 1.0F);
	}

	private static float getNoiseValue(int dx, int dy, int pix, int piy, float px, float py, Noise module, float[] cache, Heights heights) {
		int index = (dy + 2) * 5 + (dx + 2);
		float value = cache[index];
		if (value == -1.0F) {
			value = heights.get(pix + dx, piy + dy, px, py, module);
			cache[index] = value;
		}
		return value;
	}

	/**
	 * The input's height at the grid points of one octave, for the next samples: the points of neighbouring samples are
	 * mostly the same (a point is its cell's, at the same place for every sample), and the input was worked out up to 25
	 * times per sample and octave. Direct-mapped by the cell; a point's height depends on its cell only.
	 */
	static final class Heights {
		private static final int SIZE = 1024;
		private final long[] cells = new long[SIZE];
		private final float[] values = new float[SIZE];

		Heights() {
			java.util.Arrays.fill(this.cells, Long.MIN_VALUE);
		}

		float get(int cellX, int cellY, float px, float py, Noise module) {
			int slot = (cellX * 73856093 ^ cellY * 19349663) & (SIZE - 1);
			long key = ((long) cellX << 32) | (cellY & 0xFFFFFFFFL);
			if (this.cells[slot] == key) {
				return this.values[slot];
			}
			float value = module.compute(px, py, 0);
			this.cells[slot] = key;
			this.values[slot] = value;
			return value;
		}
	}

	/** One thread's arrays: the 5 x 5 cells of the sample (see getSingleErosionValue) and the heights of every octave */
	static final class Scratch {
		final float[] cells = new float[3 * 5 * 5];
		final Heights[] heights;

		Scratch(int octaves) {
			this.heights = new Heights[Math.max(octaves, 0)];
			for (int i = 0; i < this.heights.length; i++) {
				this.heights[i] = new Heights();
			}
		}
	}

	private static float sd(float px, float py, float ax, float ay, float bx, float by) {
		float padx = px - ax;
		float pady = py - ay;
		float badx = bx - ax;
		float bady = by - ay;
		float paba = padx * badx + pady * bady;
		float baba = badx * badx + bady * bady;
		
		// avoid dividing by 0
		float h = paba == 0.0F && baba == 0.0F ? 0.0F : NoiseUtil.clamp(paba / baba, 0.0F, 1.0F);
		return len2(padx, pady, badx * h, bady * h);
	}

	private static float len2(float x1, float y1, float x2, float y2) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		return dx * dx + dy * dy;
	}

	private static float sqrt(float value) {
		return (float) Math.sqrt(value);
	}

	public enum BlendMode implements StringRepresentable {
		CONSTANT("constant") {
			@Override
			public float blend(float value, float erosion, float strength) {
				return 1.0F - strength;
			}
		},
		INPUT_LINEAR("input_linear") {
			@Override
			public float blend(float value, float erosion, float strength) {
				return 1.0F - strength * value;
			}
		},
		OUTPUT_LINEAR("output_linear") {
			@Override
			public float blend(float value, float erosion, float strength) {
				return 1.0F - strength * erosion;
			}
		};
		
		public static final Codec<BlendMode> CODEC = StringRepresentable.fromEnum(BlendMode::values);
		
		private final String name;
		
		private BlendMode(String name) {
			this.name = name;
		}
		
		@Override
		public String getSerializedName() {
			return this.name;
		}

		public abstract float blend(float value, float erosion, float strength);
	}
}
