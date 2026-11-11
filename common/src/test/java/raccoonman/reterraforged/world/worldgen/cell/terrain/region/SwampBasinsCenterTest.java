package raccoonman.reterraforged.world.worldgen.cell.terrain.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;

class SwampBasinsCenterTest {

	// two region cells with one id, as RegionModule gives them (issue #42: a swamp basin in a desert); the first is x = 0,
	// the second one x: ids repeat at least where n is a multiple of 2048, its cube is then 0 in an int
	private static int sameId(int seed) {
		float first = NoiseUtil.valCoord2D(seed, 0, 0);
		for (int x = 1; x < 8192; x++) {
			if (NoiseUtil.valCoord2D(seed, x, 0) == first) {
				return x;
			}
		}
		return -1;
	}

	@Test
	void regionIdsRepeat() {
		assertTrue(sameId(0) > 0, "no two cells with one id");
	}

	@Test
	void anotherRegionWithItsIdIsNotHeld() {
		int other = sameId(0);
		assertTrue(other > 0);
		float id = NoiseUtil.valCoord2D(0, 0, 0);
		assertEquals(id, NoiseUtil.valCoord2D(0, other, 0));

		SwampBasins.Center center = new SwampBasins.Center();
		assertFalse(center.holds(id, 0.3F, 0.4F));
		center.hold(id, 0.3F, 0.4F);
		assertTrue(center.holds(id, 0.3F, 0.4F));
		// region B: same id, its own center; a fresh thread computes it, so must this one
		assertFalse(center.holds(id, other + 0.3F, 0.4F));
		assertFalse(center.holds(id + 1.0F, 0.3F, 0.4F));
	}
}
