package raccoonman.reterraforged.world.worldgen;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.RegistryAccess;

/**
 * The registries of the running server's overworld, for a random state another mod creates itself with our noise settings
 * and never hands to a ChunkMap (CityWorld's "Vanilla, cities apart" builds its own overworld generator). Dropped when the
 * server stops: a static reference kept the last world's registries in memory, and gave a later world their preset.
 */
public final class OverworldRegistries {
	@Nullable
	private static volatile RegistryAccess current;

	private OverworldRegistries() {
	}

	@Nullable
	public static RegistryAccess get() {
		return current;
	}

	public static void set(RegistryAccess registries) {
		current = registries;
	}

	public static void clear() {
		current = null;
	}
}
