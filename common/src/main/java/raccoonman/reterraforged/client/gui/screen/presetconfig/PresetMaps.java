package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.jetbrains.annotations.Nullable;

import raccoonman.reterraforged.RTFCommon;

/**
 * The small maps of the preset list, made off the render thread. Only the newest request is worked on: a request for
 * another key stops the one before between its steps (a quick run down the list does not draw every preset in turn),
 * and each finished picture is kept by key for the life of the preset screen. No textures and no widgets here: the
 * render thread asks with get, and whoever draws turns the pixels into a texture.
 */
public final class PresetMaps<I> {
	// the pictures of a screen; a player's long list does not keep them all (64 KB each)
	private static final int KEEP = 48;

	private final Executor executor;
	private final Drawer<I> drawer;
	private final Map<String, int[]> pictures = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, int[]> eldest) {
			return this.size() > KEEP;
		}
	});
	// keys whose picture failed: not tried again on every frame
	private final Set<String> failed = Collections.synchronizedSet(new HashSet<>());
	@Nullable
	private String requested;
	@Nullable
	private Job job;

	public PresetMaps(Executor executor, Drawer<I> drawer) {
		this.executor = executor;
		this.drawer = drawer;
	}

	/** The picture of key, or null while it is made; a key not seen before stops the one asked for before it */
	@Nullable
	public int[] get(String key, I input) {
		int[] picture = this.pictures.get(key);
		if (picture != null || this.failed.contains(key)) {
			return picture;
		}
		if (key.equals(this.requested) && this.job != null && !this.job.done) {
			return null;
		}
		this.cancel();
		Job job = new Job();
		this.job = job;
		this.requested = key;
		try {
			this.executor.execute(() -> this.run(job, key, input));
		} catch (RuntimeException e) {
			// a pool that refuses work (the game closing): no picture, nothing thrown at the render thread
			job.done = true;
			this.failed.add(key);
		}
		return null;
	}

	public boolean failed(String key) {
		return this.failed.contains(key);
	}

	/** Whether a picture is on its way (the GUI test waits for it) */
	public boolean isBusy() {
		Job job = this.job;
		return job != null && !job.done;
	}

	/** Stops the picture being made (the screen was left); the finished ones stay */
	public void cancel() {
		if (this.job != null) {
			this.job.cancel();
			this.job = null;
		}
		this.requested = null;
	}

	private void run(Job job, String key, I input) {
		try {
			job.check();
			int[] picture = this.drawer.draw(input, job);
			// a picture drawn to the end is right whether or not it is still wanted
			this.pictures.put(key, picture);
		} catch (CancellationException e) {
			// another preset was picked: asked again when it is picked again
		} catch (RuntimeException | LinkageError e) {
			this.failed.add(key);
			RTFCommon.LOGGER.error("Could not draw the map of a preset", e);
		} finally {
			job.done = true;
		}
	}

	/** Makes a picture; stops (CancellationException) when its job says so */
	public interface Drawer<I> {
		int[] draw(I input, Job job);
	}

	/** One picture being made: a cancel stops it between its steps and stops waiting for its tile */
	public static final class Job {
		private volatile boolean cancelled;
		private volatile boolean done;
		@Nullable
		private volatile CompletableFuture<?> waiting;

		public void check() {
			if (this.cancelled) {
				throw new CancellationException("preset map no longer wanted");
			}
		}

		public <T> T join(CompletableFuture<T> future) {
			this.waiting = future;
			// after publishing it: a cancel in between either sees the future or is seen here
			this.check();
			return future.join();
		}

		void cancel() {
			this.cancelled = true;
			CompletableFuture<?> waiting = this.waiting;
			if (waiting != null) {
				waiting.cancel(false);
			}
		}
	}
}
