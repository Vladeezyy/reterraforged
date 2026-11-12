package raccoonman.reterraforged.data.worldgen.preset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import raccoonman.reterraforged.data.worldgen.preset.settings.ContinentType;
import raccoonman.reterraforged.data.worldgen.preset.settings.IslandSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.RiverSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.SpawnType;
import raccoonman.reterraforged.data.worldgen.preset.settings.StructureSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.StructureSettings.StructureSetEntry;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;
import raccoonman.reterraforged.world.worldgen.noise.function.DistanceFunction;

/**
 * The lines a bug report gets about its preset (#42: the changed settings were not in it). On parts of a preset: a
 * Preset cannot be made here (its registry key calls a platform method, which throws in a unit test). The built-in
 * presets themselves are checked in the self test (PresetDiffCheck).
 */
class PresetDiffTest {
	private static final ResourceKey<StructureSet> VILLAGES = ResourceKey.create(Registries.STRUCTURE_SET, Identifier.withDefaultNamespace("villages"));

	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private static WorldSettings world() {
		return new WorldSettings(
			new WorldSettings.Continent(ContinentType.MULTI_IMPROVED, DistanceFunction.EUCLIDEAN, 3000, 0.7F, 0.25F, 0.25F, 5, 0.26F, 4.33F),
			new WorldSettings.ControlPoints(WorldSettings.ControlPoints.DEFAULT_ISLAND_INLAND, WorldSettings.ControlPoints.DEFAULT_ISLAND_COAST, 0.1F, 0.25F, 0.327F, 0.448F, 0.502F),
			new WorldSettings.Properties(SpawnType.CONTINENT_CENTER, 320, 64, 63, -54)
		);
	}

	private static RiverSettings rivers() {
		return new RiverSettings(0, 8, new RiverSettings.River(5, 2, 6, 20, 8, 0.75F), new RiverSettings.River(4, 1, 4, 14, 5, 0.975F), new RiverSettings.Lake(0.3F, 0.0F, 0.0F, 10, 75, 150, 2, 10), new RiverSettings.Wetland(0.6F, 175, 225));
	}

	@Test
	void theSameSettingsHaveNoLines() {
		assertEquals(List.of(), PresetDiff.lines(world(), world()));
		assertEquals(List.of(), PresetDiff.lines(IslandSettings.makeDefault(), IslandSettings.makeDefault()));
		assertEquals(List.of(), PresetDiff.lines(new StructureSettings(), new StructureSettings()));
	}

	@Test
	void aLineHasThePathTheValueAndTheDefault() {
		RiverSettings rivers = rivers();
		rivers.lakes.chance = 0.5F;
		rivers.riverCount = 12;
		rivers.valleyFollowsMeanders = true;
		assertEquals(List.of("riverCount = 12 (default 8)", "lakes.chance = 0.5 (default 0.3)", "valleyFollowsMeanders = true (default false)"), PresetDiff.lines(rivers, rivers()));
	}

	@Test
	void enumsShowTheirSavedName() {
		WorldSettings world = world();
		world.properties.spawnType = SpawnType.ISLANDS;
		// a constant with a body (INFINITE) is a subclass of its enum
		world.continent.continentType = ContinentType.INFINITE;
		assertEquals(List.of("continent.continentType = INFINITE (default MULTI_IMPROVED)", "properties.spawnType = ISLANDS (default CONTINENT_CENTER)"), PresetDiff.lines(world, world()));
	}

	@Test
	void aStructureSetOnlyOneSideHasIsOneLine() {
		StructureSettings structures = new StructureSettings();
		structures.entries.put(VILLAGES, new StructureSetEntry(34, 8, 10387312, false));
		assertEquals(List.of("entries.minecraft:villages = {spacing=34, separation=8, salt=10387312, disabled=false} (default none)"), PresetDiff.lines(structures, new StructureSettings()));
		assertEquals(List.of("entries.minecraft:villages = none (default {spacing=34, separation=8, salt=10387312, disabled=false})"), PresetDiff.lines(new StructureSettings(), structures));
	}

	@Test
	void aStructureSetBothHaveIsComparedByField() {
		StructureSettings base = new StructureSettings();
		base.entries.put(VILLAGES, new StructureSetEntry(34, 8, 10387312, false));
		StructureSettings changed = base.copy();
		changed.entries.get(VILLAGES).spacing = 40;
		changed.entries.get(VILLAGES).disabled = true;
		assertEquals(List.of("entries.minecraft:villages.spacing = 40 (default 34)", "entries.minecraft:villages.disabled = true (default false)"), PresetDiff.lines(changed, base));
	}

	// with the log's limit the changed values are kept, the structure sets the structure page filled in are cut
	@Test
	void entriesOfOneSideComeLast() {
		record Part(StructureSettings structures, IslandSettings island) {
		}
		Part part = new Part(new StructureSettings(), IslandSettings.makeDefault());
		part.structures().entries.put(VILLAGES, new StructureSetEntry(34, 8, 1, false));
		part.island().islandSize = 450.0F;
		List<String> paths = PresetDiff.paths(part, new Part(new StructureSettings(), IslandSettings.makeDefault()));
		assertEquals(List.of("island.islandSize", "structures.entries.minecraft:villages"), paths);
	}

	@Test
	void notANumberEqualsItself() {
		IslandSettings island = IslandSettings.makeDefault();
		island.beachWidth = Float.NaN;
		IslandSettings base = IslandSettings.makeDefault();
		base.beachWidth = Float.NaN;
		assertEquals(List.of(), PresetDiff.lines(island, base));
	}
}
