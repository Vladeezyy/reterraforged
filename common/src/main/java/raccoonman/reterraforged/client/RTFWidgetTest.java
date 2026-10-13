package raccoonman.reterraforged.client;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.StringRepresentable;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.client.gui.screen.page.LinkedPageScreen;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetConfigScreen;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetEditorPage;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetListPage.PresetEntry;
import raccoonman.reterraforged.client.gui.widget.Label;
import raccoonman.reterraforged.client.gui.widget.Slider;
import raccoonman.reterraforged.client.gui.widget.ValueButton;
import raccoonman.reterraforged.client.gui.widget.WidgetList;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;

/**
 * Widget check for tools/clienttest.sh &lt;loader&gt; widgets (-Drtf.clienttest=widgets): every slider, switch and
 * button of every preset settings page is used with the mouse, as a player does (sliders clicked at a fifth and at four
 * fifths of their width). After each use the preset is compared with the one before: exactly one setting must change,
 * to the value the widget shows, and the setting's name should be the widget's. Logs whether the preview was drawn
 * again and changed. At the end Done applies the preset, and the preset in the new world's registries must be the
 * edited one, setting by setting. The table of all widgets goes to rtf-widgets.txt in the game folder.
 */
public final class RTFWidgetTest {
	private static final List<String> REPORT = new ArrayList<>();
	private static final List<String> WARNINGS = new ArrayList<>();
	private static int step;
	private static int wait;
	private static int timeout;
	private static List<AbstractWidget> queue = List.of();
	private static int index;
	private static int sub;
	private static int widgets;
	private static Map<String, String> before;
	private static Map<String, String> edited;
	private static double sliderBefore;
	private static String shownBefore;
	private static Object cycleStart;
	private static int cycleClicks;
	private static int generationBefore;
	private static int hashBefore;

	private RTFWidgetTest() {
	}

	public static void tick(Minecraft mc) {
		if (wait > 0) {
			wait--;
			return;
		}
		switch (step) {
			case 0 -> {
				if (mc.gui.screen() instanceof TitleScreen title) {
					CreateWorldScreen.openFresh(mc, () -> mc.gui.setScreen(title));
					next(60);
				}
			}
			case 1 -> {
				if (!RTFGuiTest.click(mc, (w) -> RTFGuiTest.hasKey(w, "createWorld.tab.world.title"), 0.5)) RTFGuiTest.fail("no World tab");
				next(10);
			}
			case 2 -> {
				if (!RTFGuiTest.click(mc, (w) -> RTFGuiTest.hasKey(w, "selectWorld.customizeType") && w.active, 0.5)) RTFGuiTest.fail("no active Customize button");
				next(40);
			}
			case 3 -> {
				if (!RTFGuiTest.click(mc, (w) -> w instanceof PresetEntry && RTFGuiTest.hasKey(w, RTFTranslationKeys.GUI_DEFAULT_PRESET_NAME), 0.3)) {
					RTFGuiTest.fail("no Default preset in the list on " + RTFGuiTest.name(mc.gui.screen()));
					finish(mc);
					return;
				}
				next(10);
			}
			case 4 -> {
				RTFGuiTest.click(mc, ((PresetConfigScreen) mc.gui.screen()).nextButton, 0.5);
				next(100);
			}
			case 5 -> {
				// a settings page: every widget of its left column, in order
				queue = new ArrayList<>();
				for (WidgetList.Entry<AbstractWidget> entry : left(mc).children()) {
					if (!(entry.getWidget() instanceof Label)) queue.add(entry.getWidget());
				}
				index = 0;
				sub = 0;
				REPORT.add("");
				REPORT.add("== " + RTFGuiTest.page(mc) + " (" + queue.size() + " widgets)");
				RTFGuiTest.log("{}: {} widgets", RTFGuiTest.page(mc), queue.size());
				step = 6;
			}
			case 6 -> widget(mc);
			case 7 -> {
				edited = flatten(RTFGuiTest.preset(mc));
				RTFGuiTest.click(mc, ((LinkedPageScreen) mc.gui.screen()).doneButton, 0.5);
				timeout = 600;
				next(20);
			}
			case 8 -> {
				// the preset the new world will be made with is the edited one, every setting of it
				var applied = RTFGuiTest.appliedPreset(mc);
				if (applied.isEmpty()) {
					if (--timeout <= 0) {
						RTFGuiTest.fail("Done did not apply the preset: " + RTFGuiTest.name(mc.gui.screen()));
						finish(mc);
					}
					return;
				}
				Map<String, String> world = flatten(applied.get());
				List<String> lost = new ArrayList<>();
				for (String path : edited.keySet()) {
					if (!Objects.equals(edited.get(path), world.get(path))) lost.add(path + " " + edited.get(path) + " -> " + world.get(path));
				}
				RTFGuiTest.log("applied preset: {} settings, {} differ from the screen", edited.size(), lost.size());
				if (!lost.isEmpty()) RTFGuiTest.fail("settings changed on the way to the world: " + lost);
				REPORT.add("");
				REPORT.add("applied: " + edited.size() + " settings, " + (lost.isEmpty() ? "all as on the screen" : "differ: " + lost));
				finish(mc);
			}
			default -> {
			}
		}
	}

