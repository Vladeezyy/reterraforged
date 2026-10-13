package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.awt.Color;
import java.io.IOException;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import org.joml.Matrix3x2fStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
	private CycleButton<RenderMode> renderMode;
	private ValueButton<Integer> seed;
	private Preview preview;
	protected PresetEntry preset;
	
	public PresetEditorPage(PresetConfigScreen screen, PresetEntry preset) {
		super(screen);
		
		this.preset = preset;
	}
	
	public PresetEntry getPreset() {
		return this.preset;
	}

	// for the widget test (RTFGuiTest): how often the preview was drawn, and a hash of its last picture
	public int previewGeneration() {
		return this.preview.generation;
	}

	public int previewHash() {
		return this.preview.hash;
	}

	protected void regenerate() {
		this.preview.regenerate();
	}
	
	@Override
	public void init() {
		super.init();

		if(this.preview != null) {
			try {
				this.preview.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
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
	
	@Override
	public void onClose() {
		super.onClose();
	
		try {
			this.preset.save();
			this.preview.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onDone() {
		super.onDone();
		
		try {
			this.screen.applyPreset(this.preset);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public class Preview extends AbstractWidget {
	    private static final int FACTOR = 4;
	    public static final int SIZE = (1 << 4) << FACTOR;
	    private static final float[] LEGEND_SCALES = { 1, 0.9F, 0.75F, 0.6F };
	    private DynamicTexture texture = new DynamicTexture(() -> "ReTerraForged preset preview", new NativeImage(SIZE, SIZE, false));
	    private Identifier textureId = registerTexture(this.texture);
	    private Tile tile;
	    private int centerX, centerZ;

	    // one preview is open at a time: registering again under the same id closes the previous texture
	    private static Identifier registerTexture(DynamicTexture texture) {
	    	Identifier id = RTFCommon.location("preview_framebuffer");
	    	Minecraft.getInstance().getTextureManager().register(id, texture);
	    	return id;
	    }
	    
	    private String hoveredCoords = "";
	    //TODO maybe make this a map or something instead?
	    private String[] legendValues = {"", "", ""};
	    private Component[] legendLabels = { Component.translatable(RTFTranslationKeys.GUI_LABEL_PREVIEW_AREA), Component.translatable(RTFTranslationKeys.GUI_LABEL_PREVIEW_TERRAIN), Component.translatable(RTFTranslationKeys.GUI_LABEL_PREVIEW_BIOME) };
	    
	    private int offsetX, offsetZ;
	    private boolean clicked = false;
	    private int clickOffsetX, clickOffsetZ;
	    private int zoomValue = 68; // default zoom value
	    private int generation;
	    private int hash;
	
	    public Preview() {
	        super(-1, -1, -1, -1, CommonComponents.EMPTY);
	    }

	    public void regenerate() {
			WorldCreationContext settings = PresetEditorPage.this.screen.getSettings();
	        RegistryAccess.Frozen registries = settings.worldgenLoadContext();
	        HolderLookup.Provider provider = PresetEditorPage.this.preset.getPreset().buildPatch(registries);
	        HolderGetter<Preset> presets = provider.lookupOrThrow(RTFRegistries.PRESET);
	        HolderGetter<Noise> noises = provider.lookupOrThrow(RTFRegistries.NOISE);
	        Preset preset = presets.getOrThrow(Preset.KEY).value();
	        WorldSettings world = preset.world();
	        WorldSettings.Properties properties = world.properties;
	        
	        try {
				CacheManager.clear();
			} catch (Exception e) {
				e.printStackTrace();
			}
			PerformanceConfig config = PerformanceConfig.read(PerformanceConfig.DEFAULT_FILE_PATH)
				.resultOrPartial(RTFCommon.LOGGER::error)
				.orElseGet(PerformanceConfig::makeDefault);
	        GeneratorContext generatorContext = GeneratorContext.makeUncached(preset, noises, (int) settings.options().seed(), FACTOR, 0, config.batchCount());
	        
	        // Ignore continent center snapping, use offset directly
	        this.centerX = this.offsetX;
	        this.centerZ = this.offsetZ;

	        this.tile = generatorContext.generator.generateZoomed(this.centerX, this.centerZ, this.getZoom(), false).join();
	        RenderMode renderMode = PresetEditorPage.this.renderMode.getValue();
	        Levels levels = new Levels(properties.terrainScaler(), properties.seaLevel);

	        int stroke = 2;
	        int width = this.tile.getBlockSize().size();

	        NativeImage pixels = this.texture.getPixels();
	        int[] hash = { 1 };
	        this.tile.iterate((cell, x, z) -> {
	            if (x < stroke || z < stroke || x >= width - stroke || z >= width - stroke) {
	                pixels.setPixel(x, z, Color.BLACK.getRGB());
	            } else {
	                int color = ARGB.fromABGR(renderMode.getColor(cell, levels));
	                pixels.setPixel(x, z, color);
	                hash[0] = hash[0] * 31 + color;
	            }
	        });
	        this.texture.upload();
	        this.generation++;
	        this.hash = hash[0];
	    }
	    
	    public void close() throws Exception {
	    	this.texture.close();
	    	try {
	   CacheManager.clear();
	  } catch (Exception e) {
	   e.printStackTrace();
	  }
	    }
	
	    @Override
	    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
	        // No narration needed
	    }
	
	    @Override
	    public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mx, int my, float partialTicks) {
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
	        regenerate();
	        return true;
	    }
	    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	
	    @Override
	    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
	        boolean over = isMouseOver(event.x(), event.y());
	        if (over && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
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
	
	            int zoom = this.getZoom();
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
