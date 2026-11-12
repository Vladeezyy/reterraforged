package raccoonman.reterraforged.client.gui.screen.createworld;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.client.gui.Toasts;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetConfigScreen;
import raccoonman.reterraforged.concurrent.LatestTask;
import raccoonman.reterraforged.data.worldgen.PresetExport;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.Presets;
import raccoonman.reterraforged.registries.RTFRegistries;

/**
 * ReTerraForged on the create world screen: the preset goes into the new world as a datapack (a new file name each
 * time, see apply), a new world gets the Default preset at once (before, the world stayed vanilla unless the player
 * found Customize on the World tab), and the Game tab has the switch between ReTerraForged and vanilla generation and
 * the button to the settings.
 */
public final class RTFCreateWorld {
	public static final String PACK_NAME = "reterraforged-preset";
	// the files apply makes; a pack of the player's that only starts like them is theirs
	private static final Pattern OWN_PACK = Pattern.compile("^" + PACK_NAME + "-\\d+\\.zip$");
	// what an export the game was closed in the middle of left next to them
	private static final Pattern OWN_TEMP = Pattern.compile("^" + PACK_NAME + "-\\d+\\.zip\\.tmp$");
	// vanilla's message for the reload of the datapacks that follows the export: one message for both
	private static final String PREPARING = "createWorld.preparing";
	private static final AtomicInteger APPLIED = new AtomicInteger();
	// the preset each create world screen has, to put it back when the switch goes from vanilla to ReTerraForged
	private static final Map<CreateWorldScreen, Pair<Component, Preset>> APPLIED_PRESETS = new WeakHashMap<>();
	private static final Set<CreateWorldScreen> INITIALIZED = Collections.newSetFromMap(new WeakHashMap<>());
	// the export of each create world screen, off the render thread: it froze the game for seconds when the screen
	// opened and on Done. A new one supersedes the one before
	private static final Map<CreateWorldScreen, LatestTask<Path>> EXPORTS = new WeakHashMap<>();
	private static boolean freshPending;

	private RTFCreateWorld() {
	}

	// CreateWorldScreen.openFresh: the next create world screen is for a new world (not a copy of an existing one)
	public static void markFresh() {
		freshPending = true;
	}

	// CreateWorldScreen.createFromExisting: an existing world's settings, whatever openFresh left behind when it failed
	public static void markExisting() {
		freshPending = false;
	}

	public static void onInit(CreateWorldScreen screen) {
		if (!INITIALIZED.add(screen)) {
			return;
		}
		boolean fresh = freshPending;
		freshPending = false;
		if (fresh && !isEnabled(screen)) {
			// after init: applying reloads the datapacks of the screen
			Minecraft.getInstance().execute(() -> enable(screen));
		}
	}

	public static boolean isEnabled(CreateWorldScreen screen) {
		// the preset in the screen's registries, whichever pack brought it: an exported preset chosen on the Data Packs
		// screen, under its own name, was shown as vanilla generation
		return screen.getUiState().getSettings().worldgenLoadContext().lookup(RTFRegistries.PRESET).map((presets) -> presets.containsKey(Preset.KEY)).orElse(false);
	}

	// an export for the screen is on its way (the tests wait for it)
	public static boolean isExporting(CreateWorldScreen screen) {
		LatestTask<Path> task = EXPORTS.get(screen);
		return task != null && task.isBusy();
	}

	public static Component presetName(CreateWorldScreen screen) {
		Pair<Component, Preset> applied = APPLIED_PRESETS.get(screen);
		if (applied != null) {
			return applied.getFirst();
		}
		// a preset from a pack of the player's: the pack's name
		return screen.getUiState().getSettings().dataConfiguration().dataPacks().getEnabled().stream().filter((id) -> id.startsWith("file/")).reduce((first, last) -> last)
			.<Component>map((id) -> Component.literal(id.substring("file/".length()))).orElseGet(RTFCreateWorld::defaultName);
	}

	private static Component defaultName() {
		return Component.translatable(RTFTranslationKeys.GUI_DEFAULT_PRESET_NAME);
	}

