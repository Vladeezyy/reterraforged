package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.NativeImage;
import org.joml.Matrix3x2fStack;

import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.client.gui.Toasts;
import raccoonman.reterraforged.client.gui.screen.page.BisectedPage;
import raccoonman.reterraforged.client.gui.screen.page.LinkedPageScreen.Page;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetListPage.PresetEntry;
import raccoonman.reterraforged.client.gui.screen.presetconfig.SettingControl.DefaultValue;
import raccoonman.reterraforged.client.gui.widget.ChangeMarker;
import raccoonman.reterraforged.client.gui.widget.Label;
import raccoonman.reterraforged.client.gui.widget.ValueButton;
import raccoonman.reterraforged.concurrent.cache.CacheManager;
import raccoonman.reterraforged.config.PerformanceConfig;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.Presets;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;
import raccoonman.reterraforged.mixin.ScreenInvoker;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

public abstract class PresetEditorPage extends BisectedPage<PresetConfigScreen, AbstractWidget, AbstractWidget> {
	static final String SEARCH_HINT = "reterraforged.gui.search.hint";
	static final String SEARCH_NONE = "reterraforged.gui.search.none";
	private static final int SEARCH_WIDTH = 160;
	private static final int SEARCH_MIN_WIDTH = 80;
	// one texture for all the pages: a new one registered for each page stayed in the TextureManager after it was closed
	@Nullable
	private static DynamicTexture previewTexture;
	@Nullable
	private static Identifier previewTextureId;
	// Default's settings by page and setting id, for each preset screen (the same while it is open, built once). Plain
	// data: no widget or page, which would hold the screen and keep it in the map
	private static final Map<Screen, Map<Class<?>, Map<String, DefaultValue>>> DEFAULTS = new WeakHashMap<>();
	private CycleButton<RenderMode> renderMode;
	private ValueButton<Integer> seed;
	@Nullable
	private Preview preview;
	protected final PresetEntry preset;
	// built only to read its settings: no preview, no search, nothing on the screen
	private boolean detached;
	// the settings of this page, and the ones whose value is not Default's (with a mark, reset with a right click)
	private List<Row> rows = List.of();
	private final Map<AbstractWidget, Row> rowsByWidget = new IdentityHashMap<>();
	private Map<String, DefaultValue> defaults = Map.of();
	private final Set<AbstractWidget> changed = Collections.newSetFromMap(new IdentityHashMap<>());
	// all the pages in order, the others as they were when this one was opened (they do not change meanwhile)
	private List<PageInfo> pages = List.of();
	private final List<Tab> tabs = new ArrayList<>();
	@Nullable
	private EditBox searchBox;
	// kept on the page: a resize builds the screen again
	private String query = "";
	private boolean searching;
	private double scrollBeforeSearch;
	private List<AbstractWidget> pageWidgets = List.of();
	// the setting a search result on another page led to, shown when this page is opened
	@Nullable
	private String reveal;

	public PresetEditorPage(PresetConfigScreen screen, PresetEntry preset) {
		super(screen);

		this.preset = preset;
	}

	public PresetEntry getPreset() {
		return this.preset;
	}

	// a setting of a page: the id is the same in every build of the page (SettingIds)
	private record Row(String id, String section, AbstractWidget widget, SettingControl control) {
	}

	// a setting of a page as the search shows it
	private record Item(String id, String name, String section, boolean repeated, Component message) {
	}

	// items null for this page: its widgets are read when searching, with their values at that time
	private record PageInfo(Class<?> type, Component title, boolean changed, @Nullable List<Item> items) {
	}

	private record Tab(AbstractWidget button, Class<?> page) {
	}

	// the settings pages bound to a preset, in order: the first page, then next() up to the last
	static List<PresetEditorPage> pages(PresetConfigScreen screen, PresetEntry preset) {
		List<PresetEditorPage> pages = new ArrayList<>();
		Set<Class<?>> seen = new HashSet<>();
		Optional<Page> page = Optional.of(new WorldSettingsPage(screen, preset));
		while(page.isPresent() && page.get() instanceof PresetEditorPage editor && seen.add(editor.getClass())) {
			pages.add(editor);
			page = editor.next();
		}
		return pages;
	}

