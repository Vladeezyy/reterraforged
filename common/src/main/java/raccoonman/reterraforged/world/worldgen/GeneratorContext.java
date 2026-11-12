package raccoonman.reterraforged.world.worldgen;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderGetter;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.WorldLookup;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.TileCache;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.generation.TileGenerator;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.util.Seed;

public final class GeneratorContext {
	// from generation version 1 the terrain has the upper 32 bits of the world's seed too
	public static final int FULL_SEED_VERSION = 1;
    public final Seed seed;
    public final Levels levels;
    public final Preset preset;
    public final HolderGetter<Noise> noiseLookup;
    public final TileGenerator generator;
    // the world's tiles: null for a preview (the preset screen's map, the tests' estimates), which computes every cell
    @Nullable
    public final TileCache cache;
    public final WorldLookup lookup;

    private GeneratorContext(Preset preset, HolderGetter<Noise> noiseLookup, int seed, int tileSize, int tileBorder, int batchCount, boolean cached, boolean queue) {
        this.preset = preset;
        this.noiseLookup = noiseLookup;
        this.seed = new Seed(seed);
        this.levels = new Levels(preset.world().properties.terrainScaler(), preset.world().properties.seaLevel);
        this.generator = new TileGenerator(Heightmap.make(this), new WorldFilters(this), tileSize, tileBorder, batchCount);
        this.cache = cached ? new TileCache(tileSize, queue, this.generator) : null;
        this.lookup = new WorldLookup(this);
    }

    // the int seed of a world's terrain: its low 32 bits, as before; with the full seed a seed that does not fit in an int
    // gets its upper bits mixed in, and one that does (every text seed, a short number) keeps the terrain it had
    public static int terrainSeed(long seed, Preset preset) {
    	return preset.world().properties.fixes(FULL_SEED_VERSION) ? foldSeed(seed) : (int) seed;
    }

    public static int foldSeed(long seed) {
    	int low = (int) seed;
    	long high = seed >> 32;
    	// the upper half of an int seed is its sign
    	if(high == (low >> 31)) {
    		return low;
    	}
    	// SplitMix64 of the upper half, so that seeds near each other differ everywhere (and an upper half of 0 is not
    	// mixed to 0)
    	long mixed = high + 0x9E3779B97F4A7C15L;
    	mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
    	mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
    	mixed ^= mixed >>> 31;
    	return low ^ (int) (mixed ^ (mixed >>> 32));
    }

    public static GeneratorContext makeCached(Preset preset, HolderGetter<Noise> noiseLookup, int seed, int tileSize, int batchCount, boolean queue) {
    	int tileBorder = Math.min(2, Math.max(1, preset.filters().erosion.dropletLifetime / 16));
    	return new GeneratorContext(preset, noiseLookup, seed, tileSize, tileBorder, batchCount, true, queue);
    }

    public static GeneratorContext makeUncached(Preset preset, HolderGetter<Noise> noiseLookup, int seed, int tileSize, int tileBorder, int batchCount) {
    	return new GeneratorContext(preset, noiseLookup, seed, tileSize, tileBorder, batchCount, false, false);
    }
}
