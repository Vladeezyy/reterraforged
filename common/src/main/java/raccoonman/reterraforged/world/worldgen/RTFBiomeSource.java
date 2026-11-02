package raccoonman.reterraforged.world.worldgen;

import org.jetbrains.annotations.Nullable;

import raccoonman.reterraforged.world.worldgen.biome.SwampHeights;

/** The overworld's biome source (MixinMultiNoiseBiomeSource), set up by MixinChunkMap for a ReTerraForged world */
public interface RTFBiomeSource {
	void setSwampHeights(@Nullable SwampHeights swampHeights);
}
