package raccoonman.reterraforged.world.worldgen.surface;

import java.util.List;
import java.util.function.Function;

import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import raccoonman.reterraforged.world.worldgen.surface.rule.StrataRule;

public interface RTFSurfaceSystem {
	// the chunk MaterialSystem.buildSurface is building on this thread (26.3: the material rule context has no chunk)
	ThreadLocal<ChunkAccess> BUILDING_CHUNK = new ThreadLocal<>();

	List<List<StrataRule.Layer>> getOrCreateStrata(Identifier name, Function<RandomSource, List<List<StrataRule.Layer>>> factory);
}
