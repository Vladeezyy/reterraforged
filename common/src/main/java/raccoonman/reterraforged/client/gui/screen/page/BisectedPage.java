package raccoonman.reterraforged.client.gui.screen.page;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import raccoonman.reterraforged.client.gui.ColumnAlignment;
import raccoonman.reterraforged.client.gui.screen.page.LinkedPageScreen.Page;
import raccoonman.reterraforged.client.gui.widget.WidgetList;

public abstract class BisectedPage<S extends Screen, L extends AbstractWidget, R extends AbstractWidget> implements Page {
	protected S screen;
	protected WidgetList<L> left;
	protected WidgetList<R> right;
	
	public BisectedPage(S screen) {
		this.screen = screen;
	}
	
	@Override
	public void init() {
		ColumnAlignment alignment = new ColumnAlignment(this.screen, 4, 0, 10, 30);
		// the right column holds buttons like "Open Preset Folder" and the preview: at least 150 px, so they are not
		// cut off on small windows / large GUI scales
		float right = Math.min(0.45F, Math.max(0.3F, 150F / Math.max(1, this.screen.width)));
		this.left = alignment.addColumn(1F - right, this::createAndPositionColumn);
		this.right = alignment.addColumn(right, this::createAndPositionColumn);
	}

	@Override
	public double getScroll() {
		return this.left != null ? this.left.getScrollAmount() : 0.0;
	}

	@Override
	public void setScroll(double scroll) {
		if (this.left != null) {
			this.left.setScrollAmount(scroll);
		}
	}

	private <T extends AbstractWidget> WidgetList<T> createAndPositionColumn(int left, int top, int columnWidth, int height, int horizontalPadding, int verticalPadding) {
		final int padding = 30;
		final int slotHeight = 25;
		WidgetList<T> list = new WidgetList<>(this.screen.minecraft, columnWidth, height - 65, padding, slotHeight);
		list.setX(left);
		return list;
	}
}