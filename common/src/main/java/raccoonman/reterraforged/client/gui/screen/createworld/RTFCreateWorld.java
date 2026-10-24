package raccoonman.reterraforged.client.gui.screen.createworld;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetConfigScreen;
import raccoonman.reterraforged.data.worldgen.PresetExport;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.Presets;

/**
 * ReTerraForged on the create world screen: the preset goes into the new world as a datapack (a new file name each
 * time, see apply), a new world gets the Default preset at once (before, the world stayed vanilla unless the player
 * found Customize on the World tab), and the Game tab has the switch between ReTerraForged and vanilla generation and
 * the button to the settings.
 */
public final class RTFCreateWorld {
	public static final String PACK_NAME = "reterraforged-preset";
	private static final AtomicInteger APPLIED = new AtomicInteger();
	// the preset each create world screen has, to put it back when the switch goes from vanilla to ReTerraForged
	private static final Map<CreateWorldScreen, Pair<Component, Preset>> APPLIED_PRESETS = new WeakHashMap<>();
	private static final Set<CreateWorldScreen> INITIALIZED = Collections.newSetFromMap(new WeakHashMap<>());
	private static boolean freshPending;

	private RTFCreateWorld() {
	}

	// CreateWorldScreen.openFresh: the next create world screen is for a new world (not a copy of an existing one)
	public static void markFresh() {
		freshPending = true;
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
		return screen.getUiState().getSettings().dataConfiguration().dataPacks().getEnabled().stream().anyMatch((id) -> id.startsWith("file/" + PACK_NAME));
	}

	public static Component presetName(CreateWorldScreen screen) {
		Pair<Component, Preset> applied = APPLIED_PRESETS.get(screen);
		return applied != null ? applied.getFirst() : defaultName();
	}

	private static Component defaultName() {
		return Component.translatable(RTFTranslationKeys.GUI_DEFAULT_PRESET_NAME);
	}

	// the preset the screen had before, or Default
	public static void enable(CreateWorldScreen screen) {
		Pair<Component, Preset> applied = APPLIED_PRESETS.get(screen);
		try {
			apply(screen, applied != null ? applied.getFirst() : defaultName(), applied != null ? applied.getSecond() : Presets.makeDefault());
		} catch (IOException e) {
			RTFCommon.LOGGER.error("[RTF] could not apply the preset to the new world", e);
		}
	}

	public static void apply(CreateWorldScreen screen, Component name, Preset preset) throws IOException {
		Pair<Path, PackRepository> path = screen.getDataPackSelectionSettings(screen.getUiState().getSettings().dataConfiguration());
		PackRepository repository = path.getSecond();
		// the preset goes in under a new file name each time: the create world screen reloads the datapacks only when
		// the list of them changes, so a preset edited and applied again (Customize, Done, Customize, Done) used to
		// keep the first one, and the new world was made with the old settings
		removePacks(path.getFirst(), repository);
		Path exportPath = path.getFirst().resolve(PACK_NAME + "-" + APPLIED.incrementAndGet() + ".zip");
		PresetExport.toZip(preset, name.getString(), screen.getUiState().getSettings().worldgenLoadContext(), exportPath);
		APPLIED_PRESETS.put(screen, Pair.of(name, preset));
		repository.reload();
		if (repository.addPack("file/" + exportPath.getFileName())) {
			screen.tryApplyNewDataPacks(repository, false, (data) -> {
			});
		}
	}

	// vanilla generation: the preset datapack out of the new world
	public static void disable(CreateWorldScreen screen) {
		Pair<Path, PackRepository> path = screen.getDataPackSelectionSettings(screen.getUiState().getSettings().dataConfiguration());
		try {
			if (removePacks(path.getFirst(), path.getSecond())) {
				path.getSecond().reload();
				screen.tryApplyNewDataPacks(path.getSecond(), false, (data) -> {
				});
			}
		} catch (IOException e) {
			RTFCommon.LOGGER.error("[RTF] could not remove the preset from the new world", e);
		}
	}

	private static boolean removePacks(Path folder, PackRepository repository) throws IOException {
		List<Path> previous;
		try (Stream<Path> files = Files.list(folder)) {
			previous = files.filter((file) -> file.getFileName().toString().startsWith(PACK_NAME)).toList();
		}
		for (Path file : previous) {
			repository.removePack("file/" + file.getFileName());
			try {
				Files.deleteIfExists(file);
			} catch (IOException e) {
				// still open: it stays in the temporary folder, not selected
			}
		}
		return !previous.isEmpty();
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
			minecraft.gui.setScreen(new PresetConfigScreen(screen));
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
