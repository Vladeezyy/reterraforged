package raccoonman.reterraforged.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class LatestTaskTest {
	// the worker and the render thread by hand: each step runs when the test says so
	private final Queue<Runnable> worker = new ArrayDeque<>();
	private final Queue<Runnable> render = new ArrayDeque<>();
	private final LatestTask<String> task = new LatestTask<>(this.worker::add, this.render::add);
	private final List<String> done = new ArrayList<>();
	private final List<Exception> errors = new ArrayList<>();
	private final List<String> discarded = new ArrayList<>();

	private void submit(String result) {
		this.task.submit(() -> result, this.discarded::add, (value, error) -> {
			if (error != null) {
				this.errors.add(error);
			} else {
				this.done.add(value);
			}
		});
	}

	private static void runAll(Queue<Runnable> queue) {
		Runnable next;
		while ((next = queue.poll()) != null) {
			next.run();
		}
	}

	@Test
	void theResultReachesTheCallbackOnTheRenderThreadAndTheTaskIsNoLongerBusy() {
		this.submit("a");
		assertTrue(this.task.isBusy());
		runAll(this.worker);
		assertTrue(this.done.isEmpty(), "not on the worker");
		assertTrue(this.task.isBusy(), "busy until the callback ran");
		runAll(this.render);
		assertEquals(List.of("a"), this.done);
		assertFalse(this.task.isBusy());
	}

	@Test
	void aTaskSupersededBeforeItStartedDoesNotRun() {
		List<String> ran = new ArrayList<>();
		this.task.submit(() -> {
			ran.add("old");
			return "old";
		}, this.discarded::add, (value, error) -> this.done.add(value));
		this.submit("new");
		runAll(this.worker);
		runAll(this.render);
		assertEquals(List.of(), ran);
		assertEquals(List.of("new"), this.done);
		assertEquals(List.of(), this.discarded);
	}

	@Test
	void anOldResultThatEndsAfterANewerSubmitIsDiscardedNotDelivered() {
		this.submit("old");
		this.worker.poll().run();
		// the old one waits for the render thread when the new one comes
		this.submit("new");
		runAll(this.worker);
		runAll(this.render);
		assertEquals(List.of("new"), this.done);
		assertEquals(List.of("old"), this.discarded);
	}

	@Test
	void resultsComeInOrderAndOnlyTheNewestCounts() {
		for (int i = 0; i < 5; i++) {
			this.submit("r" + i);
			if (i % 2 == 0) {
				this.worker.poll().run();
			}
		}
		runAll(this.worker);
		runAll(this.render);
		// r0 had run (its result is thrown away), the others were superseded before they started
		assertEquals(List.of("r4"), this.done);
		assertEquals(List.of("r0"), this.discarded);
		assertFalse(this.task.isBusy());
	}

	@Test
	void anErrorReachesTheCallbackAndTheTaskIsNotLeftBusy() {
		this.task.submit(() -> {
			throw new IOException("disk full");
		}, this.discarded::add, (value, error) -> {
			assertNull(value);
			this.errors.add(error);
		});
		runAll(this.worker);
		runAll(this.render);
		assertEquals(1, this.errors.size());
		assertInstanceOf(IOException.class, this.errors.get(0));
		assertFalse(this.task.isBusy());
	}

	@Test
	void aBrokenClassIsAnErrorTooNotALostCallback() {
		this.task.submit(() -> {
			throw new NoClassDefFoundError("some/Mod");
		}, this.discarded::add, (value, error) -> this.errors.add(error));
		runAll(this.worker);
		runAll(this.render);
		assertEquals(1, this.errors.size());
		assertFalse(this.task.isBusy());
	}

	@Test
	void cancelDiscardsWhatIsRunningAndNothingIsDelivered() {
		this.submit("a");
		this.worker.poll().run();
		this.task.cancel();
		assertFalse(this.task.isBusy());
		runAll(this.render);
		assertEquals(List.of(), this.done);
		assertEquals(List.of("a"), this.discarded);
	}

	@Test
	void aSubmitAfterCancelWorks() {
		this.submit("a");
		this.task.cancel();
		this.submit("b");
		runAll(this.worker);
		runAll(this.render);
		assertEquals(List.of("b"), this.done);
	}

	// the shared worker thread: tasks of two screens one after another, never at once
	@Test
	void theSharedWorkerRunsOneTaskAtATime() throws InterruptedException {
		ExecutorService callbacks = Executors.newSingleThreadExecutor();
		try {
			LatestTask<String> first = new LatestTask<>(callbacks);
			LatestTask<String> second = new LatestTask<>(callbacks);
			int[] running = { 0 };
			int[] most = { 0 };
			CountDownLatch finished = new CountDownLatch(2);
			LatestTask.Work<String> work = () -> {
				synchronized (running) {
					most[0] = Math.max(most[0], ++running[0]);
				}
				Thread.sleep(50);
				synchronized (running) {
					running[0]--;
				}
				return "x";
			};
			first.submit(work, (value) -> {
			}, (value, error) -> finished.countDown());
			second.submit(work, (value) -> {
			}, (value, error) -> finished.countDown());
			assertTrue(finished.await(10, TimeUnit.SECONDS));
			assertEquals(1, most[0]);
		} finally {
			callbacks.shutdownNow();
		}
	}
}
