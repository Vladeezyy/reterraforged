package raccoonman.reterraforged.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldTileSizeTest {
	@TempDir
	Path world;

	@AfterEach
	void clear() {
		WorldTileSize.clear();
	}

	// a new world, or one made before the file: the config's size, stored
	@Test
	void theFirstStartStoresTheConfiguredSize() throws IOException {
		assertEquals(4, WorldTileSize.of(this.world, 4));
		Path file = this.world.resolve(WorldTileSize.FILE);
		assertTrue(Files.readString(file).contains("\"tileSize\":4"));
	}

	// another machine's or an edited performance.conf: no seams, the world keeps its size
	@Test
	void laterStartsKeepTheWorldsSize() {
		WorldTileSize.of(this.world, 2);
		WorldTileSize.clear();
		assertEquals(2, WorldTileSize.of(this.world, 5));
	}

	@Test
	void everyDimensionGetsTheSameSize() {
		assertEquals(3, WorldTileSize.of(this.world, 3));
		assertEquals(3, WorldTileSize.of(this.world, 1));
		assertEquals(3, WorldTileSize.current(1));
	}

	// a random state outside a ChunkMap with no world running: the config
	@Test
	void withoutAWorldTheConfigApplies() {
		assertEquals(4, WorldTileSize.current(4));
	}

	// left as it is for whoever broke it, the config's size meanwhile
	@Test
	void aBrokenFileGivesTheConfiguredSize() throws IOException {
		Path file = this.world.resolve(WorldTileSize.FILE);
		Files.createDirectories(file.getParent());
		Files.writeString(file, "{tileSize:");
		assertEquals(3, WorldTileSize.of(this.world, 3));
		assertEquals("{tileSize:", Files.readString(file));
	}

	// a hand edited size too large for the memory is cut like the config's
	@Test
	void aStoredSizeIsClamped() throws IOException {
		Path file = this.world.resolve(WorldTileSize.FILE);
		Files.createDirectories(file.getParent());
		Files.writeString(file, "{\"tileSize\": 9}");
		assertEquals(PerformanceConfig.MAX_TILE_SIZE, WorldTileSize.of(this.world, 3));
	}
}