	// the settings of a page built only for them; null if it could not be built (the page itself is never stopped by it)
	@Nullable
	private static List<Row> build(PresetEditorPage page) {
		page.detached = true;
		try {
			page.init();
			return rows(page);
		} catch (RuntimeException e) {
			RTFCommon.LOGGER.warn("Could not read the settings of {}", page.getClass().getSimpleName(), e);
			return null;
		}
	}

	private static List<Row> rows(PresetEditorPage page) {
		List<Row> rows = new ArrayList<>();
		SettingIds ids = new SettingIds();
		String section = "";
		for(AbstractWidget widget : page.left.widgets()) {
			if(widget instanceof Label) {
				section = widget.getMessage().getString();
				ids.section(section);
				continue;
			}
			// only the widgets made by PresetWidgets (the others are not settings, or are not known)
			SettingControl control = SettingControl.of(widget);
			if(control != null) {
				rows.add(new Row(ids.next(control.key), section, widget, control));
			}
		}
		return rows;
	}

	private static List<Item> items(List<Row> rows) {
		List<String> names = new ArrayList<>(rows.size());
		for(Row row : rows) {
			names.add(Component.translatable(row.control().key).getString());
		}
		boolean[] repeated = SettingSearch.repeated(names);
		List<Item> items = new ArrayList<>(rows.size());
		for(int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			items.add(new Item(row.id(), names.get(i), row.section(), repeated[i], row.widget().getMessage()));
		}
		return items;
	}

	private Map<Class<?>, Map<String, DefaultValue>> defaultsOfScreen() {
		return DEFAULTS.computeIfAbsent(this.screen, (screen) -> {
			Map<Class<?>, Map<String, DefaultValue>> pages = new HashMap<>();
			for(PresetEditorPage page : pages(this.screen, this.preset.withPreset(Presets.makeDefault()))) {
				List<Row> rows = build(page);
				if(rows != null) {
					Map<String, DefaultValue> values = new HashMap<>();
					for(Row row : rows) {
						values.put(row.id(), row.control().snapshot());
					}
					pages.put(page.getClass(), values);
				}
			}
			return pages;
		});
	}

	// the other pages built on a copy (the structure page adds the sets it shows to the preset it is built on)
	private List<PageInfo> pageInfos(Map<Class<?>, Map<String, DefaultValue>> defaults) {
		List<PageInfo> pages = new ArrayList<>();
		PresetEntry copy = this.preset.withPreset(this.preset.getPreset().copy());
		for(PresetEditorPage page : pages(this.screen, copy)) {
			if(page.getClass() == this.getClass()) {
				pages.add(new PageInfo(page.getClass(), page.title(), false, null));
				continue;
			}
			List<Row> rows = build(page);
			if(rows == null) {
				continue;
			}
			Map<String, DefaultValue> values = defaults.getOrDefault(page.getClass(), Map.of());
			boolean changed = false;
			for(Row row : rows) {
				DefaultValue value = values.get(row.id());
				changed |= value != null && !Objects.equals(value.state(), row.control().state());
			}
			pages.add(new PageInfo(page.getClass(), page.title(), changed, items(rows)));
		}
		return pages;
	}

	@Override
	protected boolean isDetached() {
		return this.detached;
	}

