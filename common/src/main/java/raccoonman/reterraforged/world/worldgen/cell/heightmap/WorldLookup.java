package raccoonman.reterraforged.world.worldgen.cell.heightmap;

import java.util.concurrent.atomic.AtomicInteger;

import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.TileCache;

public class WorldLookup {
	// the structure starts of chunks being generated (ChunkGenerator.createStructures, MixinChunkGenerator): there the
	// lookups read the tiles, as the chunks' own terrain does; elsewhere (/locate, explorer maps, dolphins, the
	// strongholds' biome search) they compute the cell without the tile's erosion and beach detection. They used to take
	// the tile when it happened to be cached: structures and their heights depended on the order chunks were generated in
	private static final ThreadLocal<int[]> STRUCTURE_GENERATION = ThreadLocal.withInitial(() -> new int[1]);
	private static final AtomicInteger NEXT_ID = new AtomicInteger();
	// tells the lookups apart without keeping them (a thread's cached cell, CellSampler.Cache2d)
	public final int id = NEXT_ID.incrementAndGet();
	private final float waterLevel;
	private final float beachLevel;
	private final TileCache cache;
	private final Heightmap heightmap;
	private final Levels levels;
	
	public WorldLookup(GeneratorContext context) {
		this.cache = context.cache;
		this.heightmap = context.generator.getHeightmap();
		this.waterLevel = context.levels.water;
		this.beachLevel = context.levels.water(5);
		this.levels = context.levels;
	}
	
	public static void beginStructureGeneration() {
		STRUCTURE_GENERATION.get()[0]++;
	}

	public static void endStructureGeneration() {
		STRUCTURE_GENERATION.get()[0]--;
	}

	public static boolean inStructureGeneration() {
		return STRUCTURE_GENERATION.get()[0] > 0;
	}

	public Heightmap getHeightmap() {
		return this.heightmap;
	}

	public boolean applyCell(Cell cell, int x, int z, boolean applyClimate) {
		return this.applyCell(cell, x, z, false, applyClimate);
	}

	public boolean applyCell(Cell cell, int x, int z, boolean load, boolean applyClimate) {
		if (load && this.computeAccurate(cell, x, z)) {
			return true;
		}
		return this.compute(cell, x, z, applyClimate);
	}

	private boolean computeAccurate(Cell cell, int x, int z) {
		int rx = this.cache.chunkToTile(x >> 4);
		int rz = this.cache.chunkToTile(z >> 4);
		Tile tile = this.cache.provide(rx, rz);
		Cell c = tile.lookup(x, z);
		if (c != null) {
			cell.copyFrom(c);
		}
		if (cell.terrain != null && cell.terrain == TerrainType.ISLAND_BEACH && cell.height > this.waterLevel && cell.height <= this.beachLevel) {
			cell.terrain = TerrainType.BEACH;
		}
		return cell.terrain != null;
	}

	private boolean compute(Cell cell, int x, int z, boolean applyClimate) {
		this.heightmap.apply(cell, x, z, applyClimate);
		if (cell.terrain == TerrainType.COAST && cell.height > this.waterLevel && cell.height <= this.beachLevel) {
			cell.terrain = TerrainType.BEACH;
		}
		if (cell.terrain == TerrainType.ISLAND_BEACH && cell.height > this.waterLevel && cell.height <= this.beachLevel) {
			cell.terrain = TerrainType.BEACH;
		}
		return false;
	}
}
