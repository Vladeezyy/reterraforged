package raccoonman.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.world.worldgen.structure.rule.StructureRule;

// 26.3: the biome check of a structure start is GenerationContext.isValidBiome
@Mixin(Structure.GenerationContext.class)
public class MixinStructure {

	@Inject(
		at = @At("HEAD"), 
		method = "isValidBiome",
		cancellable = true
	)
    private void isValidBiome(GenerationStub generationStub, CallbackInfoReturnable<Boolean> callback) {
		GenerationContext generationContext = (GenerationContext) (Object) this;
		RegistryAccess registry = generationContext.registryAccess();
		RegistryLookup<StructureRule> structureRules = registry.lookupOrThrow(RTFRegistries.STRUCTURE_RULE);
		for(StructureRule structureRule : structureRules.listElements().map(Holder::value).toList()) {
			if(!structureRule.test(generationContext.randomState(), generationStub.position())) {
				callback.setReturnValue(false);
			}
		}
    }
}