	private static void widget(Minecraft mc) {
		LinkedPageScreen screen = (LinkedPageScreen) mc.gui.screen();
		if (index >= queue.size()) {
			RTFGuiTest.shoot(mc, "widgets-" + RTFGuiTest.page(mc));
			if (screen.nextButton.active) {
				RTFGuiTest.click(mc, screen.nextButton, 0.5);
				wait = 60;
				step = 5;
			} else {
				step = 7;
			}
			return;
		}
		AbstractWidget widget = queue.get(index);
		switch (sub) {
			case 0 -> {
				// scrolled into view first: a widget gets its place on the screen when it is drawn
				left(mc).reveal(widget);
				sub = 1;
				wait = 2;
			}
			case 1 -> {
				// switched off by another setting (continent shape for the improved continents...): not used
				if (!widget.active) {
					REPORT.add(String.format(Locale.ROOT, "  %-34s inactive (%s)", label(widget), shown(widget)));
					index++;
					sub = 0;
					return;
				}
				cycleStart = widget instanceof CycleButton<?> cycle ? cycle.getValue() : null;
				cycleClicks = 0;
				use(mc, widget, 0.2);
				sub = 2;
				wait = 2;
			}
			case 2 -> {
				check(mc, widget, 0.2);
				if (widget instanceof Slider) {
					use(mc, widget, 0.8);
					sub = 3;
					wait = 2;
				} else if (widget instanceof CycleButton<?> cycle && !Objects.equals(cycle.getValue(), cycleStart) && ++cycleClicks < 12) {
					// a switch goes round all its values back to the first one, so what it switches off is used too
					use(mc, widget, 0.5);
					wait = 2;
				} else {
					index++;
					sub = 0;
				}
			}
			case 3 -> {
				check(mc, widget, 0.8);
				index++;
				sub = 0;
			}
			default -> {
			}
		}
	}

	private static void use(Minecraft mc, AbstractWidget widget, double fraction) {
		PresetEditorPage page = (PresetEditorPage) ((LinkedPageScreen) mc.gui.screen()).getCurrentPage();
		before = flatten(RTFGuiTest.preset(mc));
		sliderBefore = widget instanceof Slider slider ? slider.getValue() : 0;
		shownBefore = shown(widget);
		generationBefore = page.previewGeneration();
		hashBefore = page.previewHash();
		widgets++;
		RTFGuiTest.click(mc, widget, widget instanceof Slider ? fraction : 0.5);
	}

	private static void check(Minecraft mc, AbstractWidget widget, double fraction) {
		PresetEditorPage page = (PresetEditorPage) ((LinkedPageScreen) mc.gui.screen()).getCurrentPage();
		Map<String, String> after = flatten(RTFGuiTest.preset(mc));
		// both ways: a setting at its codec default is left out of the JSON
		Set<String> paths = new LinkedHashSet<>(before.keySet());
		paths.addAll(after.keySet());
		List<String> changed = new ArrayList<>();
		for (String path : paths) {
			if (!Objects.equals(before.get(path), after.get(path))) changed.add(path);
		}
		String label = label(widget);
		String where = RTFGuiTest.page(mc) + " '" + label + "'";
		String preview = page.previewGeneration() == generationBefore ? "preview not redrawn" : page.previewHash() == hashBefore ? "preview redrawn, same picture" : "preview changed";
		String shown = shown(widget);

		if (changed.size() > 1) {
			RTFGuiTest.fail(where + " changed several settings: " + changed);
		}
		if (changed.isEmpty()) {
			// a slider held by its neighbours (control points, spacing above separation), or on the same whole number
			boolean same = shown.equals(shownBefore);
			if (!same) RTFGuiTest.fail(where + " shows " + shownBefore + " -> " + shown + " but changed no setting");
			REPORT.add(String.format(Locale.ROOT, "  %-34s %-6s %-14s (no change: %s) %s", label, at(widget, fraction), shown, same ? "same value as before" : "BROKEN", preview));
			return;
		}
		String path = changed.get(0);
		// left out of the JSON: back at the codec's default, which the widget then shows
		String value = after.getOrDefault(path, shown);
		List<String> notes = new ArrayList<>();
		// the widget shows the value the setting got
		if (!sameValue(widget, shown, value)) {
			RTFGuiTest.fail(where + " shows " + shown + " but " + path + " is " + value);
			notes.add("SHOWS " + shown + " BUT IS " + value);
		}
		// and is named like the setting
		String key = key(widget);
		if (key != null && !sameName(key, path)) {
			WARNINGS.add(where + " (" + key + ") changes " + path);
			notes.add("NAME?");
		}
		REPORT.add(String.format(Locale.ROOT, "  %-34s %-6s %-14s -> %-55s %s %s", label, at(widget, fraction), shown, path + " = " + value, preview, String.join(" ", notes)));
	}

