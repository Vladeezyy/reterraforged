package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.util.Locale;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.client.gui.Toasts;
import raccoonman.reterraforged.client.gui.Tooltips;
import raccoonman.reterraforged.client.gui.widget.Label;
import raccoonman.reterraforged.client.gui.widget.Slider;
import raccoonman.reterraforged.client.gui.widget.Slider.Format;
import raccoonman.reterraforged.client.gui.widget.ValueButton;

@Deprecated
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
		slider.setTooltip(Tooltips.create(Tooltips.translationKey(text)));
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
		CycleButton.Builder<T> builder = CycleButton.<T>builder((e) -> {
			return e instanceof Enum<?> value ? enumName(value) : Component.literal(name.apply(e));
		}, initial).withValues(values);
		if(text.isEmpty()) {
			builder = builder.displayOnlyValue();
		}
		CycleButton<T> button = builder.create(-1, -1, -1, -1, text.map(Component::translatable).orElse(null), callback);
		text.ifPresent((key) -> {
			button.setTooltip(Tooltips.create(Tooltips.translationKey(key)));
		});
		return button;
	}
	
	// an enum value by its translation (reterraforged.gui.enum.<enum>.<value>, e.g. MULTI_IMPROVED -> "Several continents
	// (improved)"); a value without one as words ("Multi improved") instead of the raw constant
	public static Component enumName(Enum<?> value) {
		String key = RTFTranslationKeys.GUI_ENUM_PREFIX + value.getDeclaringClass().getSimpleName().toLowerCase(Locale.ROOT) + "." + value.name().toLowerCase(Locale.ROOT);
		if (Language.getInstance().has(key)) {
			return Component.translatable(key);
		}
		String words = value.name().replace('_', ' ').toLowerCase(Locale.ROOT);
		return Component.literal(Character.toUpperCase(words.charAt(0)) + words.substring(1));
	}

	public static CycleButton<Boolean> createToggle(boolean initial, String text, CycleButton.OnValueChange<Boolean> callback) {
		CycleButton<Boolean> button = CycleButton.booleanBuilder(Component.translatable(RTFTranslationKeys.GUI_BUTTON_TRUE), Component.translatable(RTFTranslationKeys.GUI_BUTTON_FALSE), initial).create(-1, -1, -1, -1, Component.translatable(text), callback);
		button.setTooltip(Tooltips.create(Tooltips.translationKey(text)));
		return button;
	}
	
	// a setting the world generation does not use (it did nothing in ReTerraForged 0.0.6 either): shown, not usable
	public static <T extends AbstractWidget> T unused(T widget) {
		widget.active = false;
		widget.setTooltip(Tooltips.create(RTFTranslationKeys.GUI_TOOLTIP_NOT_USED));
		return widget;
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
	
	@SuppressWarnings("unchecked")
	public static ValueButton<Integer> createRandomButton(String text, int initial, Consumer<Integer> onPress) {
		return createValueButton(text, (button) -> {
			if(button instanceof ValueButton valueButton) {
				valueButton.setValue(ThreadLocalRandom.current().nextInt());
				onPress.accept((Integer) valueButton.getValue());
			}
		}, initial);
	}
}
