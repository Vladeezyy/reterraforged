package raccoonman.reterraforged.world.worldgen.cell.biome.spawn;

import java.util.List;
import java.util.function.BiPredicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.SpawnTargetPoint;
import net.minecraft.world.level.levelgen.densityfunction.DensitySamplerSet;

// vanilla's NoiseSpawnFinder, but searching from the preset's spawn search center
public class SpawnFinderFix {
	// above the distance and climate fitness of any spot within the search (10^8 at 2500 blocks)
	private static final long WATER_PENALTY = 1_000_000_000_000L;
	public Result result;
	// where the generator has water (the climate takes rivers and lakes for land)
	@Nullable
	private final BiPredicate<Integer, Integer> water;

	public SpawnFinderFix(List<SpawnTargetPoint> targetPoints, DensitySamplerSet samplers, BlockPos center, @Nullable BiPredicate<Integer, Integer> water) {
		this.water = water;
		this.result = this.getSpawnPositionAndFitness(targetPoints, samplers, center.getX(), center.getZ());
		this.radialSearch(targetPoints, samplers, 2048.0f, 512.0f);
		this.radialSearch(targetPoints, samplers, 512.0f, 32.0f);
		// still in the water (an ocean around the search center, a large continent scale): further out, up to 8192
		// blocks (the water penalty is above the distance fitness there)
		if (this.result.fitness() >= WATER_PENALTY) {
			this.radialSearch(targetPoints, samplers, 8192.0f, 512.0f);
			this.radialSearch(targetPoints, samplers, 512.0f, 32.0f);
		}
	}

	private void radialSearch(List<SpawnTargetPoint> targetPoints, DensitySamplerSet samplers, float f, float g) {
		float h = 0.0f;
		float i = g;
		BlockPos blockPos = this.result.location();
		while (i <= f) {
			int j = blockPos.getX() + (int) (Math.sin(h) * (double) i);
			Result result = this.getSpawnPositionAndFitness(targetPoints, samplers, j,
					blockPos.getZ() + (int) (Math.cos(h) * (double) i));
			if (result.fitness() < this.result.fitness()) {
				this.result = result;
			}
			if (!((double) (h += g / i) > Math.PI * 2))
				continue;
			h = 0.0f;
			i += g;
		}
	}

	private Result getSpawnPositionAndFitness(List<SpawnTargetPoint> targetPoints, DensitySamplerSet samplers, int i, int j) {
		double d = Mth.square(2500.0);
		long l = (long) ((double) Mth.square(10000.0f)
				* Math.pow((double) (Mth.square((long) i) + Mth.square((long) j)) / d, 2.0));
		int quartBlockX = QuartPos.toBlock(QuartPos.fromBlock(i));
		int quartBlockZ = QuartPos.toBlock(QuartPos.fromBlock(j));
		long m = Long.MAX_VALUE;
		for (SpawnTargetPoint point : targetPoints) {
			m = Math.min(m, point.sampleFitness(samplers, quartBlockX, 0, quartBlockZ));
		}
		// the generator's water is no spawn (the climate takes rivers and lakes for land); not with the spawn types
		// without climate points (m stays Long.MAX_VALUE there, the nearest spot to the search center wins)
		if (m != Long.MAX_VALUE && this.water != null && this.water.test(i, j)) {
			m += WATER_PENALTY;
		}
		return new Result(new BlockPos(i, 0, j), l + m);
	}

	public record Result(BlockPos location, long fitness) {
	}
}