	@Override
	public void afterInit() {
		if(this.detached) {
			return;
		}
		this.rows = rows(this);
		this.pageWidgets = this.left.widgets();
		this.rowsByWidget.clear();
		this.changed.clear();
		Map<Class<?>, Map<String, DefaultValue>> defaults = this.defaultsOfScreen();
		this.defaults = defaults.getOrDefault(this.getClass(), Map.of());
		for(Row row : this.rows) {
			this.rowsByWidget.put(row.widget(), row);
			// a widget's change can only change its own value (the others' greying and tooltips go through setUsable)
			row.control().setListener(() -> this.refresh(row));
			this.refresh(row);
		}
		this.pages = this.pageInfos(defaults);
		this.left.setMarked(this.changed::contains);
		this.left.onRightClick(this::reset);
		if(!this.query.isEmpty()) {
			this.search(this.query);
		}
		if(this.reveal != null) {
			this.reveal(this.reveal);
			this.reveal = null;
		}
	}

	private void refresh(Row row) {
		DefaultValue value = this.defaults.get(row.id());
		boolean differs = value != null && !Objects.equals(value.state(), row.control().state());
		row.control().setDefaultText(differs ? value.text() : null);
		if(differs) {
			this.changed.add(row.widget());
		} else {
			this.changed.remove(row.widget());
		}
	}

	// a right click on a setting that is not at Default: back to Default through the widget's callback, as a click does.
	// Greyed-out ones too: the reset is the same everywhere, and the setting comes back when its switch does
	private boolean reset(AbstractWidget widget) {
		Row row = this.rowsByWidget.get(widget);
		DefaultValue value = row != null && this.changed.contains(widget) ? this.defaults.get(row.id()) : null;
		if(value == null) {
			return false;
		}
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		row.control().restore(value.value());
		return true;
	}

	// for the GUI test and the tabs
	public boolean isChanged(AbstractWidget widget) {
		return this.changed.contains(widget);
	}

	// whether a page has settings that are not Default's (for another page: as they were when this one was opened)
	public boolean hasChanges(Class<?> page) {
		if(page == this.getClass()) {
			return !this.changed.isEmpty();
		}
		for(PageInfo info : this.pages) {
			if(info.type() == page) {
				return info.changed();
			}
		}
		return false;
	}

	// a tab of the screen that opens a page: marked while that page has settings that are not Default's
	public void addTab(AbstractWidget button, Class<?> page) {
		this.tabs.add(new Tab(button, page));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		for(Tab tab : this.tabs) {
			if(tab.button().visible && this.hasChanges(tab.page())) {
				ChangeMarker.extractRenderState(graphics, tab.button());
			}
		}
	}

	@Nullable
	public EditBox searchBox() {
		return this.searchBox;
	}

	// right of the page title (LinkedPageScreen draws it at x 16) and above the list, its right edge on the widgets' one;
	// narrower on small screens, never hidden there
	@Nullable
	private EditBox createSearchBox() {
		Font font = Minecraft.getInstance().font;
		int rowWidth = this.left.getRowWidth();
		int optionWidth = Math.min(450, rowWidth);
		int right = this.left.getRowLeft() + (rowWidth - optionWidth) / 2 + optionWidth;
		int leftLimit = Math.max(this.left.getRowLeft(), 16 + font.width(this.title()) + 8);
		int width = Math.min(SEARCH_WIDTH, right - leftLimit);
		int x = right - width;
		if(width < SEARCH_MIN_WIDTH) {
			// a long title on a narrow screen: above the right column instead, where nothing is drawn
			int column = Math.min(SEARCH_WIDTH, this.right.getRowWidth());
			if(column > width) {
				x = this.right.getRowLeft();
				width = column;
			}
		}
		if(width < 20) {
			return null;
		}
		EditBox box = new EditBox(font, x, 6, width, 18, Component.translatable(SEARCH_HINT));
		box.setMaxLength(64);
		box.setHint(Component.translatable(SEARCH_HINT));
		box.setValue(this.query);
		box.setResponder(this::search);
		((ScreenInvoker) this.screen).invokeAddRenderableWidget(box);
		return box;
	}

