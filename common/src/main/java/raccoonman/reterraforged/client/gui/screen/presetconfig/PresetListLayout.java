package raccoonman.reterraforged.client.gui.screen.presetconfig;

/**
 * The preset list page in three columns: the list, as wide as its names need, the selected preset's map and
 * description, and the buttons. The map was under the buttons, and with eight of them on a GUI scale of 3 or 4 (most
 * players' "Auto") there was no room left for it at all; here it does not depend on how many buttons there are.
 */
record PresetListLayout(int listWidth, int infoWidth) {
	// a row's inset, the scroll bar and some air around the names
	static final int LIST_PADDING = 36;
	static final int GAP = 8;
	private static final int LIST_MIN = 100;
	// below this the names give way (they are cut with "..."); at GUI scale 4 on 1920x1080 a description is 5-6 lines
	private static final int INFO_PREFERRED = 104;
	// narrower than this, no map and description: the list keeps the whole width (the smallest windows)
	private static final int INFO_MIN = 72;
	// the map is at most 256 wide; more is only empty
	private static final int INFO_MAX = 264;

	/** total: from the list's left edge to the buttons' column; namesWidth: the widest name in the list */
	static PresetListLayout split(int total, int namesWidth) {
		int list = Math.max(LIST_MIN, Math.min(namesWidth + LIST_PADDING, total - GAP - INFO_PREFERRED));
		int info = Math.min(INFO_MAX, total - list - GAP);
		if (info < INFO_MIN) {
			return new PresetListLayout(total, 0);
		}
		// a wide window: the list gets what the map does not use
		return new PresetListLayout(total - GAP - info, info);
	}
}
