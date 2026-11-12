package raccoonman.reterraforged.data.worldgen.preset;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.StructureSettings;
import raccoonman.reterraforged.mixin.StructurePlacementAccessor;
import raccoonman.reterraforged.mixin.StructureSetAccessor;
import raccoonman.reterraforged.registries.RTFRegistries;

/**
 * The structure page's spacing, separation, salt and disabled switch. 0.0.12 to 0.0.31 wrote every set the preset
 * changes into the preset datapack, and such a file names every structure of the set and the set of its exclusion zone:
 * with the mod of one of them removed the world did not load. Now the datapack has no structure sets; when the worldgen
 * registries load (MixinRegistryDataLoader) the preset's entries go on the placement of the sets that are there, the
 * rest of it (locate offset, frequency, exclusion zone) as it is. A set whose mod is gone is skipped, a mod added later
 * brings its sets with its own placement. The files of an older preset datapack are left out (the entries the files
 * were made from do the same), and as with a datapack, a pack above the preset's that has the set wins.
 */
public final class PresetStructureSets {
	// the sets a pack above the preset's has, from the file listing to the end of the load. By the resource manager of
	// the load: since 26.1 the files are listed and the registries frozen on the executor's threads, not on one thread
	private static final Map<ResourceManager, Set<ResourceKey<StructureSet>>> ABOVE_PRESET = Collections.synchronizedMap(new WeakHashMap<>());

	private PresetStructureSets() {
	}

	// the preset file of a datapack, where the loader reads it (NeoForge and Fabric put the registry's namespace first);
	// a class of its own: the rest of this one runs without the registries (the unit tests)
	private static final class PresetFile {
		static final Identifier LOCATION = FileToIdConverter.json(Registries.elementsDirPath(RTFRegistries.PRESET)).idToFile(Preset.KEY.identifier());
	}

	// nothing in the datapack: the entries are applied when the registries load
	public static void bootstrap(Preset preset, BootstrapContext<StructureSet> ctx, RegistryAccess registries) {
	}

	// the structure set files to read, given the top one of each id: without those of preset datapacks whose preset has
	// an entry for the set (written by 0.0.12 to 0.0.31), the next one down instead
	public static Map<Identifier, Resource> structureSetFiles(Map<Identifier, Resource> resources, FileToIdConverter converter, ResourceManager manager) {
		ABOVE_PRESET.remove(manager);
		List<Resource> presets = manager.getResourceStack(PresetFile.LOCATION);
		if (presets.isEmpty()) {
			return resources;
		}
		Map<PackResources, Integer> order = new IdentityHashMap<>();
		manager.listPacks().forEach((pack) -> order.put(pack, order.size()));
		// a pack not in the list (a loader's group of mod packs) is below the world's datapacks
		int presetPack = order.getOrDefault(presets.get(presets.size() - 1).source(), Integer.MAX_VALUE);
		Map<PackResources, Set<Identifier>> written = new IdentityHashMap<>();
		Map<Identifier, Resource> files = new LinkedHashMap<>(resources);
		Set<ResourceKey<StructureSet>> above = new HashSet<>();
		List<Identifier> leftOut = new ArrayList<>();
		converter.listMatchingResourceStacks(manager).forEach((file, stack) -> {
			Identifier id = converter.fileToId(file);
			Predicate<Resource> fromPreset = (resource) -> written.computeIfAbsent(resource.source(), PresetStructureSets::presetEntries).contains(id);
			int top = topmost(stack, fromPreset);
			if (top != stack.size() - 1) {
				leftOut.add(id);
				if (top < 0) {
					files.remove(file);
				} else {
					files.put(file, stack.get(top));
				}
			}
			if (top >= 0 && order.getOrDefault(stack.get(top).source(), -1) > presetPack) {
				above.add(ResourceKey.create(Registries.STRUCTURE_SET, id));
			}
		});
		ABOVE_PRESET.put(manager, above);
		if (leftOut.isEmpty()) {
			return resources;
		}
		RTFCommon.LOGGER.info("[ReTerraForged] the structure sets of an older preset datapack are left out, the preset's structure settings apply to the loaded sets: {}", leftOut);
		return files;
	}

	// the index of the top resource of a stack (bottom first) that is not left out, -1 for none
	public static <T> int topmost(List<T> stack, Predicate<T> leftOut) {
		for (int i = stack.size() - 1; i >= 0; i--) {
			if (!leftOut.test(stack.get(i))) {
				return i;
			}
		}
		return -1;
	}

