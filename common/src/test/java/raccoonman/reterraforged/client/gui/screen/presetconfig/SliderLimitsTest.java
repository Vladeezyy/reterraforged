package raccoonman.reterraforged.client.gui.screen.presetconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SliderLimitsTest {

	@Test
	void keepsAValueBetweenItsNeighbours() {
		assertEquals(0.3D, SliderLimits.between(0.1D, 0.3D, 0.5D));
		assertEquals(0.5D, SliderLimits.between(0.9D, 0.3D, 0.5D));
		assertEquals(0.4D, SliderLimits.between(0.4D, 0.3D, 0.5D));
	}

	// Mth.clamp gave 0.448 for a click at 20 % and 0.25 for a click at 80 % on Deep Ocean
	@Test
	void neighboursOutOfOrderHoldNothing() {
		assertEquals(0.2D, SliderLimits.between(0.2D, 0.448D, 0.25D));
		assertEquals(0.8D, SliderLimits.between(0.8D, 0.448D, 0.25D));
	}

	// the arrow keys move a slider by less than its step: rounded down it could never go up
	@Test
	void aStepGoesTheWayTheSliderMoved() {
		assertEquals(256, SliderLimits.stepTowards(129.0D, 128.0D, 128));
		assertEquals(0, SliderLimits.stepTowards(127.0D, 128.0D, 128));
		assertEquals(128, SliderLimits.stepTowards(128.0D, 128.0D, 128));
		assertEquals(-256, SliderLimits.stepTowards(-129.0D, -128.0D, 128));
	}
}