	// the settings of all the pages whose name (or section: a structure set's name) has the query, in the list in place
	// of this page's widgets; an empty query brings them back where they were
	private void search(String query) {
		this.query = query;
		SettingSearch search = SettingSearch.of(query);
		if(search.isEmpty()) {
			if(this.searching) {
				this.searching = false;
				this.left.setWidgets(this.pageWidgets);
				this.left.setScrollAmount(this.scrollBeforeSearch);
			}
			return;
		}
		if(!this.searching) {
			this.scrollBeforeSearch = this.left.scrollAmount();
			this.searching = true;
		}
		List<AbstractWidget> results = new ArrayList<>();
		for(PageInfo page : this.pages) {
			for(Item item : page.items() != null ? page.items() : items(this.rows)) {
				if(search.matches(item.name()) || search.matches(item.section())) {
					results.add(this.result(page, item));
				}
			}
		}
		if(results.isEmpty()) {
			results.add(PresetWidgets.createLabel(SEARCH_NONE));
		}
		this.left.setWidgets(results);
		this.left.setScrollAmount(0.0);
	}

	// "Page » Setting: value", with the section when the page has the setting more than once (the structure sets)
	private AbstractWidget result(PageInfo page, Item item) {
		MutableComponent text = Component.empty().append(page.title().copy().withStyle(ChatFormatting.GRAY)).append(Component.literal(" » ").withStyle(ChatFormatting.GRAY));
		if(item.repeated() && !item.section().isEmpty()) {
			text.append(Component.literal(item.section() + " » ").withStyle(ChatFormatting.GRAY));
		}
		text.append(item.message());
		return Button.builder(text, (button) -> this.jump(page.type(), item.id())).build();
	}

	private void jump(Class<?> type, String id) {
		if(type == this.getClass()) {
			if(this.searchBox != null) {
				this.searchBox.setValue("");
			}
			this.search("");
			this.reveal(id);
			return;
		}
		for(PresetEditorPage page : pages(this.screen, this.preset)) {
			if(page.getClass() == type) {
				page.reveal = id;
				this.screen.setPage(page);
				return;
			}
		}
	}

	private void reveal(String id) {
		for(Row row : this.rows) {
			if(row.id().equals(id)) {
				this.left.reveal(row.widget());
				this.left.highlight(row.widget());
				return;
			}
		}
	}

	// the page's own scroll while the search results are shown, so it comes back there
	@Override
	public double getScroll() {
		return this.searching ? this.scrollBeforeSearch : super.getScroll();
	}

	// for the widget test (RTFGuiTest): how often the preview was drawn, a hash of its last picture, and whether a new
	// picture is on its way (it is drawn off the render thread)
	public int previewGeneration() {
		return this.preview.generation;
	}

	public int previewHash() {
		return this.preview.hash;
	}

	public boolean previewBusy() {
		return this.preview.dirty || this.preview.pending != null;
	}

	protected void regenerate() {
		if(this.preview != null) {
			this.preview.regenerate();
		}
	}

	@Override
	public void init() {
		super.init();
		if(this.detached) {
			return;
		}
		this.tabs.clear();
		this.searching = false;

		if(this.preview != null) {
			this.preview.close();
		}
		// the tile caches of a world that was left
		try {
			CacheManager.clear();
		} catch (Exception e) {
			RTFCommon.LOGGER.error("Could not clear the generator caches for the preview", e);
		}

		this.renderMode = PresetWidgets.createCycle(ImmutableList.copyOf(RenderMode.values()), this.renderMode != null ? this.renderMode.getValue() : RenderMode.BIOME_TYPE, Optional.empty(), (button, value) -> {
			this.regenerate();
		}, RenderMode::name);
		this.seed = PresetWidgets.createRandomButton(RTFTranslationKeys.GUI_BUTTON_SEED, (int) this.screen.getSettings().options().seed(), (i) -> {
			this.screen.setSeed(i);
			this.regenerate();
		});

		this.preview = new Preview();
		this.preview.regenerate();

		this.right.addWidget(this.renderMode);
		this.right.addWidget(this.seed);
		this.right.addWidget(this.preview);
		this.searchBox = this.createSearchBox();
	}
	
