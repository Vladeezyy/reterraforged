package raccoonman.reterraforged.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import raccoonman.reterraforged.RTFCommon;

public class ThreadPools {
	private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(0);
	private static final AtomicBoolean ENDING = new AtomicBoolean();
	private static final AtomicBoolean GENERATION_FAILED = new AtomicBoolean();
	private static final int EXIT_SECONDS = 60;
	public static final ThreadGroup RTF_GROUP = new ThreadGroup("rtf-worldgen");

	public static final ThreadPoolExecutor WORLD_GEN = new ThreadPoolExecutor(
		defaultWorkerThreads(),
		defaultWorkerThreads(),
		0L,
		TimeUnit.MILLISECONDS,
		new LinkedBlockingQueue<>(),
		ThreadPools::newWorkerThread
	);
	
	public static final Executor INVOKING = r -> {
		if(Thread.currentThread().getThreadGroup() == RTF_GROUP) {
			r.run();
		} else {
			WORLD_GEN.execute(r);
		}
	};

	private static Thread newWorkerThread(Runnable r) {
		Thread thread = new Thread(RTF_GROUP, r, "RTF worker #" + THREAD_COUNTER.getAndIncrement());
		thread.setDaemon(true);
		thread.setPriority(Thread.NORM_PRIORITY - 1);
		thread.setUncaughtExceptionHandler(ThreadPools::onWorkerDied);
		return thread;
	}

	// Out of memory while a worker completes a tile's future: the future is lost, the chunks waiting for it never finish, and
	// a server that stops waits for them before it saves, forever. It ends instead (a server host restarts it), after the
	// time a crash report and the crash of a client's game take
	private static void onWorkerDied(Thread thread, Throwable error) {
		if(!(error instanceof OutOfMemoryError)) {
			RTFCommon.LOGGER.error("Uncaught exception in {}", thread.getName(), error);
			return;
		}
		try {
			RTFCommon.LOGGER.error("ReTerraForged ran out of memory: give Java more memory (-Xmx) or lower tileSize in config/reterraforged/performance.conf. The game ends in {} seconds if it has not stopped by then", EXIT_SECONDS, error);
			endSoon();
		} catch(Throwable noMemoryLeft) {
			Runtime.getRuntime().halt(1);
		}
	}

	// A tile failed: the chunk system reports it and ends the game (Util.onThreadException, System.exit). That chunk never
	// finishes, so the exit waits for ever: for a server thread waiting for the chunk, or in stopServer for the chunk system's
	// work to end. Nothing is saved, SIGTERM is not answered and a host does not restart a process that is still there.
	// A minute after the exit started, it ends. Only after a failure: the exit of a healthy game may save a big world for longer
	public static void onGenerationFailed() {
		if(!GENERATION_FAILED.compareAndSet(false, true)) {
			return;
		}
		try {
			Runtime.getRuntime().addShutdownHook(new Thread(ThreadPools::endSoon, "RTF exit watchdog"));
		} catch(IllegalStateException exitStarted) {
			endSoon();
		}
	}

	// a daemon thread: an exit that finishes before that does not wait for it
	private static void endSoon() {
		if(!ENDING.compareAndSet(false, true)) {
			return;
		}
		Thread ending = new Thread(() -> {
			try {
				Thread.sleep(TimeUnit.SECONDS.toMillis(EXIT_SECONDS));
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			Runtime.getRuntime().halt(1);
		}, "RTF ending");
		ending.setDaemon(true);
		ending.start();
	}

	public static synchronized void configureWorldGenThreads(int threadCount) {
		int count = Math.max(1, Math.min(threadCount, availableProcessors() * 2));
		if(WORLD_GEN.getCorePoolSize() == count && WORLD_GEN.getMaximumPoolSize() == count) {
			return;
		}
		if(count > WORLD_GEN.getMaximumPoolSize()) {
			WORLD_GEN.setMaximumPoolSize(count);
			WORLD_GEN.setCorePoolSize(count);
		} else {
			WORLD_GEN.setCorePoolSize(count);
			WORLD_GEN.setMaximumPoolSize(count);
		}
	}
	
	public static int defaultWorkerThreads() {
		return Math.max(2, availableProcessors() - 2);
	}
	
	public static int availableProcessors() {
		return Math.max(2, Runtime.getRuntime().availableProcessors());
	}
}
