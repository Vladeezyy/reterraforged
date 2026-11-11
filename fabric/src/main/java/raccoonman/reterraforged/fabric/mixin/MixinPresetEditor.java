package raccoonman.reterraforged.fabric.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetConfigScreen;

// PresetEditor.EDITORS is a Map.of of the vanilla editors: ReTerraForged's editor is added for the default world type.
// The map Map.of made is changed, not the call replaced: another mod's change of it still applies (an interface's field
// cannot lose its final, so no @Mutable copy at the end of <clinit>)
@Mixin(PresetEditor.class)
interface MixinPresetEditor {
	
	@ModifyExpressionValue(
		method = "<clinit>",
		at = @At(
			value = "INVOKE",
			target = "Ljava/util/Map;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;"
		),
		remap = false
	)
	private static Map<Object, Object> of(Map<Object, Object> editors) {
		// mutable, as the map of the @Redirect before
		Map<Object, Object> map = new HashMap<>(editors);
		map.put(Optional.of(WorldPresets.NORMAL), (PresetEditor) (screen, ctx) -> new PresetConfigScreen(screen));
		return map;
    }
}
