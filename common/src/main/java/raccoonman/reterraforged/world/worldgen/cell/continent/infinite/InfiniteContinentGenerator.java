package raccoonman.reterraforged.world.worldgen.cell.continent.infinite;

import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.continent.SimpleContinent;
import raccoonman.reterraforged.world.worldgen.cell.continent.simple.SimpleRiverGenerator;
import raccoonman.reterraforged.world.worldgen.cell.rivermap.LegacyRiverCache;
import raccoonman.reterraforged.world.worldgen.cell.rivermap.RiverCache;
import raccoonman.reterraforged.world.worldgen.cell.rivermap.Rivermap;

public class InfiniteContinentGenerator implements SimpleContinent {
	private final RiverCache riverCache;
	// land with the fixes; 0, the open ocean, made a world without any land
	private final float edge;
	
	public InfiniteContinentGenerator(GeneratorContext context) {
		this.edge = context.preset.world().properties.generationFixes ? 1.0F : 0.0F;
        this.riverCache = new LegacyRiverCache(new SimpleRiverGenerator(this, context));
	}
	
	@Override
	public void apply(Cell cell, float x, float z) {
		cell.continentId = 0.0F;
		cell.continentEdge = this.edge;
		cell.continentX = 0;
		cell.continentZ = 0;
	}

	@Override
	public Rivermap getRivermap(int x, int z) {
		return this.riverCache.getRivers(x, z);
	}

	@Override
	public long getNearestCenter(float x, float z) {
		return 0;
	}

	@Override
	public float getEdgeValue(float x, float z) {
		return 1.0F;
	}
}
