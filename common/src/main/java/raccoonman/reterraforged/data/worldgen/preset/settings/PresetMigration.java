package raccoonman.reterraforged.data.worldgen.preset.settings;

// A preset saved before 0.0.20 (settingsVersion 0: old worlds, exported datapacks, preset files) gets for the settings
// that did nothing then the values that keep its world as it was: eroded slopes of the rock below (not plain stone), mountain biomes on all
// mountains, no volcanic surfaces, lakes at the source of their river, island transitions at the continent's coast and
// inland points, branch rivers sized from the river they branch from. Its other settings stay as they are.
public final class PresetMigration {

	private PresetMigration() {
	}

	public static Preset migrate(Preset preset) {
		MiscellaneousSettings misc = preset.miscellaneous();
		if (misc.settingsVersion >= MiscellaneousSettings.SETTINGS_VERSION) {
			return preset;
		}
		misc.plainStoneErosion = false;
		misc.mountainBiomeUsage = 1.0F;
		misc.volcanoBiomeUsage = 0.0F;
		RiverSettings rivers = preset.rivers();
		rivers.lakes.minStartDistance = 0.0F;
		rivers.lakes.maxStartDistance = 0.0F;
		rivers.branchSizeFromMain = true;
		WorldSettings.ControlPoints controlPoints = preset.world().controlPoints;
		controlPoints.islandCoast = controlPoints.coast;
		controlPoints.islandInland = controlPoints.inland;
		misc.settingsVersion = MiscellaneousSettings.SETTINGS_VERSION;
		return preset;
	}
}