	private static String at(AbstractWidget widget, double fraction) {
		return widget instanceof Slider ? (int) (fraction * 100) + "%" : "click";
	}

	// the value the widget shows
	private static String shown(AbstractWidget widget) {
		// as the codec writes it (enums by their serialized name)
		if (widget instanceof CycleButton<?> cycle) return cycle.getValue() instanceof StringRepresentable value ? value.getSerializedName() : String.valueOf(cycle.getValue());
		if (widget instanceof ValueButton<?> button) return String.valueOf(button.getValue());
		String message = widget.getMessage().getString();
		int colon = message.lastIndexOf(": ");
		return colon >= 0 ? message.substring(colon + 2) : message;
	}

	private static boolean sameValue(AbstractWidget widget, String shown, String value) {
		if (widget instanceof Slider) {
			try {
				return Math.abs(Double.parseDouble(shown) - Double.parseDouble(value)) <= 0.0006;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return normalize(shown).equals(normalize(value));
	}

	// the setting the widget is named after: the last part of its translation key ("...slider.continentScale")
	private static String key(AbstractWidget widget) {
		Component name = widget.getMessage();
		if (name.getContents() instanceof TranslatableContents contents && contents.getKey().equals("options.generic_value") && contents.getArgs().length > 0 && contents.getArgs()[0] instanceof Component first) {
			name = first;
		}
		if (name.getContents() instanceof TranslatableContents contents && contents.getKey().startsWith("reterraforged.")) {
			String key = contents.getKey();
			return key.substring(key.lastIndexOf('.') + 1);
		}
		return null;
	}

	private static String label(AbstractWidget widget) {
		String message = widget.getMessage().getString();
		int colon = message.lastIndexOf(": ");
		return colon >= 0 ? message.substring(0, colon) : message;
	}

	private static boolean sameName(String key, String path) {
		String leaf = path.substring(path.lastIndexOf('.') + 1);
		return normalize(leaf).contains(normalize(key)) || normalize(key).contains(normalize(leaf));
	}

	private static String normalize(String text) {
		return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.-]", "");
	}

	@SuppressWarnings("unchecked")
	private static WidgetList<AbstractWidget> left(Minecraft mc) {
		for (GuiEventListener child : mc.gui.screen().children()) {
			if (child instanceof WidgetList<?> list) return (WidgetList<AbstractWidget>) list;
		}
		throw new IllegalStateException("no widget list on " + RTFGuiTest.name(mc.gui.screen()));
	}

	// the preset as setting path -> value, from its codec (what is saved and exported)
	private static Map<String, String> flatten(Preset preset) {
		Map<String, String> values = new LinkedHashMap<>();
		flatten(Preset.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, preset).getOrThrow(), "", values);
		return values;
	}

	private static void flatten(JsonElement element, String path, Map<String, String> values) {
		if (element.isJsonObject()) {
			element.getAsJsonObject().entrySet().forEach((e) -> flatten(e.getValue(), path.isEmpty() ? e.getKey() : path + "." + e.getKey(), values));
		} else if (element.isJsonArray()) {
			for (int i = 0; i < element.getAsJsonArray().size(); i++) flatten(element.getAsJsonArray().get(i), path + "[" + i + "]", values);
		} else {
			values.put(path, element.isJsonNull() ? "null" : element.getAsString());
		}
	}

	private static void finish(Minecraft mc) {
		REPORT.add(0, widgets + " widget uses, " + RTFGuiTest.FAILURES.size() + " failures, " + WARNINGS.size() + " names to look at");
		REPORT.add("");
		REPORT.add("names to look at:");
		WARNINGS.forEach((w) -> REPORT.add("  " + w));
		REPORT.add("");
		REPORT.add("failures:");
		RTFGuiTest.FAILURES.forEach((f) -> REPORT.add("  " + f));
		try {
			Files.write(mc.gameDirectory.toPath().resolve("rtf-widgets.txt"), REPORT);
		} catch (IOException e) {
			RTFGuiTest.fail("could not write rtf-widgets.txt: " + e);
		}
		RTFGuiTest.log("{} widget uses, {}", widgets, RTFGuiTest.FAILURES.isEmpty() ? "all checks passed" : RTFGuiTest.FAILURES.size() + " failed");
		RTFGuiTest.log("done");
		mc.stop();
	}

	private static void next(int ticks) {
		step++;
		wait = ticks;
	}
}
