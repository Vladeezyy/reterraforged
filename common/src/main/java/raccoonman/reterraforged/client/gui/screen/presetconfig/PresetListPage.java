package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.JsonOps;

import io.netty.util.internal.StringUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.GsonHelper;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.client.gui.Toasts;
import raccoonman.reterraforged.client.gui.Tooltips;
import raccoonman.reterraforged.client.gui.screen.page.BisectedPage;
import raccoonman.reterraforged.client.gui.screen.page.LinkedPageScreen.Page;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetListPage.PresetEntry;
import raccoonman.reterraforged.client.gui.widget.Label;
import raccoonman.reterraforged.client.gui.widget.WidgetList;
import raccoonman.reterraforged.client.gui.widget.WidgetList.Entry;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.Presets;
import raccoonman.reterraforged.platform.ConfigUtil;

public class PresetListPage extends BisectedPage<PresetConfigScreen, PresetEntry, AbstractWidget> {
	private static final Path PRESET_PATH = ConfigUtil.rtf("presets");
	private static final Path EXPORT_PATH = ConfigUtil.rtf("exports");
	private static final Path LEGACY_PRESET_PATH = ConfigUtil.legacy("presets");
	

	private EditBox input;
	private Button createPreset;
	private Button deletePreset;
	private Button exportAsDatapack;
	private Button copyPreset;
	private Button openPresetFolder;
	private Button openExportFolder;
	// selected when the list is shown: the preset whose settings pages "<<" came back from
	@Nullable
	private final String selectName;
	@Nullable
	private PresetEntry selected;
	
	public PresetListPage(PresetConfigScreen screen) {
		this(screen, null);
	}

	public PresetListPage(PresetConfigScreen screen, @Nullable String selectName) {
		super(screen);
		this.selectName = selectName;
		
		try {
			if(!Files.exists(PRESET_PATH)) Files.createDirectory(PRESET_PATH);
			if(!Files.exists(EXPORT_PATH)) Files.createDirectory(EXPORT_PATH);
		} catch(IOException e) {
			RTFCommon.LOGGER.error("Could not create the presets folder {} or the export folder {}", PRESET_PATH, EXPORT_PATH, e);
		}
	}

	@Override
	public Component title() {
		return Component.translatable(RTFTranslationKeys.GUI_SELECT_PRESET_TITLE);
	}

	@Override
	public void init() {
		super.init();
		
		this.input = PresetWidgets.createEditBox(this.screen.font, (text) -> {
			boolean isValid = this.isValidPresetName(text);
			final int white = 14737632;
			final int red = 0xFFFF3F30;
			this.createPreset.active = isValid;
			this.input.setTextColor(isValid ? white : red);
		}, Component.translatable(RTFTranslationKeys.GUI_INPUT_PROMPT).withStyle(ChatFormatting.DARK_GRAY));
		this.createPreset = PresetWidgets.createThrowingButton(RTFTranslationKeys.GUI_BUTTON_CREATE, () -> {
			new PresetEntry(Component.literal(this.input.getValue()), Presets.makeDefault(), false, this).save();
			this.rebuildPresets();
			this.input.setValue(StringUtil.EMPTY_STRING);
		});
		this.createPreset.active = this.isValidPresetName(this.input.getValue());
		this.copyPreset = PresetWidgets.createThrowingButton(RTFTranslationKeys.GUI_BUTTON_COPY, () -> {
			PresetEntry preset = this.screen.editable(this.left.getSelected().getWidget());
			String name = fileName(preset.getName().getString());
			int counter = 1;
			String uniqueName;
			while(Files.exists(inside(PRESET_PATH, (uniqueName = name + " (" + counter + ")") + ".json"))) { 
				counter++;
			}
			new PresetEntry(Component.literal(uniqueName), preset.getPreset().copy(), false, this).save();
			this.rebuildPresets();
		});
		this.deletePreset = PresetWidgets.createThrowingButton(RTFTranslationKeys.GUI_BUTTON_DELETE, () -> {
			PresetEntry preset = this.left.getSelected().getWidget();
			this.screen.minecraft.gui.setScreen(new ConfirmScreen((confirmed) -> {
				if(confirmed) {
					Toasts.tryOrToast(Tooltips.failTranslationKey(RTFTranslationKeys.GUI_BUTTON_DELETE), () -> {
						Files.delete(preset.getPath());
						this.screen.forget(preset);
					});
				}
				this.screen.minecraft.gui.setScreen(this.screen);
			}, Component.translatable(RTFTranslationKeys.GUI_BUTTON_DELETE_CONFIRM), preset.getName()));
		});
		this.openPresetFolder = PresetWidgets.createThrowingButton(RTFTranslationKeys.GUI_BUTTON_OPEN_PRESET_FOLDER, () -> {
			Util.getPlatform().openUri(PRESET_PATH.toUri());
			this.rebuildPresets();
		});
		this.openExportFolder = PresetWidgets.createThrowingButton(RTFTranslationKeys.GUI_BUTTON_OPEN_EXPORT_FOLDER, () -> {
			Util.getPlatform().openUri(EXPORT_PATH.toUri());
			this.rebuildPresets();
		});
		this.exportAsDatapack = PresetWidgets.createThrowingButton(RTFTranslationKeys.GUI_BUTTON_EXPORT_AS_DATAPACK, () -> {
			PresetEntry preset = this.screen.editable(this.left.getSelected().getWidget());
			Path path = inside(EXPORT_PATH, fileName(preset.getName().getString()) + ".zip");
			this.screen.exportAsDatapack(path, preset);
			this.rebuildPresets();
			
			Toasts.notify(RTFTranslationKeys.GUI_BUTTON_EXPORT_SUCCESS, Component.literal(path.toString()), SystemToastId.WORLD_BACKUP);
		});

		this.right.addWidget(this.input);
		this.right.addWidget(this.createPreset);
		this.right.addWidget(this.copyPreset);
		this.right.addWidget(this.deletePreset);
		this.right.addWidget(this.openPresetFolder);
		this.right.addWidget(this.openExportFolder);
		this.right.addWidget(this.exportAsDatapack);
		
		this.left.setRenderSelected(true);
		this.left.onSelect(this::selectPreset);
		
		try {
			this.rebuildPresets();
		} catch (IOException e) {
			RTFCommon.LOGGER.error("Could not list the presets", e);
		}
	}

