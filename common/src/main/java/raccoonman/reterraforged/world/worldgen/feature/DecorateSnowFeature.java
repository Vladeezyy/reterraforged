package raccoonman.reterraforged.world.worldgen.feature;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SpreadingSnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;
import raccoonman.reterraforged.world.worldgen.feature.DecorateSnowFeature.Config;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;

public class DecorateSnowFeature extends Feature<Config> {
    private static final float SNOW_ROCK_STEEPNESS = 0.45F;
    private static final float SNOW_ROCK_HEIGHT = 95.0F / 255.0F;
    private static final float MIN = min(SnowLayerBlock.LAYERS);
    private static final float MAX = max(SnowLayerBlock.LAYERS);

	public DecorateSnowFeature(Codec<Config> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<Config> placeContext) {
		WorldGenLevel level = placeContext.level();
		RandomState randomState = level.getLevel().getChunkSource().randomState();
		
		@Nullable
		GeneratorContext generatorContext;
		if((Object) randomState instanceof RTFRandomState rtfRandomState && (generatorContext = rtfRandomState.generatorContext()) != null) {
			ChunkGenerator generator = placeContext.chunkGenerator();
			ChunkPos chunkPos = ChunkPos.containing(placeContext.origin());
			int chunkX = chunkPos.x();
			int chunkZ = chunkPos.z();
			ChunkAccess chunk = level.getChunk(chunkX, chunkZ);
			Tile.Chunk tileChunk = generatorContext.cache.provideAtChunk(chunkX, chunkZ).getChunkReader(chunkX, chunkZ);
			raccoonman.reterraforged.world.worldgen.cell.heightmap.Heightmap heightmap = generatorContext.generator.getHeightmap();
			Levels levels = heightmap.levels();
			Noise rand = randomNoise(generatorContext);
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			Config config = placeContext.config();
			ErodeFeature.Config erodeConfig = config.erodeConfig();
			
			for(int x = 0; x < 16; x++) {
				for(int z = 0; z < 16; z++) {
		        	Cell cell = tileChunk.getCell(x, z);
					int scaledY = levels.scale(cell.height);
					// before the snow line's snow: two layers and more are in the heightmap
			        int surfaceY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
					// snowLine: the Y above which the ground gets snow whatever the biome, -1 (presets exported
					// before 0.0.25): none; the layers it put on the column, 0 below the line
					int snowLineLayers = config.snowLine() >= 0 ? placeSnowLine(level, chunk, pos, x, z, chunkPos.getBlockX(x), chunkPos.getBlockZ(z), config, rand) : 0;
					boolean aboveSnowLine = snowLineLayers > 0;
			        if(scaledY == surfaceY && scaledY >= generator.getSeaLevel()) {
		        		int worldX = chunkPos.getBlockX(x);
		        		int worldZ = chunkPos.getBlockZ(z);
				        pos.set(worldX, surfaceY, worldZ);
				        
				        if(config.erode) {
				        	// steep rock above the snow line sheds its snow like a cold biome's
				        	if(aboveSnowLine || level.getBiome(pos).value().getTemperature(pos, generator.getSeaLevel()) <= 0.25) {
					            float var = -ColumnDecorator.sampleNoise(worldX, worldZ, 16, 0);
					            float hNoise = rand.compute(worldX, worldZ, 4) * erodeConfig.heightModifier();
					            float sNoise = rand.compute(worldX, worldZ, 5) * erodeConfig.slopeModifier();
					            float vModifier = cell.terrain == TerrainType.VOLCANO ? 0.15F : 0F;
					            float height = cell.height + var + hNoise + vModifier;
					            float steepness = cell.gradient + var + sNoise + vModifier;
					            if (snowErosion(erodeConfig, worldX, worldZ, steepness, height)) {
					                Predicate<BlockState> predicate = Heightmap.Types.MOTION_BLOCKING.isOpaque();
					                for (int dy = 2; dy > 0; dy--) {
					                    pos.setY(surfaceY + dy);
					                    BlockState state = chunk.getBlockState(pos);
					                    if (!predicate.test(state) || state.is(Blocks.SNOW)) {
							            	erodeSnow(chunk, pos);
					                    }
					                }
					            }
					        }
				        }
				        
				        // deeper snow of the snow line keeps its depth: the smoothing below sets the layers from the
				        // terrain's height and would take it back to one or two
				        if(config.smooth && snowLineLayers <= 1) {
				            pos.setY(surfaceY + 1);

				            BlockState state = chunk.getBlockState(pos);
				            if (state.isAir()) {
				                pos.setY(surfaceY);
				                state = chunk.getBlockState(pos);
				                if (state.isAir()) {
				                    continue;
				                }
				            }

				            if(state.is(Blocks.SNOW)) {
				            	smoothSnow(chunk, pos, cell, levels, 0.0F);
				            }
				        }
			        }
				}
			}
	        return true;
		} else {
			// not a ReTerraForged generator (nether, end, other dimensions get the feature through biome modifiers
			// without a filter): nothing to do; throwing here left the chunk unfinished and froze the server
			return false;
		}
	}

