package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.util.Locale;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.client.gui.Toasts;
import raccoonman.reterraforged.client.gui.Tooltips;
import raccoonman.reterraforged.client.gui.widget.Label;
import raccoonman.reterraforged.client.gui.widget.Slider;
import raccoonman.reterraforged.client.gui.widget.Slider.Format;
import raccoonman.reterraforged.client.gui.widget.ValueButton;

final class PresetWidgets {
	
	public static EditBox createEditBox(Font font, Consumer<String> responder, Component prompt) {
		EditBox box = new EditBox(font, -1, -1, -1, -1, CommonComponents.EMPTY);
		box.setResponder(responder);
		box.setHint(prompt);
		return box;
	}
	
	public static Label createLabel(String text) {
		return createLabel(Component.translatable(text));
	}
	
	public static Label createLabel(Component text) {
		return new Label(-1, -1, -1, -1, text);
	}

	private static Slider createSlider(float initial, float min, float max, String text, Slider.Format format, Slider.Callback callback) {
		Slider slider = new Slider(-1, -1, -1, -1, initial, min, max, Component.translatable(text), format, callback);
		SliderControl control = new SliderControl(text, slider);
		slider.onChanged(control::changed);
		SettingControl.attach(slider, control);
		slider.setTooltip(control.tooltip(Component.translatable(Tooltips.translationKey(text))));
		Function<String, Component> valueText = SliderUnits.valueText(text);
		if (valueText != null) {
			slider.withValueText(valueText);
		}
		return slider;
	}

	public static Slider createFloatSlider(float initial, float min, float max, String text, Slider.Callback callback) {
		return createSlider(initial, min, max, text, Format.FLOAT, callback);
	}
	
	public static Slider createIntSlider(int initial, int min, int max, String text, Slider.Callback callback) {
		return createSlider(initial, min, max, text, Format.INT, callback);
	}
	
	public static <T extends Enum<T>> CycleButton<T> createCycle(T[] values, T initial, String text, CycleButton.OnValueChange<T> callback) {
		return createCycle(ImmutableList.copyOf(values), initial, text, callback, T::name);
	}

	public static <T> CycleButton<T> createCycle(Collection<T> values, T initial, String text, CycleButton.OnValueChange<T> callback, Function<T, String> name) {
		return createCycle(values, initial, Optional.of(text), callback, name);
	}
	
	public static <T> CycleButton<T> createCycle(Collection<T> values, T initial, Optional<String> text, CycleButton.OnValueChange<T> callback, Function<T, String> name) {
		Function<T, Component> valueName = (e) -> {
			return e instanceof Enum<?> value ? enumName(value) : Component.literal(name.apply(e));
		};
		if(text.isEmpty()) {
			// not a setting of the preset (the preview's render mode)
			CycleButton<T> button = CycleButton.<T>builder(valueName).withInitialValue(initial).withValues(values).withTooltip((e) -> tooltipOf(cycleTooltip(text, e))).displayOnlyValue().create(-1, -1, -1, -1, null, callback);
			button.setTooltip(tooltipOf(cycleTooltip(text, initial)));
			return button;
		}
		return cycle(values, initial, text.get(), valueName, (e) -> cycleTooltip(text, e), callback);
	}

	// a cycle whose values have their own names, with the setting's tooltip for all of them
	public static <T> CycleButton<T> createNamedCycle(Collection<T> values, T initial, String text, Function<T, Component> valueName, CycleButton.OnValueChange<T> callback) {
		return cycle(values, initial, text, valueName, (e) -> Component.translatable(Tooltips.translationKey(text)), callback);
	}