	@Override
	public Optional<Page> previous() {
		return Optional.empty();
	}

	@Override
	public Optional<Page> next() {
		return Optional.ofNullable(this.left).map(WidgetList::getSelected).map((e) -> {
			return new WorldSettingsPage(this.screen, this.screen.editable(e.getWidget()));
		});
	}
	
	@Override
	public void onDone() {
		super.onDone();
		
		Entry<PresetEntry> selected = this.left.getSelected();
		if(selected != null) {
			Toasts.tryOrToast(RTFTranslationKeys.GUI_CREATE_WORLD_APPLY_FAILED, () -> this.screen.applyPreset(this.screen.editable(selected.getWidget())));
		}
	}
	
	private void selectPreset(@Nullable PresetEntry entry) {
		// the selected preset is drawn yellow with a marker (and framed by the list): nothing showed which one it was
		if(this.selected != null) {
			this.selected.setMessage(this.selected.getName());
		}
		this.selected = entry;
		if(entry != null) {
			entry.setMessage(Component.literal("» ").append(entry.getName().copy().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.YELLOW));
		}
		boolean active = entry != null;
		
		this.screen.doneButton.active = active;
		this.copyPreset.active = active;
		this.deletePreset.active = active && !entry.isBuiltin();
		this.exportAsDatapack.active = active;
		this.screen.nextButton.active = active;
	}
	
	private void rebuildPresets() throws IOException {
		this.selectPreset(null);
		
		List<PresetEntry> entries = new ArrayList<>();
		entries.addAll(this.listPresets(PRESET_PATH));
		// TerraForged's folder: the same name in the mod's own folder wins
		for(PresetEntry legacy : this.listPresets(LEGACY_PRESET_PATH)) {
			if(entries.stream().noneMatch((entry) -> PresetNames.sameFile(entry.getName().getString(), legacy.getName().getString()))) {
				entries.add(legacy);
			}
		}

		// as in ReTerraForged 0.0.6: "Default" is TerraForged's default
		entries.add(new PresetEntry(Component.translatable(RTFTranslationKeys.GUI_DEFAULT_PRESET_NAME).withStyle(ChatFormatting.GRAY), Presets.makeDefault(), true, this));
		entries.add(new PresetEntry(Component.translatable(RTFTranslationKeys.GUI_BEAUTIFUL_PRESET_NAME).withStyle(ChatFormatting.GRAY), Presets.makeLegacyBeautiful(), true, this));
		entries.add(new PresetEntry(Component.translatable(RTFTranslationKeys.GUI_HUGE_BIOMES_PRESET_NAME).withStyle(ChatFormatting.GRAY), Presets.makeLegacyHugeBiomes(), true, this));
		entries.add(new PresetEntry(Component.translatable(RTFTranslationKeys.GUI_LITE_PRESET_NAME).withStyle(ChatFormatting.GRAY), Presets.makeLegacyLite(), true, this));
		entries.add(new PresetEntry(Component.translatable(RTFTranslationKeys.GUI_VANILLAISH_PRESET_NAME).withStyle(ChatFormatting.GRAY), Presets.makeLegacyVanillaish(), true, this));
		this.left.replaceEntries(entries.stream().map(WidgetList.Entry::new).toList());
		if(this.selectName != null) {
			for(Entry<PresetEntry> entry : this.left.children()) {
				if(entry.getWidget().getName().getString().equals(this.selectName)) {
					this.left.setSelected(entry);
					this.selectPreset(entry.getWidget());
					break;
				}
			}
		}
	}
	
