package raccoonman.reterraforged.world.worldgen;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.DensityFunction;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

public interface RTFRandomState {
	void initialize(ServerLevel level);
	
	@Nullable
	Preset preset();

	// the noise router has ReTerraForged density functions (known from the RandomState's construction on)
	boolean hasReTerraForgedFunctions();

	@Nullable
	GeneratorContext generatorContext();
	
	DensityFunction wrap(DensityFunction function);

	Noise seed(Noise noise);
}
