package raccoonman.reterraforged.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PerformanceConfigTest {
	@TempDir
	Path dir;

	private PerformanceConfig read(String text) throws IOException {
		Path file = this.dir.resolve("performance.conf");
		Files.writeString(file, text);
		return PerformanceConfig.read(file).getOrThrow();
	}

	// 8 took about 9 GB, and the OutOfMemoryError left the server hanging
	@Test
	void aTileTooLargeForTheMemoryIsCut() throws IOException {
		assertEquals(PerformanceConfig.MAX_TILE_SIZE, this.read("tileSize=8\n").tileSize());
		assertEquals(1, this.read("tileSize=-3\n").tileSize());
		assertEquals(4, this.read("tileSize=4\n").tileSize());
	}

	// Properties.load throws IllegalArgumentException here, not IOException: the world did not open
	@Test
	void aBrokenFileGivesTheDefaults() throws IOException {
		assertEquals(PerformanceConfig.makeDefault(), this.read("tileSize=\\uZZZZ\n"));
	}

	@Test
	void aValueThatIsNoNumberGivesItsDefault() throws IOException {
		assertEquals(3, this.read("tileSize=large\n").tileSize());
	}

	@Test
	void writesTheDefaultsWhenThereIsNoFile() throws IOException {
		Path file = this.dir.resolve("sub/performance.conf");
		assertEquals(PerformanceConfig.makeDefault(), PerformanceConfig.read(file).getOrThrow());
		assertTrue(Files.readString(file).contains("tileSize=3"));
	}
}
