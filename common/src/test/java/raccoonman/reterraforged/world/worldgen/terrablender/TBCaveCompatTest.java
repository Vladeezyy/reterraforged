package raccoonman.reterraforged.world.worldgen.terrablender;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import raccoonman.reterraforged.data.worldgen.preset.settings.CaveSettings.CompatibilityMode;

/** Darker Depths' and Deeper Darker's regions replace TerraBlender's only in a world with a preset on AUTO */
class TBCaveCompatTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void aWorldWithoutAPresetKeepsTerraBlendersRegions() {
		// TBCaveCompat.mode: null without a preset (it was AUTO)
		assertFalse(TBCaveCompat.replacesRegions(null, true));
		assertFalse(TBCaveCompat.replacesRegions(null, false));
	}

	@Test
	void onlyAutoReplacesTheRegions() {
		assertTrue(TBCaveCompat.replacesRegions(CompatibilityMode.AUTO, true));
		assertFalse(TBCaveCompat.replacesRegions(CompatibilityMode.AUTO, false));
		assertFalse(TBCaveCompat.replacesRegions(CompatibilityMode.RTF, true));
		assertFalse(TBCaveCompat.replacesRegions(CompatibilityMode.VANILLA, true));
	}
}
