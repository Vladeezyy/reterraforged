package raccoonman.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

// the structure page's placement goes on the loaded set itself (PresetStructureSets.apply, right after the registries
// load): a structure set written into the preset datapack named every structure of the set, and a mod removed since
// left the world unable to load
@Mixin(StructureSet.class)
public interface StructureSetAccessor {

	@Mutable
	@Accessor("placement")
	void reterraforged$setPlacement(StructurePlacement placement);
}
