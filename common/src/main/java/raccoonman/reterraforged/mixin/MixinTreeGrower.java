package raccoonman.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import raccoonman.reterraforged.world.worldgen.feature.template.SaplingTrees;

@Mixin(TreeGrower.class)
class MixinTreeGrower {

	// a sapling grows the mod's tree where its preset puts the mod's trees in the biomes (#41, SaplingTrees): both the
	// 2 x 2 tree and the single one, after NeoForge's event has had its say
	@WrapOperation(method = "growTree", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/Feature;place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"), require = 2)
	private boolean growTemplateTree(Feature feature, WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos pos, Operation<Boolean> original) {
		Boolean grown = SaplingTrees.grow(level, feature, random, pos);
		return grown != null ? grown : original.call(feature, level, generator, random, pos);
	}
}
