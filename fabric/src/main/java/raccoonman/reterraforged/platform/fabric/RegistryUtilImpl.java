package raccoonman.reterraforged.platform.fabric;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import raccoonman.reterraforged.RTFCommon;

import java.util.List;

public class RegistryUtilImpl {

	public static <T> void register(Registry<T> registry, String name, T value) {
		Registry.register(registry, RTFCommon.location(name), value);
	}

	public static <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key) {
		return FabricRegistryBuilder.create(key).buildAndRegister();
	}

	public static <T> void createDataRegistry(ResourceKey<Registry<T>> key, Codec<T> codec, boolean synced) {
		if(synced) {
			DynamicRegistries.registerSynced(key, codec); // TODO what does SyncOption.SKIP_WHEN_EMPTY do?
		} else {
			DynamicRegistries.register(key, codec);
		}
	}

	public static List<RegistryDataLoader.RegistryData<?>> getDynamicRegistries() {
		return DynamicRegistries.getDynamicRegistries();
	}
}