    /**
     * The snow line: above it (give or take the variation: a wide noise and a little per column, so the line is
     * ragged) the top block of the column, ground or leaves, gets snow as vanilla's freezing gives a cold biome, and
     * still water freezes. The snow is as deep as the preset says for the column's height above the line (its depth
     * there, the peak depth further up), give or take a little per column so the steps between depths are not
     * contour lines. Returns the layers put on the column (1 for ice or a column where snow cannot lie), 0 below the
     * line.
     */
    private static int placeSnowLine(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos pos, int x, int z, int worldX, int worldZ, Config config, Noise rand) {
        int topY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        int layers = snowLineLayers(topY, worldX, worldZ, config.snowLine(), config.snowLineVariation(), config.snowLineDepth(), config.snowLinePeakDepth(), config.snowLineDepthRange(), rand);
        if (layers == 0) {
            return 0;
        }
        BlockState top = chunk.getBlockState(pos.set(worldX, topY, worldZ));
        if (top.is(Blocks.WATER) && top.getFluidState().isSource()) {
            chunk.setBlockState(pos, Blocks.ICE.defaultBlockState());
            return 1;
        }
        pos.setY(topY + 1);
        BlockState snow = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers);
        if (chunk.getBlockState(pos).isAir() && snow.canSurvive(level, pos)) {
            chunk.setBlockState(pos, snow);
            if (top.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.SNOWY)) {
                chunk.setBlockState(pos.setY(topY), top.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SNOWY, true));
            }
            return layers;
        }
        // a cold biome's own snow (vanilla's freezing comes first) is made as deep as the line's
        BlockState above = chunk.getBlockState(pos);
        if (layers > 1 && above.is(Blocks.SNOW)) {
            if (above.getValue(SnowLayerBlock.LAYERS) < layers) {
                chunk.setBlockState(pos, snow);
            }
            return layers;
        }
        return 1;
    }

    // the per column noise of the snow decorator (also rtf snowline, to know the depth each column must have)
    public static Noise randomNoise(GeneratorContext generatorContext) {
        return Noises.white(generatorContext.generator.getHeightmap().climate().randomSeed(), 1);
    }

    /**
     * The snow line's layers on a column whose top block is at topY, 0 below the line. The line is ragged (a wide noise
     * and a little per column), the depth goes from the depth at the line to the peak depth over depthRange blocks,
     * give or take a little per column so the steps between depths are not contour lines.
     */
    public static int snowLineLayers(int topY, int worldX, int worldZ, int snowLine, int variation, int depth, int peakDepth, int depthRange, Noise rand) {
        float wide = (ColumnDecorator.sampleNoise(worldX, worldZ, 1.0F, 0.0F) - 0.5F) * 2.0F;
        float local = (rand.compute(worldX, worldZ, 6) - 0.5F) * 2.0F;
        float line = snowLine + variation * (0.8F * wide + 0.2F * local);
        if (topY < line) {
            return 0;
        }
        float t = Math.max(0.0F, Math.min(1.0F, (topY - line) / Math.max(1, depthRange)));
        float layers = depth + (peakDepth - depth) * t;
        return Math.max(1, Math.min(SnowLayerBlock.MAX_HEIGHT, Math.round(layers + 0.35F * local)));
    }

    private static boolean snowErosion(ErodeFeature.Config erodeConfig, float x, float z, float steepness, float height) {
        return steepness > erodeConfig.rockSteepness() || (steepness > SNOW_ROCK_STEEPNESS && height > SNOW_ROCK_HEIGHT) || (steepness > erodeConfig.dirtSteepness() && height > ColumnDecorator.sampleNoise(x, z, erodeConfig.dirtVar(), erodeConfig.dirtMin()));
    }

    private static void erodeSnow(ChunkAccess chunk, BlockPos.MutableBlockPos pos) {
        chunk.setBlockState(pos, Blocks.AIR.defaultBlockState());

        if (pos.getY() > 0) {
            pos.setY(pos.getY() - 1);
            BlockState below = chunk.getBlockState(pos);
            if (below.hasProperty(GrassBlock.SNOWY)) {
                chunk.setBlockState(pos, below.setValue(GrassBlock.SNOWY, false));
            }
        }
    }
    
    private static void smoothSnow(ChunkAccess chunk, BlockPos.MutableBlockPos pos, Cell cell, Levels levels, float min) {
        float height = cell.height * levels.worldHeight;
        float depth = getDepth(height);
        if (depth > min) {
            int level = getLevel(depth);
            BlockState layer = getState(level);
            if (layer.is(Blocks.AIR)) {
                return;
            }
            chunk.setBlockState(pos, layer);

           fixBaseBlock(chunk, pos, layer, level);
        }
    }

    private static void fixBaseBlock(ChunkAccess chunk, BlockPos pos, BlockState layerMaterial, int level) {
        if (level > 1 && layerMaterial.is(Blocks.SNOW)) {
            BlockPos pos1 = pos.below();
            BlockState below = chunk.getBlockState(pos1);

            // Turns to dirt if submerged or the light-level is low. Light hasn't been calc'd at this
            // at this stage of world-gen so just blanket set everything to snowy dirt.
            if (below.getBlock() instanceof SpreadingSnowyBlock) {
                chunk.setBlockState(pos1, Blocks.DIRT.defaultBlockState());
            }
        }
    }

    private static BlockState getState(int level) {
        if (level < MIN) {
            return Blocks.AIR.defaultBlockState();
        }
        if (level >= MAX) {
            return Blocks.SNOW_BLOCK.defaultBlockState();
        }
        return Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, level);
    }
    
    private static int getLevel(float depth) {
        if (depth > 1) {
            depth = getDepth(depth);
        } else if (depth < 0) {
            depth = 0;
        }
        return NoiseUtil.round(depth * MAX);
    }

    private static float getDepth(float height) {
        return height - (int) height;
    }

    private static int min(Property<Integer> property) {
        return property.getPossibleValues().stream().min(Integer::compareTo).orElse(0);
    }

    private static int max(Property<Integer> property) {
        return property.getPossibleValues().stream().max(Integer::compareTo).orElse(0);
    }

	public record Config(boolean erode, boolean smooth, ErodeFeature.Config erodeConfig, int snowLine, int snowLineVariation, int snowLineDepth, int snowLinePeakDepth, int snowLineDepthRange) implements FeatureConfiguration {
		public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("erode").forGetter(Config::erode),
			Codec.BOOL.fieldOf("smooth").forGetter(Config::smooth),
			ErodeFeature.Config.CODEC.fieldOf("erode_config").forGetter(Config::erodeConfig),
			Codec.INT.optionalFieldOf("snow_line", -1).forGetter(Config::snowLine),
			Codec.INT.optionalFieldOf("snow_line_variation", 0).forGetter(Config::snowLineVariation),
			// presets exported before 0.0.26: one layer
			Codec.INT.optionalFieldOf("snow_line_depth", 1).forGetter(Config::snowLineDepth),
			Codec.INT.optionalFieldOf("snow_line_peak_depth", 1).forGetter(Config::snowLinePeakDepth),
			Codec.INT.optionalFieldOf("snow_line_depth_range", 64).forGetter(Config::snowLineDepthRange)
		).apply(instance, Config::new));
	}
}
