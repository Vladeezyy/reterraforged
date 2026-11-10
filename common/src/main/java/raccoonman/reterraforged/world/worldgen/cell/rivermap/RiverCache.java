package raccoonman.reterraforged.world.worldgen.cell.rivermap;

import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import raccoonman.reterraforged.concurrent.cache.Cache;
import raccoonman.reterraforged.concurrent.cache.ExpiringEntry;
import raccoonman.reterraforged.concurrent.cache.map.StampedLongMap;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

public class RiverCache {
    protected final RiverGenerator generator;
    protected final Cache<Entry> cache;
    
    public RiverCache(RiverGenerator generator) {
        this.cache = new Cache<>(32, 5L, 1L, TimeUnit.MINUTES, StampedLongMap::unbounded);
        this.generator = generator;
    }
    
    public Rivermap getRivers(int x, int z) {
        long id = PosUtil.pack(x, z);
        return this.cache.computeIfAbsent(id, Entry::new).get(this.generator, PosUtil.unpackLeft(id), PosUtil.unpackRight(id), id);
    }

    // Only this empty entry is made under the map's write lock, its river map after it: made under the lock, every thread
    // that needed any river map (every tile, every single cell asked for) waited until that one was done. And it lives
    // for as long as it is used: it expired 5 minutes after it was made, used or not, and was made again under that lock
    // (with an infinite continent one map is the whole world's)
    protected static final class Entry implements ExpiringEntry {
        // a second: the time is not written by every thread on every call
        private static final long TOUCH_MS = 1000L;

        @Nullable
        private volatile Rivermap rivermap;
        private volatile long timestamp;

        Entry(long id) {
            this.timestamp = System.currentTimeMillis();
        }

        Rivermap get(RiverGenerator generator, int x, int z, long id) {
            long now = System.currentTimeMillis();
            if (now - this.timestamp > TOUCH_MS) {
                this.timestamp = now;
            }
            Rivermap rivermap = this.rivermap;
            if (rivermap == null) {
                // the threads that need this map wait for it, the others do not
                synchronized (this) {
                    rivermap = this.rivermap;
                    if (rivermap == null) {
                        this.rivermap = rivermap = generator.generateRivers(x, z, id);
                    }
                }
            }
            return rivermap;
        }

        @Override
        public long getTimestamp() {
            return this.timestamp;
        }
    }
}
