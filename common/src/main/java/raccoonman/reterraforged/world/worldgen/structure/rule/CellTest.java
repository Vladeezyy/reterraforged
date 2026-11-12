package raccoonman.reterraforged.world.worldgen.structure.rule;

import java.util.List;
import java.util.Set;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.RandomState;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.WorldLookup;
import raccoonman.reterraforged.world.worldgen.cell.terrain.Terrain;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.cell.terrain.fakewater.FakeWaterBiomeTarget;

record CellTest(float cutoff, Set<Terrain> terrainTypeBlacklist) implements StructureRule {
	// from generation version 1 the structures far below the ground (strongholds, ancient cities, trial chambers) are
	// also left alone under rivers, lakes and the fake water biomes. Not before: /locate would lead to a structure in
	// chunks of an existing world that were made without it
	static final int DEEP_UNDER_WATER_VERSION = 1;
	// a structure that starts this far below the ground does not reach the surface
	static final int DEEP = 16;
	public static final MapCodec<CellTest> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.FLOAT.fieldOf("cutoff").forGetter(CellTest::cutoff),
		Codec.STRING.xmap(TerrainType::get, Terrain::getName).listOf().fieldOf("terrain_type_blacklist").forGetter((set) -> set.terrainTypeBlacklist().stream().toList())
	).apply(instance, CellTest::new));

	public CellTest(float cutoff, List<Terrain> terrainTypeBlacklist) {
		this(cutoff, ImmutableSet.copyOf(terrainTypeBlacklist));
	}
	
	@Override
	public boolean test(RandomState randomState, BlockPos pos) {
		if((Object) randomState instanceof RTFRandomState rtfRandomState) {
			@Nullable
			GeneratorContext generatorContext = rtfRandomState.generatorContext();
			if(generatorContext != null) {
				WorldLookup worldLookup = generatorContext.lookup;
				Cell cell = new Cell();
				worldLookup.applyCell(cell.reset(), pos.getX(), pos.getZ(), WorldLookup.inStructureGeneration(), false);
				boolean water = cell.fakeWaterBiome != FakeWaterBiomeTarget.NONE;
				// no structures in rivers and their valleys; a wetland is land too (its valley mask is 0 all over), and the
				// swamps grow there: the swamp huts (and the swamp structures of other mods) were never placed (#34)
				boolean river = cell.riverMask < this.cutoff && !cell.terrain.isWetland();
				boolean blacklisted = this.terrainTypeBlacklist.contains(cell.terrain);
				if(!water && !river && !blacklisted) {
					return true;
				}
				int surface = generatorContext.levels.scale(cell.height);
				return allows(water, river, blacklisted, pos.getY(), surface, generatorContext.preset.world().properties.fixes(DEEP_UNDER_WATER_VERSION));
			}
			return true;
		} else {
			throw new IllegalStateException();
		}
	}

	// as in ReTerraForged 0.0.6: no structures in water or on the blacklisted terrain (mountains); for structures that
	// start at the surface only, those far below it (ancient cities, trial chambers) are left alone on that terrain, and
	// with deepUnderWater also under water
	static boolean allows(boolean water, boolean river, boolean blacklisted, int y, int surface, boolean deepUnderWater) {
		boolean deep = y < surface - DEEP;
		if((water || river) && !(deepUnderWater && deep)) {
			return false;
		}
		return !blacklisted || deep;
	}

	@Override
	public MapCodec<CellTest> codec() {
		return CODEC;
	}
}
