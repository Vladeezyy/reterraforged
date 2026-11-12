package raccoonman.reterraforged.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.permissions.Permissions;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.concurrent.ThreadPools;
import raccoonman.reterraforged.config.DiagnosticsConfig;
import raccoonman.reterraforged.config.PerformanceConfig;
import raccoonman.reterraforged.config.WorldTileSize;
import raccoonman.reterraforged.data.worldgen.preset.PresetDiff;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;
import raccoonman.reterraforged.platform.ModLoaderUtil;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;

/**
 * What a bug report needs about the running world (#42 could not be reproduced: no seed, no preset), taken once when
 * the overworld loads, before its spawn chunks: into the log, a crash report (MixinSystemReport) and the chat
 * (/reterraforged info). An immutable snapshot: a crash report is made on any thread and must not touch the world.
 */
public record WorldInfo(List<String> head, long seed, boolean showSeedInLog, List<String> tail, @Nullable String notAppliedFolder) {
	public static final String LOG_PREFIX = "[RTF world] ";
	/** Chat to an operator: the preset datapack's biome modifiers are only for the other loader */
	public static final String BIOME_MODIFIERS_NOT_APPLIED = RTFCommon.MOD_ID + ".chat.biomeModifiersNotApplied";
	private static final String BIOME_MODIFIERS_NOT_APPLIED_FALLBACK = "[ReTerraForged] This world's preset datapack has its biome modifiers only in %s, for the other mod loader: the world generates without the preset's trees, erosion and swamp surface. Export the preset again with this version and replace the datapack.";
	// more would bury the rest of the log; the GUI's button has all of them
	private static final int MAX_DIFF_LINES = 40;
	private static final String NOT_RTF = "not a ReTerraForged world";
	// mods that change how chunks are generated or which biomes they get: [name shown, mod ids on Fabric and NeoForge]
	private static final Map<String, List<String>> KNOWN_MODS = knownMods();

	/** What the loader knows: versions and which biome modifier folder it reads */
	public record Platform(String loader, String loaderVersion, String minecraftVersion, String modVersion, String ownModifierFolder, String otherModifierFolder, boolean readsOtherFolder) {
		static final Platform UNKNOWN = new Platform("unknown loader", "?", "?", "?", "", "", true);
	}

	private static volatile Platform platform = Platform.UNKNOWN;
	@Nullable
	private static volatile WorldInfo current;

	public static void setPlatform(Platform value) {
		platform = value;
	}

	/** The running world's, if one was taken */
	@Nullable
	public static WorldInfo current() {
		return current;
	}

	/** The overworld loaded: take the snapshot and log it */
	public static void onOverworldLoad(ServerLevel overworld) {
		try {
			WorldInfo info = capture(overworld);
			current = info;
			for (String line : info.lines(info.showSeedInLog())) {
				RTFCommon.LOGGER.info(LOG_PREFIX + "{}", line);
			}
		} catch (Throwable t) {
			// a report that fails must not stop the world
			RTFCommon.LOGGER.warn(LOG_PREFIX + "could not collect the world's details", t);
		}
	}

	public static void onServerStopped() {
		current = null;
	}

