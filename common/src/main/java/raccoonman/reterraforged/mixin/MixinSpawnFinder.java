package raccoonman.reterraforged.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Climate.ParameterPoint;
import net.minecraft.world.level.biome.Climate.Sampler;
import raccoonman.reterraforged.world.worldgen.biome.RTFClimateSampler;
import raccoonman.reterraforged.world.worldgen.cell.biome.spawn.SpawnFinderFix;

// the whole spawn search is replaced (a @ModifyArg of the search center in Climate.SpawnFinder's constructor did not apply),
// only for a sampler of ReTerraForged's (a search center or the generator's water, MixinRandomState): vanilla worlds and
// other dimensions keep vanilla's search and other mods' changes to it
@Mixin(Climate.class)
class MixinSpawnFinder {

	@Inject(at = @At("HEAD"), method = "findSpawnPosition", cancellable = true)
    private static void findSpawnPosition(List<ParameterPoint> list, Sampler sampler, CallbackInfoReturnable<BlockPos> callback) {
		if((Object) sampler instanceof RTFClimateSampler rtfClimateSampler && (rtfClimateSampler.getSpawnWater() != null || !BlockPos.ZERO.equals(rtfClimateSampler.getSpawnSearchCenter()))) {
			callback.setReturnValue(new SpawnFinderFix(list, sampler).result.location());
		}
    }
}