	// nothing saved here: going to another page keeps the edits in the screen's copy, Done saves them, Cancel drops them
	@Override
	public void onClose() {
		super.onClose();

		if(this.preview != null) {
			this.preview.close();
		}
	}
	
	@Override
	public void onDone() {
		super.onDone();
		
		Toasts.tryOrToast(RTFTranslationKeys.GUI_CREATE_WORLD_APPLY_FAILED, () -> this.screen.applyPreset(this.preset));
	}
	
	private static Identifier previewTexture() {
		if(previewTexture == null) {
			previewTexture = new DynamicTexture(() -> "ReTerraForged preset preview", new NativeImage(Preview.SIZE, Preview.SIZE, false));
			previewTextureId = RTFCommon.location("preview_framebuffer");
			Minecraft.getInstance().getTextureManager().register(previewTextureId, previewTexture);
		}
		return previewTextureId;
	}
	
	// a picture of the preview, made off the render thread
	private record Drawn(Tile tile, Levels levels, int topY, int[] colors, int hash, int centerX, int centerZ, int zoom) {
	}

	// a picture being made: a closed preview (another page, a resize, the screen left) stops it between its steps and
	// stops waiting for its tile, whose filters then do not run. Each page made a new preview, and the old one's picture
	// was still drawn to the end on the background threads
	private static final class Job {
		private volatile boolean cancelled;
		@Nullable
		private volatile CompletableFuture<Tile> tile;

		private void check() {
			if(this.cancelled) {
				throw new CancellationException("preset preview closed");
			}
		}

		private Tile join(CompletableFuture<Tile> tile) {
			this.tile = tile;
			// after publishing it: a cancel in between either sees the tile or is seen here
			this.check();
			return tile.join();
		}

		private void cancel() {
			this.cancelled = true;
			CompletableFuture<Tile> tile = this.tile;
			if(tile != null) {
				tile.cancel(false);
			}
		}
	}
	
	public class Preview extends AbstractWidget {
	    private static final int FACTOR = 4;
	    public static final int SIZE = (1 << 4) << FACTOR;
	    private static final float[] LEGEND_SCALES = { 1, 0.9F, 0.75F, 0.6F };
	    private Identifier textureId = previewTexture();
	    private Tile tile;
	    private int centerX, centerZ, drawnZoom = 1;
	    
	    private String hoveredCoords = "";
	    //TODO maybe make this a map or something instead?
	    private String[] legendValues = {"", "", ""};
	    private Component[] legendLabels = { Component.translatable(RTFTranslationKeys.GUI_LABEL_PREVIEW_AREA), Component.translatable(RTFTranslationKeys.GUI_LABEL_PREVIEW_TERRAIN), Component.translatable(RTFTranslationKeys.GUI_LABEL_PREVIEW_BIOME) };
	    
	    private int offsetX = PresetEditorPage.this.screen.previewOffsetX, offsetZ = PresetEditorPage.this.screen.previewOffsetZ;
	    private boolean clicked = false;
	    private int clickOffsetX, clickOffsetZ;
	    private int zoomValue = PresetEditorPage.this.screen.previewZoom;
	    private int generation;
	    private int hash;
	    // the world top and the preview's levels: the legend tells where the terrain goes above the top (cut flat there)
	    private int topY = Integer.MAX_VALUE;
	    private Levels levels;
	    // a change asked for a new picture; only one is made at a time: a dragged slider asks far more often than it can
	    // be drawn, and everything between the last two pictures is skipped
	    private boolean dirty;
	    private boolean closed;
	    @Nullable
	    private CompletableFuture<Drawn> pending;
	    @Nullable
	    private Job job;
	
	    public Preview() {
	        super(-1, -1, -1, -1, CommonComponents.EMPTY);
	    }

	    public void regenerate() {
	    	this.dirty = true;
	    }
	    
