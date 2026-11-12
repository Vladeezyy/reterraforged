package raccoonman.reterraforged.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Lifecycle;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceManagerRegistryLoadTask;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import raccoonman.reterraforged.data.worldgen.preset.PresetStructureSets;

// The structure page of the preset (PresetStructureSets), the files' half; the loaded sets' half is in
// MixinRegistryDataLoader. Up to 1.21.11 this was RegistryDataLoader.loadContentsFromManager.
@Mixin(ResourceManagerRegistryLoadTask.class)
abstract class MixinResourceManagerRegistryLoadTask<T> extends RegistryLoadTask<T> {

	private MixinResourceManagerRegistryLoadTask(RegistryDataLoader.RegistryData<T> data, Lifecycle lifecycle, Map<ResourceKey<?>, Exception> loadingErrors) {
		super(data, lifecycle, loadingErrors);
	}

	// the structure set files about to be read: those a preset datapack of an older version wrote are left out (the
	// preset's entries do the same now), so a mod removed since no longer stops the world from loading. A wrap, not a
	// @Redirect: NeoForge's conditions and Fabric's resource conditions stay. The lambda of load() that lists the
	// registry's files (the same name in vanilla's and in NeoForge's class)
	@WrapOperation(
		method = "lambda$load$0",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/resources/FileToIdConverter;listMatchingResources(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;"
		),
		require = 1
	)
	private Map<Identifier, Resource> listMatchingResources(FileToIdConverter converter, ResourceManager manager, Operation<Map<Identifier, Resource>> original) {
		Map<Identifier, Resource> resources = original.call(converter, manager);
		return this.registryKey().equals(Registries.STRUCTURE_SET) ? PresetStructureSets.structureSetFiles(resources, converter, manager) : resources;
	}
}
