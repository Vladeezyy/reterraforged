package raccoonman.reterraforged.data.worldgen.preset;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.StructureSettings;
import raccoonman.reterraforged.mixin.StructurePlacementAccessor;

/**
 * The structure page's spacing, separation, salt and disabled switch, which did nothing (not in 0.0.6 either, and
 * 0.0.7 left it commented out): every structure set the preset changes is written into the preset datapack with them,
 * the rest of its placement (locate offset, frequency, the exclusion zone of pillager outposts) as it is. Sets the
 * preset leaves as they are are not written, so datapacks and mods that change them keep working.
 */
public final class PresetStructureSets {

	private PresetStructureSets() {
	}

	public static void bootstrap(Preset preset, BootstrapContext<StructureSet> ctx, RegistryAccess registries) {
		HolderGetter<Structure> structures = ctx.lookup(Registries.STRUCTURE);
		HolderGetter<StructureSet> sets = ctx.lookup(Registries.STRUCTURE_SET);
		Registry<StructureSet> current = registries.registryOrThrow(Registries.STRUCTURE_SET);
		preset.structures().entries.forEach((key, entry) -> {
			Optional<Holder.Reference<StructureSet>> holder = current.getHolder(key);
			if (holder.isEmpty() || !(holder.get().value().placement() instanceof RandomSpreadStructurePlacement placement)) {
				return;
			}
			// a salt is never negative in a datapack; the salt button gives any int
			int salt = entry.salt & Integer.MAX_VALUE;
			if (placement.spacing() == entry.spacing && placement.separation() == entry.separation && placement.salt() == salt && !entry.disabled) {
				return;
			}
			StructurePlacementAccessor accessor = (StructurePlacementAccessor) (Object) placement;
			Optional<StructurePlacement.ExclusionZone> exclusion = accessor.reterraforged$exclusionZone().map((zone) -> {
				return new StructurePlacement.ExclusionZone(sets.getOrThrow(zone.otherSet().unwrapKey().orElseThrow()), zone.chunkCount());
			});
			// disabled: the set is still there (other sets may refer to it), with a frequency of 0 it places nothing
			RandomSpreadStructurePlacement changed = new RandomSpreadStructurePlacement(accessor.reterraforged$locateOffset(), accessor.reterraforged$frequencyReductionMethod(), entry.disabled ? 0.0F : accessor.reterraforged$frequency(), salt, exclusion, entry.spacing, entry.separation, placement.spreadType());
			List<StructureSet.StructureSelectionEntry> entries = holder.get().value().structures().stream().map((selection) -> {
				return StructureSet.entry(structures.getOrThrow(selection.structure().unwrapKey().orElseThrow()), selection.weight());
			}).toList();
			ctx.register(key, new StructureSet(entries, changed));
		});
	}

	// the set the preset makes of a structure page entry, for the self test
	public static boolean matches(StructureSet set, StructureSettings.StructureSetEntry entry) {
		return set.placement() instanceof RandomSpreadStructurePlacement placement && placement.spacing() == entry.spacing && placement.separation() == entry.separation && placement.salt() == (entry.salt & Integer.MAX_VALUE);
	}
}
