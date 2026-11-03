package raccoonman.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.WorldLookup;

@Mixin(ChunkGenerator.class)
class MixinChunkGenerator {

	// the structure starts of a chunk being generated read the tiles (WorldLookup.inStructureGeneration)
	@WrapMethod(method = "createStructures")
	private void createStructures(RegistryAccess registryAccess, ChunkGeneratorStructureState structureState, StructureManager structureManager, ChunkAccess chunk, StructureTemplateManager templateManager, ResourceKey<Level> levelKey, Operation<Void> original) {
		WorldLookup.beginStructureGeneration();
		try {
			original.call(registryAccess, structureState, structureManager, chunk, templateManager, levelKey);
		} finally {
			WorldLookup.endStructureGeneration();
		}
	}
}
