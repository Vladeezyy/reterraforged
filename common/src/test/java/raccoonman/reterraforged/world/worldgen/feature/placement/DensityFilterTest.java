package raccoonman.reterraforged.world.worldgen.feature.placement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import raccoonman.reterraforged.data.worldgen.preset.settings.SpawnType;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings.Properties;

/** The density sliders only where the preset builds the world; everywhere in worlds of generation version 0 */
class DensityFilterTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private static Properties properties(boolean generationFixes, int generationVersion) {
		Properties properties = new Properties(SpawnType.CONTINENT_CENTER, 320, 64, 63, -54);
		properties.generationFixes = generationFixes;
		properties.generationVersion = generationVersion;
		return properties;
	}

	@Test
	void theWorldThePresetBuildsHasThem() {
		assertTrue(DensityFilter.scales(true, properties(true, Properties.CURRENT_GENERATION_VERSION)));
		assertTrue(DensityFilter.scales(true, properties(true, 0)));
		assertTrue(DensityFilter.scales(true, properties(false, 0)));
		assertTrue(DensityFilter.scales(true, null));
	}

	@Test
	void otherDimensionsOfANewWorldDoNot() {
		assertFalse(DensityFilter.scales(false, properties(true, 1)));
		assertFalse(DensityFilter.scales(false, null));
	}

	@Test
	void otherDimensionsOfAnOlderWorldKeepThem() {
		assertTrue(DensityFilter.scales(false, properties(true, 0)));
		assertTrue(DensityFilter.scales(false, properties(false, 1)));
	}
}
