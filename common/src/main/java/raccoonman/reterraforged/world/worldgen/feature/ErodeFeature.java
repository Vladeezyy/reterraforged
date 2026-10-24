package raccoonman.reterraforged.world.worldgen.feature;

import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import raccoonman.reterraforged.tags.RTFBlockTags;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;
import raccoonman.reterraforged.world.worldgen.feature.ErodeFeature.Config;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;

public class ErodeFeature extends Feature<Config> {

	public ErodeFeature(Codec<Config> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<Config> placeContext) {
		WorldGenLevel level = placeContext.level();
		ChunkAccess chunk = level.getChunk(placeContext.origin());
		RandomState randomState = level.getLevel().getChunkSource().randomState();
		// chunks are eroded when their surface is built (MixinNoiseBasedChunkGenerator, ERODED marks them): eroding
		// here, at the start of the chunk's own features, overwrote what the features of neighbour chunks generated
		// earlier had already placed in it (issue #2: William Wythers' Overhauled Overworld's blackstone and tuff cut
		// off along chunk borders on cliffs). Only a chunk whose surface an older version built is eroded here.
		if(chunk.getInhabitedTime() >= ERODED || !((Object) randomState instanceof RTFRandomState rtfRandomState) || rtfRandomState.generatorContext() == null) {
			// not a ReTerraForged generator (nether, end, other dimensions get the feature through biome modifiers
			// without a filter): nothing to do; throwing here left the chunk unfinished and froze the server
			return false;
		}
		BlockPos.MutableBlockPos above = new MutableBlockPos();
		erodeChunk(placeContext.config(), rtfRandomState.generatorContext(), placeContext.chunkGenerator(), chunk, level::getBiome, (x, surfaceY, z) -> {
			//remove any foliage that may have generated above
			above.set(x, surfaceY, z);
			while(!level.getBlockState(above.setY(above.getY() + 1)).canSurvive(level, above)) {
				level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
			}
		});
		return true;
	}

	public interface ErodedColumn {
		void accept(int x, int surfaceY, int z);
	}

	/**
	 * Marks a chunk eroded as its surface was built: its inhabited time (saved with chunks that are not finished yet
	 * too, and carried over to the finished chunk) is at least 1 tick. Regional difficulty counts up to 3600000 ticks.
	 */
	public static final long ERODED = 1L;

	/**
	 * Erodes a chunk as its surface is built, before any feature of it or of a neighbour chunk runs: the result no
	 * longer depends on the order in which the chunks are generated. Called by MixinNoiseBasedChunkGenerator after the
	 * surface rules when the preset has the erosion decorator (the reterraforged:erode placed feature exists).
	 */
	public static void erodeChunk(Config config, GeneratorContext generatorContext, ChunkGenerator generator, ChunkAccess chunk, Function<BlockPos, Holder<Biome>> biomes, @Nullable ErodedColumn eroded) {
		ChunkPos chunkPos = chunk.getPos();
		int chunkX = chunkPos.x();
		int chunkZ = chunkPos.z();
		Tile.Chunk tileChunk = generatorContext.cache.provideAtChunk(chunkX, chunkZ).getChunkReader(chunkX, chunkZ);
		raccoonman.reterraforged.world.worldgen.cell.heightmap.Heightmap heightmap = generatorContext.generator.getHeightmap();
		Levels levels = heightmap.levels();
		Noise rand = Noises.white(heightmap.climate().randomSeed(), 1);
		BlockPos.MutableBlockPos pos = new MutableBlockPos();
		for(int x = 0; x < 16; x++) {
			for(int z = 0; z < 16; z++) {
				int worldX = chunkPos.getBlockX(x);
				int worldZ = chunkPos.getBlockZ(z);
				
				Cell cell = tileChunk.getCell(x, z);
				int scaledY = levels.scale(cell.height);
				int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
				Holder<Biome> biome = biomes.apply(pos.set(worldX, surfaceY, worldZ));
				
				pos.set(worldX, surfaceY, worldZ);
				
		        if(surfaceY <= scaledY && surfaceY >= generator.getSeaLevel() - 1 && !biome.is(Biomes.WOODED_BADLANDS) && !biome.is(Biomes.BADLANDS)) {
					erodeColumn(config, rand, generator, chunk, cell, pos, surfaceY);
					if(eroded != null) {
						eroded.accept(worldX, surfaceY, worldZ);
					}
				}
			}
		}
	}
	
