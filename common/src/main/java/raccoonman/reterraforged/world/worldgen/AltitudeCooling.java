package raccoonman.reterraforged.world.worldgen;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.RandomState;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;

/**
 * Vanilla cools a biome above y 80 by its height (Biome.getHeightAdjustedTemperature): rain turns into snow, and snow
 * has no sound. RTF scales its terrain to the world height, so in a taller world (700 instead of 320) most land sits
 * so high that it snows nearly everywhere and rain is never heard. With "Altitude Cooling Follows World Height" the
 * height above 80 counts as if the world had the vanilla height.
 *
 * Only in a dimension RTF generates: biomes are shared by all dimensions and Biome has no level, so the scale is per
 * thread. The entries that know their level set it (mixin.cooling: a server level's tick, Level.isRainingAt, a
 * chunk's features and surface); the client sets its main thread's from the server's packet (sent for every dimension
 * the player enters). Anywhere else the server's default applies, the overworld's preset: what every path had before,
 * so the overworld never loses its scale.
 */
public final class AltitudeCooling {
	/** Vanilla's overworld top (RTF's default world height) */
	public static final float VANILLA_HEIGHT = 320.0F;
	/** Where vanilla starts to cool a biome */
	private static final int COOLING_START = 80;
	/** No scale of the thread's own: the default applies */
	public static final float UNSET = Float.NaN;

	private static volatile float scale = 1.0F;
	// an array, not a Float: no boxing on the hot path
	private static final ThreadLocal<float[]> THREAD = ThreadLocal.withInitial(() -> new float[] { UNSET });

	private AltitudeCooling() {
	}

	/** The scale on this thread: its own, else the default */
	public static float scale() {
		float s = THREAD.get()[0];
		return Float.isNaN(s) ? scale : s;
	}

	/** The server's default (the overworld's preset) */
	public static void set(float value) {
		scale = sanitize(value);
	}

	public static void reset() {
		scale = 1.0F;
	}

	/** This thread's own scale until exit(previous); returns the previous one to restore (calls nest) */
	public static float enter(float value) {
		float[] holder = THREAD.get();
		float previous = holder[0];
		holder[0] = sanitize(value);
		return previous;
	}

	public static void exit(float previous) {
		THREAD.get()[0] = previous;
	}

	/** The client's scale, on its main thread (the server's packet): the integrated server's threads keep theirs */
	public static void setClient(float value) {
		THREAD.get()[0] = sanitize(value);
	}

	public static void clearClient() {
		THREAD.get()[0] = UNSET;
	}

	private static float sanitize(float value) {
		return value > 0.0F && Float.isFinite(value) ? value : 1.0F;
	}

	/** The scale of a preset: 1 when the setting is off */
	public static float of(Preset preset) {
		WorldSettings.Properties properties = preset.world().properties;
		return properties.altitudeCoolingFollowsHeight ? VANILLA_HEIGHT / Math.max(16, properties.worldHeight) : 1.0F;
	}

	/** The scale of a random state: its RTF preset's, or 1 for one RTF does not generate with */
	public static float of(@Nullable RandomState randomState) {
		if ((Object) randomState instanceof RTFRandomState rtf) {
			Preset preset = rtf.preset();
			if (preset != null) {
				return of(preset);
			}
		}
		return 1.0F;
	}

	/** The scale of a server level: its RTF preset's, or 1 for a dimension RTF does not generate */
	public static float of(ServerLevel level) {
		return of(level.getChunkSource().randomState());
	}

	/** The position whose height vanilla's cooling should see */
	public static BlockPos adjust(BlockPos pos) {
		// most positions are below the cooling: no thread local read for them
		if (pos.getY() <= COOLING_START) {
			return pos;
		}
		float s = scale();
		if (s == 1.0F) {
			return pos;
		}
		return new BlockPos(pos.getX(), COOLING_START + Math.round((pos.getY() - COOLING_START) * s), pos.getZ());
	}
}
