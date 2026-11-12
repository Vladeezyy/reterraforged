package raccoonman.reterraforged.client.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;

/**
 * The mark of a setting that is not Default's, and of a page tab with such settings: a bar left of the widget. A shape
 * next to the widget and not a colour of its text, so it is seen without telling colours apart, and the widget's text
 * (which the widget tests read) stays as it was.
 */
public final class ChangeMarker {
	private static final int COLOR = 0xFFFFAA00;
	private static final int OUTLINE = 0xFF000000;

	private ChangeMarker() {
	}

	public static void extractRenderState(GuiGraphicsExtractor graphics, AbstractWidget widget) {
		int x = widget.getX() - 6;
		int top = widget.getY() + 2;
		int bottom = widget.getY() + widget.getHeight() - 2;
		graphics.fill(x - 1, top - 1, x + 3, bottom + 1, OUTLINE);
		graphics.fill(x, top, x + 2, bottom, COLOR);
	}
}
