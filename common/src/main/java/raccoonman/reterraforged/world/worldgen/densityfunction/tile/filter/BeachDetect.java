package raccoonman.reterraforged.world.worldgen.densityfunction.tile.filter;

import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings.ControlPoints;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;

// A coast below the beach point is a beach whatever its slope. The slope test it had compared heights on the 0 to 1 scale with
// a limit they cannot reach, so it held nothing back. The steep ones get a stony shore's climate after all the filters
// (SteepShores, #38, #40), on the beaches the corrections spread too.
public record BeachDetect(ControlPoints transition) implements Filter {

    @Override
    public void apply(Filterable map, int seedX, int seedZ, int iterations) {
        int total = map.getBlockSize().total();

        for(int x = 0; x < total; x++) {
        	for(int z = 0; z < total; z++) {
        		Cell cell = map.getCellRaw(x, z);
	        	if (cell.terrain.isCoast() && !cell.terrain.isWetland() && cell.continentEdge < this.transition.beach) {
	        		cell.terrain = TerrainType.BEACH;
	            }
	        }
        }
    }

    public static BeachDetect make(GeneratorContext ctx) {
    	return new BeachDetect(ctx.preset.world().controlPoints);
    }
}
