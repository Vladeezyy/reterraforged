package raccoonman.reterraforged.server;

import java.util.Locale;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;

/**
 * "rtf bench [size]" — with -Drtf.selftest=true (tools/benchmark.sh). Generates a square of size x size chunks
 * (default 32) far from the spawn, where nothing is generated yet, to the full status, and logs the time, chunks per
 * second and the heap in use. A smaller square elsewhere is generated first, so the JIT and the generator's caches are
 * warm. The chunks are asked for from a thread of its own, as a player moving through the world asks for them: the
 * server keeps ticking meanwhile, so tick times and CPU use (spark) can be measured during the generation. The same
 * command in a vanilla world (no preset datapack) gives the baseline.
 */
public final class RTFBench {
	// chunk coordinates: 48 000 blocks out, away from what the self test and the structure test generated
	private static final int ORIGIN = 3000;

	private RTFBench() {
	}

	public static String start(MinecraftServer server, int size) {
		ServerLevel level = server.overworld();
		boolean rtf = (Object) level.getChunkSource().randomState() instanceof RTFRandomState state && state.generatorContext() != null;
		Thread thread = new Thread(() -> {
			generate(level, ORIGIN - 64, ORIGIN - 64, 6);
			RTFCommon.LOGGER.info("[RTF bench] warmed up, measuring");
			Runtime runtime = Runtime.getRuntime();
			long start = System.nanoTime();
			generate(level, ORIGIN, ORIGIN, size);
			double seconds = (System.nanoTime() - start) / 1.0E9;
			int chunks = size * size;
			long heap = (runtime.totalMemory() - runtime.freeMemory()) >> 20;
			RTFCommon.LOGGER.info(String.format(Locale.ROOT, "[RTF bench] %s: %d chunks in %.1f s, %.1f chunks/s, %.1f ms per chunk, heap in use %d MB of %d MB, %d cores",
				rtf ? "ReTerraForged" : "vanilla", chunks, seconds, chunks / seconds, seconds * 1000.0 / chunks, heap, runtime.maxMemory() >> 20, runtime.availableProcessors()));
		}, "RTF bench");
		thread.setDaemon(true);
		thread.start();
		return "[RTF] bench started: " + size + " x " + size + " chunks (" + (rtf ? "ReTerraForged" : "vanilla") + ")";
	}

	// off the server thread, every getChunk is handed to the server thread and waited for; one requesting thread per
	// core keeps the generator busy, as fast flight or a pregenerator does
	private static void generate(ServerLevel level, int minX, int minZ, int size) {
		int threads = Runtime.getRuntime().availableProcessors();
		java.util.concurrent.atomic.AtomicInteger next = new java.util.concurrent.atomic.AtomicInteger();
		Thread[] workers = new Thread[threads];
		for (int t = 0; t < threads; t++) {
			workers[t] = new Thread(() -> {
				for (int i = next.getAndIncrement(); i < size * size; i = next.getAndIncrement()) {
					level.getChunk(minX + i / size, minZ + i % size, ChunkStatus.FULL, true);
				}
			}, "RTF bench " + t);
			workers[t].setDaemon(true);
			workers[t].start();
		}
		for (Thread worker : workers) {
			try {
				worker.join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}
}
