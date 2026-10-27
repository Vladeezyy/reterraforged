package raccoonman.reterraforged.world.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;

/**
 * Vanilla cools a biome above y 80 by its height (Biome.getHeightAdjustedTemperature): rain turns into snow, and snow
 * has no sound. RTF scales its terrain to the world height, so in a taller world (700 instead of 320) most land sits
 * so high that it snows nearly everywhere and rain is never heard. With "Altitude Cooling Follows World Height" the
 * height above 80 counts as if the world had the vanilla height. The server takes the scale from the overworld's
 * preset; clients get it in a packet when they join (the rain they see and hear is decided on the client).
 */
public final class AltitudeCooling {
	/** Vanilla's overworld top (RTF's default world height) */
	public static final float VANILLA_HEIGHT = 320.0F;
	/** Where vanilla starts to cool a biome */
	private static final int COOLING_START = 80;

	private static volatile float scale = 1.0F;

	private AltitudeCooling() {
	}

	public static float scale() {
		return scale;
	}

	public static void set(float value) {
		scale = value > 0.0F && Float.isFinite(value) ? value : 1.0F;
	}

	public static void reset() {
		scale = 1.0F;
	}

	/** The scale of a preset: 1 when the setting is off */
	public static float of(Preset preset) {
		WorldSettings.Properties properties = preset.world().properties;
		return properties.altitudeCoolingFollowsHeight ? VANILLA_HEIGHT / Math.max(16, properties.worldHeight) : 1.0F;
	}

	/** The scale of a server level: the RTF preset's, or 1 for a world RTF does not generate */
	public static float of(ServerLevel level) {
		if ((Object) level.getChunkSource().randomState() instanceof RTFRandomState rtf && rtf.preset() != null) {
			return of(rtf.preset());
		}
		return 1.0F;
	}

	/** The position whose height vanilla's cooling should see */
	public static BlockPos adjust(BlockPos pos) {
		float s = scale;
		if (s == 1.0F || pos.getY() <= COOLING_START) {
			return pos;
		}
		return new BlockPos(pos.getX(), COOLING_START + Math.round((pos.getY() - COOLING_START) * s), pos.getZ());
	}
}