	private static void erodeColumn(Config config, Noise rand, ChunkGenerator generator, ChunkAccess chunk, Cell cell, BlockPos.MutableBlockPos pos, int surfaceY) {
        if (cell.terrain.isRiver() || cell.terrain.isWetland()) {
            return;
        }

        boolean volcanic = isActiveVolcano(config, cell);
        if (cell.terrain == TerrainType.VOLCANO_PIPE) {
            if (volcanic) {
                volcanicCrater(chunk, pos, surfaceY);
            }
            return;
        }
		
        BlockState top = chunk.getBlockState(pos);
        if(top.is(RTFBlockTags.ERODIBLE)) {
            BlockState material = getMaterial(config, rand, cell, pos, top, generator instanceof NoiseBasedChunkGenerator noiseChunkGenerator ? noiseChunkGenerator.generatorSettings().value().defaultBlock() : Blocks.STONE.defaultBlockState());
            if (material != top) {
                if (material.is(RTFBlockTags.ROCK)) {
                	erodeRock(config, volcanic, chunk, cell, pos, surfaceY);
                    return;
                } else {
                    ColumnDecorator.fillDownSolid(chunk, pos, surfaceY, surfaceY - 4, material);
                }
            }
            placeScree(config, rand, chunk, cell, pos, surfaceY);
        }
	}

    // Volcano Biome Usage: this share of the volcanoes (by their terrain region) has a volcanic surface, basalt on
    // the rock of its cone and magma and blackstone in its crater. 0 (presets saved before 0.0.20, when the setting did
    // nothing): none
    private static boolean isActiveVolcano(Config config, Cell cell) {
        if (config.volcanoSurface() <= 0.0F || (cell.terrain != TerrainType.VOLCANO && cell.terrain != TerrainType.VOLCANO_PIPE)) {
            return false;
        }
        long hash = (long) Math.floor(cell.terrainRegionCenterX) * 73856093L ^ (long) Math.floor(cell.terrainRegionCenterZ) * 19349663L;
        hash = (hash ^ (hash >>> 29)) * 0x9E3779B97F4A7C15L;
        return Math.floorMod(hash >>> 11, 10000L) / 10000.0F < config.volcanoSurface();
    }

    private static void volcanicCrater(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int y) {
        int x = pos.getX();
        int z = pos.getZ();
        // magma in patches on blackstone
        BlockState top = ((x * 31 + z * 17) & 3) == 0 ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
        ColumnDecorator.replaceSolid(chunk, pos.setY(y), top);
        for (int dy = 1; dy < 4; dy++) {
            ColumnDecorator.replaceSolid(chunk, pos.setY(y - dy), Blocks.BLACKSTONE.defaultBlockState());
        }
    }

    private static void erodeRock(Config config, boolean volcanic, ChunkAccess chunk, Cell cell, BlockPos.MutableBlockPos pos, int y) {
        int depth = 32;
        BlockState material = Blocks.GRAVEL.defaultBlockState();
        // find the uppermost layer of rock & record it's depth
        for (int dy = 3; dy < 32; dy++) {
            pos.setY(y - dy);
            BlockState state = chunk.getBlockState(pos);
            if (state.is(RTFBlockTags.ROCK)) {
                material = volcanic ? Blocks.BASALT.defaultBlockState() : config.plainStone() ? Blocks.STONE.defaultBlockState() : state;
                depth = dy + 1;
                break;
            }
        }

        // fill downwards to the first rock layer
        for (int dy = 0; dy < depth; dy++) {
            ColumnDecorator.replaceSolid(chunk, pos.setY(y - dy), material);
        }
    }
	
	private static void placeScree(Config config, Noise rand, ChunkAccess chunk, Cell cell, BlockPos.MutableBlockPos pos, int surfaceY) {
    	int x = pos.getX();
    	int z = pos.getZ();
    	float steepness = cell.gradient + rand.compute(x, z, 1) * config.slopeModifier();
        if (steepness < config.screeSteepness()) {
            return;
        }

        float sediment = cell.sediment * config.sedimentNoise();
        float noise = rand.compute(x, z, 2) * config.sedimentNoise();
        if (sediment + noise > config.screeValue()) {
            ColumnDecorator.fillDownSolid(chunk, pos, surfaceY, surfaceY - 2, Blocks.GRAVEL.defaultBlockState());
        }
	}
	
