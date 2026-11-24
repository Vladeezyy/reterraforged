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
import net.minecraft.resources.Identifier;
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
	// one of the two wraps below ran in this finalizeWorldGen (reset at its start)
	@Unique
	private boolean wrappedModifiers;

	@Inject(
		method = "finalizeWorldGen",
		at = @At("HEAD"),
		remap = false
	)
	private void resetModifierWraps(RegistryAccess registries, CallbackInfo callback) {
		this.wrappedModifiers = false;
		this.addedModifiers = false;
	}

	// Two targets, each require = 0: Fabric API changed the call in 0.166.0+26.3 (biome API 20.2.0, FabricMC/fabric#5672,
	// data driven biome modifiers). Up to 0.165 finalizeWorldGen calls getSortedModifiers(), from 0.166 on
	// getSortedModifiers(RegistryAccess), which also adds the "fabric:biome_modifier" entries of the world. One of the two
	// applies on any version; if neither does (a later change), rebuildFeaturesPerStep at the end logs it as an error.
	// A wrap, not a @Redirect: another mod's wrap of the same call still applies.
	@WrapOperation(
		method = "finalizeWorldGen",
		at = @At(
			value = "INVOKE",
			target = "Lnet/fabricmc/fabric/impl/biome/modification/BiomeModificationImpl;getSortedModifiers()Ljava/util/List;"
		),
		remap = false,
		require = 0
	)
	private List<Object> getSortedModifiers(BiomeModificationImpl self, Operation<List<Object>> original, RegistryAccess registries) {
		return this.withPresetModifiers(original.call(self), registries);
	}

	// Fabric API 0.166.0+26.3 and later
	@WrapOperation(
		method = "finalizeWorldGen",
		at = @At(
			value = "INVOKE",
			target = "Lnet/fabricmc/fabric/impl/biome/modification/BiomeModificationImpl;getSortedModifiers(Lnet/minecraft/core/RegistryAccess;)Ljava/util/List;"
		),
		remap = false,
		require = 0
	)
	private List<Object> getSortedModifiersOfRegistries(BiomeModificationImpl self, RegistryAccess sortedFor, Operation<List<Object>> original, RegistryAccess registries) {
		return this.withPresetModifiers(original.call(self, sortedFor), registries);
	}

	@Unique
	private List<Object> withPresetModifiers(List<Object> sorted, RegistryAccess registries) {
		// both wraps in one finalizeWorldGen (a Fabric API that calls both): the preset's modifiers once
		if(this.wrappedModifiers) {
			return sorted;
		}
		this.wrappedModifiers = true;
		// a copy: up to Fabric API 0.165 the list is the one of the whole game. Added to it, the modifiers of a world
		// stayed for every world loaded after it in the same session (a vanilla world got the preset's trees, an RTF world
		// its density twice). From 0.166 on Fabric returns a new list, copied once more here: once per world, cheap
		List<Object> modifiers = new ArrayList<>(sorted);
		int fabricModifiers = modifiers.size();
		// the own folder's, and NeoForge's folder for a pack exported there by an older version (the order is Fabric's
		// comparator below: by name, as before)
		for(Holder.Reference<BiomeModifier> holder : BiomeModifiersImpl.modifiers(registries)) {
			if(holder.value() instanceof FabricBiomeModifier modifier) {
				Object record = this.makeModifierRecord(holder.key().identifier(), ModificationPhase.POST_PROCESSING, (ctx) -> {
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
		// neither wrap above applied (a Fabric API whose finalizeWorldGen no longer calls getSortedModifiers): said
		// loudly, not a quiet world without the preset's trees, erosion and swamp surface. Only when this world has
		// such modifiers (a vanilla world loses nothing)
		if(!this.wrappedModifiers && BiomeModifiersImpl.modifiers(registries).stream().anyMatch((holder) -> holder.value() instanceof FabricBiomeModifier)) {
			RTFCommon.LOGGER.error("ReTerraForged could not hook into Fabric API's biome modification (BiomeModificationImpl.finalizeWorldGen no longer calls getSortedModifiers): the biome modifiers of the ReTerraForged preset (trees, erosion, swamp surface, vegetation density) are NOT applied in this world. Please report it with your Fabric API version at https://github.com/Vladeezyy/reterraforged/issues");
		}
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
	private Object makeModifierRecord(Identifier id, ModificationPhase phase, Predicate<BiomeSelectionContext> selector, BiConsumer<BiomeSelectionContext, BiomeModificationContext> modifier) {
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
			ctor = Class.forName("net.fabricmc.fabric.impl.biome.modification.BiomeModificationImpl$ModifierRecord").getDeclaredConstructor(ModificationPhase.class, Identifier.class, Predicate.class, BiConsumer.class);
		} catch (NoSuchMethodException | SecurityException | ClassNotFoundException e) {
			RTFCommon.LOGGER.error("Fabric API's biome modification internals changed: the ReTerraForged biome modifiers will not apply", e);
			ctor = null;
		}
		CTOR = ctor;
	}
}
