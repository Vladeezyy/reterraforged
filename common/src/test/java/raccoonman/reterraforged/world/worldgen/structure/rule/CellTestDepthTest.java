package raccoonman.reterraforged.world.worldgen.structure.rule;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Structures far below the ground: under water only from generation version 1, on the mountains always */
class CellTestDepthTest {
	private static final int SURFACE = 60;
	private static final int DEEP = SURFACE - CellTest.DEEP - 1;
	private static final int SHALLOW = SURFACE - CellTest.DEEP;

	@Test
	void anOlderWorldRefusesEveryDepthUnderWater() {
		for(int y : new int[] { -40, 0, DEEP, SHALLOW, SURFACE }) {
			assertFalse(CellTest.allows(true, false, false, y, SURFACE, false));
			assertFalse(CellTest.allows(false, true, false, y, SURFACE, false));
			assertFalse(CellTest.allows(true, true, true, y, SURFACE, false));
		}
	}

	@Test
	void aNewWorldHasDeepStructuresUnderWater() {
		assertTrue(CellTest.allows(false, true, false, -27, SURFACE, true));
		assertTrue(CellTest.allows(true, false, false, 0, SURFACE, true));
		assertTrue(CellTest.allows(true, true, true, DEEP, SURFACE, true));
		// a structure at the surface is still kept out of rivers and lakes
		assertFalse(CellTest.allows(false, true, false, SHALLOW, SURFACE, true));
		assertFalse(CellTest.allows(true, false, false, SURFACE, SURFACE, true));
		assertFalse(CellTest.allows(true, false, true, SURFACE, SURFACE, true));
	}

	@Test
	void theMountainsAreTheSameInBothVersions() {
		for(boolean version : new boolean[] { false, true }) {
			assertTrue(CellTest.allows(false, false, true, DEEP, SURFACE, version));
			assertFalse(CellTest.allows(false, false, true, SHALLOW, SURFACE, version));
			assertTrue(CellTest.allows(false, false, false, SURFACE, SURFACE, version));
		}
	}
}
