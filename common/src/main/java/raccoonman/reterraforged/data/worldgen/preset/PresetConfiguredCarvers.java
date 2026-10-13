package raccoonman.reterraforged.data.worldgen.preset;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.TrapezoidFloat;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.VeryBiasedToBottomInt;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.carver.CanyonWorldCarver;
import net.minecraft.world.level.levelgen.carver.CaveWorldCarver;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import raccoonman.reterraforged.data.worldgen.preset.settings.CaveSettings.CompatibilityMode;
import raccoonman.reterraforged.data.worldgen.preset.settings.CaveSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.platform.ModLoaderUtil;
import raccoonman.reterraforged.world.worldgen.floatproviders.LegacyCanyonYScale;
import raccoonman.reterraforged.world.worldgen.heightproviders.LegacyCarverHeight;

public class PresetConfiguredCarvers {

	// 26.3: a carver is its own configuration; the lava level, debug settings and replaceable blocks are gone from it, and
	// what the cave carver used to hard-code (count, thickness, start radius) is configured as in vanilla's carvers
	public static void bootstrap(Preset preset, BootstrapContext<WorldCarver> ctx) {
		CaveSettings caveSettings = preset.caves();
		if(preserveVanillaCarvers(caveSettings.compatibilityMode)) {
			return;
		}
        ctx.register(Carvers.CAVE, new CaveWorldCarver(caveSettings.caveCarverProbability, modifiedCaveY(caveSettings), VeryBiasedToBottomInt.of(0, 14), TrapezoidFloat.of(0.0F, 3.0F, 1.0F), true, modifiedCaveYScale(caveSettings), modifiedCaveHorizontalRadiusMultiplier(caveSettings), modifiedCaveVerticalRadiusMultiplier(caveSettings), ConstantFloat.of(1.0F), modifiedCaveFloorLevel(caveSettings)));
        ctx.register(Carvers.CAVE_EXTRA_UNDERGROUND, new CaveWorldCarver(modifiedDeepCaveProbability(caveSettings), UniformHeight.of(VerticalAnchor.aboveBottom(8), VerticalAnchor.absolute(47)), VeryBiasedToBottomInt.of(0, 14), TrapezoidFloat.of(0.0F, 3.0F, 1.0F), true, UniformFloat.of(0.1F, 0.9F), UniformFloat.of(0.7F, 1.4F), UniformFloat.of(0.8F, 1.3F), ConstantFloat.of(1.0F), UniformFloat.of(-1.0F, -0.4F)));
        ctx.register(Carvers.CANYON, new CanyonWorldCarver(caveSettings.ravineCarverProbability, modifiedRavineY(caveSettings), modifiedRavineYScale(caveSettings), new CanyonWorldCarver.Shape(UniformFloat.of(0.75F, 1.0F), TrapezoidFloat.of(0.0F, 6.0F, 2.0F), 3, UniformFloat.of(0.75F, 1.0F), 1.0F, 0.0F, ConstantFloat.of(3.0F))));
	}

	private static boolean preserveVanillaCarvers(CompatibilityMode mode) {
		return switch(mode) {
		case AUTO -> isUndergroundBiomeCompatLoaded();
		case RTF -> false;
		case VANILLA -> true;
		};
	}

	private static boolean isUndergroundBiomeCompatLoaded() {
		return ModLoaderUtil.isLoaded("darkerdepths") || ModLoaderUtil.isLoaded("deeperdarker");
	}

	private static HeightProvider modifiedCaveY(CaveSettings caveSettings) {
        return caveSettings.legacyCarverDistribution ? LegacyCarverHeight.of(0, 8, 120) : UniformHeight.of(VerticalAnchor.aboveBottom(8), VerticalAnchor.absolute(180));
	}
	
	private static FloatProvider modifiedCaveYScale(CaveSettings caveSettings) {
		return caveSettings.legacyCarverDistribution ? ConstantFloat.of(0.5F) : UniformFloat.of(0.1F, 0.9F);
	}
	
	private static FloatProvider modifiedCaveFloorLevel(CaveSettings caveSettings) {
		return caveSettings.legacyCarverDistribution ? ConstantFloat.of(-0.7F) : UniformFloat.of(-1.0F, -0.4F);
	}
	
	private static FloatProvider modifiedCaveHorizontalRadiusMultiplier(CaveSettings caveSettings) {
		return caveSettings.legacyCarverDistribution ? ConstantFloat.of(1.0F) : UniformFloat.of(0.7F, 1.4F);
	}
	
	private static FloatProvider modifiedCaveVerticalRadiusMultiplier(CaveSettings caveSettings) {
		return caveSettings.legacyCarverDistribution ? ConstantFloat.of(1.0F) : UniformFloat.of(0.8F, 1.3F);
	}
	
	private static float modifiedDeepCaveProbability(CaveSettings caveSettings) {
		return caveSettings.legacyCarverDistribution ? 0.0F : caveSettings.deepCaveCarverProbability;
	}
	
	private static HeightProvider modifiedRavineY(CaveSettings caveSettings) {
        return caveSettings.legacyCarverDistribution ? LegacyCarverHeight.of(20, 8, 40) : UniformHeight.of(VerticalAnchor.absolute(10), VerticalAnchor.absolute(67));
	}
	
	private static FloatProvider modifiedRavineYScale(CaveSettings caveSettings) {
		return caveSettings.legacyCarverDistribution ? new LegacyCanyonYScale() : UniformFloat.of(-0.125F, 0.125F);
	}
}
