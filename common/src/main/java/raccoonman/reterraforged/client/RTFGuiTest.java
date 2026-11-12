package raccoonman.reterraforged.client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.client.gui.screen.createworld.RTFCreateWorld;
import raccoonman.reterraforged.client.gui.screen.page.LinkedPageScreen;
import raccoonman.reterraforged.client.gui.screen.presetconfig.BuiltinPresets;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetConfigScreen;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetEditorPage;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetListPage;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetListPage.PresetEntry;
import raccoonman.reterraforged.client.gui.screen.presetconfig.RenderMode;
import raccoonman.reterraforged.client.gui.widget.WidgetList;
import raccoonman.reterraforged.data.worldgen.preset.settings.ContinentType;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;

/**
 * GUI check for tools/clienttest.sh &lt;loader&gt; gui (-Drtf.clienttest=gui): makes a world the way a player does, with
 * mouse clicks: Create New World, the World tab, Customize, a click on the "Default" preset, ">>", a click on the
 * continent scale slider, every settings page forward and back, Done; then Customize once more with another continent
 * scale (a preset applied a second time) and Create New World. Checks that the click selects the preset, that ">>" and
 * "<<" go one page, that the applied preset is the edited one each time and that the new world is built by
 * ReTerraForged with the last value and its features. Screenshots of every screen. Called every client tick.
 */
public final class RTFGuiTest {
	private static final List<String> EXPECTED_PAGES = List.of("WorldSettingsPage", "SurfaceSettingsPage", "CaveSettingsPage", "ClimateSettingsPage", "TerrainSettingsPage", "FakeWaterBiomeSettingsPage", "IslandSettingsPage", "RiverSettingsPage", "FilterSettingsPage", "StructureSettingsPage", "MiscellaneousPage");
	static final List<String> FAILURES = new ArrayList<>();
	private static final List<String> PAGES = new ArrayList<>();
	private static final Map<String, Double> SCROLLS = new HashMap<>();
	private static int step;
	private static int wait;
	private static int shot;
	private static int round;
	private static int expectedScale;
	// the third round leaves with Cancel: the seed of the create world screen before it, and it must be there after
	private static String seedBeforeCancel;
	private static int timeout;
	private static int readyWait;
	// the shot of the preset list with a new preset's map: 0 to do, 1 waiting for the map, 2 done
	private static int infoState;
	private static int infoWait;
	// tooltips to see on the screenshots: an enum option (what it does, its old name), greyed-out sliders (what they
	// need), the structure placement seed
	private static final List<TooltipShot> TOOLTIP_SHOTS = List.of(
		new TooltipShot("WorldSettingsPage", "continent-type", (w) -> w instanceof CycleButton<?> cycle && cycle.getValue() instanceof ContinentType),
		new TooltipShot("WorldSettingsPage", "render-mode", (w) -> w instanceof CycleButton<?> cycle && cycle.getValue() instanceof RenderMode),
		new TooltipShot("IslandSettingsPage", "island-density", (w) -> startsWith(w, RTFTranslationKeys.GUI_SLIDER_ISLAND_DENSITY)),
		new TooltipShot("StructureSettingsPage", "placement-seed", (w) -> startsWith(w, RTFTranslationKeys.GUI_BUTTON_SALT)),
		// the five tooltips whose texts were lost once (0.0.22 to 0.0.31), the longest of the screen; down their pages, in the
		// pages' order from the top (the page is only scrolled down to a widget)
		new TooltipShot("RiverSettingsPage", "lake-min-start-distance", (w) -> startsWith(w, RTFTranslationKeys.GUI_SLIDER_LAKE_MIN_START_DISTANCE)),
		new TooltipShot("RiverSettingsPage", "lake-max-start-distance", (w) -> startsWith(w, RTFTranslationKeys.GUI_SLIDER_LAKE_MAX_START_DISTANCE)),
		new TooltipShot("MiscellaneousPage", "plain-stone-erosion", (w) -> startsWith(w, RTFTranslationKeys.GUI_BUTTON_PLAIN_STONE_EROSION)),
		new TooltipShot("MiscellaneousPage", "mountain-biome-usage", (w) -> startsWith(w, RTFTranslationKeys.GUI_SLIDER_MOUNTAIN_BIOME_USAGE)),
		new TooltipShot("MiscellaneousPage", "volcano-biome-usage", (w) -> startsWith(w, RTFTranslationKeys.GUI_SLIDER_VOLCANO_BIOME_USAGE))
	);
	// the wheel steps a page was scrolled by for a tooltip's widget
	private static int tooltipScrolls;
	private static final Set<String> TOOLTIPS_SHOT = new HashSet<>();
	@Nullable
	private static String tooltipShot;

