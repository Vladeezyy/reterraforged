package raccoonman.reterraforged.data.worldgen.preset;

import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

public class PresetNoiseGeneratorSettings {
	
	public static void bootstrap(Preset preset, BootstrapContext<NoiseGeneratorSettings> ctx) {
		HolderGetter<DensityFunction> densityFunctions = ctx.lookup(Registries.DENSITY_FUNCTION);
		HolderGetter<NormalNoise> noiseParams = ctx.lookup(Registries.NOISE);
		HolderGetter<Noise> noises = ctx.lookup(RTFRegistries.NOISE);
		
		WorldSettings worldSettings = preset.world();
		WorldSettings.Properties properties = worldSettings.properties;
		int worldHeight = properties.worldHeight;
		int worldDepth = properties.worldDepth;

		// 26.3: no cell size (the router interpolates 4 x 8 itself), aquifers configured here, ore veins in the material rule
		ctx.register(NoiseGeneratorSettings.OVERWORLD, new NoiseGeneratorSettings(
			NoiseSettings.create(-worldDepth, worldDepth + worldHeight), 
			Blocks.STONE.defaultBlockState(), Blocks.WATER.defaultBlockState(), 
			PresetNoiseRouterData.overworld(preset, densityFunctions, noiseParams),
			PresetSurfaceRuleData.overworld(preset, ctx.lookup(Registries.MATERIAL_RULE), noises),
			PresetNoiseRouterData.spawnTarget(preset, densityFunctions), 
			properties.seaLevel, 
			false, 
			Optional.of(PresetNoiseRouterData.aquifers(densityFunctions, noiseParams)), 
			false,
			NoiseGeneratorSettings.DebugFunctions.EMPTY
		));
    }
}