	    private void pollPicture() {
	    	if(this.pending != null && this.pending.isDone()) {
	    		try {
	    			this.show(this.pending.join());
	    		} catch (RuntimeException e) {
	    			RTFCommon.LOGGER.error("Could not draw the preset preview", e);
	    		}
	    		this.pending = null;
	    		this.job = null;
	    	}
	    	if(this.pending == null && this.dirty) {
	    		this.dirty = false;
	    		// on this thread what the sliders change; off it the rest, which took long enough to stop the game at each move
	    		Preset edited = PresetEditorPage.this.preset.getPreset().copy();
	    		WorldCreationContext settings = PresetEditorPage.this.screen.getSettings();
	    		RegistryAccess.Frozen registries = settings.worldgenLoadContext();
	    		int seed = GeneratorContext.terrainSeed(settings.options().seed(), edited);
	    		RenderMode mode = PresetEditorPage.this.renderMode.getValue();
	    		int x = this.offsetX, z = this.offsetZ, zoom = this.getZoom();
	    		Job job = new Job();
	    		this.job = job;
	    		this.pending = CompletableFuture.supplyAsync(() -> draw(job, edited, registries, seed, x, z, zoom, mode), Util.backgroundExecutor());
	    	}
	    }
	    
	    private static Drawn draw(Job job, Preset edited, RegistryAccess.Frozen registries, int seed, int centerX, int centerZ, int zoom, RenderMode renderMode) {
	        job.check();
	        HolderLookup.Provider provider = edited.buildPatch(registries);
	        HolderGetter<Preset> presets = provider.lookupOrThrow(RTFRegistries.PRESET);
	        HolderGetter<Noise> noises = provider.lookupOrThrow(RTFRegistries.NOISE);
	        Preset preset = presets.getOrThrow(Preset.KEY).value();
	        WorldSettings.Properties properties = preset.world().properties;
	        
	        job.check();
	        GeneratorContext generatorContext = GeneratorContext.makeUncached(preset, noises, seed, FACTOR, 0, PerformanceConfig.makeDefault().batchCount());
	        job.check();
	        Tile tile = job.join(generatorContext.generator.generateZoomed(centerX, centerZ, zoom, false));
	        job.check();
	        Levels levels = new Levels(properties.terrainScaler(), properties.seaLevel);
	        int topY = properties.worldHeight - 1;

	        int stroke = 2;
	        int width = tile.getBlockSize().size();
	        int[] colors = new int[width * width];
	        int[] hash = { 1 };
	        tile.iterate((cell, x, z) -> {
	            if (x < stroke || z < stroke || x >= width - stroke || z >= width - stroke) {
	                colors[z * width + x] = Color.BLACK.getRGB();
	            } else {
	                int color = ARGB.fromABGR(renderMode.getColor(cell, levels, topY));
	                colors[z * width + x] = color;
	                hash[0] = hash[0] * 31 + color;
	            }
	        });
	        return new Drawn(tile, levels, topY, colors, hash[0], centerX, centerZ, zoom);
	    }
	    
	    private void show(Drawn drawn) {
	    	if(this.closed) {
	    		return;
	    	}
	    	this.tile = drawn.tile();
	    	this.levels = drawn.levels();
	    	this.topY = drawn.topY();
	    	this.centerX = drawn.centerX();
	    	this.centerZ = drawn.centerZ();
	    	this.drawnZoom = drawn.zoom();
	    	int width = this.tile.getBlockSize().size();
	    	NativeImage pixels = previewTexture.getPixels();
	    	int[] colors = drawn.colors();
	    	for(int z = 0; z < width; z++) {
	    		for(int x = 0; x < width; x++) {
	    			pixels.setPixel(x, z, colors[z * width + x]);
	    		}
	    	}
	    	previewTexture.upload();
	    	this.generation++;
	    	this.hash = drawn.hash();
	    }
	    
	    // the texture is the pages' one (previewTexture), kept for the next preview: nothing of this one to free
	    public void close() {
	    	this.closed = true;
	    	if(this.job != null) {
	    		this.job.cancel();
	    		this.job = null;
	    	}
	    	if(this.pending != null) {
	    		this.pending.cancel(false);
	    		this.pending = null;
	    	}
	    }
	