    private static BlockState getMaterial(Config config, Noise rand, Cell cell, BlockPos.MutableBlockPos pos, BlockState top, BlockState middle) {
    	int x = pos.getX();
    	int z = pos.getZ();
        float height = cell.height + rand.compute(x, z, 0) * config.heightModifier();
        float steepness = cell.gradient + rand.compute(x, z, 1) * config.slopeModifier();

        if (steepness > config.rockSteepness() || height > ColumnDecorator.sampleNoise(x, z, config.rockVar(), config.rockMin())) {
            return rock(middle);
        }

        if (steepness > config.dirtSteepness() && height > ColumnDecorator.sampleNoise(x, z, config.dirtVar(), config.dirtMin())) {
            return ground(top);
        }

        return top;
    }
    
    private static BlockState rock(BlockState state) {
        if (state.is(RTFBlockTags.ROCK)) {
            return state;
        }
        return Blocks.STONE.defaultBlockState();
    }

    private static BlockState ground(BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM)) {
        	return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (state.is(BlockTags.BASE_STONE_OVERWORLD)) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        if (state.is(BlockTags.SUBSTRATE_OVERWORLD)) {
            return state;
        }
        if (state.is(Blocks.SAND)) {
        	return Blocks.SMOOTH_SANDSTONE.defaultBlockState();
        }
        if (state.is(Blocks.RED_SAND)) {
        	return Blocks.SMOOTH_RED_SANDSTONE.defaultBlockState();
        }
        return Blocks.COARSE_DIRT.defaultBlockState();
    }

    // plainStone (Plain Stone Erosion): eroded rock slopes of plain stone; off, of the rock layer found below them (the
    // strata), as before 0.0.20 when the setting did nothing
    // volcanoSurface (Volcano Biome Usage): the share of volcanoes with a volcanic surface (isActiveVolcano)
    public record Config(int rockVar, int rockMin, int dirtVar, int dirtMin, float rockSteepness, float dirtSteepness, float screeSteepness, float heightModifier, float slopeModifier, float sedimentModifier, float sedimentNoise, float screeValue, boolean plainStone, float volcanoSurface) implements FeatureConfiguration {

		public Config(int rockVar, int rockMin, int dirtVar, int dirtMin, float rockSteepness, float dirtSteepness, float screeSteepness, float heightModifier, float slopeModifier, float sedimentModifier, float sedimentNoise, float screeValue) {
			this(rockVar, rockMin, dirtVar, dirtMin, rockSteepness, dirtSteepness, screeSteepness, heightModifier, slopeModifier, sedimentModifier, sedimentNoise, screeValue, false, 0.0F);
		}

		public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("rock_var").forGetter(Config::rockVar),
			Codec.INT.fieldOf("rock_min").forGetter(Config::rockMin),
			Codec.INT.fieldOf("dirt_var").forGetter(Config::dirtVar),
			Codec.INT.fieldOf("dirt_min").forGetter(Config::dirtMin),
			Codec.FLOAT.fieldOf("rock_steepness").forGetter(Config::rockSteepness),
			Codec.FLOAT.fieldOf("dirt_steepness").forGetter(Config::dirtSteepness),
			Codec.FLOAT.fieldOf("scree_steepness").forGetter(Config::screeSteepness),
			Codec.FLOAT.fieldOf("height_modifier").forGetter(Config::heightModifier),
			Codec.FLOAT.fieldOf("slope_modifier").forGetter(Config::slopeModifier),
			Codec.FLOAT.fieldOf("sediment_modifier").forGetter(Config::sedimentModifier),
			Codec.FLOAT.fieldOf("sediment_noise").forGetter(Config::sedimentNoise),
			Codec.FLOAT.fieldOf("screeValue").forGetter(Config::screeValue),
			Codec.BOOL.optionalFieldOf("plain_stone", false).forGetter(Config::plainStone),
			Codec.FLOAT.optionalFieldOf("volcano_surface", 0.0F).forGetter(Config::volcanoSurface)
		).apply(instance, Config::new));
	}
}
