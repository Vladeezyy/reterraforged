package raccoonman.reterraforged.client.gui.screen.presetconfig;

/** The rules of the sliders that depend on each other, without the screen, so the unit tests can run them */
final class SliderLimits {

	private SliderLimits() {
	}

	/**
	 * A slider position between its two neighbours. Neighbours out of order (a preset saved before 0.0.30 can have them)
	 * hold nothing: Mth.clamp with a minimum above the maximum gave the minimum for anything below it and the maximum for
	 * the rest, so a click at 20 % set a higher value than a click at 80 %.
	 */
	static double between(double value, double low, double high) {
		if (low > high) {
			return value;
		}
		return Math.max(low, Math.min(high, value));
	}

	/**
	 * The multiple of the step a slider moved to, in the direction it moved: rounded down it could never go up with the
	 * arrow keys, whose steps are smaller than the slider's step.
	 */
	static int stepTowards(double value, double previous, int step) {
		double steps = value / step;
		return (int) (value > previous ? Math.ceil(steps) : Math.floor(steps)) * step;
	}
}
