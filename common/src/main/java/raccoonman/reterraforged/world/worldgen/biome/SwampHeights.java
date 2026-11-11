package raccoonman.reterraforged.world.worldgen.biome;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.WorldLookup;
import raccoonman.reterraforged.world.worldgen.terrablender.TBTargetPoint;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

/**
 * Highest Swamps (ClimateSettings.swampMaxHeight, #28): a swamp biome more than that many blocks above the sea is
 * replaced by the biome the place would have with less erosion (one band rougher, then two, then three). Biome mods
 * such as Terralith put their swamps on the erosion of ReTerraForged's hills, 36 blocks above the sea on average. The
 * biome is picked again through the biome source (MixinMultiNoiseBiomeSource), with the erosion of this thread's climate
 * samples offset (MixinClimateSampler), so TerraBlender's regions and the mods' tables choose as they always do. The
 * height is the generator's own (the cell without the tile), the same whichever tiles are cached.
 *
 * Lowland Swamps with generation fixes (ClimateSettings.lowlandSwamps, #28): the lowlands it gives a swamp's climate only get
 * a swamp where the biome table keeps one on it. Terralith keeps them on another erosion and weirdness, and there the setting
 * did nothing: such a place is picked again on those, and takes the result only if it is a swamp. So is low land of a dry or
 * cold climate, which gets no swamp's climate itself: Terralith's orchid swamp and ice marsh are there.
 */
public final class SwampHeights {
	private static final long[] EROSION_OFFSETS = { Climate.quantizeCoord(-0.25F), Climate.quantizeCoord(-0.5F), Climate.quantizeCoord(-0.75F) };
	private static final long LOWLAND_EROSION = Climate.quantizeCoord(Erosion.LEVEL_3.mid());
	private static final long LOWLAND_WEIRDNESS = Climate.quantizeCoord(Weirdness.LOW_SLICE_NORMAL_DESCENDING.mid());
	private static final TagKey<Biome> IS_SWAMP = TagKey.create(Registries.BIOME, ResourceLocation.parse("c:is_swamp"));
	// for the mods that do not tag their swamps
	private static final String[] SWAMP_NAMES = { "swamp", "marsh", "bayou", "bog", "fen", "mire", "wetland", "mangrove", "muskeg", "floodplain" };
	// a surface this far above the place: its caves keep their biomes
	private static final int LOWLAND_DEPTH = 16;
	// of this thread's climate samples while a biome is picked again: the erosion offset, then the erosion and weirdness that
	// replace the sample's; any thread doing it (almost never one): the samples of the others are not even looked at
	private static final ThreadLocal<long[]> OFFSET = ThreadLocal.withInitial(() -> new long[3]);
	private static final AtomicInteger OFFSETTING = new AtomicInteger();

	private final WorldLookup lookup;
	private final Levels levels;
	private final int maxY;
	private final boolean lowlands;
	// the lowlands are the swamp basins' only
	private final boolean basinsOnly;
	// of this world: a static map kept the holders, with their registries, of every world played since the game started
	private final Map<Holder<Biome>, Boolean> swamps = new ConcurrentHashMap<>();
	// the columns a thread asked about last, one per quart column of a 4 x 4 block: a section's biomes come x, then y, then z
	// (LevelChunkSection.fillBiomesFromNoise), so a cache of one column missed on every call
	private final ThreadLocal<Columns> columns = ThreadLocal.withInitial(Columns::new);

	/**
	 * @param maxAboveSea Highest Swamps, 0 no limit
	 * @param lowlands Lowland Swamps and Swamp Basins also on the biome tables that keep their swamps elsewhere
	 * @param basinsOnly without Lowland Swamps: most columns are told apart by their terrain region alone
	 */
	public SwampHeights(GeneratorContext context, int maxAboveSea, boolean lowlands, boolean basinsOnly) {
		this.lookup = context.lookup;
		this.levels = context.levels;
		this.maxY = maxAboveSea > 0 ? context.levels.waterLevel + maxAboveSea : Integer.MAX_VALUE;
		this.lowlands = lowlands;
		this.basinsOnly = basinsOnly;
	}

	public static Climate.TargetPoint offset(Climate.TargetPoint point) {
		if (OFFSETTING.get() == 0) {
			return point;
		}
		long[] offset = OFFSET.get();
		if (offset[1] != 0) {
			Climate.TargetPoint forced = new Climate.TargetPoint(point.temperature(), point.humidity(), point.continentalness(), offset[1], point.depth(), offset[2]);
			// the same place: the same TerraBlender region
			if ((Object) point instanceof TBTargetPoint from && (Object) forced instanceof TBTargetPoint to) {
				to.setUniqueness(from.getUniqueness());
			}
			return forced;
		}
		if (offset[0] == 0) {
			return point;
		}
		Climate.TargetPoint rougher = new Climate.TargetPoint(point.temperature(), point.humidity(), point.continentalness(), point.erosion() + offset[0], point.depth(), point.weirdness());
		// as for the lowlands: TerraBlender's sampler sets the uniqueness again after this, the copy only matters when it does not
		if ((Object) point instanceof TBTargetPoint from && (Object) rougher instanceof TBTargetPoint to) {
			to.setUniqueness(from.getUniqueness());
		}
		return rougher;
	}

