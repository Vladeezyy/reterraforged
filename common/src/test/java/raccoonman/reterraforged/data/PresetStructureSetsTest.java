package raccoonman.reterraforged.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import raccoonman.reterraforged.data.worldgen.preset.PresetStructureSets;
import raccoonman.reterraforged.data.worldgen.preset.settings.StructureSettings.StructureSetEntry;

/** The structure page's entries on the loaded structure sets, and the files of older preset datapacks left out */
class PresetStructureSetsTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void thePresetFolderHasTheNamespaceFirstOnBothLoaders() {
		// NeoForge: Registries.elementsDirPath gives it with the namespace; Fabric: without (it adds it when it loads)
		assertEquals("reterraforged/worldgen/preset", PresetStructureSets.presetDir("reterraforged/worldgen/preset", "reterraforged"));
		assertEquals("reterraforged/worldgen/preset", PresetStructureSets.presetDir("worldgen/preset", "reterraforged"));
	}

	@Test
	void anEntryAsTheSetIsChangesNothing() {
		// villages: 34, 8, 10387312 (an entry the page made from the set)
		assertFalse(PresetStructureSets.changes(34, 8, 10387312, new StructureSetEntry(34, 8, 10387312, false)));
		assertTrue(PresetStructureSets.changes(34, 8, 10387312, new StructureSetEntry(21, 8, 10387312, false)));
		assertTrue(PresetStructureSets.changes(34, 8, 10387312, new StructureSetEntry(34, 9, 10387312, false)));
		assertTrue(PresetStructureSets.changes(34, 8, 10387312, new StructureSetEntry(34, 8, 1, false)));
		// disabled: a frequency of 0, also with the set's own spacing
		assertTrue(PresetStructureSets.changes(34, 8, 10387312, new StructureSetEntry(34, 8, 10387312, true)));
	}

	@Test
	void aNegativeSaltIsTheDatapacksSalt() {
		StructureSetEntry entry = new StructureSetEntry(21, 9, -12345, false);
		assertEquals(-12345 & Integer.MAX_VALUE, PresetStructureSets.salt(entry));
		assertFalse(PresetStructureSets.changes(21, 9, -12345 & Integer.MAX_VALUE, entry));
	}

	@Test
	void anEntryMinecraftRefusesIsNotApplied() {
		assertTrue(PresetStructureSets.valid(new StructureSetEntry(1, 0, 0, false)));
		assertTrue(PresetStructureSets.valid(new StructureSetEntry(4096, 4095, 0, false)));
		assertFalse(PresetStructureSets.valid(new StructureSetEntry(0, 0, 0, false)));
		assertFalse(PresetStructureSets.valid(new StructureSetEntry(8, 8, 0, false)));
		assertFalse(PresetStructureSets.valid(new StructureSetEntry(8, -1, 0, false)));
		assertFalse(PresetStructureSets.valid(new StructureSetEntry(4097, 8, 0, false)));
	}

	@Test
	void theFileOfAPresetPackGivesWayToTheOneBelow() {
		// bottom first: vanilla, a mod, the preset datapack's file
		List<String> stack = List.of("vanilla", "mod", "preset");
		assertEquals(1, PresetStructureSets.topmost(stack, "preset"::equals));
		assertEquals(2, PresetStructureSets.topmost(stack, (pack) -> false));
		// a set only the preset datapack has (its mod's own file gone): none, the set is not loaded
		assertEquals(-1, PresetStructureSets.topmost(List.of("preset"), "preset"::equals));
		// a pack above the preset's keeps its file
		assertEquals(2, PresetStructureSets.topmost(List.of("vanilla", "preset", "other"), "preset"::equals));
	}
}
