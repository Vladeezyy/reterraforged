package raccoonman.reterraforged.mixin.cooling;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import raccoonman.reterraforged.world.worldgen.AltitudeCooling;

@Mixin(ChunkGenerator.class)
class MixinChunkGenerator {

	// a chunk's features (snow and ice, lakes, RTF's snow decoration, other mods' features) on a generation thread,
	// which serves every dimension: the chunk's own dimension's scale (AltitudeCooling)
	@WrapMethod(method = "applyBiomeDecoration(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/StructureManager;)V")
	private void reterraforged$coolingFeatures(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager, Operation<Void> original) {
		float previous = AltitudeCooling.enter(AltitudeCooling.of(level.getLevel()));
		try {
			original.call(level, chunk, structureManager);
		} finally {
			AltitudeCooling.exit(previous);
		}
	}
}
