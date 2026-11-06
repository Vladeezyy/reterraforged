package raccoonman.reterraforged.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;
import raccoonman.reterraforged.data.worldgen.preset.PresetTemplatePaths;

class PresetTemplatePathsTest {

	// spruce_large_1 to 5 were named in 0.0.6 and never existed: the large spruces pasted nothing, without a word in the log
	@Test
	@SuppressWarnings("unchecked")
	void everyTemplateHasItsFile() throws IllegalAccessException {
		List<String> missing = new ArrayList<>();
		int templates = 0;
		for (Field field : PresetTemplatePaths.class.getFields()) {
			if (!Modifier.isStatic(field.getModifiers()) || !List.class.isAssignableFrom(field.getType())) {
				continue;
			}
			for (ResourceLocation location : (List<ResourceLocation>) field.get(null)) {
				templates++;
				String file = "/data/" + location.getNamespace() + "/" + location.getPath();
				if (PresetTemplatePathsTest.class.getResource(file) == null) {
					missing.add(field.getName() + ": " + file);
				}
			}
		}
		assertTrue(templates > 50, "only " + templates + " templates");
		assertTrue(missing.isEmpty(), "missing: " + missing);
		assertFalse(PresetTemplatePaths.class.getFields().length == 0);
	}
}
