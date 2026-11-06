package raccoonman.reterraforged.world.worldgen.densityfunction.tile.generation;

import java.util.concurrent.CompletableFuture;

import raccoonman.reterraforged.concurrent.ThreadPools;
import raccoonman.reterraforged.world.worldgen.WorldFilters;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.cell.rivermap.Rivermap;
import raccoonman.reterraforged.world.worldgen.densityfunction.CellSampler;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Size;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile.Chunk;

public class TileGenerator {
	private final Heightmap heightmap;
	private final WorldFilters filters;
	private final int tileChunks;
	private final int tileBorder;
	private final Size tileSizeBlocks;
	private final Size tileSizeChunks;
	private final int batchSize;
	private final int batchCount;
	
	public TileGenerator(Heightmap heightmap, WorldFilters filters, int tileChunks, int tileBorder, int batchCount) {
		this.heightmap = heightmap;
		this.filters = filters;
		this.tileChunks = tileChunks;
		this.tileBorder = tileBorder;
		this.tileSizeBlocks = Size.blocks(tileChunks, tileBorder);
		this.tileSizeChunks = Size.chunks(tileChunks, tileBorder);
		this.batchSize = getBatchSize(batchCount, this.tileSizeChunks);
		this.batchCount = batchCount;
	}
	
	public long tileCells() {
		return this.tileSizeBlocks.arraySize();
	}

	public Heightmap getHeightmap() {
		return this.heightmap;
	}
	
	public CompletableFuture<Tile> generate(int tileX, int tileZ) {
		// x * 1 + 0 is x in floats: the cells are those of the block positions
		return this.generate(this.makeTile(tileX, tileZ), 1.0F, 0.0F, 0.0F, true);
	}

	/** A map around a center, one cell every zoom blocks (the preset screen's preview) */
	public CompletableFuture<Tile> generateZoomed(float centerX, float centerZ, float zoom, boolean applyOptionalFilters) {
		float translateX = centerX - this.tileSizeBlocks.size() * zoom / 2.0F;
		float translateZ = centerZ - this.tileSizeBlocks.size() * zoom / 2.0F;
		return this.generate(this.makeTile(0, 0), zoom, translateX, translateZ, applyOptionalFilters);
	}

	// the tile's chunks in batchCount x batchCount batches on the generation threads, then the filters (erosion, smoothing)
	private CompletableFuture<Tile> generate(Tile tile, float zoom, float translateX, float translateZ, boolean applyOptionalFilters) {
		CompletableFuture<?>[] futures = new CompletableFuture<?>[this.batchCount * this.batchCount];
		for (int batchZ = 0; batchZ < this.batchCount; batchZ++) {
			for (int batchX = 0; batchX < this.batchCount; batchX++) {
				int chunkX = batchX * this.batchSize;
				int chunkZ = batchZ * this.batchSize;
				futures[batchX * this.batchCount + batchZ] = CompletableFuture.runAsync(() -> {
					int maxX = Math.min(this.tileSizeChunks.total(), chunkX + this.batchSize);
					int maxZ = Math.min(this.tileSizeChunks.total(), chunkZ + this.batchSize);
					for (int cZ = chunkZ; cZ < maxZ; cZ++) {
						for (int cX = chunkX; cX < maxX; cX++) {
							Chunk chunk = tile.getChunkWriter(cX, cZ);
							Rivermap rivers = null;
							for (int dz = 0; dz < 16; dz++) {
								for (int dx = 0; dx < 16; dx++) {
									float worldX = (chunk.getBlockX() + dx) * zoom + translateX;
									float worldZ = (chunk.getBlockZ() + dz) * zoom + translateZ;
									Cell cell = chunk.getCell(dx, dz);
									this.heightmap.applyTerrain(cell, worldX, worldZ);
									rivers = Rivermap.get(cell, rivers, this.heightmap);
									this.heightmap.applyRivers(cell, worldX, worldZ, rivers);
									this.heightmap.applyClimate(cell, worldX, worldZ, true);
								}
							}
						}
					}
					CellSampler.clearThreadLocalCache();
				}, ThreadPools.WORLD_GEN);
			}
		}
		return CompletableFuture.allOf(futures).thenApply((v) -> {
			this.filters.apply(tile, applyOptionalFilters);
			return tile;
		});
	}

	private Tile makeTile(int x, int z) {
		// new arrays for every tile: a dropped tile may still be read, so its arrays are not reused
		Cell[] cells = new Cell[this.tileSizeBlocks.arraySize()];
		for (int i = 0; i < cells.length; i++) {
			cells[i] = new Cell();
		}
		return new Tile(x, z, this.tileChunks, this.tileBorder, this.tileSizeBlocks, this.tileSizeChunks, cells, new Chunk[this.tileSizeChunks.arraySize()]);
	}
	
    private static int getBatchSize(int batchCount, Size chunkSize) {
        int batchSize = chunkSize.total() / batchCount;
        if (batchSize * batchCount < chunkSize.total()) {
            ++batchSize;
        }
        return batchSize;
    }
}
