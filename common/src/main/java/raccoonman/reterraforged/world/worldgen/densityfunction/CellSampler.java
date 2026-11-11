package raccoonman.reterraforged.world.worldgen.densityfunction;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.SectionPos;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.StringRepresentable;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings.ControlPoints;
import raccoonman.reterraforged.world.worldgen.biome.Continentalness;
import raccoonman.reterraforged.world.worldgen.biome.Erosion;
import raccoonman.reterraforged.world.worldgen.biome.Weirdness;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.WorldLookup;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainCategory;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.cell.terrain.fakewater.FakeWaterBiomeTarget;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

public record CellSampler(Supplier<WorldLookup> deferredLookup, Field field) implements MarkerFunction.Mapped {
	private static final ThreadLocal<Cache2d> CELL = ThreadLocal.withInitial(() -> new Cache2d(Cache2d.THREAD_BITS));
	
	public static void clearThreadLocalCache() {
		CELL.remove();
	}
	
	@Override
	public double compute(FunctionContext ctx) {
		WorldLookup worldLookup = this.deferredLookup.get();
		if(worldLookup == null) {
			return 0.0D;
		}
		Cell cell = CELL.get().getAndUpdate(worldLookup, ctx.blockX(), ctx.blockZ(), true, WorldLookup.inStructureGeneration());
		return this.field.read(cell, worldLookup.getHeightmap());
	}

	@Override
	public double minValue() {
		return 0.0F;
	}

	@Override
	public double maxValue() {
		return 1.0F;
	}

	private static FakeWaterBiomeTarget refreshFakeWaterBiome(Cell cell, Heightmap heightmap) {
		heightmap.fakeWaterBiomeResolver().apply(cell);
		return cell.fakeWaterBiome;
	}

	/**
	 * The cells of the places asked about last, one per slot: a slot is taken by the place's low bits, the key (place,
	 * lookup, tiles, climate) tells which cell it holds. A hit is the cell the computation would give: the cell depends on
	 * the key alone, and what the fields write into it (fakeWaterBiome, from its height and terrain) is what it already
	 * holds. The returned cell is only read before the next call on the same cache (CellSampler, CacheChunk).
	 */
	public static class Cache2d {
		// the thread's cache outside the noise chunks (surface, the surface rules' biomes, features): they ask about
		// places in turn, the biome manager about the 8 corners of a block's quarts, and one slot missed nearly always
		static final int THREAD_BITS = 3;
		private final int bits, mask;
		private final long[] positions;
		// a thread goes on to the next world, and the same place there is another cell; 0, no lookup's id: empty
		private final int[] lookups;
		private final byte[] flags;
		private final Cell[] cells;

		// one slot: a noise chunk's reads outside its own chunk, and the one-cell queries
		public Cache2d() {
			this(0);
		}

		// 4 ^ bits slots
		public Cache2d(int bits) {
			this.bits = bits;
			this.mask = (1 << bits) - 1;
			int size = 1 << (bits * 2);
			this.positions = new long[size];
			this.lookups = new int[size];
			this.flags = new byte[size];
			this.cells = new Cell[size];
		}

		// tiles: the cell of the place's tile (generated if it is not cached), else computed without the tile
		public Cell getAndUpdate(WorldLookup lookup, int blockX, int blockZ, boolean sampleClimate, boolean tiles) {
			long packedPos = PosUtil.pack(blockX, blockZ);
			byte flags = (byte) ((tiles ? 1 : 0) | (sampleClimate ? 2 : 0));
			int slot = this.slot(blockX, blockZ);
			Cell cell = this.cells[slot];
			if(cell == null || this.positions[slot] != packedPos || this.flags[slot] != flags || this.lookups[slot] != lookup.id) {
				if(cell == null) {
					cell = this.cells[slot] = new Cell();
				}
				// empty until the cell is complete: an exception on the way leaves no half cell behind
				this.lookups[slot] = 0;
				lookup.applyCell(cell.reset(), blockX, blockZ, tiles, sampleClimate);
				lookup.getHeightmap().fakeWaterBiomeResolver().apply(cell);
				if (cell.fakeWaterBiome.isRiver()) {
					cell.erosion = Erosion.LEVEL_6.mid();
					cell.weirdness = Weirdness.VALLEY.mid();
				}
				if (cell.terrain == TerrainType.ISLAND_BEACH) {
					cell.erosion = Erosion.LEVEL_4.mid();
					cell.weirdness = Weirdness.MID_SLICE_NORMAL_DESCENDING.mid();
				}
				this.positions[slot] = packedPos;
				this.flags[slot] = flags;
				this.lookups[slot] = lookup.id;
			}
			return cell;
		}

