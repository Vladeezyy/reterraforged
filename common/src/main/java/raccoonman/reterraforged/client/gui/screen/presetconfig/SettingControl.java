package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * What the settings pages know of a setting's widget besides the widget: its translation key, its value as the player
 * sees it, and a way to give it another value through the widget's own callback (the same path as a click, so the preview,
 * the limits of the neighbours and the greyed-out widgets follow). Made by PresetWidgets for every setting widget.
 */
abstract class SettingControl {
	static final String DEFAULT_TOOLTIP = "reterraforged.gui.setting.default";
	// by widget, for the code that only has the widget (setUsable, the pages). The control is held by its widget (the
	// widget's callback has it) and only weakly here: a control holds its widget, and a strong value would keep both
	private static final Map<AbstractWidget, WeakReference<SettingControl>> CONTROLS = new WeakHashMap<>();

	final String key;
	// the widget's own tooltip, without the line about Default
	@Nullable
	private Component tooltip;
	// Default's value while the setting has another one, else null
	@Nullable
	private Component defaultText;
	@Nullable
	private Runnable listener;

	SettingControl(String key) {
		this.key = key;
	}

	static void attach(AbstractWidget widget, SettingControl control) {
		CONTROLS.put(widget, new WeakReference<>(control));
	}

	@Nullable
	static SettingControl of(AbstractWidget widget) {
		WeakReference<SettingControl> control = CONTROLS.get(widget);
		return control != null ? control.get() : null;
	}

	abstract AbstractWidget widget();

	// equal for two widgets that show the same value
	abstract Object state();

	abstract Component valueText();

	// what restore takes: plain data, kept after the widget is gone
	abstract Object value();

	abstract void restore(Object value);

	final DefaultValue snapshot() {
		return new DefaultValue(this.state(), this.valueText(), this.value());
	}

	// called by the widget after each change of its value
	final void changed() {
		if (this.listener != null) {
			this.listener.run();
		}
	}

	final void setListener(@Nullable Runnable listener) {
		this.listener = listener;
	}

	// the tooltip the widget is to show, with the line about Default when it is not at Default
	@Nullable
	final Tooltip tooltip(@Nullable Component tooltip) {
		this.tooltip = tooltip;
		return this.build();
	}

	final void setDefaultText(@Nullable Component defaultText) {
		if (this.defaultText != defaultText) {
			this.defaultText = defaultText;
			this.widget().setTooltip(this.build());
		}
	}

	@Nullable
	private Tooltip build() {
		if (this.defaultText == null) {
			return this.tooltip != null ? Tooltip.create(this.tooltip) : null;
		}
		MutableComponent text = Component.empty();
		if (this.tooltip != null) {
			text.append(this.tooltip).append("\n\n");
		}
		text.append(Component.translatable(DEFAULT_TOOLTIP, this.defaultText).withStyle(ChatFormatting.GRAY));
		return Tooltip.create(text);
	}

	// a setting of Default: what it shows, its text for the tooltip and what restore takes
	record DefaultValue(Object state, Component text, Object value) {
	}
}
