package raccoonman.reterraforged.concurrent.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import raccoonman.reterraforged.concurrent.cache.map.LongMap;
import raccoonman.reterraforged.concurrent.cache.map.StampedBoundLongMap;

@Deprecated
public class CacheManager {
	// weakly: the manager must not keep caches alive (see Cache.Poller)
	private static final Set<Cache<?>> CACHES = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
	
    public static <V extends ExpiringEntry> Cache<V> createCache(long expireTime, long pollInterval, TimeUnit unit) {
    	return createCache(256, expireTime, pollInterval, unit);
    }
	
	public static <V extends ExpiringEntry> Cache<V> createCache(int capacity, long expireTime, long pollInterval, TimeUnit unit) {
		return createCache(capacity, expireTime, pollInterval, unit, StampedBoundLongMap::new);
	}
	
	public static <V extends ExpiringEntry> Cache<V> createCache(int capacity, long expireTime, long pollInterval, TimeUnit unit, IntFunction<LongMap<V>> mapFunc) {
		Cache<V> cache = new Cache<>(capacity, expireTime, pollInterval, unit, mapFunc);
		CACHES.add(cache);
		return cache;
	}
	
	public static void clear() throws Exception {
		List<Cache<?>> caches;
		synchronized(CACHES) {
			caches = new ArrayList<>(CACHES);
			CACHES.clear();
		}
		for(Cache<?> cache : caches) {
			cache.close();
		}
	}
}