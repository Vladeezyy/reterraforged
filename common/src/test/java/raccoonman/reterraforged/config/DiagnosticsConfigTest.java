package raccoonman.reterraforged.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** A dedicated server's seed stays out of its (often public) log unless its owner says otherwise */
class DiagnosticsConfigTest {
	@TempDir
	Path dir;

	private DiagnosticsConfig read(String text) throws IOException {
		Path file = this.dir.resolve("diagnostics.conf");
		Files.writeString(file, text);
		return DiagnosticsConfig.read(file);
	}

	@Test
	void theSeedIsHiddenByDefault() throws IOException {
		Path file = this.dir.resolve("sub/diagnostics.conf");
		assertFalse(DiagnosticsConfig.read(file).logSeed());
		assertTrue(Files.readString(file).contains("logSeed=false"));
		// the file written is read back the same
		assertFalse(DiagnosticsConfig.read(file).logSeed());
	}

	@Test
	void theOwnerCanShowIt() throws IOException {
		assertTrue(this.read("logSeed=true\n").logSeed());
		assertTrue(this.read("logSeed = TRUE \n").logSeed());
	}

	@Test
	void aFileWithoutTheLineHidesIt() throws IOException {
		assertFalse(this.read("# an older file\n").logSeed());
	}

	@Test
	void anythingButTrueHidesIt() throws IOException {
		assertFalse(this.read("logSeed=yes\n").logSeed());
		assertFalse(this.read("logSeed=\\uZZZZ\n").logSeed());
	}
}