	private record TooltipShot(String page, String name, Predicate<AbstractWidget> widget) {
	}

	private RTFGuiTest() {
	}

	// the create world screen with the generation on: it comes after reloads of the datapacks, which take as long as they
	// take. A fixed wait failed on a busy machine ("no World tab"), with the message of the reload still on the screen
	static boolean createWorldReady(Minecraft mc) {
		// and no export of its preset on the way (it runs in the background)
		if (mc.screen instanceof CreateWorldScreen screen && RTFCreateWorld.isEnabled(screen) && !RTFCreateWorld.isExporting(screen)) {
			readyWait = 0;
			return true;
		}
		if (++readyWait <= 1200) return false;
		// a minute: the next step fails with what is on the screen
		readyWait = 0;
		return true;
	}

	// the mouse to a point of the window (in pixels): the screens read its position from MouseHandler for hovering
	private static void moveMouse(Minecraft mc, double x, double y) {
		try {
			Field xpos = MouseHandler.class.getDeclaredField("xpos"), ypos = MouseHandler.class.getDeclaredField("ypos");
			xpos.setAccessible(true);
			ypos.setAccessible(true);
			xpos.setDouble(mc.mouseHandler, x);
			ypos.setDouble(mc.mouseHandler, y);
		} catch (ReflectiveOperationException e) {
			fail("cannot move the mouse: " + e);
		}
	}

