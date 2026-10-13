package raccoonman.reterraforged.client.gui.widget;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetEditorPage;

public class WidgetList<T extends AbstractWidget> extends ContainerObjectSelectionList<WidgetList.Entry<T>> {
	private boolean renderSelected;
	
    public WidgetList(Minecraft minecraft, int i, int j, int k, int l) {
        super(minecraft, i, j, k, l);
    }

    public void select(T widget) {
    	for(Entry<T> entry : this.children()) {
    		if(entry.widget.equals(widget)) {
    			this.setSelected(entry);
    			return;
    		}
    	}
    }
    
    public <W extends T> W addWidget(W widget) {
        super.addEntry(new Entry<>(widget));
        return widget;
    }

    public void setRenderSelected(boolean renderSelected) {
    	this.renderSelected = renderSelected;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // First check preview widgets
        for (Entry<T> entry : this.children()) {
            T widget = entry.getWidget();
            if (widget instanceof PresetEditorPage.Preview preview && widget.isMouseOver(event.x(), event.y())) {
                if (preview.mouseClicked(event, doubleClick)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        // Forward drag events to any preview widget (assuming only one)
        for (Entry<T> entry : this.children()) {
            T widget = entry.getWidget();
            if (widget instanceof PresetEditorPage.Preview preview) {
                // Always forward drag events to preview, regardless of mouse position
                if (preview.mouseDragged(event, dragX, dragY)) {
                    return true;
                }
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Forward scroll events to any preview widget (assuming only one)
        for (Entry<T> entry : this.children()) {
            T widget = entry.getWidget();
            if (widget instanceof PresetEditorPage.Preview preview) {
                // Always forward scroll events to preview, regardless of mouse position
                if (preview.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void extractSelection(GuiGraphicsExtractor guiGraphics, Entry<T> entry, int color) {
        if (this.renderSelected) {
            super.extractSelection(guiGraphics, entry, color);
        }
    }

    @Override
    public int getRowWidth() {
        return this.width - 20;
    }

    @Override
    protected int scrollBarX() {
        return this.getRowRight();
    }

    public static class Entry<T extends AbstractWidget> extends ContainerObjectSelectionList.Entry<Entry<T>> {
        private T widget;

        public Entry(T widget) {
            this.widget = widget;
        }

        public T getWidget() {
        	return this.widget;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.singletonList(this.widget);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTicks) {
            int top = this.getContentY(), left = this.getContentX(), width = this.getContentWidth(), height = this.getContentHeight();
            int optionWidth = Math.min(450, width);
            int padding = (width - optionWidth) / 2;
            widget.setX(left + padding);
            widget.setY(top);
            widget.visible = true;
            widget.setWidth(optionWidth);
            widget.setHeight(height - 1);
            if(widget instanceof PresetEditorPage.Preview preview) {
            	widget.setHeight(widget.getWidth());
            }
            widget.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        }

		@Override
		public List<T> narratables() {
			return Collections.singletonList(this.widget);
		}
    }
}
