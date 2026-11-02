package raccoonman.reterraforged.world.worldgen.feature.template.template;

import raccoonman.reterraforged.RTFCommon;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableList;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class FeatureTemplateManager {
	private ResourceManager resourceManager;
	private final Map<ResourceLocation, FeatureTemplate> cache;
	
	public FeatureTemplateManager(ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
		this.cache = new ConcurrentHashMap<>();
	}
	
	public void onReload(ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
		this.cache.clear();
	}

	// the server's resource manager is a new object after every reload (/reload, datapack changes): templates are read
	// again from the new one. Checked on use, so it does not depend on the name of the server's reload lambda.
	public synchronized FeatureTemplateManager forResources(ResourceManager resourceManager) {
		if (resourceManager != this.resourceManager) {
			this.onReload(resourceManager);
		}
		return this;
	}
	
	public FeatureTemplate load(ResourceLocation location) {
		return this.cache.computeIfAbsent(location, this::read);
	}
	
	private FeatureTemplate read(ResourceLocation location) {
		return this.resourceManager.getResource(location).flatMap((resource) -> {
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
