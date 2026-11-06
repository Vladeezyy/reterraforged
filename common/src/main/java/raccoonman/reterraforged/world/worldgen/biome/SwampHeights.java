package raccoonman.reterraforged.world.worldgen.biome;

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
 * did nothing: such a place is picked again on those, and takes the result only if it is a swamp.
 */
public final class SwampHeights {
	private static final long[] EROSION_OFFSETS = { Climate.quantizeCoord(-0.25F), Climate.quantizeCoord(-0.5F), Climate.quantizeCoord(-0.75F) };
	private static final long LOWLAND_EROSION = Climate.quantizeCoord(Erosion.LEVEL_3.mid());
	private static final long LOWLAND_WEIRDNESS = Climate.quantizeCoord(Weirdness.LOW_SLICE_NORMAL_DESCENDING.mid());
	private static final TagKey<Biome> IS_SWAMP = TagKey.create(Registries.BIOME, ResourceLocation.parse("c:is_swamp"));
	// for the mods that do not tag their swamps
	private static final String[] SWAMP_NAMES = { "swamp", "marsh", "bayou", "bog", "fen", "mire", "wetland", "mangrove", "muskeg" };
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
	// of this world: a static map kept the holders, with their registries, of every world played since the game started
	private final Map<Holder<Biome>, Boolean> swamps = new ConcurrentHashMap<>();
	// the last column a thread asked about, a chunk's biomes come column by column: quart x, z packed; surface; lowland swamp
	private final ThreadLocal<long[]> column = ThreadLocal.withInitial(() -> new long[] { Long.MIN_VALUE, 0, 0 });

	/**
	 * @param maxAboveSea Highest Swamps, 0 no limit
	 * @param lowlands Lowland Swamps also on the biome tables that keep their swamps elsewhere
	 */
	public SwampHeights(GeneratorContext context, int maxAboveSea, boolean lowlands) {
		this.lookup = context.lookup;
		this.levels = context.levels;
		this.maxY = maxAboveSea > 0 ? context.levels.waterLevel + maxAboveSea : Integer.MAX_VALUE;
		this.lowlands = lowlands;
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
		return offset[0] == 0 ? point : new Climate.TargetPoint(point.temperature(), point.humidity(), point.continentalness(), point.erosion() + offset[0], point.depth(), point.weirdness());
	}

	/** The biome of the quart: biome itself, unless it is a swamp too high up or no swamp on a lowland swamp's surface */
	public Holder<Biome> limit(int quartX, int quartY, int quartZ, Holder<Biome> biome, Supplier<Holder<Biome>> pickAgain) {
		if (!this.swamp(biome)) {
			return this.lowlands ? this.lowland(quartX, quartY, quartZ, biome, pickAgain) : biome;
		}
		if (this.maxY == Integer.MAX_VALUE || this.column(quartX, quartZ)[1] <= this.maxY) {
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
		long[] column = this.column(quartX, quartZ);
		if (column[2] == 0 || column[1] > this.maxY || QuartPos.toBlock(quartY) < column[1] - LOWLAND_DEPTH) {
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

	private long[] column(int quartX, int quartZ) {
		long key = PosUtil.pack(quartX, quartZ);
		long[] last = this.column.get();
		if (last[0] != key) {
			Cell cell = new Cell();
			this.lookup.applyCell(cell, quartX << 2, quartZ << 2, false, this.lowlands);
			last[0] = key;
			last[1] = this.levels.scale(cell.height);
			last[2] = this.lowlands && this.lookup.getHeightmap().isLowlandSwamp(cell) ? 1 : 0;
		}
		return last;
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
}
