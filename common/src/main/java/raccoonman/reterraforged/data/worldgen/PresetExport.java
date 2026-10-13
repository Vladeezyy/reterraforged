package raccoonman.reterraforged.data.worldgen;

import java.io.IOException;
import java.net.URI;
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

		DataGenerator dataGenerator = Datapacks.makePreset(preset, registryAccess, datagenPath, datagenOutputPath, presetName);
		dataGenerator.run();
		copyToZip(datagenOutputPath, outputPath);
		PathUtils.deleteDirectory(datagenPath);

		RTFCommon.LOGGER.info("Exported datapack to {}", outputPath);
	}

	private static void copyToZip(Path input, Path output) throws IOException {
		Map<String, String> env = ImmutableMap.of("create", "true");
		URI uri = URI.create("jar:" + output.toUri());
		try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
			PathUtils.copyDirectory(input, fs.getPath("/"), StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
