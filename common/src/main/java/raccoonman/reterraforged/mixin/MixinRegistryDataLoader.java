package raccoonman.reterraforged.mixin;

import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.packs.resources.ResourceManager;
import raccoonman.reterraforged.data.worldgen.preset.PresetStructureSets;

// The structure page of the preset (PresetStructureSets). No event comes between the loading of the worldgen registries
// and their first use on either loader (the server's levels and the create world screen read them), so it is here.
// The structure set files about to be read are in MixinResourceManagerRegistryLoadTask (26.1 loads each registry in a
// task of its own, on the executor's threads).
@Mixin(RegistryDataLoader.class)
class MixinRegistryDataLoader {

	// the registries are frozen and nothing has read a placement yet: the future the callers get completes after the
	// preset's entries are on the sets. NeoForge has a second load with the pending tags (WorldLoader calls it, the
	// first one hands over to it), Fabric and vanilla only the first; applying twice changes nothing
	@ModifyReturnValue(
		method = {
			"load(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/List;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;",
			"load(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/List;Ljava/util/concurrent/Executor;Ljava/util/List;)Ljava/util/concurrent/CompletableFuture;"
		},
		at = @At("RETURN"),
		require = 1
	)
	private static CompletableFuture<RegistryAccess.Frozen> load(CompletableFuture<RegistryAccess.Frozen> original, @Local(argsOnly = true) ResourceManager manager) {
		return original.thenApply((registries) -> {
			PresetStructureSets.apply(registries, manager);
			return registries;
		});
	}
}
