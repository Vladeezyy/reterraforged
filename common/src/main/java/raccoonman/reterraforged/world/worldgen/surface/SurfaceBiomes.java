package raccoonman.reterraforged.world.worldgen.surface;

import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;

/**
 * The biomes a chunk's surface is decorated by. The surface rules get the region's biome manager (vanilla
 * NoiseBasedChunkGenerator.buildSurface): the biomes saved in the chunk and its neighbours at the BIOMES step, which the
 * tile's cells decided (the beaches, the shores after its filters). The SURFACE step has its neighbours at BIOMES within 1
 * chunk (ChunkPyramid), all the fuzzy zoom of a column in the chunk reads. The erosion asked the biome source again, with
 * the cell computed without the tile: at the shores and on the slopes another biome than the chunk's (#38).
 */
public final class SurfaceBiomes {
	// from this generation version the erosion reads the chunk's biomes, as the surface rules
	public static final int CHUNK_BIOMES_VERSION = 1;

	private SurfaceBiomes() {
	}

	// the biomes of the erosion decorator for a chunk whose surface is built in the region with these biomes
	public static BiomeManager erosion(GeneratorContext generatorContext, BiomeManager regionBiomes, ChunkGenerator generator, RandomState randomState) {
		return erosion(generatorContext.preset.world().properties.fixes(CHUNK_BIOMES_VERSION), regionBiomes, (x, y, z) -> generator.getBiomeSource().getNoiseBiome(x, y, z, randomState.sampler()));
	}

	// chunkBiomes off (older worlds): the biome source's, as before, for the same blocks
	static BiomeManager erosion(boolean chunkBiomes, BiomeManager regionBiomes, BiomeManager.NoiseBiomeSource biomeSource) {
		return chunkBiomes ? regionBiomes : regionBiomes.withDifferentSource(biomeSource);
	}
}
