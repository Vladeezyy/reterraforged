package raccoonman.reterraforged.concurrent.cache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;

/**
 * Cache entry that wraps a CompletableFuture and exposes the same surface the
 * rest of ReTerraForged expects (get / isDone / timestamp).
 *
 * <p>Historical note: this used to extend {@code LazyCallable}, which held a
 * StampedLock write lock across {@code ForkJoinTask.join()}. Under the vanilla
 * chunk scheduler that runs world-gen on {@code Util.backgroundExecutor()} (a
 * ForkJoinPool), the join triggered {@code managedBlock} compensation, spawning
 * fresh workers that hit the same cache entry and parked on the StampedLock
 * read path — a full ForkJoinPool-wide deadlock (observed: 80+ Worker-Main
 * threads on the same {@code StampedLock.acquireRead}).
 *
 * <p>The new implementation delegates directly to a {@code CompletableFuture},
 * which is safe under concurrent {@code join()} and cannot deadlock on itself.
 * The upstream {@code TileCache.computeIfAbsent} already guarantees a single
 * {@code CacheEntry} per key, so no extra single-flight lock is needed here.
 */
public class CacheEntry<T> implements ExpiringEntry {
	private final CompletableFuture<T> future;
	private volatile long timestamp;
	private volatile long earlier;

	public CacheEntry(Future<T> task) {
		this.future = toCompletableFuture(task);
		this.timestamp = System.currentTimeMillis();
	}

	public T get() {
		this.timestamp = System.currentTimeMillis();
		try {
			return this.future.join();
		} catch (CompletionException e) {
			Throwable c = e.getCause();
			if (c instanceof RuntimeException r) throw r;
			// not the Error itself: it ended the chunk system's worker with the chunk's future unfinished, and the server
			// waited for that chunk for ever (no crash report, no stop). As an exception the chunk fails and is reported
			throw new IllegalStateException("A ReTerraForged tile could not be generated: " + c, c);
		}
	}

	public boolean isDone() {
		return this.future.isDone();
	}

	@Override
	public long getTimestamp() {
		return this.timestamp - this.earlier;
	}

	// from now on the entry counts as this much older than it is: it leaves the cache that much sooner after its last use
	public void expireSooner(long milliseconds) {
		this.earlier = milliseconds;
	}

	public static <T> CacheEntry<T> supply(Future<T> task) {
		return new CacheEntry<>(task);
	}

	private static <T> CompletableFuture<T> toCompletableFuture(Future<T> task) {
		if (task instanceof CompletableFuture<T> cf) {
			return cf;
		}
		if (task instanceof CompletionStage<?> stage) {
			@SuppressWarnings("unchecked")
			CompletionStage<T> typed = (CompletionStage<T>) stage;
			return typed.toCompletableFuture();
		}
		if (task instanceof ForkJoinTask<T> fjt) {
			return CompletableFuture.supplyAsync(fjt::join);
		}
		return CompletableFuture.supplyAsync(() -> {
			try {
				return task.get();
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
		});
	}
}
