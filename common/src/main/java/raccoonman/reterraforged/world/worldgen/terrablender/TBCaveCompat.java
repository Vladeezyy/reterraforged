package raccoonman.reterraforged.world.worldgen.terrablender;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.RegistryAccess;
import raccoonman.reterraforged.data.worldgen.preset.settings.CaveSettings.CompatibilityMode;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.registries.RTFRegistries;

/**
 * Darker Depths' and Deeper Darker's underground regions (MixinParameterList): with the preset's cave compatibility on
 * AUTO a share of the deep points goes to them. Only in a world with a preset: a world without one (vanilla generation
 * with TerraBlender) keeps TerraBlender's choice, as without this mod; its mode used to default to AUTO.
 */
public final class TBCaveCompat {

	private TBCaveCompat() {
	}

	// the preset's mode, null without a preset
	@Nullable
	public static CompatibilityMode mode(RegistryAccess registries) {
		return registries.lookup(RTFRegistries.PRESET).flatMap((registry) -> registry.get(Preset.KEY)).map((holder) -> holder.value().caves().compatibilityMode).orElse(null);
	}

	// the underground regions of these mods replace TerraBlender's choice
	public static boolean replacesRegions(@Nullable CompatibilityMode mode, boolean hasTargetRegion) {
		return mode == CompatibilityMode.AUTO && hasTargetRegion;
	}
}
