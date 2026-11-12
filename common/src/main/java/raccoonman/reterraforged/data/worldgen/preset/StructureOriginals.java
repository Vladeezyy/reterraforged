package raccoonman.reterraforged.data.worldgen.preset;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.StructureSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.StructureSettings.StructureSetEntry;
import raccoonman.reterraforged.registries.RTFRegistries;

/**
 * The placement a structure set has without the preset. The structure page fills in an entry for every set it shows,
 * with the set's own values: such an entry is no change of the player's, and in PresetDiff it made every preset made
 * on the screen "custom" with a line per set.
 */
@FunctionalInterface
public interface StructureOriginals {
	/** Nothing known: every entry counts */
	StructureOriginals NONE = (key) -> null;

	/** The set's own spacing, separation and salt, null when not known (not a random spread, not loaded) */
	@Nullable
	StructureSetEntry get(ResourceKey<StructureSet> key);

	/**
	 * The sets of these registries, for the create world screen (WorldCreationContext.worldgenLoadContext()), whose
	 * page took its entries from them. Not for a running world: PresetStructureSets has put the preset's entries on
	 * its sets, so a changed one would look untouched there.
	 */
	static StructureOriginals of(RegistryAccess registries) {
		return (key) -> registries.lookup(Registries.STRUCTURE_SET).map((sets) -> sets.getValue(key)).map((set) -> set.placement() instanceof RandomSpreadStructurePlacement spread ? new StructureSetEntry(spread.spacing(), spread.separation(), spread.salt(), false) : null).orElse(null);
	}

	/**
	 * The sets as their files have them, for a running world: the top file of each set not in a preset datapack (one of
	 * 0.0.12 to 0.0.31 wrote the preset's values into it), what the world's registry had before the preset's entries
	 */
	static StructureOriginals of(ResourceManager resources) {
		FileToIdConverter files = FileToIdConverter.json(Registries.elementsDirPath(Registries.STRUCTURE_SET));
		Identifier presetFile = PresetStructureSets.presetFile();
		Map<ResourceKey<StructureSet>, Optional<StructureSetEntry>> read = new HashMap<>();
		return (key) -> read.computeIfAbsent(key, (k) -> Optional.ofNullable(fromFiles(resources.getResourceStack(files.idToFile(k.identifier())), presetFile))).orElse(null);
	}

	@Nullable
	private static StructureSetEntry fromFiles(List<Resource> stack, Identifier presetFile) {
		for (int i = stack.size() - 1; i >= 0; i--) {
			Resource resource = stack.get(i);
			if (resource.source().getResource(PackType.SERVER_DATA, presetFile) != null) {
				continue;
			}
			try (Reader reader = resource.openAsReader()) {
				JsonElement placement = JsonParser.parseReader(reader).getAsJsonObject().get("placement");
				if (placement instanceof JsonObject spread && spread.has("type") && spread.get("type").getAsString().endsWith("random_spread")) {
					return new StructureSetEntry(spread.get("spacing").getAsInt(), spread.get("separation").getAsInt(), spread.get("salt").getAsInt(), false);
				}
			} catch (Exception e) {
				// unknown: the entry stays in the list
			}
			return null;
		}
		return null;
	}

	/** The structure settings without the entries that are the set's own values */
	static StructureSettings changedOnly(StructureSettings structures, StructureOriginals originals) {
		StructureSettings changed = structures.copy();
		changed.entries.entrySet().removeIf((entry) -> same(entry.getValue(), originals.get(entry.getKey())));
		return changed;
	}

	/** The entry places the set as its own placement does: spacing, separation, salt (as PresetStructureSets applies it), not disabled */
	static boolean same(StructureSetEntry entry, @Nullable StructureSetEntry original) {
		return original != null && !entry.disabled && !original.disabled && entry.spacing == original.spacing && entry.separation == original.separation && (entry.salt & Integer.MAX_VALUE) == (original.salt & Integer.MAX_VALUE);
	}
}
