package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import raccoonman.reterraforged.RTFCommon;

/**
 * "Copy World Info" on the preset list: what a bug report needs to make the same world, as plain English text (it goes
 * to the mod's issues whatever the game's language)
 */
public final class CopyWorldInfo {
	private CopyWorldInfo() {
	}

	/** source: "built-in <id>", or the preset's file name */
	public static String text(String modVersion, String game, long seed, String presetName, String source, List<String> differences) {
		StringBuilder text = new StringBuilder();
		text.append("ReTerraForged ").append(modVersion).append(" (").append(game).append(")\n");
		text.append("Seed: ").append(seed).append('\n');
		text.append("Preset: ").append(presetName).append(" (").append(source).append(")\n");
		if (differences.isEmpty()) {
			text.append("Settings: same as Default\n");
		} else {
			text.append("Settings that differ from Default (").append(differences.size()).append("):\n");
			for (String line : differences) {
				text.append("  ").append(line).append('\n');
			}
		}
		return text.toString();
	}

	/** The mod's version from whichever loader runs it, "unknown" when neither answers */
	public static String modVersion() {
		return fabricVersion().or(CopyWorldInfo::neoForgeVersion).orElse("unknown");
	}

	/** "NeoForge" or "Fabric" */
	public static String loader() {
		try {
			Class.forName("net.neoforged.fml.ModList", false, CopyWorldInfo.class.getClassLoader());
			return "NeoForge";
		} catch (ClassNotFoundException | LinkageError e) {
			return "Fabric";
		}
	}

	// Fabric Loader's own API (common compiles against it); on NeoForge the class is missing and nothing is found
	private static Optional<String> fabricVersion() {
		try {
			return Fabric.version();
		} catch (RuntimeException | LinkageError e) {
			return Optional.empty();
		}
	}

	// NeoForge's by name: common is not compiled against it
	private static Optional<String> neoForgeVersion() {
		try {
			Object modList = Class.forName("net.neoforged.fml.ModList").getMethod("get").invoke(null);
			Optional<?> container = (Optional<?>) modList.getClass().getMethod("getModContainerById", String.class).invoke(modList, RTFCommon.MOD_ID);
			if (container.isEmpty()) {
				return Optional.empty();
			}
			Object info = call(container.get(), "getModInfo");
			return Optional.of(String.valueOf(call(info, "getVersion")));
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return Optional.empty();
		}
	}

	private static final class Fabric {
		private static Optional<String> version() {
			return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(RTFCommon.MOD_ID).map((mod) -> mod.getMetadata().getVersion().getFriendlyString());
		}
	}

	// through the public interface's method: the implementation classes are not always public
	private static Object call(Object target, String name) throws ReflectiveOperationException {
		for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
			for (Class<?> face : type.getInterfaces()) {
				try {
					Method method = face.getMethod(name);
					return method.invoke(target);
				} catch (NoSuchMethodException e) {
					// another interface
				}
			}
		}
		return target.getClass().getMethod(name).invoke(target);
	}
}
