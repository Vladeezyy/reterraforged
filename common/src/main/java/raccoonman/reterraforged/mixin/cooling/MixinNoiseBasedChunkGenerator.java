package raccoonman.reterraforged.mixin.cooling;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.Holder;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.blending.Blender;
import raccoonman.reterraforged.world.worldgen.AltitudeCooling;

@Mixin(NoiseBasedChunkGenerator.class)
class MixinNoiseBasedChunkGenerator {

	// a chunk's surface (the surface rules' temperature condition, frozen ocean icebergs) on a generation thread: the
	// scale of the random state it is built with (AltitudeCooling). The overload with the random state: the other one
	// calls it, and no level is needed (26.2: the possible biomes of the region last, no biome registry)
	@WrapMethod(method = "buildSurface(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/WorldGenerationContext;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/biome/BiomeManager;Lnet/minecraft/world/level/levelgen/blending/Blender;Ljava/util/Set;)V")
	private void reterraforged$coolingSurface(ChunkAccess chunk, WorldGenerationContext context, RandomState randomState, StructureManager structureManager, BiomeManager biomeManager, Blender blender, Set<Holder<Biome>> possibleBiomes, Operation<Void> original) {
		float previous = AltitudeCooling.enter(AltitudeCooling.of(randomState));
		try {
			original.call(chunk, context, randomState, structureManager, biomeManager, blender, possibleBiomes);
		} finally {
			AltitudeCooling.exit(previous);
		}
	}
}
