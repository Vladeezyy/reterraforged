package raccoonman.reterraforged.data.worldgen;

import java.io.IOException;
import java.nio.file.Path;

// the package-private zip copy for the tests in another package
public final class PresetExportAccess {
	private PresetExportAccess() {
	}

	public static void copyToZip(Path input, Path output) throws IOException {
		PresetExport.copyToZip(input, output);
	}
}
