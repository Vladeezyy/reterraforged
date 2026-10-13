package raccoonman.reterraforged.world.worldgen.terrablender;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;

public interface TBClimateSampler {
	
	void setUniqueness(DensitySampler.Bound uniqueness);
	
	@Nullable
	DensitySampler.Bound getUniqueness();
}
