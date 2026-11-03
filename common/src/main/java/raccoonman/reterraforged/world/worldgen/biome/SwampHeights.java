package raccoonman.reterraforged.world.worldgen.biome;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.WorldLookup;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

/**
 * Highest Swamps (ClimateSettings.swampMaxHeight, #28): a swamp biome more than that many blocks above the sea is
 * replaced by the biome the place would have with less erosion (one band rougher, then two, then three). Biome mods
 * such as Terralith put their swamps on the erosion of ReTerraForged's hills, 36 blocks above the sea on average. The
 * biome is picked again through the biome source (MixinMultiNoiseBiomeSource), with the erosion of this thread's climate
 * samples offset (MixinClimateSampler), so TerraBlender's regions and the mods' tables choose as they always do. The
 * height is the generator's own (the cell without the tile), the same whichever tiles are cached.
 */
public final class SwampHeights {
	private static final long[] EROSION_OFFSETS = { Climate.quantizeCoord(-0.25F), Climate.quantizeCoord(-0.5F), Climate.quantizeCoord(-0.75F) };
	private static final TagKey<Biome> IS_SWAMP = TagKey.create(Registries.BIOME, Identifier.parse("c:is_swamp"));
	// for the mods that do not tag their swamps
	private static final String[] SWAMP_NAMES = { "swamp", "marsh", "bayou", "bog", "fen", "mire", "wetland", "mangrove", "muskeg" };
	private static final Map<Holder<Biome>, Boolean> SWAMPS = new ConcurrentHashMap<>();
	// the erosion offset of this thread's climate samples while a biome is picked again; any thread doing it (almost
	// never one): the samples of the others are not even looked at
	private static final ThreadLocal<long[]> OFFSET = ThreadLocal.withInitial(() -> new long[1]);
	private static final AtomicInteger OFFSETTING = new AtomicInteger();

	private final WorldLookup lookup;
	private final Levels levels;
	private final int maxY;
	// the surface of the last column a thread asked about (quart x, z packed; height): a chunk's biomes come column by column
	private final ThreadLocal<long[]> column = ThreadLocal.withInitial(() -> new long[] { Long.MIN_VALUE, 0 });

	public SwampHeights(GeneratorContext context, int maxAboveSea) {
		this.lookup = context.lookup;
		this.levels = context.levels;
		this.maxY = context.levels.waterLevel + maxAboveSea;
	}

	public static Climate.TargetPoint offset(Climate.TargetPoint point) {
		if (OFFSETTING.get() == 0) {
			return point;
		}
		long offset = OFFSET.get()[0];
		return offset == 0 ? point : new Climate.TargetPoint(point.temperature(), point.humidity(), point.continentalness(), point.erosion() + offset, point.depth(), point.weirdness());
	}

	/** The biome of quart (x, z) under the limit: biome itself unless it is a swamp above it */
	public Holder<Biome> limit(int quartX, int quartZ, Holder<Biome> biome, Supplier<Holder<Biome>> pickAgain) {
		if (!isSwamp(biome) || this.surface(quartX, quartZ) <= this.maxY) {
			return biome;
		}
		long[] offset = OFFSET.get();
		OFFSETTING.incrementAndGet();
		try {
			for (long erosion : EROSION_OFFSETS) {
				offset[0] = erosion;
				Holder<Biome> other = pickAgain.get();
				if (!isSwamp(other)) {
					return other;
				}
			}
		} finally {
			offset[0] = 0;
			OFFSETTING.decrementAndGet();
		}
		return biome;
	}

	private int surface(int quartX, int quartZ) {
		long key = PosUtil.pack(quartX, quartZ);
		long[] last = this.column.get();
		if (last[0] != key) {
			Cell cell = new Cell();
			this.lookup.applyCell(cell, quartX << 2, quartZ << 2, false, false);
			last[0] = key;
			last[1] = this.levels.scale(cell.height);
		}
		return (int) last[1];
	}

	public static boolean isSwamp(Holder<Biome> biome) {
		return SWAMPS.computeIfAbsent(biome, (b) -> {
			if (b.is(IS_SWAMP)) {
				return true;
			}
			String path = b.unwrapKey().map((k) -> k.identifier().getPath()).orElse("");
			for (String name : SWAMP_NAMES) {
				if (path.contains(name)) {
					return true;
				}
			}
			return false;
		});
	}
}
