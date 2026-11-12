package raccoonman.reterraforged.mixin.cooling;

import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import raccoonman.reterraforged.world.worldgen.AltitudeCooling;

@Mixin(NoiseBasedChunkGenerator.class)
class MixinNoiseBasedChunkGenerator {

	// a chunk's surface (the surface rules' temperature condition, frozen ocean icebergs) on a generation thread: the
	// scale of the random state it is built with (AltitudeCooling). 26.3: the surface is a private step of buildTerrain
	// (noise, surface and carvers are one status), with the random state and no level
	@WrapMethod(method = "buildSurface(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/NoiseChunk;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/biome/BiomeManager;Ljava/util/Set;Lnet/minecraft/world/level/levelgen/material/rule/MaterialRule;)V")
	private void reterraforged$coolingSurface(ChunkAccess chunk, NoiseChunk noiseChunk, RandomState randomState, BiomeManager biomeManager, @Nullable Set<Holder<Biome>> possibleBiomes, MaterialRule rule, Operation<Void> original) {
		float previous = AltitudeCooling.enter(AltitudeCooling.of(randomState));
		try {
			original.call(chunk, noiseChunk, randomState, biomeManager, possibleBiomes, rule);
		} finally {
			AltitudeCooling.exit(previous);
		}
	}
}
