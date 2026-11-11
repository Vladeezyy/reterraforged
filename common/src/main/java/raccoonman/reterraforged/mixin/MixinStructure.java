package raccoonman.reterraforged.mixin;

import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.structure.rule.StructureRule;

// 26.3: the biome check of a structure start is GenerationContext.isValidBiome
@Mixin(Structure.GenerationContext.class)
public class MixinStructure {
	// the rules of the last registry seen: isValidBiome runs for every structure start in every world, and listing the
	// registry each time was a stream and a list per call (the lookup is the registry's own, one per server)
	// (a pair, not a record of its own: a class in the mixin package cannot be loaded)
	@Unique
	private static volatile Map.Entry<RegistryLookup<StructureRule>, List<StructureRule>> reterraforged$rules;

	@Inject(
		at = @At("HEAD"), 
		method = "isValidBiome",
		cancellable = true
	)
    private void isValidBiome(GenerationStub generationStub, CallbackInfoReturnable<Boolean> callback) {
		GenerationContext generationContext = (GenerationContext) (Object) this;
		// the only rule type (cell_test) passes everything without a ReTerraForged context: vanilla worlds and other
		// dimensions skip the registry
		if(!((Object) generationContext.randomState() instanceof RTFRandomState rtfRandomState) || rtfRandomState.generatorContext() == null) {
			return;
		}
		RegistryLookup<StructureRule> structureRules = generationContext.registryAccess().lookupOrThrow(RTFRegistries.STRUCTURE_RULE);
		Map.Entry<RegistryLookup<StructureRule>, List<StructureRule>> cached = reterraforged$rules;
		if(cached == null || cached.getKey() != structureRules) {
			cached = Map.entry(structureRules, structureRules.listElements().map(Holder::value).toList());
			reterraforged$rules = cached;
		}
		for(StructureRule structureRule : cached.getValue()) {
			if(!structureRule.test(generationContext.randomState(), generationStub.position())) {
				callback.setReturnValue(false);
				return;
			}
		}
    }
}
