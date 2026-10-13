package raccoonman.reterraforged.platform.neoforge;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.RegistriesDatapackGenerator;
import net.neoforged.neoforge.registries.DataPackRegistriesHooks;

public final class DataGenUtilImpl {

	public static DataProvider createRegistryProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> providerLookup) {
		// the world registries with the ones mods (the preset registries) add
		return new RegistriesDatapackGenerator(output, "world", DataPackRegistriesHooks.getWorldRegistries(), providerLookup);
	}
}