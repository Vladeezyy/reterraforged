package raccoonman.reterraforged.data.worldgen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.apache.commons.io.file.PathUtils;

import com.google.common.collect.ImmutableMap;

import net.minecraft.core.RegistryAccess;
import net.minecraft.data.DataGenerator;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;

/**
 * Writes a preset as a datapack zip. Used by the preset screen ("Export As Datapack", applying a preset) and by the
 * server test command "rtf exportpreset", so a dedicated server can get the preset datapack without a client.
 */
public final class PresetExport {

	private PresetExport() {
	}

	public static void toZip(Preset preset, String presetName, RegistryAccess registryAccess, Path outputPath) throws IOException {
		Path datagenPath = Files.createTempDirectory("datagen-target-");
		Path datagenOutputPath = datagenPath.resolve("output");
		// a new archive replaces the old one: zip file systems open an existing archive and only add to it, so a preset
		// exported again kept the files its new settings no longer make (erosion switched off stayed in the pack, #5)
		Path tempPath = outputPath.resolveSibling(outputPath.getFileName() + ".tmp");
		try {
			DataGenerator dataGenerator = Datapacks.makePreset(preset, registryAccess, datagenPath, datagenOutputPath, presetName);
			dataGenerator.run();
			Files.deleteIfExists(tempPath);
			copyToZip(datagenOutputPath, tempPath);
			Files.move(tempPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
		} finally {
			// also when the export failed: every failure left a folder in the system's temporary files. A cleanup that
			// fails (Windows: an antivirus still holds a file) is only logged: it hid the export that worked, and the
			// world was made without the preset
			try {
				Files.deleteIfExists(tempPath);
				PathUtils.deleteDirectory(datagenPath);
			} catch (IOException | UncheckedIOException e) {
				RTFCommon.LOGGER.warn("Could not delete the temporary files of the export in {}", datagenPath, e);
			}
		}

		RTFCommon.LOGGER.info("Exported datapack to {}", outputPath);
	}

	// the path, not a "jar:" URI: the zip provider cuts such a URI at its first "!/", and Path.toUri does not escape "!".
	// With a folder whose name ends in "!" (an instance "Pack!", a Windows user "Name!" for the temporary files) the zip
	// was written to the path before it, and the export or the preset of every new world failed
	static void copyToZip(Path input, Path output) throws IOException {
		Map<String, String> env = ImmutableMap.of("create", "true");
		try (FileSystem fs = FileSystems.newFileSystem(output, env)) {
			PathUtils.copyDirectory(input, fs.getPath("/"), StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