		// x ^ x >> 2: the low bits differ for the 8 blocks of an aligned row (half a chunk) and for any 8 quarts in a row
		int slot(int blockX, int blockZ) {
			int x = (blockX ^ (blockX >> 2)) & this.mask;
			int z = (blockZ ^ (blockZ >> 2)) & this.mask;
			return x | (z << this.bits);
		}
	}
	
	public class CacheChunk implements MarkerFunction.Mapped {
		@Nullable
		private final Tile.Chunk chunk;
		private final Cache2d cache2d;
		private final int chunkX, chunkZ;
		
		public CacheChunk(@Nullable Tile.Chunk chunk, @Nullable Cache2d cache2d, int chunkX, int chunkZ) {
			this.chunk = chunk;
			this.cache2d = cache2d != null ? cache2d : new Cache2d();
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
		}

		@Override
		public double compute(FunctionContext ctx) {
			int blockX = ctx.blockX();
			int blockZ = ctx.blockZ();
			int chunkX = SectionPos.blockToSectionCoord(blockX);
			int chunkZ = SectionPos.blockToSectionCoord(blockZ);
			WorldLookup worldLookup = CellSampler.this.deferredLookup.get();
			if(worldLookup == null) {
				return 0.0D;
			}
			Cell cell = (this.chunk != null && this.chunkX == chunkX && this.chunkZ == chunkZ) ? 
				this.chunk.getCell(blockX, blockZ) :
				// a chunk being generated reads its neighbours' places (the aquifers) from their tiles too; a one-cell
				// base height query (chunk null) only during structure generation
				this.cache2d.getAndUpdate(worldLookup, blockX, blockZ, false, this.chunk != null || WorldLookup.inStructureGeneration());
			return CellSampler.this.field.read(cell, worldLookup.getHeightmap());
		}

		// reads the tile's cells of this chunk: there it depends on the place's x and z alone (MixinNoiseChunk, ColumnCache)
		public boolean readsTileOf(@Nullable Tile.Chunk chunk, int chunkX, int chunkZ) {
			return this.chunk != null && this.chunk == chunk && this.chunkX == chunkX && this.chunkZ == chunkZ;
		}

		@Override
		public double minValue() {
			return CellSampler.this.minValue();
		}

		@Override
		public double maxValue() {
			return CellSampler.this.maxValue();
		}
	}
	
