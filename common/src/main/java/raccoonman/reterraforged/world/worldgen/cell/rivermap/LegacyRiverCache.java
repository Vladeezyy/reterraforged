package raccoonman.reterraforged.world.worldgen.cell.rivermap;

import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;

public class LegacyRiverCache extends RiverCache {
	
    public LegacyRiverCache(RiverGenerator generator) {
        super(generator);
    }
    
    @Override
    public Rivermap getRivers(int x, int z) {
        long id = NoiseUtil.seed(x, z);
        return this.cache.computeIfAbsent(id, Entry::new).get(this.generator, x, z, id);
    }
}
