package raccoonman.reterraforged.world.worldgen.densityfunction.tile.filter;

import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings.ControlPoints;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;

// A coast below the beach point is a beach whatever its slope. The slope test it had compared heights on the 0 to 1 scale with
// a limit they cannot reach, so it held nothing back: removed, not repaired, since rock beaches would change the coasts of
// every world and of the Default preset.
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
