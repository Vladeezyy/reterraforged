package raccoonman.reterraforged.concurrent.cache;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.function.LongFunction;

import raccoonman.reterraforged.concurrent.cache.map.LongMap;

public class Cache<V extends ExpiringEntry> implements AutoCloseable {
	public static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor((r) -> {
		Thread thread = new Thread(r);
		thread.setName("CacheScheduler");
		return thread;
	});
	
    private LongMap<V> map;
    private long lifetimeMS;
    private volatile long timeout;
    private ScheduledFuture<?> poll;
    
    public Cache(int capacity, long expireTime, long pollInterval, TimeUnit unit, IntFunction<LongMap<V>> mapFunc) {
        this.timeout = 0L;
        this.map = mapFunc.apply(capacity);
        this.lifetimeMS = unit.toMillis(expireTime);
        
        long intervalMillis = unit.toMillis(pollInterval);
        Poller poller = new Poller(this);
        this.poll = SCHEDULER.scheduleAtFixedRate(poller, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
        poller.future = this.poll;
    }

    // The scheduler only holds the cache weakly: a cache nobody uses any more (a replaced preview, a world that was
    // left) is collected with everything in it and its polling stops. Holding it strongly kept every cache ever made,
    // with its tiles and river maps, until the game was closed.
    private static final class Poller implements Runnable {
        private final WeakReference<Cache<?>> cache;
        private volatile ScheduledFuture<?> future;

        Poller(Cache<?> cache) {
            this.cache = new WeakReference<>(cache);
        }

        @Override
        public void run() {
            Cache<?> cache = this.cache.get();
            if (cache != null) {
                cache.poll();
            } else if (this.future != null) {
                this.future.cancel(false);
            }
        }
    }
    
    public void remove(long key) {
        this.map.remove(key, ExpiringEntry::close);
    }
    
    public V get(long key) {
        return this.map.get(key);
    }
    
    public V computeIfAbsent(long key, LongFunction<V> func) {
        return this.map.computeIfAbsent(key, func);
    }
    
    public void poll() {
        this.timeout = System.currentTimeMillis() - this.lifetimeMS;
        this.map.removeIf((entry) -> entry.getTimestamp() < this.timeout);
    }

	// stops the polling and drops the entries (not closing them: a generation thread may still read a tile)
	@Override
	public void close() throws Exception {
		this.poll.cancel(false);
		this.map.clear();
	}
}
