package raccoonman.reterraforged.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import raccoonman.reterraforged.data.worldgen.preset.PresetDiff;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.Presets;

/**
 * PresetDiff on the built-in presets, for the self test: a unit test cannot make a Preset (its registry key calls a
 * platform method). Each new preset shows exactly the settings it changes from Default, Default none, and a preset
 * read back through its codec (a world's datapack) is still the built-in one.
 */
public final class PresetDiffCheck {

	private PresetDiffCheck() {
	}

	/** What failed; empty when all is right */
	public static List<String> run() {
		List<String> failures = new ArrayList<>();
		try {
			expect(failures, "Default", Presets::makeDefault, Set.of());
			expect(failures, "Archipelago", Presets::makeArchipelago, Set.of("island.enableArchipelago", "island.islandDensity", "island.islandSize", "island.continentDistance", "island.volcanoChance", "world.properties.spawnType"));
			expect(failures, "Wetlands", Presets::makeWetlands, Set.of("climate.swampBasins", "climate.lowlandSwamps", "rivers.lakes.chance", "rivers.wetlands.chance"));
			expect(failures, "Towering Peaks", Presets::makeToweringPeaks, Set.of("world.properties.worldHeight", "climate.biomeShape.biomeSize", "terrain.mountains.weight", "terrain.mountains.verticalScale", "terrain.hills.weight", "terrain.plains.weight", "terrain.steppe.weight", "terrain.dales.weight"));
			expect(failures, "Endless Continent", Presets::makeEndlessContinent, Set.of("world.continent.continentType", "rivers.riverCount", "rivers.lakes.chance"));
			expect(failures, "River Valleys", Presets::makeRiverValleys, Set.of("rivers.riverCount", "rivers.valleyWidth", "rivers.valleyFollowsMeanders", "rivers.gorgeChance", "rivers.lakes.chance"));
			expect(failures, "Lush Wilds", Presets::makeLushWilds, Set.of("miscellaneous.treeDensity", "miscellaneous.grassDensity", "miscellaneous.plantDensity"));
			expect(failures, "Rolling Plains", Presets::makeRollingPlains, Set.of("terrain.plains.weight", "terrain.steppe.weight", "terrain.hills.weight", "terrain.mountains.weight", "terrain.volcano.weight", "terrain.general.globalVerticalScale"));
			expect(failures, "Rugged Highlands", Presets::makeRuggedHighlands, Set.of("terrain.plateau.weight", "terrain.badlands.weight", "terrain.torridonian.weight", "terrain.hills.weight", "terrain.volcano.weight", "terrain.plains.weight", "terrain.steppe.weight", "terrain.dales.weight"));

			List<String> wetlands = PresetDiff.lines(Presets.makeWetlands());
			if (!wetlands.contains("climate.swampBasins = 0.25 (default 0.0)")) {
				failures.add("Wetlands lines " + wetlands);
			}

			Map<String, Supplier<Preset>> builtIn = new LinkedHashMap<>(PresetDiff.builtIn());
			builtIn.forEach((name, preset) -> {
				Optional<String> found = PresetDiff.builtInName(preset.get());
				if (!found.equals(Optional.of(name))) {
					failures.add(name + " named " + found);
				}
				Preset original = preset.get();
				JsonElement json = Preset.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
				List<String> readBack = PresetDiff.lines(Preset.DIRECT_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(), original);
				if (!readBack.isEmpty()) {
					failures.add(name + " read back " + readBack);
				}
			});

			// an older world of Default: its generation version and fixes are on their own line, its name stays
			Preset older = Presets.makeDefault();
			older.world().properties.generationVersion = 0;
			older.world().properties.generationFixes = false;
			if (!PresetDiff.builtInName(older).equals(Optional.of("Default")) || PresetDiff.lines(older).size() != 2) {
				failures.add("older Default " + PresetDiff.builtInName(older) + " " + PresetDiff.lines(older));
			}
			Preset custom = Presets.makeDefault();
			custom.climate().swampBasins = 0.5F;
			if (PresetDiff.builtInName(custom).isPresent()) {
				failures.add("custom named " + PresetDiff.builtInName(custom));
			}
			// a structure set the structure page filled in with its own values: still Default, no line
			Preset filled = Presets.makeLushWilds();
			net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.structure.StructureSet> villages = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE_SET, net.minecraft.resources.Identifier.withDefaultNamespace("villages"));
			filled.structures().entries.put(villages, new raccoonman.reterraforged.data.worldgen.preset.settings.StructureSettings.StructureSetEntry(34, 8, 10387312, false));
			raccoonman.reterraforged.data.worldgen.preset.StructureOriginals originals = (key) -> key.equals(villages) ? new raccoonman.reterraforged.data.worldgen.preset.settings.StructureSettings.StructureSetEntry(34, 8, 10387312, false) : null;
			if (!PresetDiff.builtInName(filled, originals).equals(Optional.of("Lush Wilds")) || PresetDiff.lines(filled, originals).size() != 3 || PresetDiff.builtInName(filled).isPresent()) {
				failures.add("filled structure page " + PresetDiff.builtInName(filled, originals) + " " + PresetDiff.lines(filled, originals));
			}
		} catch (RuntimeException e) {
			failures.add("exception " + e);
		}
		return failures;
	}

	private static void expect(List<String> failures, String name, Supplier<Preset> preset, Set<String> paths) {
		Set<String> found = Set.copyOf(PresetDiff.paths(preset.get(), Presets.makeDefault()));
		if (!found.equals(paths)) {
			failures.add(name + " changes " + found + ", expected " + paths);
		}
	}
}