	/** The biome of the quart: biome itself, unless it is a swamp too high up or no swamp on a lowland swamp's surface */
	public Holder<Biome> limit(int quartX, int quartY, int quartZ, Holder<Biome> biome, Supplier<Holder<Biome>> pickAgain) {
		if (!this.swamp(biome)) {
			return this.lowlands ? this.lowland(quartX, quartY, quartZ, biome, pickAgain) : biome;
		}
		if (this.maxY == Integer.MAX_VALUE) {
			return biome;
		}
		Columns columns = this.columns.get();
		if (columns.surface[this.column(columns, quartX, quartZ)] <= this.maxY) {
			return biome;
		}
		long[] offset = OFFSET.get();
		OFFSETTING.incrementAndGet();
		try {
			for (long erosion : EROSION_OFFSETS) {
				offset[0] = erosion;
				Holder<Biome> other = pickAgain.get();
				if (!this.swamp(other)) {
					return other;
				}
			}
		} finally {
			offset[0] = 0;
			OFFSETTING.decrementAndGet();
		}
		return biome;
	}

	private Holder<Biome> lowland(int quartX, int quartY, int quartZ, Holder<Biome> biome, Supplier<Holder<Biome>> pickAgain) {
		if (this.basinsOnly && !this.inBasinRegion(quartX, quartZ)) {
			return biome;
		}
		Columns columns = this.columns.get();
		int slot = this.column(columns, quartX, quartZ);
		int surface = columns.surface[slot];
		if (!columns.lowland[slot] || surface > this.maxY || QuartPos.toBlock(quartY) < surface - LOWLAND_DEPTH) {
			return biome;
		}
		long[] offset = OFFSET.get();
		OFFSETTING.incrementAndGet();
		try {
			offset[1] = LOWLAND_EROSION;
			offset[2] = LOWLAND_WEIRDNESS;
			Holder<Biome> other = pickAgain.get();
			return this.swamp(other) ? other : biome;
		} finally {
			offset[1] = 0;
			offset[2] = 0;
			OFFSETTING.decrementAndGet();
		}
	}

	// the whole cell of every column of land was 3.7 % of the generation's samples
	private boolean inBasinRegion(int quartX, int quartZ) {
		long key = PosUtil.pack(quartX, quartZ);
		Columns columns = this.columns.get();
		int slot = Columns.slot(quartX, quartZ);
		if (columns.basinKeys[slot] != key) {
			Heightmap heightmap = this.lookup.getHeightmap();
			Cell cell = new Cell();
			heightmap.region().apply(cell, quartX << 2, quartZ << 2);
			columns.basin[slot] = heightmap.swampBasins().isBasinRegion(cell);
			columns.basinKeys[slot] = key;
		}
		return columns.basin[slot];
	}

	// the slot of the column in this thread's table, filled if it held another column
	private int column(Columns columns, int quartX, int quartZ) {
		long key = PosUtil.pack(quartX, quartZ);
		int slot = Columns.slot(quartX, quartZ);
		if (columns.keys[slot] != key) {
			Cell cell = new Cell();
			this.lookup.applyCell(cell, quartX << 2, quartZ << 2, false, this.lowlands);
			columns.surface[slot] = this.levels.scale(cell.height);
			Heightmap heightmap = this.lookup.getHeightmap();
			columns.lowland[slot] = this.lowlands && (heightmap.isLowlandSwamp(cell) || heightmap.isLowSwampLand(cell));
			columns.keys[slot] = key;
		}
		return slot;
	}

	private boolean swamp(Holder<Biome> biome) {
		return this.swamps.computeIfAbsent(biome, SwampHeights::isSwamp);
	}

	public static boolean isSwamp(Holder<Biome> biome) {
		if (biome.is(IS_SWAMP)) {
			return true;
		}
		String path = biome.unwrapKey().map((k) -> k.location().getPath()).orElse("");
		for (String name : SWAMP_NAMES) {
			if (path.contains(name)) {
				return true;
			}
		}
		return false;
	}

	/** A thread's columns: the slot of a column is its place in a 4 x 4 block of quart columns, the key tells which one it holds */
	private static final class Columns {
		private static final int SIZE = 16;
		// quart x, z packed; surface; lowland swamp
		private final long[] keys = new long[SIZE];
		private final int[] surface = new int[SIZE];
		private final boolean[] lowland = new boolean[SIZE];
		// for basinsOnly: quart x, z packed; in a basin's region
		private final long[] basinKeys = new long[SIZE];
		private final boolean[] basin = new boolean[SIZE];

		private Columns() {
			Arrays.fill(this.keys, Long.MIN_VALUE);
			Arrays.fill(this.basinKeys, Long.MIN_VALUE);
		}

		private static int slot(int quartX, int quartZ) {
			return (quartX & 3) | (quartZ & 3) << 2;
		}
	}
}
