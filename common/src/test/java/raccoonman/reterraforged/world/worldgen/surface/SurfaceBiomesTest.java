package raccoonman.reterraforged.world.worldgen.surface;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;

/** The erosion of a new world reads the chunk's biomes (the region's manager), an older world's the biome source's */
class SurfaceBiomesTest {
	private static final long SEED = BiomeManager.obfuscateSeed(1234L);
	// no biome needed: the holders are told apart by identity
	private static final Holder<Biome> CHUNK = Holder.direct(null), SOURCE = Holder.direct(null);

	@Test
	void aNewWorldErodesByTheChunksBiomes() {
		BiomeManager region = new BiomeManager((x, y, z) -> CHUNK, SEED);
		BiomeManager biomes = SurfaceBiomes.erosion(true, region, (x, y, z) -> SOURCE);
		assertSame(region, biomes);
		assertSame(CHUNK, biomes.getBiome(new BlockPos(7, 70, -9)));
	}

	@Test
	void anOlderWorldErodesByTheBiomeSource() {
		BiomeManager region = new BiomeManager((x, y, z) -> CHUNK, SEED);
		BiomeManager biomes = SurfaceBiomes.erosion(false, region, (x, y, z) -> SOURCE);
		for(BlockPos pos : new BlockPos[] { new BlockPos(0, 0, 0), new BlockPos(15, 64, 15), new BlockPos(-17, 200, 31) }) {
			assertSame(SOURCE, biomes.getBiome(pos));
		}
	}
}
