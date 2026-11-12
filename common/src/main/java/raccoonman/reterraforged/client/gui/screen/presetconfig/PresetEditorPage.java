package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.awt.Color;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.NativeImage;
import org.joml.Matrix3x2fStack;

import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.client.gui.Toasts;
import raccoonman.reterraforged.client.gui.screen.page.BisectedPage;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetListPage.PresetEntry;
import raccoonman.reterraforged.client.gui.widget.ValueButton;
import raccoonman.reterraforged.concurrent.cache.CacheManager;
import raccoonman.reterraforged.config.PerformanceConfig;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

public abstract class PresetEditorPage extends BisectedPage<PresetConfigScreen, AbstractWidget, AbstractWidget> {
	// one texture for all the pages: a new one registered for each page stayed in the TextureManager after it was closed
	@Nullable
	private static DynamicTexture previewTexture;
	@Nullable
	private static Identifier previewTextureId;
	private CycleButton<RenderMode> renderMode;
	private ValueButton<Integer> seed;
	private Preview preview;
	protected final PresetEntry preset;
	
	public PresetEditorPage(PresetConfigScreen screen, PresetEntry preset) {
		super(screen);
		
		this.preset = preset;
	}
	
	public PresetEntry getPreset() {
		return this.preset;
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
		this.preview.regenerate();
	}
	
	@Override
	public void init() {
		super.init();

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
	}
	
	// nothing saved here: going to another page keeps the edits in the screen's copy, Done saves them, Cancel drops them
	@Override
	public void onClose() {
		super.onClose();
		
		this.preview.close();
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
	    public void renderWidget(GuiGraphics guiGraphics, int mx, int my, float partialTicks) {
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
	        if (Minecraft.getInstance().screen != null) {
	            Minecraft.getInstance().screen.setFocused(this);
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

	    private void renderLegend(GuiGraphics guiGraphics, int mx, int my, Component[] labels, String[] values, int left, int top, int lineHeight, int color) {
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
	
	            guiGraphics.drawString(renderer, label, 0, i * lineHeight, color);
	            guiGraphics.drawString(renderer, value, spacing, i * lineHeight, color);
	        }
	
	        pose.popMatrix();
	
	        if (!this.hoveredCoords.isEmpty()) {
	        	guiGraphics.drawCenteredString(renderer, this.hoveredCoords, mx, my - 10, 0xFFFFFFFF);
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
