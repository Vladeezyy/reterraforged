package raccoonman.reterraforged.world.worldgen.terrablender;

public interface TBTargetPoint {
	double getUniqueness();
	
	void setUniqueness(double uniqueness);

	// the temperature and humidity the tree of another mod's region (not vanilla's) is searched with, Long.MIN_VALUE
	// for the point's own (MixinParameterList)
	long getRegionTemperature();

	long getRegionHumidity();

	void setRegionClimate(long temperature, long humidity);
}
