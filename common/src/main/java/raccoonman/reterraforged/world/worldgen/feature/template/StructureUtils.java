package raccoonman.reterraforged.world.worldgen.feature.template;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;

public class StructureUtils {
	// the surface structures of the registry asked last: every tree with check_bounds asked for them, and the whole
	// registry was listed each time. The registry's lookup is one object per world (MappedRegistry.asLookup), another
	// world's reads its own; a soft reference, so a world left behind is not held
	private static volatile SoftReference<Surface> surface = new SoftReference<>(null);

    public static boolean hasOvergroundStructure(HolderLookup<Structure> structures, ChunkAccess chunk) {
    	Map<Structure, LongSet> references = chunk.getAllReferences();
    	if (references.isEmpty()) {
    		return false;
    	}
    	Set<Structure> surface = surfaceStructures(structures);
    	for (Map.Entry<Structure, LongSet> entry : references.entrySet()) {
            LongSet refs = entry.getValue();
            if (refs != null && refs.size() > 0 && surface.contains(entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    private static Set<Structure> surfaceStructures(HolderLookup<Structure> structures) {
    	Surface cached = surface.get();
    	if (cached == null || cached.lookup() != structures) {
    		cached = new Surface(structures, structures.listElements().map(Holder::value).filter((structure) -> structure.step() == GenerationStep.Decoration.SURFACE_STRUCTURES).collect(Collectors.toUnmodifiableSet()));
    		surface = new SoftReference<>(cached);
    	}
    	return cached.structures();
    }

    private record Surface(HolderLookup<Structure> lookup, Set<Structure> structures) {
    }
}
