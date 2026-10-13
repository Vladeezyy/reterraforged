package raccoonman.reterraforged.world.worldgen.densityfunction;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Interval;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings.ControlPoints;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;



import raccoonman.reterraforged.world.worldgen.biome.Continentalness;
import raccoonman.reterraforged.world.worldgen.biome.Erosion;
import raccoonman.reterraforged.world.worldgen.biome.Humidity;
import raccoonman.reterraforged.world.worldgen.biome.Temperature;
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

public final class CellSampler {
	private static final ThreadLocal<Cache2d> CELL = ThreadLocal.withInitial(Cache2d::new);

	private CellSampler() {
	}

	public static void clearThreadLocalCache() {
		CELL.remove();
	}

	private static FakeWaterBiomeTarget refreshFakeWaterBiome(Cell cell, Heightmap heightmap) {
		heightmap.fakeWaterBiomeResolver().apply(cell);
		return cell.fakeWaterBiome;
	}

	// a field of the preset's cells (height, climate, ...); 2D, the engine samples it once per column
	public record Marker(Field field) implements DensityFunction {
		public static final MapCodec<Marker> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Field.CODEC.fieldOf("field").forGetter(Marker::field)
		).apply(instance, Marker::new));

		@Override
		public DensitySampler compileSampler(CompileContext context) {
			return new Sampler(RTFCompileContext.randomState(context), this.field);
		}

		@Override
		public DensityFunction rewriteChildren(DfRewriteRule rule) {
			return this;
		}

		@Override
		public Interval range() {
			// covers every field (height 0..1, climate -1..1, region, gradient): since 26.3 the compiler drops MIN/MAX
			// branches by the ranges of their inputs, so the range has to hold every value (before 26.3 it was 0..1 and
			// only made the old engine log a warning)
			return Interval.of(-2.0F, 2.0F);
		}

		@Override
		public int domainAxes() {
			return AXIS_X | AXIS_Z;
		}

		@Override
		public MapCodec<Marker> codec() {
			return CODEC;
		}
	}

	/**
	 * The GeneratorContext is read when sampling: samplers are compiled once per RandomState, possibly before
	 * RTFRandomState.initialize sets the context. A volume inside one chunk (chunk generation, base height queries)
	 * reads that chunk's tile cells at block resolution, like the per-chunk cache attached in NoiseChunk before 26.3;
	 * other positions use the quart-resolution Cache2d.
	 */
	public record Sampler(@Nullable RTFRandomState randomState, Field field) implements DensitySampler {

		@Override
		public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
			GeneratorContext generatorContext = this.randomState != null ? this.randomState.generatorContext() : null;
			if(generatorContext == null) {
				outputBuffer.fill(0.0F);
				return;
			}
			WorldLookup worldLookup = generatorContext.lookup;
			Heightmap heightmap = worldLookup.getHeightmap();
			int chunkX = SectionPos.blockToSectionCoord(volume.minBlockX());
			int chunkZ = SectionPos.blockToSectionCoord(volume.minBlockZ());
			// A single column is a base height or base column query. The 1.21.1 port gives it the tile of its chunk too
			// (heights with erosion, as the chunk gets them); that is a whole tile for every place asked about, and
			// /locate, explorer maps and dolphins (thousands of places) took minutes. ReTerraForged 0.0.6 reads the cells
			// of those places directly.
			boolean column = volume.sizeX() == 1 && volume.sizeZ() == 1;
			Tile.Chunk chunk = column && worldLookup.isOriginal() ? null : generatorContext.cache.provideAtChunk(chunkX, chunkZ).getChunkReader(chunkX, chunkZ);
			Cache2d cache2d = CELL.get();
			int sizeY = volume.sizeY();
			int index = 0;
			for(int z = 0; z < volume.sizeZ(); z++) {
				int blockZ = volume.blockZ(z);
				for(int x = 0; x < volume.sizeX(); x++) {
					int blockX = volume.blockX(x);
					Cell cell = chunk != null && SectionPos.blockToSectionCoord(blockX) == chunkX && SectionPos.blockToSectionCoord(blockZ) == chunkZ ?
						chunk.getCell(blockX, blockZ) :
						cache2d.getAndUpdate(worldLookup, blockX, blockZ, true);
					float value = this.field.read(cell, heightmap);
					outputBuffer.setRange(index, sizeY, value);
					index += sizeY;
				}
			}
		}

		@Override
		public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
			GeneratorContext generatorContext = this.randomState != null ? this.randomState.generatorContext() : null;
			if(generatorContext == null) {
				return 0.0F;
			}
			WorldLookup worldLookup = generatorContext.lookup;
			Cell cell = CELL.get().getAndUpdate(worldLookup, blockX, blockZ, true);
			return this.field.read(cell, worldLookup.getHeightmap());
		}
	}

	public static class Cache2d {
		private long lastPos = Long.MAX_VALUE;
		private Cell cell = new Cell();
		
		public Cell getAndUpdate(WorldLookup lookup, int blockX, int blockZ, boolean sampleClimate) {
			// the 1.21.1 port samples on the 4 block grid of the biomes; ReTerraForged 0.0.6 at the block itself
			if(!lookup.isOriginal()) {
				blockX = QuartPos.toBlock(QuartPos.fromBlock(blockX));
				blockZ = QuartPos.toBlock(QuartPos.fromBlock(blockZ));
			}
			
			long packedPos = PosUtil.pack(blockX, blockZ);
			if(this.lastPos != packedPos) {
				lookup.applyCell(this.cell.reset(), blockX, blockZ, false, sampleClimate);
				lookup.getHeightmap().fakeWaterBiomeResolver().apply(this.cell);
				this.lastPos = packedPos;
				if (this.cell.fakeWaterBiome.isRiver()) {
					this.cell.erosion = Erosion.LEVEL_6.mid();
					this.cell.weirdness = Weirdness.VALLEY.mid();
				} else if (this.cell.fakeWaterBiome.isOcean()) {
					this.cell.erosion = Erosion.LEVEL_3.mid();
					this.cell.weirdness = Weirdness.VALLEY.mid();
				}
				if (this.cell.terrain == TerrainType.ISLAND_BEACH) {
					this.cell.erosion = Erosion.LEVEL_4.mid();
					this.cell.weirdness = Weirdness.MID_SLICE_NORMAL_DESCENDING.mid();
					this.cell.temperature = Temperature.LEVEL_3.mid();
					this.cell.moisture = Humidity.LEVEL_1.mid();
				}
			}
			return this.cell;
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
				if (fakeWaterBiome.isOcean()) {
					return Continentalness.OCEAN.mid();
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
				if(fakeWaterBiome.isOcean()) {
					return Erosion.LEVEL_3.mid();
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
				if(fakeWaterBiome.isRiver() || fakeWaterBiome.isOcean()) {
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
				if(cell.terrain == TerrainType.ISLAND_BEACH) {
					return Temperature.LEVEL_3.mid();
				}
				return cell.temperature;
			}
		},
		MOISTURE("moisture") {
			
			@Override
			public float read(Cell cell, Heightmap heightmap) {
				if(cell.terrain == TerrainType.ISLAND_BEACH) {
					return Humidity.LEVEL_1.mid();
				}
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
		
		private String name;
		
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
