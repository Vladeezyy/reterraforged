package raccoonman.reterraforged.concurrent.cache;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;

import org.jetbrains.annotations.Nullable;

import raccoonman.reterraforged.concurrent.ThreadPools;

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
	// a second: the time is not written by every thread on every call (RiverCache.Entry)
	private static final long TOUCH_MS = 1000L;
	private final CompletableFuture<T> future;
	private volatile long timestamp;
	private volatile long earlier;

	public CacheEntry(Future<T> task) {
		this.future = toCompletableFuture(task);
		this.timestamp = System.currentTimeMillis();
	}

	public T get() {
		this.touch();
		try {
			return this.future.join();
		} catch (CompletionException e) {
			Throwable c = e.getCause();
			// a world that stopped (TileCache.closeAll): not a failure that ends the game
			if (c instanceof CancellationException cancelled) throw cancelled;
			ThreadPools.onGenerationFailed();
			if (c instanceof RuntimeException r) throw r;
			// not the Error itself: it ended the chunk system's worker with the chunk's future unfinished, and the server
			// waited for that chunk for ever (no crash report, no stop). As an exception the chunk fails and is reported
			throw new IllegalStateException("A ReTerraForged tile could not be generated: " + c, c);
		}
	}

	/** The value if it is done and did not fail, else null: neither waits nor throws */
	@Nullable
	public T getNow() {
		if (!this.future.isDone() || this.future.isCompletedExceptionally()) {
			return null;
		}
		this.touch();
		return this.future.join();
	}

	public boolean isDone() {
		return this.future.isDone();
	}

	public void touch() {
		long now = System.currentTimeMillis();
		if (now - this.timestamp > TOUCH_MS) {
			this.timestamp = now;
		}
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

	/** Not started: made under the map's lock, its work after it (complete) */
	public static <T> CacheEntry<T> pending() {
		return new CacheEntry<>(new CompletableFuture<>());
	}

	// the stage's result, or how it failed; a stage that could not even start fails the entry the same way
	public void complete(CompletionStage<T> stage) {
		stage.whenComplete((value, error) -> {
			if (error != null) {
				this.future.completeExceptionally(error instanceof CompletionException && error.getCause() != null ? error.getCause() : error);
			} else {
				this.future.complete(value);
			}
		});
	}

	public void fail(Throwable error) {
		this.future.completeExceptionally(error);
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