	// the preset the screen had before, or Default
	public static void enable(CreateWorldScreen screen) {
		Pair<Component, Preset> applied = APPLIED_PRESETS.get(screen);
		Toasts.tryOrToast(RTFTranslationKeys.GUI_CREATE_WORLD_APPLY_FAILED, () -> apply(screen, applied != null ? applied.getFirst() : defaultName(), applied != null ? applied.getSecond() : Presets.makeDefault()));
	}

	// The data generation runs off the render thread, and until its pack is in the screen's datapacks the create world
	// screen waits behind vanilla's "preparing" message, the one vanilla shows for the reload that follows. So Create New
	// World cannot be pressed with the old datapacks (the world would get the old preset or none), and no vanilla button,
	// different on every version, has to be watched for it. Throws only for what fails before the export starts; a
	// failed export is a toast, and the create world screen comes back
	public static void apply(CreateWorldScreen screen, Component name, Preset preset) throws IOException {
		Minecraft minecraft = Minecraft.getInstance();
		Pair<Path, PackRepository> path = screen.getDataPackSelectionSettings(screen.getUiState().getSettings().dataConfiguration());
		Path folder = path.getFirst();
		PackRepository repository = path.getSecond();
		// the preset goes in under a new file name each time: the create world screen reloads the datapacks only when
		// the list of them changes, so a preset edited and applied again (Customize, Done, Customize, Done) used to
		// keep the first one, and the new world was made with the old settings
		removePacks(folder, repository);
		Path exportPath = folder.resolve(PACK_NAME + "-" + APPLIED.incrementAndGet() + ".zip");
		// what the export reads, taken on the render thread: the name, and the screen's worldgen registries, which are
		// frozen. The preset is not copied: nothing edits it any more (the preset screen is closed, the waiting screen
		// is in front of everything until the export is over)
		String presetName = name.getString();
		RegistryAccess registries = screen.getUiState().getSettings().worldgenLoadContext();
		Screen waiting = new GenericMessageScreen(Component.translatable(PREPARING));
		minecraft.setScreen(waiting);
		exports(screen).submit(() -> {
			removeLeftovers(folder);
			PresetExport.toZip(preset, presetName, registries, exportPath);
			return exportPath;
		}, RTFCreateWorld::deleteQuietly, (file, error) -> {
			if (minecraft.screen != waiting) {
				// another screen took over meanwhile: the pack is not put in behind its back
				deleteQuietly(exportPath);
				return;
			}
			try {
				if (error != null) {
					throw error;
				}
				repository.reload();
				if (!repository.addPack("file/" + exportPath.getFileName())) {
					throw new IOException("the game did not find the pack " + exportPath);
				}
				APPLIED_PRESETS.put(screen, Pair.of(name, preset));
				// brings the create world screen back when the reload is over, also when it fails
				screen.tryApplyNewDataPacks(repository, false, (data) -> {
				});
			} catch (Exception e) {
				Toasts.tryOrToast(RTFTranslationKeys.GUI_CREATE_WORLD_APPLY_FAILED, () -> {
					throw e;
				});
				// never left on the waiting screen
				if (minecraft.screen == waiting) {
					minecraft.setScreen(screen);
				}
			}
		});
	}

	private static LatestTask<Path> exports(CreateWorldScreen screen) {
		return EXPORTS.computeIfAbsent(screen, (key) -> new LatestTask<>(Minecraft.getInstance()));
	}

	// vanilla generation: the preset datapack out of the new world
	public static void disable(CreateWorldScreen screen) {
		// an export still on its way does not put the pack back in afterwards
		LatestTask<Path> task = EXPORTS.get(screen);
		boolean exporting = task != null && task.isBusy();
		if (task != null) {
			task.cancel();
		}
		Minecraft minecraft = Minecraft.getInstance();
		Pair<Path, PackRepository> path = screen.getDataPackSelectionSettings(screen.getUiState().getSettings().dataConfiguration());
		boolean[] reloading = { false };
		Toasts.tryOrToast(RTFTranslationKeys.GUI_CREATE_WORLD_APPLY_FAILED, () -> {
			if (removePacks(path.getFirst(), path.getSecond())) {
				path.getSecond().reload();
				reloading[0] = true;
				screen.tryApplyNewDataPacks(path.getSecond(), false, (data) -> {
				});
			}
		});
		// the cancelled export's waiting screen would stay
		if (exporting && !reloading[0] && minecraft.screen instanceof GenericMessageScreen) {
			minecraft.setScreen(screen);
		}
	}

