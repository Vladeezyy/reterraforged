package raccoonman.reterraforged.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.resources.ResourceLocation;
import raccoonman.reterraforged.data.worldgen.PresetExport;
import raccoonman.reterraforged.data.worldgen.PresetExportAccess;
import raccoonman.reterraforged.world.worldgen.biome.modifier.BiomeModifiers;

class PresetExportZipTest {
	@TempDir
	Path dir;

	// a folder whose name ends in "!" (an instance "Pack!"): the zip was written to "Pack" with a "jar:" URI
	@Test
	void aZipInAFolderWithAnExclamationMarkIsWrittenThere() throws IOException {
		Path input = Files.createDirectories(this.dir.resolve("in/data/reterraforged"));
		Files.writeString(input.resolve("preset.json"), "{}");
		Path folder = Files.createDirectories(this.dir.resolve("My Pack!/exports"));
		Path zip = folder.resolve("preset.zip.tmp");

		PresetExportAccess.copyToZip(this.dir.resolve("in"), zip);

		assertTrue(Files.isRegularFile(zip), "the zip is where it was asked for");
		assertFalse(Files.exists(this.dir.resolve("My Pack")), "nothing written to the path before the '!'");
		try (FileSystem fs = FileSystems.newFileSystem(zip)) {
			assertTrue(Files.exists(fs.getPath("/data/reterraforged/preset.json")));
		}
	}

	// NeoForge reads neoforge/biome_modifier, Fabric forge/biome_modifier: a pack exported on one loader had no biome
	// modifiers on the other (the release file is the NeoForge export)
	@Test
	void aPackExportedOnNeoForgeHasTheBiomeModifiersForFabricToo() throws IOException {
		assertBothFolders("neoforge");
	}

	@Test
	void aPackExportedOnFabricHasTheBiomeModifiersForNeoForgeToo() throws IOException {
		assertBothFolders("forge");
	}

	private void assertBothFolders(String exportedOn) throws IOException {
		Path pack = this.dir.resolve("pack");
		Path modifiers = Files.createDirectories(pack.resolve("data/reterraforged/" + exportedOn + "/biome_modifier"));
		Files.writeString(modifiers.resolve("add_erosion.json"), "{\"type\":\"reterraforged:add\"}");
		Files.writeString(modifiers.resolve("replace_forest_trees.json"), "{\"type\":\"reterraforged:replace\"}");
		Files.writeString(Files.createDirectories(pack.resolve("data/reterraforged/worldgen/preset")).resolve("preset.json"), "{}");
		Path zip = this.dir.resolve("preset.zip");

		PresetExport.mirrorBiomeModifiers(pack);
		PresetExportAccess.copyToZip(pack, zip);

		try (FileSystem fs = FileSystems.newFileSystem(zip)) {
			Path neoforge = fs.getPath("/data/reterraforged/neoforge/biome_modifier");
			Path fabric = fs.getPath("/data/reterraforged/forge/biome_modifier");
			assertEquals(List.of("add_erosion.json", "replace_forest_trees.json"), names(neoforge));
			assertEquals(names(neoforge), names(fabric), "the same modifiers in both folders");
			for (String name : names(neoforge)) {
				assertEquals(Files.readString(neoforge.resolve(name)), Files.readString(fabric.resolve(name)), name);
			}
			assertTrue(Files.exists(fs.getPath("/data/reterraforged/worldgen/preset/preset.json")), "the rest of the pack is there");
		}
	}

	// a pack that already has both folders keeps each as it is: neither is written over by the other
	@Test
	void bothFoldersAreLeftAsTheyAre() throws IOException {
		Path pack = this.dir.resolve("pack");
		Files.writeString(Files.createDirectories(pack.resolve("data/reterraforged/neoforge/biome_modifier")).resolve("add_erosion.json"), "neoforge");
		Files.writeString(Files.createDirectories(pack.resolve("data/reterraforged/forge/biome_modifier")).resolve("add_swamp_surface.json"), "fabric");

		PresetExport.mirrorBiomeModifiers(pack);

		assertEquals(List.of("add_erosion.json"), names(pack.resolve("data/reterraforged/neoforge/biome_modifier")));
		assertEquals(List.of("add_swamp_surface.json"), names(pack.resolve("data/reterraforged/forge/biome_modifier")));
	}

	// the other loader's folder only for a namespace without modifiers in the own one: a pack with both folders applies
	// once, and a pack of this loader stays as it was whatever another pack has in the other folder
	@Test
	void theOtherLoadersModifiersOnlyForANamespaceWithoutOwnOnes() {
		ResourceLocation erosion = ResourceLocation.fromNamespaceAndPath("reterraforged", "add_erosion");
		ResourceLocation swamp = ResourceLocation.fromNamespaceAndPath("reterraforged", "add_swamp_surface");
		ResourceLocation otherMod = ResourceLocation.fromNamespaceAndPath("othermod", "add_features");

		assertEquals(List.of(erosion, otherMod), BiomeModifiers.withFallback(List.of(erosion), List.of(erosion, swamp, otherMod), Function.identity()));
		assertEquals(List.of(erosion, swamp), BiomeModifiers.withFallback(List.of(), List.of(erosion, swamp), Function.identity()));
		assertEquals(List.of(erosion), BiomeModifiers.withFallback(List.of(erosion), List.of(), Function.identity()));
	}

	private static List<String> names(Path folder) throws IOException {
		try (Stream<Path> files = Files.list(folder)) {
			return files.map((file) -> file.getFileName().toString()).sorted().toList();
		}
	}
}
