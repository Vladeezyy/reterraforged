package raccoonman.reterraforged.mixin;

import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import raccoonman.reterraforged.data.worldgen.preset.PresetStructureSets;

// The structure page of the preset (PresetStructureSets). No event comes between the loading of the worldgen registries
// and their first use on either loader (the server's levels and the create world screen read them), so it is here.
@Mixin(RegistryDataLoader.class)
class MixinRegistryDataLoader {

	// the structure set files about to be read: those a preset datapack of an older version wrote are left out (the
	// preset's entries do the same now), so a mod removed since no longer stops the world from loading. A wrap, not a
	// @Redirect: NeoForge's conditions and Fabric's resource conditions stay
	@WrapOperation(
		method = "loadContentsFromManager",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/resources/FileToIdConverter;listMatchingResources(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;"
		),
		require = 1
	)
	private static Map<ResourceLocation, Resource> listMatchingResources(FileToIdConverter converter, ResourceManager manager, Operation<Map<ResourceLocation, Resource>> original, @Local(argsOnly = true) WritableRegistry<?> registry) {
		Map<ResourceLocation, Resource> resources = original.call(converter, manager);
		return registry.key().equals(Registries.STRUCTURE_SET) ? PresetStructureSets.structureSetFiles(resources, converter, manager) : resources;
	}

	// the registries are frozen and nothing has read a placement yet
	@Inject(
		method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;",
		at = @At("RETURN"),
		require = 1
	)
	private static void load(ResourceManager manager, RegistryAccess registries, List<RegistryDataLoader.RegistryData<?>> data, CallbackInfoReturnable<RegistryAccess.Frozen> callback) {
		PresetStructureSets.apply(callback.getReturnValue());
	}
}
