package raccoonman.reterraforged.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.platform.ConfigUtil;

/**
 * What the world's block in the log and the crash report may show (WorldInfo). A file of its own: performance.conf is
 * about the generation threads and tiles and is deleted before a modpack is exported.
 */
public record DiagnosticsConfig(boolean logSeed) {
	public static final String FILE_NAME = "diagnostics.conf";

	// a dedicated server's owner often hides the seed, and its logs end up in public bug reports
	public static DiagnosticsConfig makeDefault() {
		return new DiagnosticsConfig(false);
	}

	public static Path defaultFile() {
		return ConfigUtil.rtf(FILE_NAME);
	}

	public static DiagnosticsConfig read(Path path) {
		if (!Files.exists(path)) {
			DiagnosticsConfig defaults = makeDefault();
			writeDefaults(path, defaults);
			return defaults;
		}
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			Properties props = new Properties();
			props.load(reader);
			// a file without the line (or with anything but true) keeps the seed hidden
			return new DiagnosticsConfig("true".equalsIgnoreCase(props.getProperty("logSeed", "false").trim()));
		} catch (IOException | IllegalArgumentException e) {
			// IllegalArgumentException: Properties.load on a malformed \\uxxxx
			RTFCommon.LOGGER.error("Failed to read diagnostics config {}, using defaults", path, e);
			return makeDefault();
		}
	}

	private static void writeDefaults(Path path, DiagnosticsConfig config) {
		try {
			Path parent = path.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			try (BufferedWriter writer = Files.newBufferedWriter(path)) {
				writer.write("# ReTerraForged Diagnostics Config\n");
				writer.write("#\n");
				writer.write("# logSeed: write a dedicated server's world seed into the log and crash reports (true/false), default false\n");
				writer.write("#   Single player always writes it. Operators see it with /reterraforged info or /seed.\n");
				writer.write("#\n");
				writer.write("logSeed=" + config.logSeed() + "\n");
			}
		} catch (IOException e) {
			RTFCommon.LOGGER.error("Failed to write default diagnostics config", e);
		}
	}
}
