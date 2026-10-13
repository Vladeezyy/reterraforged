package raccoonman.reterraforged.world.worldgen.cell.biome.spawn;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.SpawnTargetPoint;
import net.minecraft.world.level.levelgen.densityfunction.DensitySamplerSet;

// vanilla's NoiseSpawnFinder, but searching from the preset's spawn search center
public class SpawnFinderFix {
	public Result result;

	public SpawnFinderFix(List<SpawnTargetPoint> targetPoints, DensitySamplerSet samplers, BlockPos center) {
		this.result = SpawnFinderFix.getSpawnPositionAndFitness(targetPoints, samplers, center.getX(), center.getZ());
		this.radialSearch(targetPoints, samplers, 2048.0f, 512.0f);
		this.radialSearch(targetPoints, samplers, 512.0f, 32.0f);
	}

	private void radialSearch(List<SpawnTargetPoint> targetPoints, DensitySamplerSet samplers, float f, float g) {
		float h = 0.0f;
		float i = g;
		BlockPos blockPos = this.result.location();
		while (i <= f) {
			int j = blockPos.getX() + (int) (Math.sin(h) * (double) i);
			Result result = SpawnFinderFix.getSpawnPositionAndFitness(targetPoints, samplers, j,
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

	private static Result getSpawnPositionAndFitness(List<SpawnTargetPoint> targetPoints, DensitySamplerSet samplers, int i, int j) {
		double d = Mth.square(2500.0);
		long l = (long) ((double) Mth.square(10000.0f)
				* Math.pow((double) (Mth.square((long) i) + Mth.square((long) j)) / d, 2.0));
		int quartBlockX = QuartPos.toBlock(QuartPos.fromBlock(i));
		int quartBlockZ = QuartPos.toBlock(QuartPos.fromBlock(j));
		long m = Long.MAX_VALUE;
		for (SpawnTargetPoint point : targetPoints) {
			m = Math.min(m, point.sampleFitness(samplers, quartBlockX, 0, quartBlockZ));
		}
		return new Result(new BlockPos(i, 0, j), l + m);
	}

	public record Result(BlockPos location, long fitness) {
	}
}
