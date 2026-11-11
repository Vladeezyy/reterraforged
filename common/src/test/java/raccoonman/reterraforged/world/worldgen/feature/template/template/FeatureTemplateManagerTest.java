package raccoonman.reterraforged.world.worldgen.feature.template.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.resources.ResourceManager;

/** Templates are read once per resource manager, and a reload never keeps one read with the old manager */
class FeatureTemplateManagerTest {
	private static final ResourceLocation TREE = ResourceLocation.fromNamespaceAndPath("reterraforged", "trees/oak/small/oak_small_1.nbt");

	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	// a resource manager without the file (each read gives a new empty template), running hook on every read
	private static ResourceManager resources(AtomicInteger reads, Runnable hook) {
		return (ResourceManager) Proxy.newProxyInstance(ResourceManager.class.getClassLoader(), new Class<?>[] { ResourceManager.class }, (proxy, method, args) -> {
			switch (method.getName()) {
				case "getResource":
					reads.incrementAndGet();
					hook.run();
					return Optional.empty();
				case "hashCode":
					return System.identityHashCode(proxy);
				case "equals":
					return proxy == args[0];
				case "toString":
					return "resources";
				default:
					throw new UnsupportedOperationException(method.getName());
			}
		});
	}

	@Test
	void readsOncePerManager() {
		AtomicInteger readsA = new AtomicInteger(), readsB = new AtomicInteger();
		ResourceManager a = resources(readsA, () -> {}), b = resources(readsB, () -> {});
		FeatureTemplateManager manager = new FeatureTemplateManager(a);
		FeatureTemplate first = manager.forResources(a).load(TREE);
		assertSame(first, manager.forResources(a).load(TREE));
		assertEquals(1, readsA.get());
		FeatureTemplate reloaded = manager.forResources(b).load(TREE);
		assertNotSame(first, reloaded);
		assertSame(reloaded, manager.forResources(b).load(TREE));
		assertEquals(1, readsA.get());
		assertEquals(1, readsB.get());
	}

	@Test
	void readWithTheOldManagerIsNotKeptAfterAReload() throws Exception {
		CountDownLatch reading = new CountDownLatch(1), reloaded = new CountDownLatch(1);
		AtomicInteger readsA = new AtomicInteger(), readsB = new AtomicInteger();
		ResourceManager a = resources(readsA, () -> {
			reading.countDown();
			try {
				reloaded.await(10, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		ResourceManager b = resources(readsB, () -> {});
		FeatureTemplateManager manager = new FeatureTemplateManager(a);
		// a generation thread reads the template with the old manager while the server reloads
		CompletableFuture<FeatureTemplate> old = CompletableFuture.supplyAsync(() -> manager.forResources(a).load(TREE));
		reading.await(10, TimeUnit.SECONDS);
		manager.forResources(b);
		reloaded.countDown();
		FeatureTemplate stale = old.get(10, TimeUnit.SECONDS);
		FeatureTemplate fresh = manager.forResources(b).load(TREE);
		assertNotSame(stale, fresh);
		assertEquals(1, readsB.get());
	}
}
