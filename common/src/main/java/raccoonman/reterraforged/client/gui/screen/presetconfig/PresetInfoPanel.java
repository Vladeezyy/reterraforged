package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import raccoonman.reterraforged.RTFCommon;

/**
 * The selected preset's map and description under the preset list's buttons. Only the drawing: the pictures come from
 * PresetMaps, what is shown from PresetListPage. One texture for the screen, made when a picture is first drawn and
 * freed when the screen is left (PresetConfigScreen.removed), both on the render thread.
 */
final class PresetInfoPanel {
	private static final int MIN_MAP = 64;
	private static final int MAX_MAP = 2 * PresetMapDrawer.SIZE;
	private static final int LINE = 10;
	private static final int TEXT = 0xFFA0A0A0;
	private static final int BACKGROUND = 0xFF101010;
	private static final int FRAME = 0xFF505050;

	private final PresetMaps<PresetMapDrawer.Input> maps;
	@Nullable
	private Shown shown;
	@Nullable
	private DynamicTexture texture;
	@Nullable
	private Identifier textureId;
	// the key of the picture in the texture
	@Nullable
	private String uploaded;
	// what the last frame did, for the GUI test's message: the box, the map's size, the key of the map drawn in it
	private int boxWidth, boxHeight, mapSize;
	@Nullable
	private String drawn;

	PresetInfoPanel(PresetMaps<PresetMapDrawer.Input> maps) {
		this.maps = maps;
	}

	/** What is shown: the map's key (preset and seed) and what it is drawn from, and the description */
	record Shown(String mapKey, PresetMapDrawer.Input input, Component description) {
	}

	void show(@Nullable Shown shown) {
		this.shown = shown;
	}

	/** The selected preset's map is on the screen (the GUI test waits for it) */
	boolean mapShown() {
		return this.shown != null && this.shown.mapKey().equals(this.drawn);
	}

	boolean mapFailed() {
		return this.shown != null && this.maps.failed(this.shown.mapKey());
	}

	/** The page has no room for the box (the smallest windows) */
	void noRoom(int width) {
		this.boxWidth = width;
		this.boxHeight = 0;
		this.mapSize = 0;
		this.drawn = null;
	}

	/** Why the map is or is not on the screen, for the GUI test's log */
	String diagnosis() {
		String box = "box " + this.boxWidth + "x" + this.boxHeight + ", map " + this.mapSize;
		if(this.shown == null) {
			return box + ": nothing selected";
		}
		if(this.boxWidth <= 0 || this.mapSize < MIN_MAP) {
			return box + ": no room (the map needs " + MIN_MAP + ")";
		}
		if(this.mapFailed()) {
			return box + ": the map could not be made (see the log)";
		}
		return box + (this.mapShown() ? ": shown" : ": not made yet" + (this.maps.isBusy() ? " (being made)" : ""));
	}

	/** In the box x, y, width, height: the map as large as fits above the description, or the description alone */
	void render(GuiGraphics graphics, int x, int y, int width, int height) {
		Shown shown = this.shown;
		this.boxWidth = width;
		this.boxHeight = height;
		this.mapSize = 0;
		this.drawn = null;
		if(shown == null || width <= 0 || height < LINE) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		List<FormattedCharSequence> lines = font.split(shown.description(), width);
		int textHeight = lines.size() * LINE;
		// the whole description or none of it: half a sentence says nothing
		boolean text = textHeight <= height;
		int size = Math.min(Math.min(width, MAX_MAP), height - (text ? textHeight + 4 : 0));
		int top = y;
		this.mapSize = size;
		if(size >= MIN_MAP) {
			int left = x + (width - size) / 2;
			// the same box while the picture is made: nothing below it moves when it comes
			graphics.fill(left, top, left + size, top + size, BACKGROUND);
			int[] picture = this.maps.get(shown.mapKey(), shown.input());
			if(picture != null) {
				this.upload(shown.mapKey(), picture);
				graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureId, left + 1, top + 1, 0, 0, size - 2, size - 2, size - 2, size - 2);
				this.drawn = shown.mapKey();
			}
			graphics.renderOutline(left, top, size, size, FRAME);
			top += size + 4;
		}
		if(text) {
			for(FormattedCharSequence line : lines) {
				graphics.drawString(font, line, x, top, TEXT);
				top += LINE;
			}
		}
	}

	private void upload(String key, int[] picture) {
		if(this.texture == null) {
			this.texture = new DynamicTexture(() -> "ReTerraForged preset map", PresetMapDrawer.SIZE, PresetMapDrawer.SIZE, false);
			this.textureId = RTFCommon.location("preset_map");
			Minecraft.getInstance().getTextureManager().register(this.textureId, this.texture);
			this.uploaded = null;
		}
		if(key.equals(this.uploaded)) {
			return;
		}
		NativeImage pixels = this.texture.getPixels();
		int size = PresetMapDrawer.SIZE;
		for(int z = 0; z < size; z++) {
			for(int x = 0; x < size; x++) {
				pixels.setPixel(x, z, ARGB.fromABGR(picture[z * size + x]));
			}
		}
		this.texture.upload();
		this.uploaded = key;
	}

	/** Frees the texture (it closes with its release); a later picture makes a new one */
	void release() {
		if(this.textureId != null) {
			Minecraft.getInstance().getTextureManager().release(this.textureId);
		}
		this.texture = null;
		this.textureId = null;
		this.uploaded = null;
		this.drawn = null;
	}
}