	// tooltips given to the builder: a tooltip set on the button only is dropped at the first click
	private static <T> CycleButton<T> cycle(Collection<T> values, T initial, String text, Function<T, Component> valueName, Function<T, Component> tooltip, CycleButton.OnValueChange<T> callback) {
		CycleControl<T> control = new CycleControl<>(text, valueName);
		CycleButton.OnValueChange<T> changed = (button, value) -> {
			callback.onValueChange(button, value);
			control.changed();
		};
		CycleButton<T> button = CycleButton.<T>builder(valueName).withInitialValue(initial).withValues(values).withTooltip((e) -> control.tooltip(tooltip.apply(e))).create(-1, -1, -1, -1, Component.translatable(text), changed);
		control.button = button;
		control.callback = changed;
		SettingControl.attach(button, control);
		button.setTooltip(control.tooltip(tooltip.apply(initial)));
		return button;
	}

	@Nullable
	private static Tooltip tooltipOf(@Nullable Component text) {
		return text != null ? Tooltip.create(text) : null;
	}

	// the setting's tooltip, then for an enum value what it does and the name the button showed before 0.0.22 (the
	// enum constant, e.g. MULTI_IMPROVED), so presets and guides written for the old names still make sense
	@Nullable
	private static <T> Component cycleTooltip(Optional<String> text, T value) {
		MutableComponent tooltip = Component.empty();
		text.ifPresent((key) -> tooltip.append(Component.translatable(Tooltips.translationKey(key))));
		if (value instanceof Enum<?> e) {
			if (text.isPresent()) {
				tooltip.append("\n\n");
			}
			String description = Tooltips.translationKey(enumKey(e));
			if (Language.getInstance().has(description)) {
				tooltip.append(enumName(e).copy().withStyle(ChatFormatting.YELLOW)).append(": ").append(Component.translatable(description)).append("\n");
			}
			tooltip.append(Component.translatable(RTFTranslationKeys.GUI_ENUM_OLD_NAME, e.name()).withStyle(ChatFormatting.GRAY));
		}
		return text.isEmpty() && !(value instanceof Enum<?>) ? null : tooltip;
	}
	
	private static String enumKey(Enum<?> value) {
		return RTFTranslationKeys.GUI_ENUM_PREFIX + value.getDeclaringClass().getSimpleName().toLowerCase(Locale.ROOT) + "." + value.name().toLowerCase(Locale.ROOT);
	}
	
	// an enum value by its translation (reterraforged.gui.enum.<enum>.<value>, e.g. MULTI_IMPROVED -> "Several continents
	// (improved)"); a value without one as words ("Multi improved") instead of the raw constant
	public static Component enumName(Enum<?> value) {
		String key = enumKey(value);
		if (Language.getInstance().has(key)) {
			return Component.translatable(key);
		}
		String words = value.name().replace('_', ' ').toLowerCase(Locale.ROOT);
		return Component.literal(Character.toUpperCase(words.charAt(0)) + words.substring(1));
	}

	// the same as CycleButton.booleanBuilder, through cycle so the switch is a setting like the others
	public static CycleButton<Boolean> createToggle(boolean initial, String text, CycleButton.OnValueChange<Boolean> callback) {
		Component on = Component.translatable(RTFTranslationKeys.GUI_BUTTON_TRUE);
		Component off = Component.translatable(RTFTranslationKeys.GUI_BUTTON_FALSE);
		return createNamedCycle(ImmutableList.of(Boolean.TRUE, Boolean.FALSE), initial, text, (value) -> value ? on : off, callback);
	}

	// a setting another one switches off (the island sliders while Enable Archipelago is off): greyed out, and its
	// tooltip says what it needs
	public static void setUsable(AbstractWidget widget, String text, boolean usable, String requirement) {
		widget.active = usable;
		MutableComponent tooltip = Component.translatable(Tooltips.translationKey(text));
		if (!usable) {
			tooltip.append("\n\n").append(Component.translatable(requirement).withStyle(ChatFormatting.GRAY));
		}
		SettingControl control = SettingControl.of(widget);
		widget.setTooltip(control != null ? control.tooltip(tooltip) : Tooltip.create(tooltip));
	}
	

