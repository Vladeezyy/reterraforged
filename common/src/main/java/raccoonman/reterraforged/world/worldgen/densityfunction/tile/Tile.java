package raccoonman.reterraforged.world.worldgen.densityfunction.tile;

import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.CellLookup;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.filter.Filterable;

public class Tile implements Filterable, CellLookup {
	private final int x, z;
	private final int chunkX, chunkZ;
	private final int size;
	private final int border;
	private final Size blockSize;
	private final Size chunkSize;
	private final Cell[] cache;
	private final Chunk[] chunks;
	
	public Tile(int x, int z, int size, int border, Size blockSize, Size chunkSize, Cell[] cells, Chunk[] chunks) {
		this.x = x;
		this.z = z;
        this.chunkX = x << size;
        this.chunkZ = z << size;
        this.size = size;
        this.border = border;
		this.blockSize = blockSize;
		this.chunkSize = chunkSize;
		this.cache = cells;
		this.chunks = chunks;
	}
	
	public int getX() {
		return this.x;
	}
	
	public int getZ() {
		return this.z;
	}
	
	@Override
	public Cell lookup(int blockX, int blockZ) {
		int border = this.blockSize.border();
        int relBlockX = border + this.blockSize.mask(blockX);
        int relBlockZ = border + this.blockSize.mask(blockZ);
        int index = this.blockSize.indexOf(relBlockX, relBlockZ);
        return this.cache[index];
	}
	
	public Chunk getChunkWriter(int chunkX, int chunkZ) {
        int index = this.chunkSize.indexOf(chunkX, chunkZ);
        return this.computeChunk(index, chunkX, chunkZ);
	}
	
	public Chunk getChunkReader(int chunkX, int chunkZ) {
        int relChunkX = this.chunkSize.border() + this.chunkSize.mask(chunkX);
        int relChunkZ = this.chunkSize.border() + this.chunkSize.mask(chunkZ);
        int index = this.chunkSize.indexOf(relChunkX, relChunkZ);
        return this.computeChunk(index, chunkX, chunkZ);
	}
	
    public void iterate(Cell.Visitor visitor) {
        for (int dz = 0; dz < this.blockSize.size(); ++dz) {
            int z = this.blockSize.border() + dz;
            for (int dx = 0; dx < this.blockSize.size(); ++dx) {
                int x = this.blockSize.border() + dx;
                int index = this.blockSize.indexOf(x, z);
                Cell cell = this.cache[index];
                visitor.visit(cell, dx, dz);
            }
        }
    }

	@Override
	public int getBlockX() {
		return Size.chunkToBlock(this.chunkX);
	}

	@Override
	public int getBlockZ() {
		return Size.chunkToBlock(this.chunkZ);
	}

	@Override
	public Size getBlockSize() {
		return this.blockSize;
	}

	public Size getChunksSize() {
		return this.chunkSize;
	}

	@Override
	public Cell[] getBacking() {
		return this.cache;
	}

	@Override
	public Cell getCellRaw(int x, int z) {
		int index = Tile.this.blockSize.indexOf(x, z);
        if (index < 0 || index >= Tile.this.blockSize.arraySize()) {
            return Cell.empty();
        }
        return Tile.this.cache[index];
	}

    private Chunk computeChunk(int index, int chunkX, int chunkZ) {
        Chunk chunk = this.chunks[index];
        if (chunk == null) {
            chunk = new Chunk(chunkX, chunkZ);
            this.chunks[index] = chunk;
        }
        return chunk;
    }
	
	public class Chunk {
        private final int chunkX;
        private final int chunkZ;
        private final int blockX;
        private final int blockZ;
        private final int regionBlockX;
        private final int regionBlockZ;
		
		public Chunk(int regionChunkX, int regionChunkZ) {
            this.regionBlockX = regionChunkX << 4;
            this.regionBlockZ = regionChunkZ << 4;
            this.chunkX = Tile.this.chunkX + regionChunkX - Tile.this.border;
            this.chunkZ = Tile.this.chunkZ + regionChunkZ - Tile.this.border;
            this.blockX = this.chunkX << 4;
            this.blockZ = this.chunkZ << 4;
		}
        
        public int getBlockX() {
            return this.blockX;
        }
        
        public int getBlockZ() {
            return this.blockZ;
        }
        
        public Cell getCell(int blockX, int blockZ) {
            int relX = this.regionBlockX + (blockX & 0xF);
            int relZ = this.regionBlockZ + (blockZ & 0xF);
            int index = Tile.this.blockSize.indexOf(relX, relZ);
            return Tile.this.cache[index];
        }
	}
}
