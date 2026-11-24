package raccoonman.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;

// the surface rules the noise settings hold, not what surfaceRule() returns: Alex's Caves (unofficial port 2.0.10)
// replaces that method's return value with its own rules merged in, which then went into the preset datapack of every
// world made with it (43 Alex's Caves blocks and conditions in overworld.json); without the mod such a world did not
// load. The mod still merges its rules into the preset's noise settings when the world runs (PresetSurfaceRuleData.base)
@Mixin(NoiseGeneratorSettings.class)
public interface NoiseGeneratorSettingsAccessor {

	@Accessor("surfaceRule")
	SurfaceRules.RuleSource reterraforged$getSurfaceRule();
}