	    @Override
	    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
	        // No narration needed
	    }
	
	    @Override
	    public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mx, int my, float partialTicks) {
	        this.pollPicture();
	        
	        int x = this.getX();
	        int y = this.getY();
	        
	        this.height = this.getWidth(); // ensure height equals width for click area
	        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, this.textureId, x, y, 0, 0, this.width, this.height, this.width, this.height);

	        this.updateLegend(mx, my);

	        this.renderLegend(guiGraphics, mx, my, this.legendLabels, this.legendValues, x, y + this.width, 10, 0xFFFFFFFF);
	    }
	
	    @Override
	    public void onClick(MouseButtonEvent event, boolean doubleClick) {
	        if (Minecraft.getInstance().gui.screen() != null) {
	            Minecraft.getInstance().gui.screen().setFocused(this);
	        }
	        clicked = true;
	        clickOffsetX = offsetX;
	        clickOffsetZ = offsetZ;
	    }
	
	    @Override
	    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
	        int zoom = getZoom();
	        double blocksPerPixel = zoom; // preview size = 256
	        offsetX -= dragX * blocksPerPixel;
	        offsetZ -= dragY * blocksPerPixel;
	        PresetEditorPage.this.screen.previewOffsetX = offsetX;
	        PresetEditorPage.this.screen.previewOffsetZ = offsetZ;
	        regenerate();
	    }
	
	    @Override
	    public void onRelease(MouseButtonEvent event) {
	        double mouseX = event.x(), mouseY = event.y();
	        if (!clicked) {
	            return;
	        }
	        clicked = false;
	
	        // Check if drag distance is minimal (i.e., click)
	        int dragDeltaX = offsetX - clickOffsetX;
	        int dragDeltaZ = offsetZ - clickOffsetZ;
	        if (Math.abs(dragDeltaX) <= 4 && Math.abs(dragDeltaZ) <= 4) {
	            // Treat as click: copy coordinates if hoveredCoords is not empty
	            if (updateLegend((int) mouseX, (int) mouseY) && !hoveredCoords.isEmpty()) {
	                playDownSound(Minecraft.getInstance().getSoundManager());
	                PresetEditorPage.this.screen.minecraft.keyboardHandler.setClipboard(hoveredCoords);
	            }
	        }
	        // Otherwise, drag already applied in onDrag, nothing more to do
	    }
	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
	    return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
	    if (this.isMouseOver(mouseX, mouseY)) {
	        // Dynamic step: larger when zoom is small (close to 1), smaller when zoom is large (close to 100)
	        int dynamicStep = Math.max(1, (100 - zoomValue) / 10);
	        int step = (int) Math.signum(scrollY) * dynamicStep;
	        zoomValue = Math.max(1, Math.min(100, zoomValue + step));
	        PresetEditorPage.this.screen.previewZoom = zoomValue;
	        regenerate();
	        return true;
	    }
	    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	
	    @Override
	    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
	        boolean over = isMouseOver(event.x(), event.y());
	        if (over && event.button() == 0) {
	            // Ensure onClick is called even if super.mouseClicked doesn't call it
	            onClick(event, doubleClick);
	        }
	        boolean result = super.mouseClicked(event, doubleClick);
	        // Always return true if mouse is over to indicate event handled
	        return over || result;
	    }
	
	    @Override
	    public boolean isMouseOver(double mouseX, double mouseY) {
	        return super.isMouseOver(mouseX, mouseY);
	    }
	
	    private boolean updateLegend(int mx, int my) {
	        if (this.tile != null) {
	            int left = this.getX();
	            int top = this.getY();
	            float size = this.width;
	
	            // of the picture on the screen: the mouse wheel may have asked for another one already
	            int zoom = this.drawnZoom;
	            int width = Math.max(1, this.tile.getBlockSize().size() * zoom);
	            int height = Math.max(1, this.tile.getBlockSize().size() * zoom);
	            this.legendValues[0] = width + "x" + height;
	            if (mx >= left && mx <= left + size && my >= top && my <= top + size) {
	                float fx = (mx - left) / size;
	                float fz = (my - top) / size;
	                int ix = NoiseUtil.round(fx * this.tile.getBlockSize().size());
	                int iz = NoiseUtil.round(fz * this.tile.getBlockSize().size());
	                Cell cell = this.tile.lookup(ix, iz);
	                this.legendValues[1] = getTerrainName(cell);
	                int y = this.levels != null ? this.levels.scale(cell.height) : 0;
	                if (y > this.topY) {
	                    this.legendValues[1] += " (y " + y + " > " + this.topY + ")";
	                }
	                this.legendValues[2] = getBiomeName(cell);
	
	                int dx = (ix - (this.tile.getBlockSize().size() / 2)) * zoom;
	                int dz = (iz - (this.tile.getBlockSize().size() / 2)) * zoom;
	
	                this.hoveredCoords = (this.centerX + dx) + ":" + (this.centerZ + dz);
	                return true;
	            } else {
	            	this.hoveredCoords = "";
	            }
	        }
	        return false;
	    }

	    private float getLegendScale() {
	        int index = PresetEditorPage.this.screen.minecraft.options.guiScale().get() - 1;
	        if (index < 0 || index >= LEGEND_SCALES.length) {
	            // index=-1 == GuiScale(AUTO) which is the same as GuiScale(4)
	            // values above 4 don't exist but who knows what mods might try set it to
	            // in both cases use the smallest acceptable scale
	            index = LEGEND_SCALES.length - 1;
	        }
	        return LEGEND_SCALES[index];
	    }

	    private void renderLegend(GuiGraphicsExtractor guiGraphics, int mx, int my, Component[] labels, String[] values, int left, int top, int lineHeight, int color) {
	        float scale = this.getLegendScale();
	        Matrix3x2fStack pose = guiGraphics.pose();
	        	
	        pose.pushMatrix();
	        pose.translate(left + 3.75F * scale, top - lineHeight * (3.2F * scale));
	        pose.scale(scale, scale);
	
	        Minecraft mc = Minecraft.getInstance();
	        Font renderer = mc.font;
	        int spacing = 0;
	        for (Component s : labels) {
	            spacing = Math.max(spacing, renderer.width(s));
	        }
	
	        float maxWidth = (this.width - 4) / scale;
	        for (int i = 0; i < labels.length && i < values.length; i++) {
	        	Component label = labels[i];
	            String value = values[i];
	
	            while (value.length() > 0 && spacing + renderer.width(value) > maxWidth) {
	                value = value.substring(0, value.length() - 1);
	            }
	
	            guiGraphics.text(renderer, label, 0, i * lineHeight, color);
	            guiGraphics.text(renderer, value, spacing, i * lineHeight, color);
	        }
	
	        pose.popMatrix();
	
	        if (!this.hoveredCoords.isEmpty()) {
	        	guiGraphics.centeredText(renderer, this.hoveredCoords, mx, my - 10, 0xFFFFFFFF);
	        }
	    }
	
	    private int getZoom() {
	        return NoiseUtil.round(1.5F * (101 - (float) this.zoomValue));
	    }
	
	    private static String getTerrainName(Cell cell) {
	        if (cell.terrain.isRiver()) {
	            return "river";
	        }
	        return cell.terrain.getName().toLowerCase();
	    }
	
	    private static String getBiomeName(Cell cell) {
	        String terrain = cell.terrain.getName().toLowerCase();
	        if (terrain.contains("ocean")) {
	            if (cell.temperature < 0.3F) {
	                return "cold_" + terrain;
	            }
	            if (cell.temperature > 0.6F) {
	                return "warm_" + terrain;
	            }
	            return terrain;
	        }
	        if (terrain.contains("river")) {
	            return "river";
	        }
	        return cell.biome.name().toLowerCase();
	    }
	}
}
