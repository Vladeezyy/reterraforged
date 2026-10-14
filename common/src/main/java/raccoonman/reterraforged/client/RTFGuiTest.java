package raccoonman.reterraforged.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.client.gui.screen.page.LinkedPageScreen;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetConfigScreen;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetEditorPage;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetListPage;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetListPage.PresetEntry;
import raccoonman.reterraforged.client.gui.widget.WidgetList;
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
	private static int timeout;

	private RTFGuiTest() {
	}

	public static void tick(Minecraft mc) {
		if (wait > 0) {
			wait--;
			return;
		}
		switch (step) {
			case 0 -> {
				if (mc.screen instanceof TitleScreen title) {
					CreateWorldScreen.openFresh(mc, title);
					next(60);
				}
			}
			case 1 -> {
				// the World tab of the create world screen, then its Customize button (opens the preset screen)
				if (!click(mc, (w) -> hasKey(w, "createWorld.tab.world.title"), 0.5)) fail("no World tab");
				next(10);
			}
			case 2 -> {
				shoot(mc, "gui-" + round + "-create-world");
				if (!click(mc, (w) -> hasKey(w, "selectWorld.customizeType") && w.active, 0.5)) fail("no active Customize button");
				next(40);
			}
			case 3 -> {
				if (!(mc.screen instanceof PresetConfigScreen)) {
					fail("Customize did not open the preset screen: " + name(mc.screen));
					finish(mc);
					return;
				}
				shoot(mc, "gui-" + round + "-presets");
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
				// a click at a quarter (first round) or three quarters (second round) of the continent scale slider
				int before = preset(mc).world().continent.continentScale;
				if (!click(mc, (w) -> startsWith(w, RTFTranslationKeys.GUI_SLIDER_CONTINENT_SCALE), round == 0 ? 0.25 : 0.75)) fail("no continent scale slider");
				expectedScale = preset(mc).world().continent.continentScale;
				log("continent scale {} -> {} (slider click)", before, expectedScale);
				if (expectedScale == before) fail("the continent scale slider click changed nothing");
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
				// every page forward with ">>", each scrolled down with the mouse wheel first
				LinkedPageScreen screen = (LinkedPageScreen) mc.screen;
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
					step = 9;
				}
			}
			case 9 -> {
				click(mc, ((LinkedPageScreen) mc.screen).doneButton, 0.5);
				timeout = 600;
				next(20);
			}
			case 10 -> {
				// the preset datapack is applied: the create world screen is back, with the new worldgen registries
				Optional<Preset> applied = appliedPreset(mc);
				int scale = applied.map((p) -> p.world().continent.continentScale).orElse(-1);
				if (mc.screen instanceof CreateWorldScreen && scale == expectedScale) {
					log("round {}: the world will use the preset with continent scale {}", round, scale);
					shoot(mc, "gui-" + round + "-applied");
					round++;
					step = round == 1 ? 1 : 11;
					wait = 20;
				} else if (--timeout <= 0) {
					fail("round " + round + ": the preset screen's Done did not apply the edited preset: " + (applied.isPresent() ? "continent scale " + scale + ", set " + expectedScale : "no ReTerraForged preset") + ", screen " + name(mc.screen));
					round++;
					step = round == 1 ? 1 : 11;
					wait = 20;
				}
			}
			case 11 -> {
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
				command(mc, "gamemode spectator");
				command(mc, "time set 6000");
				command(mc, "weather clear");
				command(mc, "tp @s ~ ~40 ~ 0 25");
				mc.options.hideGui = true;
				next(500);
			}
			case 13 -> {
				shoot(mc, "gui-world");
				next(20);
			}
			default -> finish(mc);
		}
	}

	private static void checkWorld(MinecraftServer server) {
		ServerLevel level = server.overworld();
		if ((Object) level.getChunkSource().randomState() instanceof RTFRandomState state && state.generatorContext() != null) {
			int scale = state.preset().world().continent.continentScale;
			log("the new world is built by ReTerraForged, continent scale {}", scale);
			if (scale != expectedScale) fail("the new world has continent scale " + scale + ", the preset screen set " + expectedScale);
		} else {
			fail("the new world is not built by ReTerraForged (vanilla generator)");
		}
		// the preset's trees replace the vanilla ones in the biomes
		Biome forest = level.registryAccess().registryOrThrow(Registries.BIOME).get(Biomes.FOREST);
		List<String> trees = forest.getGenerationSettings().features().stream().flatMap(HolderSet::stream).map(Holder::unwrapKey).flatMap(Optional::stream)
			.map((key) -> key.location().toString()).filter((id) -> id.endsWith("trees") || id.contains("trees_")).toList();
		log("trees in the forest: {}", trees);
		if (!trees.equals(List.of(RTFCommon.MOD_ID + ":forest_trees"))) fail("the forest of the new world does not grow ReTerraForged trees: " + trees);
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
		Screenshot.grab(mc.gameDirectory, "rtf-" + name + ".png", mc.getMainRenderTarget(), (message) -> {
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

	@Nullable
	private static PresetEntry selectedPreset(Screen screen) {
		for (GuiEventListener child : screen.children()) {
			if (child instanceof WidgetList<?> list && list.getSelected() != null && list.getSelected().getWidget() instanceof PresetEntry entry) return entry;
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
		screen.mouseMoved(x, y);
		screen.mouseClicked(x, y, GLFW.GLFW_MOUSE_BUTTON_LEFT);
		screen.mouseReleased(x, y, GLFW.GLFW_MOUSE_BUTTON_LEFT);
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
			server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), command);
		});
	}
}
