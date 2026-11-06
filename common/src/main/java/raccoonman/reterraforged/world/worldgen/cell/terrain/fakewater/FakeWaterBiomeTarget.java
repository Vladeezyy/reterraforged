package raccoonman.reterraforged.world.worldgen.cell.terrain.fakewater;

public enum FakeWaterBiomeTarget {
	NONE,
	RIVER;
	
	public boolean isRiver() {
		return this == RIVER;
	}
}
