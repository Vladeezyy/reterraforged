package raccoonman.reterraforged.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import raccoonman.reterraforged.RTFCommon;

/**
 * Slow work of a screen (a preset datapack export) off the render thread, the newest task only. A task submitted again
 * supersedes the one before it: a superseded task that has not started does not run, and the result of one that was
 * already running goes to its discard, never to its callback, so an old export that ends late does not replace a newer
 * one. The tasks of all screens run one after another on one thread: two exports never write the same file at once, and
 * the worldgen threads (ThreadPools.WORLD_GEN) are not taken from a world that is loading.
 */
public final class LatestTask<T> {
	private static final AtomicInteger THREADS = new AtomicInteger();
	// one daemon thread, gone after a while without work: nothing keeps the game from closing
	private static final ThreadPoolExecutor SHARED = new ThreadPoolExecutor(1, 1, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), (runnable) -> {
		Thread thread = new Thread(runnable, "RTF export #" + THREADS.getAndIncrement());
		thread.setDaemon(true);
		return thread;
	});
	static {
		SHARED.allowCoreThreadTimeOut(true);
	}

	private final Executor worker;
	private final Executor callbacks;
	private final AtomicInteger latest = new AtomicInteger();
	// the newest task that has ended (its callback ran) or was cancelled
	private volatile int settled;

	// callbacks: where results go, the render thread (Minecraft is an Executor)
	public LatestTask(Executor callbacks) {
		this(SHARED, callbacks);
	}

	// the worker too: for the tests
	public LatestTask(Executor worker, Executor callbacks) {
		this.worker = worker;
		this.callbacks = callbacks;
	}

	// done gets the result or the error (never both) on the callbacks executor, unless a newer task or cancel came first;
	// discard gets a superseded task's result (to delete its file)
	public void submit(Work<T> work, Consumer<? super T> discard, BiConsumer<? super T, ? super Exception> done) {
		int id = this.latest.incrementAndGet();
		this.worker.execute(() -> {
			if (!this.isLatest(id)) {
				return;
			}
			T result = null;
			Exception error = null;
			try {
				result = work.run();
			} catch (Exception e) {
				error = e;
			} catch (LinkageError | AssertionError e) {
				// another mod's broken class in the data generation: shown, and the screen is not left waiting forever
				error = new ExecutionException(e.toString(), e);
			}
			if (!this.isLatest(id)) {
				discard(discard, result);
				return;
			}
			T finalResult = result;
			Exception finalError = error;
			this.callbacks.execute(() -> {
				// a newer task or cancel may have come while this waited for the render thread
				if (!this.isLatest(id)) {
					discard(discard, finalResult);
					return;
				}
				this.settled = id;
				done.accept(finalResult, finalError);
			});
		});
	}

	// nothing of the tasks so far reaches its callback (the screen was left)
	public void cancel() {
		this.settled = this.latest.incrementAndGet();
	}

	// a task whose callback has not run yet
	public boolean isBusy() {
		return this.settled != this.latest.get();
	}

	private boolean isLatest(int id) {
		return this.latest.get() == id;
	}

	private static <T> void discard(Consumer<? super T> discard, T result) {
		if (result == null) {
			return;
		}
		try {
			discard.accept(result);
		} catch (RuntimeException e) {
			RTFCommon.LOGGER.warn("Could not throw away the result of an old export {}", result, e);
		}
	}

	public interface Work<T> {
		T run() throws Exception;
	}
}