	public static Button createThrowingButton(String text, Toasts.ThrowingRunnable run) {
		return Button.builder(Component.translatable(text), (b) -> {
			Toasts.tryOrToast(Tooltips.failTranslationKey(text), run);
		}).build();
	}
	
	public static <T> ValueButton<T> createValueButton(String text, Button.OnPress onPress, T initial) {
		ValueButton<T> button = new ValueButton<>(-1, -1, -1, -1, Component.translatable(text), onPress, initial);
		button.setTooltip(Tooltips.create(Tooltips.translationKey(text)));
		return button;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static ValueButton<Integer> createRandomButton(String text, int initial, Consumer<Integer> onPress) {
		ValueControl control = new ValueControl(text);
		Consumer<Integer> changed = (value) -> {
			onPress.accept(value);
			control.changed();
		};
		ValueButton<Integer> button = createValueButton(text, (b) -> {
			if(b instanceof ValueButton valueButton) {
				valueButton.setValue(ThreadLocalRandom.current().nextInt());
				changed.accept((Integer) valueButton.getValue());
			}
		}, initial);
		control.button = button;
		control.callback = changed;
		SettingControl.attach(button, control);
		button.setTooltip(control.tooltip(Component.translatable(Tooltips.translationKey(text))));
		return button;
	}

	// a slider's value as shown, and its position on the slider with the slider's range (the structure page's sliders
	// reach further for a sparse set of another mod)
	private record SliderValue(double position, double min, double max) {
	}

	private static final class SliderControl extends SettingControl {
		private final Slider slider;

		private SliderControl(String key, Slider slider) {
			super(key);
			this.slider = slider;
		}

		@Override
		AbstractWidget widget() {
			return this.slider;
		}

		@Override
		Object state() {
			return this.slider.getValueString();
		}

		@Override
		Component valueText() {
			return this.slider.getValueText();
		}

		@Override
		Object value() {
			return new SliderValue(this.slider.getValue(), this.slider.getMin(), this.slider.getMax());
		}

		@Override
		void restore(Object value) {
			SliderValue to = (SliderValue) value;
			if (to.min() == this.slider.getMin() && to.max() == this.slider.getMax()) {
				this.slider.moveTo(to.position());
			} else {
				double real = Mth.lerp(to.position(), to.min(), to.max());
				this.slider.moveTo((real - this.slider.getMin()) / (this.slider.getMax() - this.slider.getMin()));
			}
		}
	}

	private static final class CycleControl<T> extends SettingControl {
		private final Function<T, Component> valueName;
		private CycleButton<T> button;
		private CycleButton.OnValueChange<T> callback;

		private CycleControl(String key, Function<T, Component> valueName) {
			super(key);
			this.valueName = valueName;
		}

		@Override
		AbstractWidget widget() {
			return this.button;
		}

		@Override
		Object state() {
			return this.button.getValue();
		}

		@Override
		Component valueText() {
			return this.valueName.apply(this.button.getValue());
		}

		@Override
		Object value() {
			return this.button.getValue();
		}

		@SuppressWarnings("unchecked")
		@Override
		void restore(Object value) {
			// setValue does not call the callback, a click does
			this.button.setValue((T) value);
			this.callback.onValueChange(this.button, (T) value);
		}
	}

	private static final class ValueControl extends SettingControl {
		private ValueButton<Integer> button;
		private Consumer<Integer> callback;

		private ValueControl(String key) {
			super(key);
		}

		@Override
		AbstractWidget widget() {
			return this.button;
		}

		@Override
		Object state() {
			return this.button.getValue();
		}

		@Override
		Component valueText() {
			return Component.literal(String.valueOf(this.button.getValue()));
		}

		@Override
		Object value() {
			return this.button.getValue();
		}

		@Override
		void restore(Object value) {
			this.button.setValue((Integer) value);
			this.callback.accept((Integer) value);
		}
	}
}
