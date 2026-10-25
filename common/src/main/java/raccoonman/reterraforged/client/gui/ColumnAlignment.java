package raccoonman.reterraforged.client.gui;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import raccoonman.reterraforged.mixin.ScreenInvoker;

public class ColumnAlignment {
	private Screen parent;
	private int horizontalPadding;
	private int verticalPadding;
	private int horizontalMargin;
	private int verticalMargin;
	private int left;
	
	private int leftInset;

	public ColumnAlignment(Screen parent, int horizontalPadding, int verticalPadding, int horizontalMargin, int verticalMargin) {
		this(parent, horizontalPadding, verticalPadding, horizontalMargin, verticalMargin, 0);
	}

	// leftInset: width kept free on the left of the columns (the tabs of the preset screen)
	public ColumnAlignment(Screen parent, int horizontalPadding, int verticalPadding, int horizontalMargin, int verticalMargin, int leftInset) {
		this.parent = parent;
		this.leftInset = leftInset;
		this.horizontalPadding = horizontalPadding;
		this.verticalPadding = verticalPadding;
		this.horizontalMargin = horizontalMargin;
		this.verticalMargin = verticalMargin;
		this.left = horizontalMargin + leftInset;
	}

	public <T extends GuiEventListener & Renderable> T addColumn(float columnSize, ColumnFactory<T> factory) {
		int pageWidth = this.parent.width - (this.horizontalMargin * 2) - this.leftInset;
		int height = this.parent.height;
		int columnWidth = Math.max(0, Math.round(columnSize * pageWidth) - (2 * this.horizontalPadding));
		T column = factory.apply(this.left, this.verticalMargin, columnWidth, height, this.horizontalPadding, this.verticalPadding);
		this.left += columnWidth > 0 ? columnWidth + (2 * this.horizontalPadding) : 0;
		((ScreenInvoker) this.parent).invokeAddRenderableWidget(column);
		return column;
	}

	public interface ColumnFactory<T extends GuiEventListener> {
		T apply(int left, int top, int columnWidth, int height, int horizontalPadding, int verticalPadding);
	}
}
