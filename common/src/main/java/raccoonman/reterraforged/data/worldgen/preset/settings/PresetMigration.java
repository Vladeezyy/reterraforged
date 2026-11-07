package raccoonman.reterraforged.data.worldgen.preset.settings;

// A preset saved before 0.0.20 (settingsVersion 0: old worlds, exported datapacks, preset files) gets for the settings
// that did nothing then the values that keep its world as it was: eroded slopes of the rock below (not plain stone), mountain biomes on all
// mountains, no volcanic surfaces, lakes at the source of their river, branch rivers sized from the river they branch from.
// Its other settings stay as they are.
// Version 2: Island Coast and Island Inland are points of the island's profile and do shape the islands. Before they stood at the
// continents' Coast and Inland unless someone moved them, and did nearly nothing: those get the defaults, moved ones stay.
public final class PresetMigration {

	private PresetMigration() {
	}

	public static Preset migrate(Preset preset) {
		MiscellaneousSettings misc = preset.miscellaneous();
		if (misc.settingsVersion >= MiscellaneousSettings.SETTINGS_VERSION) {
			return preset;
		}
		WorldSettings.ControlPoints controlPoints = preset.world().controlPoints;
		boolean islandPointsMoved = misc.settingsVersion >= 1 && (controlPoints.islandCoast != controlPoints.coast || controlPoints.islandInland != controlPoints.inland);
		if (!islandPointsMoved) {
			controlPoints.islandCoast = WorldSettings.ControlPoints.DEFAULT_ISLAND_COAST;
			controlPoints.islandInland = WorldSettings.ControlPoints.DEFAULT_ISLAND_INLAND;
		}
		if (misc.settingsVersion < 1) {
			misc.plainStoneErosion = false;
			misc.mountainBiomeUsage = 1.0F;
			misc.volcanoBiomeUsage = 0.0F;
			RiverSettings rivers = preset.rivers();
			rivers.lakes.minStartDistance = 0.0F;
			rivers.lakes.maxStartDistance = 0.0F;
			rivers.branchSizeFromMain = true;
		}
		misc.settingsVersion = MiscellaneousSettings.SETTINGS_VERSION;
		return preset;
	}
}
