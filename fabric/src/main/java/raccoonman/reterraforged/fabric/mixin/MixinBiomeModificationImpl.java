package raccoonman.reterraforged.fabric.mixin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.base.Suppliers;

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
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.world.worldgen.biome.modifier.BiomeModifier;
import raccoonman.reterraforged.world.worldgen.biome.modifier.fabric.FabricBiomeModifier;

@Mixin(BiomeModificationImpl.class)
public class MixinBiomeModificationImpl {
	@Shadow(remap = false)
	@Final
	private static Comparator<Object> MODIFIER_ORDER_COMPARATOR;
	
	private static final Constructor<?> CTOR;
	
	@Redirect(
		method = "finalizeWorldGen",
		at = @At(
			value = "INVOKE",
			target = "getSortedModifiers"
		),
		remap = false
	)
	public List<Object> getSortedModifiers(BiomeModificationImpl self, RegistryAccess registries) {
		List<Object> modifiers = this.getSortedModifiers();
		for(Holder.Reference<BiomeModifier> holder : registries.lookupOrThrow(RTFRegistries.BIOME_MODIFIER).listElements().toList()) {
			if(holder.value() instanceof FabricBiomeModifier modifier) {
				modifiers.add(this.makeModifierRecord(holder.key().identifier(), ModificationPhase.POST_PROCESSING, (ctx) -> {
					return true;
				}, modifier::apply));
			}
		}
		modifiers.sort(MODIFIER_ORDER_COMPARATOR);
		return modifiers;
    }
	
	// The modifiers above change biome feature lists directly, not through Fabric's context, so Fabric does not
	// know the features changed and keeps the generators' cached per-step feature lists. If that cache was built
	// before the modifications, the step the erosion is added to is empty there and decorating a new chunk fails
	// with "Index -1 out of bounds" (seen when loading a world in single player): rebuild it, as Fabric does.
	@Inject(
		method = "finalizeWorldGen",
		at = @At("TAIL"),
		remap = false
	)
	private void rebuildFeaturesPerStep(RegistryAccess registries, CallbackInfo callback) {
		registries.lookupOrThrow(Registries.LEVEL_STEM).listElements().forEach((stem) -> {
			ChunkGenerator generator = stem.value().generator();
			((ChunkGeneratorAccessor) generator).reterraforged$setFeaturesPerStep(Suppliers.memoize(() -> {
				return FeatureSorter.buildFeaturesPerStep(List.copyOf(generator.getBiomeSource().possibleBiomes()), (biome) -> {
					return generator.getBiomeGenerationSettings(biome).features();
				}, true);
			}));
		});
	}

	@Shadow(remap = false)
	private List<Object> getSortedModifiers() {
		throw new UnsupportedOperationException();
	}
	
	private Object makeModifierRecord(Identifier id, ModificationPhase phase, Predicate<BiomeSelectionContext> selector, BiConsumer<BiomeSelectionContext, BiomeModificationContext> modifier) {
		try {
			return CTOR.newInstance(phase, id, selector, modifier);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	static {
		Constructor<?> ctor;
		try {
			ctor = Class.forName("net.fabricmc.fabric.impl.biome.modification.BiomeModificationImpl$ModifierRecord").getDeclaredConstructor(ModificationPhase.class, Identifier.class, Predicate.class, BiConsumer.class);
		} catch (NoSuchMethodException | SecurityException | ClassNotFoundException e) {
			e.printStackTrace();
			ctor = null;
		}
		CTOR = ctor;
	}
}