	// a file name from a preset's shown name (a built-in preset's is its translation, any text): what a typed name
	// may have (PresetNames) and the brackets of a copy's number, the rest '_'
	private static String fileName(String name) {
		String safe = name.replaceAll("[^\\p{L}\\p{M}\\p{N}\\-_ ()]", "_").strip();
		return safe.isEmpty() ? "preset" : safe;
	}

	// the file of that name, and only in the folder
	private static Path inside(Path folder, String fileName) throws IOException {
		Path base = folder.toAbsolutePath().normalize();
		Path file = base.resolve(fileName).normalize();
		if(!base.equals(file.getParent())) {
			throw new IOException("Not a file name in " + base + ": " + fileName);
		}
		return file;
	}

	private boolean isValidPresetName(String text) {
		// the file too: on macOS and Windows "Foo" would overwrite "foo"
		return PresetNames.isValid(text) && !this.hasPresetWithName(text) && !Files.exists(PRESET_PATH.resolve(text + ".json"));
	}
	
	private boolean hasPresetWithName(String name) {
		return this.left.children().stream().filter((entry) -> {
			return PresetNames.sameFile(entry.getWidget().getName().getString(), name);
		}).findAny().isPresent();
	}
	
	private List<PresetEntry> listPresets(Path path) throws IOException	{
		List<PresetEntry> presets = new ArrayList<>();
		if(Files.exists(path)) {
			// the presets' JSON files (not a temporary file a failed save left); the listing is closed (Files.list keeps
			// the directory open until it is)
			List<Path> files;
			try (java.util.stream.Stream<Path> listing = Files.list(path)) {
				files = listing.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().endsWith(".json")).toList();
			}
			for(Path presetPath : files) {
				// a broken or foreign file in the presets folder (not JSON, another encoding, not a preset) is skipped:
				// the JSON parser throws for it, which used to crash the game when the preset screen opened
				try(Reader reader = Files.newBufferedReader(presetPath)) {
					String fileName = presetPath.getFileName().toString();
					String base = fileName.substring(0, fileName.length() - ".json".length());
					DataResult<Preset> result = Preset.DIRECT_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader));
					Optional<Error<Preset>> error = result.error();
					if(error.isPresent()) {
						RTFCommon.LOGGER.error("Skipping preset {}: {}", presetPath.getFileName(), error.get().message());
						continue;
					}
					Preset preset = result.result().get();
					PresetEntry entry = new PresetEntry(Component.literal(base), preset, false, this);
					entry.path = presetPath;
					presets.add(entry);
				} catch(IOException | RuntimeException e) {
					RTFCommon.LOGGER.error("Skipping preset {}: {}", presetPath.getFileName(), e.toString());
				}
			}
		}
		return presets;
	}
	
	public static class PresetEntry extends Label {
		private final Component name;
		private final Preset preset;
		private final boolean builtin;
		// the file it was read from (the presets folder or TerraForged's); null: a new one in the presets folder
		@Nullable
		private Path path;
		
		public PresetEntry(Component name, Preset preset, boolean builtin, OnPress onPress) {
			super(-1, -1, -1, -1, onPress, name);
			
			this.name = name;
			this.preset = preset;
			this.builtin = builtin;
		}
		
		public PresetEntry(Component name, Preset preset, boolean builtin, PresetListPage page) {
			this(name, preset, builtin, (b) -> {
				if(b instanceof PresetEntry entry) {
					page.selectPreset(entry);
				}
			});
		}

		public Component getName() {
			return this.name;
		}
		
		public Preset getPreset() {
			return this.preset;
		}
		
		public boolean isBuiltin() {
			return this.builtin;
		}
		
		public Path getPath() {
			return this.path != null ? this.path : PRESET_PATH.resolve(this.name.getString() + ".json");
		}
		
		// the same preset file with these settings
		public PresetEntry withPreset(Preset preset) {
			PresetEntry entry = new PresetEntry(this.name, preset, this.builtin, (b) -> {});
			entry.path = this.path;
			return entry;
		}
		
		// encoded first, written to a temporary file and moved over the old one: the file was opened (emptied) before the
		// preset was encoded, so a failure left the player's preset an empty file
		public void save() throws IOException {
			if(!this.builtin) {
				JsonElement element = Preset.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, this.preset).result().orElseThrow(() -> new IOException("could not encode preset " + this.name.getString()));
				Path path = this.getPath();
				Path temp = path.resolveSibling(path.getFileName() + ".tmp");
				try(
					Writer writer = Files.newBufferedWriter(temp);
					JsonWriter jsonWriter = new JsonWriter(writer);
				) {
					jsonWriter.setSerializeNulls(false);
					jsonWriter.setIndent("  ");
					GsonHelper.writeValue(jsonWriter, element, null);
				}
				try {
					Files.move(temp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
				} catch (java.nio.file.AtomicMoveNotSupportedException e) {
					Files.move(temp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}
}