	public static void tick(Minecraft mc) {
		if (wait > 0) {
			wait--;
			return;
		}
		switch (step) {
			case 0 -> {
				if (mc.screen instanceof TitleScreen title) {
					CreateWorldScreen.openFresh(mc, () -> mc.setScreen(title));
					next(60);
				} else if (mc.screen != null && mc.screen.getClass().getSimpleName().equals("LoadingErrorScreen")) {
					// NeoForge: mods with loading warnings (the deprecated logoFile of other mods) show a screen that waits for
					// "Proceed", and a click on it did not work
					log("closing {}", name(mc.screen));
					mc.setScreen(new TitleScreen());
				}
			}
			case 1 -> {
				if (!createWorldReady(mc)) return;
				// the World tab of the create world screen, then its Customize button (opens the preset screen)
				if (!click(mc, (w) -> hasKey(w, "createWorld.tab.world.title"), 0.5)) fail("no World tab");
				next(10);
			}
			case 2 -> {
				shoot(mc, "gui-" + round + "-create-world");
				if (round == 2 && mc.screen instanceof CreateWorldScreen create) seedBeforeCancel = create.getUiState().getSeed();
				if (!click(mc, (w) -> hasKey(w, "selectWorld.customizeType") && w.active, 0.5)) fail("no active Customize button");
				next(40);
			}
			case 3 -> {
				if (!(mc.screen instanceof PresetConfigScreen)) {
					fail("Customize did not open the preset screen: " + name(mc.screen));
					finish(mc);
					return;
				}
				if (infoState == 1) {
					// the first round: a new preset's map and description next to the list, then Default as always
					PresetListPage list = (PresetListPage) ((PresetConfigScreen) mc.screen).getCurrentPage();
					if (!list.infoMapShown() && !list.infoMapFailed() && ++infoWait < 400) return;
					log("preset map of Towering Peaks after {} ticks: {}", infoWait, list.infoDiagnosis());
					if (!list.infoMapShown()) fail("the preset list shows no map of the selected preset: " + list.infoDiagnosis());
					if (!I18n.exists(BuiltinPresets.descriptionKey("toweringPeaks"))) fail("no description of Towering Peaks in the language file");
					shoot(mc, "gui-preset-info");
					infoState = 2;
				} else {
					shoot(mc, "gui-" + round + "-presets");
					if (round == 0 && infoState == 0) {
						if (!selectBuiltin(mc.screen, "toweringPeaks")) fail("no Towering Peaks preset in the list");
						infoState = 1;
						infoWait = 0;
						wait = 2;
						return;
					}
				}
				if (!click(mc, (w) -> w instanceof PresetEntry && hasKey(w, RTFTranslationKeys.GUI_DEFAULT_PRESET_NAME), 0.3)) fail("no Default preset in the list");
				next(10);
			}
			case 4 -> {
				PresetEntry selected = selectedPreset(mc.screen);
				log("selected after the click: {}", selected != null ? selected.getName().getString() : "nothing");
				if (selected == null) fail("a click on a preset does not select it");
				shoot(mc, "gui-" + round + "-preset-clicked");
				click(mc, ((PresetConfigScreen) mc.screen).nextButton, 0.5);
				next(100);
			}
			case 5 -> {
				String page = page(mc);
				if (!page.equals("WorldSettingsPage")) {
					fail(">> on the preset list opened " + page);
					finish(mc);
					return;
				}
				// a click at a quarter (first round), three quarters (second round) or the middle (third round, which Cancel
				// undoes, with the preview's seed button) of the continent scale slider
				int before = preset(mc).world().continent.continentScale;
				if (round == 3) {
					// the screen opened again: the edit Cancel threw away is not in the preset either
					if (before != expectedScale) fail("Cancel kept the edit in the preset: continent scale " + before + ", it was " + expectedScale);
					next(100);
					return;
				}
				if (!click(mc, (w) -> startsWith(w, RTFTranslationKeys.GUI_SLIDER_CONTINENT_SCALE), round == 0 ? 0.25 : round == 1 ? 0.75 : 0.5)) fail("no continent scale slider");
				int clicked = preset(mc).world().continent.continentScale;
				log("continent scale {} -> {} (slider click)", before, clicked);
				if (clicked == before) fail("the continent scale slider click changed nothing");
				if (round < 2) {
					expectedScale = clicked;
				} else if (!click(mc, (w) -> startsWith(w, RTFTranslationKeys.GUI_BUTTON_SEED), 0.5)) {
					fail("no seed button");
				}
				next(100);
			}
			case 6 -> {
				shoot(mc, "gui-" + round + "-continent-scale");
				if (round == 0) {
					PAGES.clear();
					PAGES.add(page(mc));
					step = 7;
				} else {
					step = 9;
				}
			}
			case 7 -> {
				// every page forward with ">>", each scrolled down with the mouse wheel first; on the way a screenshot
				// of some tooltips (the mouse on the widget, the shot a few ticks later)
				LinkedPageScreen screen = (LinkedPageScreen) mc.screen;
				if (tooltipShot != null) {
					shoot(mc, tooltipShot);
					tooltipShot = null;
					moveMouse(mc, 0, 0);
				}
				for (TooltipShot tooltip : TOOLTIP_SHOTS) {
					if (tooltip.page().equals(page(mc)) && TOOLTIPS_SHOT.add(tooltip.name())) {
						AbstractWidget widget = find(mc.screen, tooltip.widget());
						if (widget == null) {
							fail("no widget for the tooltip " + tooltip.name() + " on " + page(mc));
							continue;
						}
						// a widget down the page is not drawn and has no place yet: a wheel step down, and again after the next frames
						WidgetList<?> list = settingsList(mc);
						if (list != null && (widget.getY() < list.getY() || widget.getY() + widget.getHeight() > list.getY() + list.getHeight())) {
							if (++tooltipScrolls > 60) {
								fail("the widget of the tooltip " + tooltip.name() + " does not come into view on " + page(mc));
								tooltipScrolls = 0;
								continue;
							}
							mc.screen.mouseScrolled(list.getX() + list.getWidth() / 2.0, list.getY() + list.getHeight() / 2.0, 0, -1);
							TOOLTIPS_SHOT.remove(tooltip.name());
							wait = 2;
							return;
						}
						tooltipScrolls = 0;
						double scale = mc.getWindow().getGuiScale();
						moveMouse(mc, (widget.getX() + widget.getWidth() / 2.0) * scale, (widget.getY() + widget.getHeight() / 2.0) * scale);
						tooltipShot = "gui-tooltip-" + tooltip.name();
						wait = 5;
						return;
					}
				}
				if (screen.nextButton.active) {
					scrollDown(mc);
					SCROLLS.put(page(mc), screen.getCurrentPage().getScroll());
					click(mc, screen.nextButton, 0.5);
					PAGES.add(page(mc));
					wait = 5;
				} else {
					log("pages with >>: {}", PAGES);
					if (!PAGES.equals(EXPECTED_PAGES)) fail(">> goes " + PAGES + ", expected " + EXPECTED_PAGES);
					shoot(mc, "gui-last-page");
					PAGES.clear();
					PAGES.add(page(mc));
					step = 8;
				}
			}
			case 8 -> {
				// and back with "<<" up to the first settings page
				LinkedPageScreen screen = (LinkedPageScreen) mc.screen;
				if (!page(mc).equals("WorldSettingsPage") && screen.previousButton.active && PAGES.size() < 20) {
					click(mc, screen.previousButton, 0.5);
					PAGES.add(page(mc));
					// back on the page where it was scrolled to
					double scroll = screen.getCurrentPage().getScroll(), was = SCROLLS.getOrDefault(page(mc), 0.0);
					if (Math.abs(scroll - was) > 1) fail("<< to " + page(mc) + " is scrolled to " + (int) scroll + ", it was left at " + (int) was);
					wait = 5;
				} else {
					log("pages with <<: {}, scrolled when left: {}", PAGES, SCROLLS);
					if (!PAGES.equals(EXPECTED_PAGES.reversed())) fail("<< goes " + PAGES + ", expected " + EXPECTED_PAGES.reversed());
					int scale = preset(mc).world().continent.continentScale;
					if (scale != expectedScale) fail("the continent scale was lost on the way through the pages: " + scale + ", set " + expectedScale);
					step = 40;
				}
			}
			// settings that are not Default's, on the World page: a slider moved off Default is marked, a right click puts
			// it back; then a search for a part of a setting's name and an empty one. Then Done (step 9)
			case 40 -> {
				AbstractWidget jitter = find(mc.screen, (w) -> startsWith(w, RTFTranslationKeys.GUI_SLIDER_CONTINENT_JITTER));
				if (jitter == null) {
					fail("no continent jitter slider");
					step = 9;
					return;
				}
				@SuppressWarnings("unchecked") WidgetList<AbstractWidget> list = (WidgetList<AbstractWidget>) settingsList(mc);
				list.reveal(jitter);
				next(5);
			}
			case 41 -> {
				AbstractWidget jitter = find(mc.screen, (w) -> startsWith(w, RTFTranslationKeys.GUI_SLIDER_CONTINENT_JITTER));
				PresetEditorPage page = (PresetEditorPage) ((LinkedPageScreen) mc.screen).getCurrentPage();
				float before = preset(mc).world().continent.continentJitter;
				click(mc, jitter, 0.9);
				log("continent jitter {} -> {} (slider click), marked {}", before, preset(mc).world().continent.continentJitter, page.isChanged(jitter));
				if (preset(mc).world().continent.continentJitter == before) fail("the continent jitter slider click changed nothing");
				if (!page.isChanged(jitter)) fail("a slider moved off Default's value is not marked");
				if (!page.hasChanges(page.getClass())) fail("the World page with changed settings is not marked");
				// the mouse on it for the shot: its tooltip has Default's value
				double scale = mc.getWindow().getGuiScale();
				moveMouse(mc, (jitter.getX() + jitter.getWidth() / 2.0) * scale, (jitter.getY() + jitter.getHeight() / 2.0) * scale);
				next(10);
			}
			case 42 -> {
				shoot(mc, "gui-changed-marker");
				AbstractWidget jitter = find(mc.screen, (w) -> startsWith(w, RTFTranslationKeys.GUI_SLIDER_CONTINENT_JITTER));
				PresetEditorPage page = (PresetEditorPage) ((LinkedPageScreen) mc.screen).getCurrentPage();
				double x = jitter.getX() + jitter.getWidth() / 2.0, y = jitter.getY() + jitter.getHeight() / 2.0;
				MouseButtonEvent rightClick = new MouseButtonEvent(x, y, new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_RIGHT, 0));
				mc.screen.mouseClicked(rightClick, false);
				mc.screen.mouseReleased(rightClick);
				float reset = preset(mc).world().continent.continentJitter, standard = raccoonman.reterraforged.data.worldgen.preset.settings.Presets.makeDefault().world().continent.continentJitter;
				log("continent jitter {} after the right click, Default {}, marked {}", reset, standard, page.isChanged(jitter));
				if (Math.abs(reset - standard) > 1.0E-6F) fail("a right click did not put the continent jitter back to Default: " + reset + ", Default " + standard);
				if (page.isChanged(jitter)) fail("the continent jitter is still marked after the reset");
				moveMouse(mc, 0, 0);
				next(5);
			}
			case 43 -> {
				PresetEditorPage page = (PresetEditorPage) ((LinkedPageScreen) mc.screen).getCurrentPage();
				net.minecraft.client.gui.components.EditBox box = page.searchBox();
				if (box == null) {
					fail("no search field on " + page(mc));
					step = 9;
					return;
				}
				// typed as a player does: a click on the field, then the letters
				String name = I18n.get(RTFTranslationKeys.GUI_SLIDER_CONTINENT_SCALE);
				String query = name.substring(0, Math.min(6, name.length()));
				click(mc, box, 0.5);
				for (char c : query.toCharArray()) mc.screen.charTyped(new net.minecraft.client.input.CharacterEvent(c, 0));
				log("search '{}': '{}' in the field", query, box.getValue());
				if (!box.getValue().equals(query)) fail("the search field took '" + box.getValue() + "', typed '" + query + "'");
				next(5);
			}
			case 44 -> {
				shoot(mc, "gui-search");
				PresetEditorPage page = (PresetEditorPage) ((LinkedPageScreen) mc.screen).getCurrentPage();
				WidgetList<?> list = settingsList(mc);
				String name = I18n.get(RTFTranslationKeys.GUI_SLIDER_CONTINENT_SCALE);
				if (list.children().stream().noneMatch((e) -> e.getWidget().getMessage().getString().contains(name))) fail("the search does not list the continent scale");
				page.searchBox().setValue("");
				if (list.children().stream().noneMatch((e) -> startsWith(e.getWidget(), RTFTranslationKeys.GUI_SLIDER_CONTINENT_SCALE))) fail("an empty search does not bring the page back");
				step = 9;
				wait = 5;
			}
			case 9 -> {
				click(mc, round < 2 ? ((LinkedPageScreen) mc.screen).doneButton : ((LinkedPageScreen) mc.screen).cancelButton, 0.5);
				timeout = 600;
				next(20);
			}
			case 10 -> {
				// the preset datapack is applied: the create world screen is back, with the new worldgen registries
				Optional<Preset> applied = appliedPreset(mc);
				int scale = applied.map((p) -> p.world().continent.continentScale).orElse(-1);
				if (mc.screen instanceof CreateWorldScreen create && scale == expectedScale) {
					log("round {}: the world will use the preset with continent scale {}", round, scale);
					if (round == 2) {
						if (!create.getUiState().getSeed().equals(seedBeforeCancel)) fail("Cancel kept the preview's seed: " + create.getUiState().getSeed() + ", it was " + seedBeforeCancel);
					}
					shoot(mc, "gui-" + round + "-applied");
					round++;
					step = round < 4 ? 1 : 11;
					wait = 20;
				} else if (--timeout <= 0) {
					fail("round " + round + ": the preset screen's " + (round < 2 ? "Done did not apply the edited preset: " : "Cancel changed the preset: ") + (applied.isPresent() ? "continent scale " + scale + ", set " + expectedScale : "no ReTerraForged preset") + ", screen " + name(mc.screen));
					round++;
					step = round < 4 ? 1 : 11;
					wait = 20;
				}
			}
			case 11 -> {
				// the rain check's comparison: the same screens, then vanilla generation
				if (Boolean.getBoolean("rtf.clientrainvanilla") && mc.screen instanceof CreateWorldScreen screen && RTFCreateWorld.isEnabled(screen)) {
					RTFCreateWorld.disable(screen);
					wait = 200;
					return;
				}
				if (!click(mc, (w) -> hasKey(w, "selectWorld.create") && w.active, 0.5)) fail("no Create New World button on " + name(mc.screen));
				timeout = 2400;
				next(20);
			}
			case 12 -> {
				if (mc.player == null) {
					// a question before the world is made (experimental settings): answered as a player does, logged
					if (mc.screen instanceof ConfirmScreen confirm && timeout % 20 == 0) {
						log("{}: {}", name(mc.screen), confirm.getTitle().getString());
						RTFClientTest.confirm(mc.screen);
					}
					if (--timeout <= 0) {
						fail("the new world did not load: " + name(mc.screen));
						finish(mc);
					}
					return;
				}
				MinecraftServer server = mc.getSingleplayerServer();
				server.submit(() -> checkWorld(server)).join();
				if (RTFRainProbe.ENABLED) {
					step = 14;
					return;
				}
				command(mc, "gamemode spectator");
				command(mc, "time set 6000");
				command(mc, "weather clear");
				command(mc, "tp @s ~ ~40 ~ 0 25");
				mc.options.hideGui = true;
				next(500);
			}
			case 13 -> {
				shoot(mc, "gui-world");
				step = 15;
				wait = 20;
			}
			case 14 -> {
				if (RTFRainProbe.tick(mc)) step = 15;
			}
			default -> finish(mc);
		}
	}

	private static void checkWorld(MinecraftServer server) {
		ServerLevel level = server.overworld();
		if ((Object) level.getChunkSource().randomState() instanceof RTFRandomState state && state.generatorContext() != null) {
			int scale = state.preset().world().continent.continentScale;
			log("the new world is built by ReTerraForged, continent scale {}", scale);
			// the world's shape for tools/uiparity.sh: a server world from the exported datapack, with this seed and scale,
			// must be the same
			raccoonman.reterraforged.server.RTFWorldChecks.heights(server, "gui-" + scale);
			if (scale != expectedScale) fail("the new world has continent scale " + scale + ", the preset screen set " + expectedScale);
		} else {
			fail("the new world is not built by ReTerraForged (vanilla generator)");
		}
		// the preset's trees replace the vanilla ones in the biomes
		Biome forest = level.registryAccess().lookupOrThrow(Registries.BIOME).getValue(Biomes.FOREST);
		List<String> trees = forest.getGenerationSettings().features().stream().flatMap(HolderSet::stream).map(Holder::unwrapKey).flatMap(Optional::stream)
			.map((key) -> key.identifier().toString()).filter((id) -> id.endsWith("trees") || id.contains("trees_")).toList();
		log("trees in the forest: {}", trees);
		if (trees.stream().anyMatch((id) -> !id.startsWith("minecraft:") && !id.startsWith(RTFCommon.MOD_ID + ":"))) {
			// another mod's datapack rewrites the forest's features (Terralith: terralith:forest/vanilla/trees): the forest
			// grows that mod's trees and our modifier finds no vanilla trees to replace
			log("the forest grows another mod's trees, not checked: {}", trees);
		} else if (!trees.equals(List.of(RTFCommon.MOD_ID + ":forest_trees"))) {
			fail("the forest of the new world does not grow ReTerraForged trees: " + trees);
		}
	}

	private static void finish(Minecraft mc) {
		log("{} screenshots, {}", shot, FAILURES.isEmpty() ? "all checks passed" : FAILURES.size() + " failed: " + FAILURES);
		log("done");
		mc.stop();
	}

	private static void next(int ticks) {
		step++;
		wait = ticks;
	}

	static void fail(String message) {
		FAILURES.add(message);
		RTFCommon.LOGGER.error("[RTF clienttest] FAIL {}", message);
	}

	static void log(String message, Object... args) {
		RTFCommon.LOGGER.info("[RTF clienttest] " + message, args);
	}

	static void shoot(Minecraft mc, String name) {
		Screenshot.grab(mc.gameDirectory, "rtf-" + name + ".png", mc.getMainRenderTarget(), 1, (message) -> {
		});
		shot++;
	}

	static String name(@Nullable Screen screen) {
		return screen == null ? "none" : screen.getClass().getSimpleName();
	}

	static String page(Minecraft mc) {
		return mc.screen instanceof LinkedPageScreen screen ? screen.getCurrentPage().getClass().getSimpleName() : name(mc.screen);
	}

	static Preset preset(Minecraft mc) {
		return ((PresetEditorPage) ((LinkedPageScreen) mc.screen).getCurrentPage()).getPreset().getPreset();
	}

	// the preset in the worldgen registries the create world screen will make the world with
	static Optional<Preset> appliedPreset(Minecraft mc) {
		if (!(mc.screen instanceof CreateWorldScreen screen)) return Optional.empty();
		return screen.getUiState().getSettings().worldgenLoadContext().lookup(RTFRegistries.PRESET).flatMap((registry) -> registry.get(Preset.KEY)).map(Holder::value);
	}

	// selects a built-in preset by its id, as a click would (the list's selection calls the page)
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static boolean selectBuiltin(Screen screen, String id) {
		for (GuiEventListener child : screen.children()) {
			if (child instanceof WidgetList<?> list) {
				for (WidgetList.Entry<?> entry : list.children()) {
					if (entry.getWidget() instanceof PresetEntry preset && id.equals(preset.builtinId())) {
						((WidgetList) list).setSelected(entry);
						return true;
					}
				}
			}
		}
		return false;
	}

	@Nullable
	private static PresetEntry selectedPreset(Screen screen) {
		for (GuiEventListener child : screen.children()) {
			if (child instanceof WidgetList<?> list && list.getSelected() != null && list.getSelected().getWidget() instanceof PresetEntry entry) return entry;
		}
		return null;
	}

	// the left column (the settings) of a page
	@Nullable
	private static WidgetList<?> settingsList(Minecraft mc) {
		for (GuiEventListener child : mc.screen.children()) {
			if (child instanceof WidgetList<?> list) return list;
		}
		return null;
	}

	// five mouse wheel steps down over the left column (the settings)
	private static void scrollDown(Minecraft mc) {
		for (GuiEventListener child : mc.screen.children()) {
			if (child instanceof WidgetList<?> list) {
				mc.screen.mouseScrolled(list.getX() + list.getWidth() / 2.0, list.getY() + list.getHeight() / 2.0, 0, -5);
				return;
			}
		}
	}

	static boolean hasKey(AbstractWidget widget, String key) {
		return widget.getMessage().getContents() instanceof TranslatableContents contents && contents.getKey().equals(key);
	}

	static boolean startsWith(AbstractWidget widget, String key) {
		return widget.getMessage().getString().startsWith(I18n.get(key));
	}

	// a mouse click (press and release) at a point of the first visible widget that matches, x at the given fraction
	static boolean click(Minecraft mc, Predicate<AbstractWidget> predicate, double fraction) {
		AbstractWidget widget = find(mc.screen, predicate);
		if (widget == null) return false;
		click(mc, widget, fraction);
		return true;
	}

	static void click(Minecraft mc, AbstractWidget widget, double fraction) {
		double x = widget.getX() + widget.getWidth() * fraction;
		double y = widget.getY() + widget.getHeight() / 2.0;
		log("click {} '{}' at {} {} on {}", widget.getClass().getSimpleName(), widget.getMessage().getString(), (int) x, (int) y, name(mc.screen));
		Screen screen = mc.screen;
		MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0));
		screen.mouseMoved(x, y);
		screen.mouseClicked(event, false);
		screen.mouseReleased(event);
	}

	@Nullable
	static AbstractWidget find(@Nullable GuiEventListener parent, Predicate<AbstractWidget> predicate) {
		if (parent instanceof ContainerEventHandler container) {
			for (GuiEventListener child : container.children()) {
				if (child instanceof AbstractWidget widget && widget.visible && predicate.test(widget)) return widget;
				AbstractWidget found = find(child, predicate);
				if (found != null) return found;
			}
		}
		return null;
	}

	static void command(Minecraft mc, String command) {
		MinecraftServer server = mc.getSingleplayerServer();
		server.execute(() -> {
			ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
			server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.OWNER), command);
		});
	}
}
