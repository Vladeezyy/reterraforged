package raccoonman.reterraforged.world.worldgen;

import java.util.function.BiPredicate;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

public interface RTFRandomState {
	void initialize(ServerLevel level);
	
	@Nullable
	Preset preset();

	@Nullable
	GeneratorContext generatorContext();

	// where the world spawn is searched from, null without a generator context
	@Nullable
	BlockPos spawnSearchCenter();

	// where the generator has water, kept out of the spawn search
	@Nullable
	BiPredicate<Integer, Integer> spawnWater();

	Noise seed(Noise noise);
}