	public record Marker(Field field) implements MarkerFunction {
		public static final MapCodec<Marker> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Field.CODEC.fieldOf("field").forGetter(Marker::field)
		).apply(instance, Marker::new));
		
		@Override
		public KeyDispatchDataCodec<Marker> codec() {
			return new KeyDispatchDataCodec<>(CODEC);
		}
	}
	
	public enum Field implements StringRepresentable {
		HEIGHT("height") {
			
			@Override
			public float read(Cell cell, Heightmap heightmap) {
				return cell.height;
			}
		},
		CONTINENT("continent") {
			
			//TODO move this somewhere else
			@Override
			public float read(Cell cell, Heightmap heightmap) {
				Levels levels = heightmap.levels();
				ControlPoints controlPoints = heightmap.controlPoints();
				
				float deepOcean = controlPoints.deepOcean;
				float shallowOcean = controlPoints.shallowOcean;
				float beach = controlPoints.beach;
				float coast = controlPoints.coast;
				float inland = controlPoints.inland;
				
				FakeWaterBiomeTarget fakeWaterBiome = refreshFakeWaterBiome(cell, heightmap);
				if (fakeWaterBiome.isRiver()) {
					return Continentalness.NEAR_INLAND.mid();
				}
				
				if(cell.terrain == TerrainType.MUSHROOM_FIELDS) {
					return Continentalness.MUSHROOM_FIELDS.mid();
				}
				
				if(cell.terrain.isDeepOcean()) {
					float alpha = NoiseUtil.clamp(cell.continentEdge, 0.0F, deepOcean);
					alpha = NoiseUtil.lerp(alpha, 0.0F, deepOcean, 0.0F, 1.0F);
					return NoiseUtil.lerp(Continentalness.DEEP_OCEAN.min() + 0.05F, Continentalness.DEEP_OCEAN.max(), alpha);					
				}
				
				if(cell.terrain.isShallowOcean()) {
					float alpha = NoiseUtil.clamp(cell.continentEdge, deepOcean, shallowOcean);
					alpha = NoiseUtil.lerp(alpha, deepOcean, shallowOcean, 0.0F, 0.98F);
					return NoiseUtil.lerp(Continentalness.OCEAN.min(), Continentalness.OCEAN.max(), alpha);
				}
				
				if(cell.terrain == TerrainType.ISLAND_BEACH) {
					float alpha = NoiseUtil.clamp(cell.continentEdge, shallowOcean, beach);
					alpha = NoiseUtil.lerp(alpha, shallowOcean, beach, 0.0F, 1.0F);
					return NoiseUtil.lerp(Continentalness.COAST.min(), Continentalness.COAST.min() + (Continentalness.COAST.max() - Continentalness.COAST.min()) * 0.3F, alpha);
				}
				
				if(cell.terrain.getDelegate() == TerrainCategory.BEACH && cell.height + cell.beachNoise < levels.water(5)) {
					float alpha = NoiseUtil.clamp(cell.continentEdge, shallowOcean, beach);
					alpha = NoiseUtil.lerp(alpha, shallowOcean, beach, 0.0F, 1.0F);
					return NoiseUtil.lerp(Continentalness.COAST.min(), Continentalness.COAST.max(), alpha);
				}
			
				float alpha = NoiseUtil.clamp(cell.continentEdge, beach, inland);
				alpha = NoiseUtil.lerp(alpha, beach, inland, 0.0F, 1.0F);
				return NoiseUtil.lerp(Continentalness.NEAR_INLAND.mid(), Continentalness.FAR_INLAND.max(), alpha);
			}
		},
		EROSION("erosion") {
			
			@Override
			public float read(Cell cell, Heightmap heightmap) {
				FakeWaterBiomeTarget fakeWaterBiome = refreshFakeWaterBiome(cell, heightmap);
				if(fakeWaterBiome.isRiver()) {
					return Erosion.LEVEL_6.mid();
				}
				if(cell.terrain == TerrainType.ISLAND_BEACH) {
					return Erosion.LEVEL_4.mid();
				}
				return cell.erosion;
			}
		},
		WEIRDNESS("weirdness") {
			
			@Override
			public float read(Cell cell, Heightmap heightmap) {
				FakeWaterBiomeTarget fakeWaterBiome = refreshFakeWaterBiome(cell, heightmap);
				if(fakeWaterBiome.isRiver()) {
					return Weirdness.VALLEY.mid();
				}
				if(cell.terrain == TerrainType.ISLAND_BEACH) {
					return Weirdness.MID_SLICE_NORMAL_DESCENDING.mid();
				}
				return cell.weirdness;
			}
		},
		BIOME_REGION("biome_region") {
			
			@Override
			public float read(Cell cell, Heightmap heightmap) {
				return cell.biomeRegionId;
			}
		},
		TEMPERATURE("temperature") {
			
			@Override
			public float read(Cell cell, Heightmap heightmap) {
				return cell.temperature;
			}
		},
		MOISTURE("moisture") {
			
			@Override
			public float read(Cell cell, Heightmap heightmap) {
				return cell.moisture;
			}
		},
		GRADIENT("gradient") {
			
			@Override
			public float read(Cell cell, Heightmap heightmap) {
				return cell.gradient;
			}
		},
		HEIGHT_EROSION("height_erosion") {
			
			@Override
			public float read(Cell cell, Heightmap heightmap) {
				return cell.heightErosion;
			}
		},
		SEDIMENT("sediment") {
			
			@Override
			public float read(Cell cell, Heightmap heightmap) {
				return cell.sediment;
			}
		};

		public static final Codec<Field> CODEC = StringRepresentable.fromEnum(Field::values);
		
		private final String name;
		
		private Field(String name) {
			this.name = name;
		}
		
		@Override
		public String getSerializedName() {
			return this.name;
		}
		
		public abstract float read(Cell cell, Heightmap heightmap);
	}
}