	// the structure sets the preset of a pack has entries for; none without a preset (or one that does not read)
	private static Set<Identifier> presetEntries(PackResources pack) {
		IoSupplier<InputStream> file = pack.getResource(PackType.SERVER_DATA, PresetFile.LOCATION);
		if (file == null) {
			return Set.of();
		}
		try (Reader reader = new InputStreamReader(file.get(), StandardCharsets.UTF_8)) {
			JsonElement structures = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("structures").get("structures");
			Set<Identifier> ids = new HashSet<>();
			if (structures instanceof JsonObject entries) {
				for (String key : entries.keySet()) {
					Identifier id = Identifier.tryParse(key);
					if (id != null) {
						ids.add(id);
					}
				}
			}
			return ids;
		} catch (Exception e) {
			RTFCommon.LOGGER.warn("[ReTerraForged] the preset of pack {} does not read, its structure sets stay: {}", pack.packId(), e.toString());
			return Set.of();
		}
	}

	// the preset's entries on the loaded structure sets: right after the registries load, before anything reads a
	// placement (the chunk generators' structure state, locate, the create world screen)
	public static void apply(RegistryAccess registries, ResourceManager manager) {
		// not removed: the entry goes with its resource manager, and a second call for the same load skips the same sets
		Set<ResourceKey<StructureSet>> above = ABOVE_PRESET.get(manager);
		Optional<Registry<StructureSet>> sets = registries.lookup(Registries.STRUCTURE_SET);
		Optional<Preset> preset = registries.lookup(RTFRegistries.PRESET).flatMap((registry) -> registry.getOptional(Preset.KEY));
		if (sets.isEmpty() || preset.isEmpty()) {
			return;
		}
		List<ResourceKey<StructureSet>> applied = new ArrayList<>();
		List<ResourceKey<StructureSet>> missing = new ArrayList<>();
		preset.get().structures().entries.forEach((key, entry) -> {
			StructureSet set = sets.get().getValue(key);
			if (set == null) {
				// its mod is not installed (any more): nothing to place, as in vanilla
				missing.add(key);
				return;
			}
			if (above != null && above.contains(key)) {
				return;
			}
			StructurePlacement placement = placement(set.placement(), entry);
			if (placement != null) {
				((StructureSetAccessor) (Object) set).reterraforged$setPlacement(placement);
				applied.add(key);
			}
		});
		if (!applied.isEmpty() || !missing.isEmpty()) {
			RTFCommon.LOGGER.info("[ReTerraForged] structure settings of the preset: {} changed, {} not in this world", applied, missing);
		}
	}

	// the placement the entry gives a set, null when it keeps its own: not a random spread (strongholds), as it is, or
	// an entry Minecraft would refuse (a datapack with it did not load)
	@Nullable
	public static RandomSpreadStructurePlacement placement(StructurePlacement current, StructureSettings.StructureSetEntry entry) {
		if (!(current instanceof RandomSpreadStructurePlacement placement) || !changes(placement.spacing(), placement.separation(), placement.salt(), entry) || !valid(entry)) {
			return null;
		}
		StructurePlacementAccessor accessor = (StructurePlacementAccessor) (Object) placement;
		// disabled: the set is still there (other sets may refer to it), with a frequency of 0 it places nothing
		return new RandomSpreadStructurePlacement(accessor.reterraforged$locateOffset(), accessor.reterraforged$frequencyReductionMethod(), entry.disabled ? 0.0F : accessor.reterraforged$frequency(), salt(entry), accessor.reterraforged$exclusionZone(), entry.spacing, entry.separation, placement.spreadType());
	}

	// the entry is not what the set has (the datapack had a set written for exactly these)
	public static boolean changes(int spacing, int separation, int salt, StructureSettings.StructureSetEntry entry) {
		return spacing != entry.spacing || separation != entry.separation || salt != salt(entry) || entry.disabled;
	}

	// as the structure set codec checks it: a spacing of 0 divides by zero
	public static boolean valid(StructureSettings.StructureSetEntry entry) {
		return entry.spacing >= 1 && entry.spacing <= 4096 && entry.separation >= 0 && entry.separation < entry.spacing;
	}

	// a salt is never negative in a datapack; the salt button gives any int
	public static int salt(StructureSettings.StructureSetEntry entry) {
		return entry.salt & Integer.MAX_VALUE;
	}

	// the placement the preset gives a set (spacing, separation, salt), for the self test
	public static boolean matches(@Nullable StructurePlacement placement, StructureSettings.StructureSetEntry entry) {
		return placement instanceof RandomSpreadStructurePlacement spread && spread.spacing() == entry.spacing && spread.separation() == entry.separation && spread.salt() == salt(entry);
	}
}
