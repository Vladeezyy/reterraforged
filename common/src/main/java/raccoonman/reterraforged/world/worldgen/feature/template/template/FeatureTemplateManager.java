package raccoonman.reterraforged.world.worldgen.feature.template.template;

import raccoonman.reterraforged.RTFCommon;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableList;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public class FeatureTemplateManager {
	// the resource manager and the templates read from it, swapped as one: a paste still reading with the old manager
	// fills the old map, never the new one (clearing a shared map left old templates in it after a reload)
	private volatile Cache cache;
	
	public FeatureTemplateManager(ResourceManager resourceManager) {
		this.cache = new Cache(resourceManager);
	}
	
	public synchronized void onReload(ResourceManager resourceManager) {
		this.cache = new Cache(resourceManager);
	}

	// the server's resource manager is a new object after every reload (/reload, datapack changes): templates are read
	// again from the new one. Checked on use, so it does not depend on the name of the server's reload lambda. Called
	// for every template pasted from all generation threads: a plain read, the lock only when the manager changed
	public FeatureTemplateManager forResources(ResourceManager resourceManager) {
		if (this.cache.resourceManager != resourceManager) {
			synchronized (this) {
				if (this.cache.resourceManager != resourceManager) {
					this.cache = new Cache(resourceManager);
				}
			}
		}
		return this;
	}
	
	public FeatureTemplate load(Identifier location) {
		Cache cache = this.cache;
		// a plain read first: computeIfAbsent may lock its bin even when the template is there
		FeatureTemplate template = cache.templates.get(location);
		return template != null ? template : cache.templates.computeIfAbsent(location, (key) -> read(cache.resourceManager, key));
	}

	private record Cache(ResourceManager resourceManager, Map<Identifier, FeatureTemplate> templates) {

		Cache(ResourceManager resourceManager) {
			this(resourceManager, new ConcurrentHashMap<>());
		}
	}
	
	private static FeatureTemplate read(ResourceManager resourceManager, Identifier location) {
		return resourceManager.getResource(location).flatMap((resource) -> {
			try(InputStream stream = resource.open()) {
				return FeatureTemplate.load(stream);
			} catch (IOException e) {
				RTFCommon.LOGGER.error("Could not read the feature template {}", location, e);
				return Optional.empty();
			}
		}).orElseGet(() -> {
			// once per template (the manager keeps it): a missing file pastes nothing where the tree should stand
			RTFCommon.LOGGER.warn("Feature template {} not found or unreadable: it places nothing", location);
			return new FeatureTemplate(ImmutableList.of());
		});
	}
}
