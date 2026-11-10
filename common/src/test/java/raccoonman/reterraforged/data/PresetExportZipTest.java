package raccoonman.reterraforged.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import raccoonman.reterraforged.data.worldgen.PresetExportAccess;

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
}
