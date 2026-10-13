package raccoonman.reterraforged.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SpawnTargetPoint;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.cell.biome.spawn.SpawnFinderFix;

@Mixin(NoiseBasedChunkGenerator.class)
class MixinNoiseBasedChunkGenerator {
	@Shadow
	@Final
	private Holder<NoiseGeneratorSettings> settings;

	// the world spawn is searched around the preset's spawn search center (vanilla: around 0, 0)
	@Inject(at = @At("HEAD"), method = "getOrigin", cancellable = true)
	private void getOrigin(RandomState randomState, CallbackInfoReturnable<ChunkPos> callback) {
		List<SpawnTargetPoint> spawnTarget = this.settings.value().spawnTarget();
		if((Object) randomState instanceof RTFRandomState rtfRandomState && !spawnTarget.isEmpty()) {
			BlockPos center = rtfRandomState.spawnSearchCenter();
			if(center != null) {
				SamplerContext samplerContext = SamplerContext.builder().enableCaches().build();
				callback.setReturnValue(ChunkPos.containing(new SpawnFinderFix(spawnTarget, randomState.samplersWithContext(samplerContext), center, rtfRandomState.spawnWater()).result.location()));
			}
		}
	}
}
