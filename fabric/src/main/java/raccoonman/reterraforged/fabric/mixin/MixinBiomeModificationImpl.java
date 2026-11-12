package raccoonman.reterraforged.fabric.mixin;

import raccoonman.reterraforged.RTFCommon;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.base.Suppliers;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.impl.biome.modification.BiomeModificationImpl;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.chunk.ChunkGenerator;
import raccoonman.reterraforged.world.worldgen.biome.modifier.BiomeModifier;
import raccoonman.reterraforged.world.worldgen.biome.modifier.fabric.BiomeModifiersImpl;
import raccoonman.reterraforged.world.worldgen.biome.modifier.fabric.FabricBiomeModifier;

@Mixin(BiomeModificationImpl.class)
public class MixinBiomeModificationImpl {
	@Shadow(remap = false)
	@Final
	private static Comparator<Object> MODIFIER_ORDER_COMPARATOR;
	
	@Unique
	private static final Constructor<?> CTOR;
	// this world's registries gave biome modifiers of ReTerraForged (finalizeWorldGen, server thread)
	@Unique
	private boolean addedModifiers;
	
	// a wrap, not a @Redirect: another mod's wrap of the same call still applies
	@WrapOperation(
		method = "finalizeWorldGen",
		at = @At(
			value = "INVOKE",
			target = "Lnet/fabricmc/fabric/impl/biome/modification/BiomeModificationImpl;getSortedModifiers()Ljava/util/List;"
		),
		remap = false
	)
	private List<Object> getSortedModifiers(BiomeModificationImpl self, Operation<List<Object>> original, RegistryAccess registries) {
		// a copy: Fabric's list is the one of the whole game. Added to it, the modifiers of a world stayed for every world
		// loaded after it in the same session (a vanilla world got the preset's trees, an RTF world its density twice)
		List<Object> modifiers = new ArrayList<>(original.call(self));
		int fabricModifiers = modifiers.size();
		// the own folder's, and NeoForge's folder for a pack exported there by an older version (the order is Fabric's
		// comparator below: by name, as before)
		for(Holder.Reference<BiomeModifier> holder : BiomeModifiersImpl.modifiers(registries)) {
			if(holder.value() instanceof FabricBiomeModifier modifier) {
				Object record = this.makeModifierRecord(holder.key().location(), ModificationPhase.POST_PROCESSING, (ctx) -> {
					return true;
				}, modifier::apply);
				// null: Fabric API's internals changed (logged). The world loads, without the preset's biome modifiers
				if(record != null) {
					modifiers.add(record);
				}
			}
		}
		this.addedModifiers = modifiers.size() > fabricModifiers;
		modifiers.sort(MODIFIER_ORDER_COMPARATOR);
		return modifiers;
    }
	
	// The modifiers above change biome feature lists directly, not through Fabric's context, so Fabric does not
	// know the features changed and keeps the generators' cached per-step feature lists. If that cache was built
	// before the modifications, the step the erosion is added to is empty there and decorating a new chunk fails
	// with "Index -1 out of bounds" (seen when loading a world in single player): rebuild it, as Fabric does. Only when
	// there were such modifiers: the other worlds keep the lists Fabric left them.
	@Inject(
		method = "finalizeWorldGen",
		at = @At("TAIL"),
		remap = false
	)
	private void rebuildFeaturesPerStep(RegistryAccess registries, CallbackInfo callback) {
		if(!this.addedModifiers) {
			return;
		}
		this.addedModifiers = false;
		registries.lookupOrThrow(Registries.LEVEL_STEM).listElements().forEach((stem) -> {
			ChunkGenerator generator = stem.value().generator();
			((ChunkGeneratorAccessor) generator).reterraforged$setFeaturesPerStep(Suppliers.memoize(() -> {
				return FeatureSorter.buildFeaturesPerStep(List.copyOf(generator.getBiomeSource().possibleBiomes()), (biome) -> {
					return generator.getBiomeGenerationSettings(biome).features();
				}, true);
			}));
		});
	}

	@Unique
	private Object makeModifierRecord(ResourceLocation id, ModificationPhase phase, Predicate<BiomeSelectionContext> selector, BiConsumer<BiomeSelectionContext, BiomeModificationContext> modifier) {
		if(CTOR == null) {
			return null;
		}
		try {
			return CTOR.newInstance(phase, id, selector, modifier);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			RTFCommon.LOGGER.error("Could not make Fabric's biome modifier record for {}", id, e);
			return null;
		}
	}
	
	static {
		Constructor<?> ctor;
		try {
			ctor = Class.forName("net.fabricmc.fabric.impl.biome.modification.BiomeModificationImpl$ModifierRecord").getDeclaredConstructor(ModificationPhase.class, ResourceLocation.class, Predicate.class, BiConsumer.class);
		} catch (NoSuchMethodException | SecurityException | ClassNotFoundException e) {
			RTFCommon.LOGGER.error("Fabric API's biome modification internals changed: the ReTerraForged biome modifiers will not apply", e);
			ctor = null;
		}
		CTOR = ctor;
	}
}
