package raccoonman.reterraforged.client.gui.screen.presetconfig;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class PresetMapsTest {
	// the background by hand: each picture is made when the test says so
	private final Queue<Runnable> worker = new ArrayDeque<>();
	private final List<String> drawn = new ArrayList<>();
	private final PresetMaps<String> maps = new PresetMaps<>(this.worker::add, (input, job) -> {
		job.check();
		this.drawn.add(input);
		return new int[] { input.length() };
	});

	private void runAll() {
		while (!this.worker.isEmpty()) {
			this.worker.poll().run();
		}
	}

	@Test
	void aPictureIsMadeOnceAndKept() {
		assertNull(this.maps.get("a", "aa"));
		assertTrue(this.maps.isBusy());
		// asked again while it is made: not made twice
		assertNull(this.maps.get("a", "aa"));
		assertEquals(1, this.worker.size());
		this.runAll();
		assertFalse(this.maps.isBusy());
		assertArrayEquals(new int[] { 2 }, this.maps.get("a", "aa"));
		assertArrayEquals(new int[] { 2 }, this.maps.get("a", "aa"));
		assertEquals(List.of("aa"), this.drawn);
	}

	@Test
	void anotherKeyStopsThePictureBefore() {
		this.maps.get("a", "aa");
		this.maps.get("b", "bbb");
		this.runAll();
		// the first was cancelled before it started
		assertEquals(List.of("bbb"), this.drawn);
		assertArrayEquals(new int[] { 3 }, this.maps.get("b", "bbb"));
		// and is made when asked for again
		assertNull(this.maps.get("a", "aa"));
		this.runAll();
		assertArrayEquals(new int[] { 2 }, this.maps.get("a", "aa"));
	}

	@Test
	void cancelStopsWaitingForTheTile() throws InterruptedException {
		// a tile that never comes: only the cancel ends the wait
		CompletableFuture<int[]> tile = new CompletableFuture<>();
		CountDownLatch started = new CountDownLatch(1);
		Thread[] thread = new Thread[1];
		PresetMaps<String> waiting = new PresetMaps<>((runnable) -> {
			thread[0] = new Thread(runnable);
			thread[0].start();
		}, (input, job) -> {
			started.countDown();
			return job.join(tile);
		});
		assertNull(waiting.get("a", "a"));
		assertTrue(started.await(5, TimeUnit.SECONDS));
		waiting.cancel();
		thread[0].join(5000);
		assertFalse(thread[0].isAlive());
		assertFalse(waiting.isBusy());
		// stopped, not broken: made again when it is picked again
		assertFalse(waiting.failed("a"));
	}

	@Test
	void aFailedPictureIsNotTriedOnEveryFrame() {
		PresetMaps<String> failing = new PresetMaps<>(this.worker::add, (input, job) -> {
			throw new IllegalStateException("broken preset");
		});
		assertNull(failing.get("a", "a"));
		this.runAll();
		assertTrue(failing.failed("a"));
		assertNull(failing.get("a", "a"));
		assertTrue(this.worker.isEmpty());
	}

	@Test
	void aRefusingPoolIsAFailureNotAnError() {
		PresetMaps<String> refused = new PresetMaps<>((runnable) -> {
			throw new java.util.concurrent.RejectedExecutionException();
		}, (input, job) -> new int[0]);
		assertNull(refused.get("a", "a"));
		assertTrue(refused.failed("a"));
		assertFalse(refused.isBusy());
	}
}