	private static boolean removePacks(Path folder, PackRepository repository) throws IOException {
		List<Path> previous;
		try (Stream<Path> files = Files.list(folder)) {
			previous = files.filter((file) -> OWN_PACK.matcher(file.getFileName().toString()).matches()).toList();
		}
		for (Path file : previous) {
			repository.removePack("file/" + file.getFileName());
			deleteQuietly(file);
		}
		return !previous.isEmpty();
	}

	// on the export thread, before an export: the half-written zips of an export the game was closed in the middle of (no
	// other export runs then, LatestTask has one thread) and the data generation folders such exports left
	private static void removeLeftovers(Path folder) throws IOException {
		List<Path> temporary;
		try (Stream<Path> files = Files.list(folder)) {
			temporary = files.filter((file) -> OWN_TEMP.matcher(file.getFileName().toString()).matches()).toList();
		}
		temporary.forEach(RTFCreateWorld::deleteQuietly);
		PresetExport.removeOldTemporaryFolders(TimeUnit.HOURS.toMillis(1));
	}

	private static void deleteQuietly(Path file) {
		try {
			Files.deleteIfExists(file);
		} catch (IOException e) {
			// still open: it stays in the temporary folder, not selected
		}
	}

	// the Game tab (MixinCreateWorldGameTab): below its buttons, the generation switch, the settings button and the
	// preset the world gets
	public static void addGameTabWidgets(CreateWorldScreen screen, GridLayout layout) {
		Minecraft minecraft = Minecraft.getInstance();
		int[] rows = { 0 };
		layout.visitChildren((child) -> rows[0]++);
		boolean enabled = isEnabled(screen);
		CycleButton<Boolean> generation = CycleButton.booleanBuilder(Component.translatable(RTFTranslationKeys.GUI_CREATE_WORLD_GENERATION_RTF), Component.translatable(RTFTranslationKeys.GUI_CREATE_WORLD_GENERATION_VANILLA), enabled)
			.withTooltip((value) -> Tooltip.create(Component.translatable(RTFTranslationKeys.GUI_CREATE_WORLD_GENERATION_TOOLTIP)))
			.create(0, 0, 210, 20, Component.translatable(RTFTranslationKeys.GUI_CREATE_WORLD_GENERATION), (button, value) -> {
				if (value) {
					enable(screen);
				} else {
					disable(screen);
				}
			});
		Button settings = Button.builder(Component.translatable(RTFTranslationKeys.GUI_CREATE_WORLD_SETTINGS), (button) -> {
			minecraft.setScreen(new PresetConfigScreen(screen));
		}).width(210).tooltip(Tooltip.create(Component.translatable(RTFTranslationKeys.GUI_CREATE_WORLD_SETTINGS_TOOLTIP))).build();
		StringWidget status = new StringWidget(210, 9, status(screen, enabled), minecraft.font);
		layout.addChild(generation, rows[0], 0, layout.newCellSettings().alignHorizontallyCenter());
		layout.addChild(settings, rows[0] + 1, 0, layout.newCellSettings().alignHorizontallyCenter());
		layout.addChild(status, rows[0] + 2, 0, layout.newCellSettings().alignHorizontallyCenter());
		settings.active = enabled;
		screen.getUiState().addListener((state) -> {
			boolean on = isEnabled(screen);
			generation.setValue(on);
			settings.active = on;
			status.setMessage(status(screen, on));
		});
	}

	private static Component status(CreateWorldScreen screen, boolean enabled) {
		return enabled ? Component.translatable(RTFTranslationKeys.GUI_CREATE_WORLD_PRESET, presetName(screen)).withStyle(ChatFormatting.GRAY) : Component.translatable(RTFTranslationKeys.GUI_CREATE_WORLD_VANILLA).withStyle(ChatFormatting.GRAY);
	}
}