	/** An operator (in single player the world's owner) is told when the preset's trees and erosion are missing; once per join */
	public static void onPlayerJoin(ServerPlayer player) {
		WorldInfo info = current;
		if (info == null || info.notAppliedFolder() == null) {
			return;
		}
		MinecraftServer server = player.level().getServer();
		if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) || (server != null && server.isSingleplayerOwner(player.nameAndId()))) {
			player.sendSystemMessage(Component.translatableWithFallback(BIOME_MODIFIERS_NOT_APPLIED, BIOME_MODIFIERS_NOT_APPLIED_FALLBACK, info.notAppliedFolder()).withStyle(ChatFormatting.GOLD));
		}
	}

	/** The crash report's section: the log's lines, the seed by the same rule. Never throws */
	public static String crashReportDetail() {
		try {
			WorldInfo info = current;
			if (info == null) {
				return "no world running";
			}
			StringBuilder builder = new StringBuilder();
			for (String line : info.lines(info.showSeedInLog())) {
				// the system report's own indent for a value of several lines (as the loaders' mod lists)
				builder.append("\n\t\t").append(line);
			}
			return builder.toString();
		} catch (Throwable t) {
			return "unavailable (" + t + ")";
		}
	}

	/** The lines, the seed shown or hidden */
	public List<String> lines(boolean showSeed) {
		List<String> lines = new ArrayList<>(this.head);
		// not a ReTerraForged world: that line and nothing else
		if (!this.tail.isEmpty()) {
			lines.add(showSeed ? "seed: " + this.seed : "seed: hidden (set logSeed = true in " + configFileName() + " to log it, or run /seed)");
		}
		lines.addAll(this.tail);
		return lines;
	}

	static WorldInfo capture(ServerLevel overworld) {
		MinecraftServer server = overworld.getServer();
		long seed = overworld.getSeed();
		boolean showSeed = !server.isDedicatedServer() || DiagnosticsConfig.read(DiagnosticsConfig.defaultFile()).logSeed();
		Platform platform = WorldInfo.platform;
		String versions = "ReTerraForged " + platform.modVersion() + ", Minecraft " + platform.minecraftVersion() + ", " + platform.loader() + " " + platform.loaderVersion();

		Preset preset = (Object) overworld.getChunkSource().randomState() instanceof RTFRandomState rtf && rtf.generatorContext() != null ? rtf.preset() : null;
		if (preset == null) {
			return new WorldInfo(List.of(NOT_RTF), seed, showSeed, List.of(), null);
		}
		List<String> tail = new ArrayList<>();
		WorldSettings.Properties properties = preset.world().properties;
		tail.add("generation version " + properties.generationVersion + " (current " + WorldSettings.Properties.CURRENT_GENERATION_VERSION + "), Generation Fixes " + (properties.generationFixes ? "on" : "off"));

		PerformanceConfig config = PerformanceConfig.current();
		int tileSize = WorldTileSize.current(config.tileSize());
		tail.add("tileSize " + tileSize + (tileSize != config.tileSize() ? " (the world's; performance.conf has " + config.tileSize() + ")" : "") + ", batchCount " + config.batchCount() + ", generation threads " + ThreadPools.WORLD_GEN.getMaximumPoolSize() + ", CPU cores " + Runtime.getRuntime().availableProcessors());

		List<String> diff = PresetDiff.lines(preset);
		String name = PresetDiff.builtInName(preset).orElse("custom");
		tail.add("preset: " + name + (diff.isEmpty() ? ", no changes from Default" : ", " + diff.size() + " change" + (diff.size() == 1 ? "" : "s") + " from Default:"));
		for (int i = 0; i < Math.min(diff.size(), MAX_DIFF_LINES); i++) {
			tail.add("  " + diff.get(i));
		}
		if (diff.size() > MAX_DIFF_LINES) {
			tail.add("  and " + (diff.size() - MAX_DIFF_LINES) + " more");
		}

		List<String> mods = new ArrayList<>();
		KNOWN_MODS.forEach((shown, ids) -> {
			if (ids.stream().anyMatch(WorldInfo::isLoaded)) {
				mods.add(shown);
			}
		});
		tail.add("mods affecting generation: " + (mods.isEmpty() ? "none of the known ones" : String.join(", ", mods)));

		String notApplied = null;
		ResourceManager resources = server.getResourceManager();
		boolean own = hasModifiers(resources, platform.ownModifierFolder());
		boolean other = hasModifiers(resources, platform.otherModifierFolder());
		String modifiers;
		if (own) {
			modifiers = platform.ownModifierFolder() + " (this loader's)" + (other ? ", " + platform.otherModifierFolder() + " too" : "");
		} else if (other && platform.readsOtherFolder()) {
			modifiers = "only " + platform.otherModifierFolder() + " (the other loader's), read in its place";
		} else if (other) {
			notApplied = "data/" + RTFCommon.MOD_ID + "/" + platform.otherModifierFolder();
			modifiers = "only " + platform.otherModifierFolder() + " (the other loader's): NOT applied, no trees, erosion or swamp surface of the preset";
		} else {
			modifiers = "none in the datapacks";
		}
		tail.add("biome modifiers: " + modifiers);
		return new WorldInfo(List.of(versions), seed, showSeed, List.copyOf(tail), notApplied);
	}

	// the preset datapack's, as BiomeModifiersImpl (NeoForge) checks them
	private static boolean hasModifiers(ResourceManager resources, String folder) {
		if (folder.isEmpty()) {
			return false;
		}
		return !resources.listResources(folder, (id) -> id.getNamespace().equals(RTFCommon.MOD_ID) && id.getPath().endsWith(".json")).isEmpty();
	}

	private static boolean isLoaded(String id) {
		try {
			return ModLoaderUtil.isLoaded(id);
		} catch (RuntimeException e) {
			return false;
		}
	}

	private static Map<String, List<String>> knownMods() {
		Map<String, List<String>> mods = new LinkedHashMap<>();
		// C2ME's main id differs between its Fabric and NeoForge builds; its base module is always there
		mods.put("c2me", List.of("c2me", "c2me-base", "c2me_base"));
		mods.put("noisium", List.of("noisium"));
		mods.put("lithium", List.of("lithium"));
		mods.put("modernfix", List.of("modernfix"));
		mods.put("terrablender", List.of("terrablender"));
		mods.put("lithostitched", List.of("lithostitched"));
		mods.put("terralith", List.of("terralith"));
		mods.put("tectonic", List.of("tectonic"));
		mods.put("biomesoplenty", List.of("biomesoplenty"));
		mods.put("regions_unexplored", List.of("regions_unexplored"));
		mods.put("distanthorizons", List.of("distanthorizons"));
		mods.put("chunky", List.of("chunky"));
		mods.put("sodium", List.of("sodium"));
		return mods;
	}

	private static String configFileName() {
		try {
			Path file = DiagnosticsConfig.defaultFile().toAbsolutePath().normalize();
			Path game = Path.of("").toAbsolutePath().normalize();
			return (file.startsWith(game) ? game.relativize(file) : file).toString().replace('\\', '/');
		} catch (RuntimeException e) {
			return "config/" + RTFCommon.MOD_ID + "/" + DiagnosticsConfig.FILE_NAME;
		}
	}

	/** The lines with the prefix of the log, the seed shown (the command: an operator has /seed anyway) */
	public List<String> chatLines() {
		return this.lines(true).stream().map((line) -> LOG_PREFIX + line).toList();
	}
}
