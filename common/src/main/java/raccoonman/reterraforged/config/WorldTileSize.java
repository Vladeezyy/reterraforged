package raccoonman.reterraforged.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import raccoonman.reterraforged.RTFCommon;

/**
 * The tile size a world was made with. Erosion runs per tile, so another size (another server's performance.conf, an
 * edited one) put seams between the chunks made before and after. The first start of a world writes the configured
 * size into the world's folder; later starts take it from there, and the config applies to new worlds only. A world
 * made before this has no file: it gets the configured size, as it always did, and keeps it from then on.
 *
 * A small JSON file rather than SavedData: its API changes between Minecraft versions, a file in the world's folder
 * does not. Previews and random states made outside a ChunkMap know no world: they take the config (or the running
 * world's size, current).
 */
public final class WorldTileSize {
	/** In the world's folder, next to level.dat */
	public static final String FILE = "reterraforged/generation.json";
	private static final String KEY = "tileSize";

	// the running world's: every dimension asks, the file is read once and the line logged once
	@Nullable
	private static Path cachedDir;
	private static int cachedSize;

	private WorldTileSize() {
	}

	/** The tile size of the level's world (a server level a ChunkMap set up) */
	public static int of(ServerLevel level, int configured) {
		return of(level.getServer().getWorldPath(LevelResource.ROOT), configured);
	}

	/** The tile size of the world in this folder: the stored one, else the configured one, stored */
	public static synchronized int of(Path worldDir, int configured) {
		Path dir = worldDir.toAbsolutePath().normalize();
		if (dir.equals(cachedDir)) {
			return cachedSize;
		}
		int size = resolve(dir.resolve(FILE), clamp(configured));
		cachedDir = dir;
		cachedSize = size;
		return size;
	}

	/** The running world's tile size if one was resolved, else the configured one (a random state made outside a ChunkMap) */
	public static synchronized int current(int configured) {
		return cachedDir != null ? cachedSize : configured;
	}

	/** The world stopped: the next one is read from its own folder */
	public static synchronized void clear() {
		cachedDir = null;
	}

	private static int resolve(Path file, int configured) {
		if (Files.exists(file)) {
			Integer stored = read(file);
			if (stored == null) {
				// not overwritten: whoever broke it may still fix it
				RTFCommon.LOGGER.warn("[RTF] could not read the tile size in {}: using performance.conf's {}", file, configured);
				return configured;
			}
			int size = clamp(stored);
			if (size != configured) {
				RTFCommon.LOGGER.info("[RTF] this world was made with tileSize {}: performance.conf's {} applies to new worlds only", size, configured);
			}
			return size;
		}
		write(file, configured);
		return configured;
	}

	@Nullable
	private static Integer read(Path file) {
		try (Reader reader = Files.newBufferedReader(file)) {
			JsonElement json = JsonParser.parseReader(reader);
			JsonElement value = json.isJsonObject() ? json.getAsJsonObject().get(KEY) : null;
			if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
				return value.getAsInt();
			}
		} catch (IOException | RuntimeException e) {
			// RuntimeException: Gson's JsonParseException on a broken file
			RTFCommon.LOGGER.debug("[RTF] {} is unreadable", file, e);
		}
		return null;
	}

	private static void write(Path file, int size) {
		JsonObject json = new JsonObject();
		json.addProperty(KEY, size);
		try {
			Files.createDirectories(file.getParent());
			// whole or not at all: a crash while writing must not leave a broken file
			Path temp = file.resolveSibling(file.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(temp)) {
				writer.write(json.toString());
				writer.write("\n");
			}
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			// the world still opens with the configured size; the next start tries again
			RTFCommon.LOGGER.error("[RTF] could not store the world's tile size in {}", file, e);
		}
	}

	private static int clamp(int size) {
		return Math.max(1, Math.min(PerformanceConfig.MAX_TILE_SIZE, size));
	}
}
