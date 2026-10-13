package raccoonman.reterraforged.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement;

// the placement settings a structure set keeps (PresetStructureSets builds a set with the preset's spacing on them)
@Mixin(AbstractSpreadingStructurePlacement.class)
public interface StructurePlacementAccessor {

	@Invoker("locateOffset")
	Vec3i reterraforged$locateOffset();

	@Invoker("frequencyReductionMethod")
	AbstractSpreadingStructurePlacement.FrequencyReductionMethod reterraforged$frequencyReductionMethod();

	@Invoker("frequency")
	float reterraforged$frequency();

	@Invoker("exclusionZone")
	Optional<AbstractSpreadingStructurePlacement.ExclusionZone> reterraforged$exclusionZone();
}
