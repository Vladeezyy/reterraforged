package raccoonman.reterraforged.world.worldgen.biome.modifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;

import com.mojang.serialization.MapCodec;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import raccoonman.reterraforged.platform.RegistryUtil;
import raccoonman.reterraforged.registries.RTFBuiltInRegistries;

public class BiomeModifiers {
	
	@ExpectPlatform
	public static void bootstrap() {
		throw new UnsupportedOperationException();
	}

	// the namespace of the registries the modifiers are kept in: "neoforge" on NeoForge, whose own registries they go
	// in (NeoForge applies them to the biomes), "forge" on Fabric, where the mod makes them (as in the 1.20 Forge
	// version; MixinBiomeModificationImpl applies them)
	@ExpectPlatform
	public static String registryNamespace() {
		throw new UnsupportedOperationException();
	}

	// the registry of the modifier types: NeoForge's on NeoForge, a new one on Fabric
	@ExpectPlatform
	public static Registry<MapCodec<? extends BiomeModifier>> typeRegistry() {
		throw new UnsupportedOperationException();
	}

	
	public static BiomeModifier add(Order order, GenerationStep.Decoration step, HolderSet<PlacedFeature> features) {
		return add(order, step, Optional.empty(), features);
	}

	public static BiomeModifier add(Order order, GenerationStep.Decoration step, Filter.Behavior filterBehavior, HolderSet<Biome> biomes, HolderSet<PlacedFeature> features) {
		return add(order, step, Optional.of(Pair.of(filterBehavior, biomes)), features);
	}
	
	@ExpectPlatform
	public static BiomeModifier add(Order order, GenerationStep.Decoration step, Optional<Pair<Filter.Behavior, HolderSet<Biome>>> biomes, HolderSet<PlacedFeature> features) {
		throw new UnsupportedOperationException();
	}

	public static BiomeModifier replace(GenerationStep.Decoration step, HolderSet<Biome> biomes, Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> replacements) {
		return replace(step, Optional.of(biomes), replacements);
	}
	
	@ExpectPlatform
	public static BiomeModifier replace(GenerationStep.Decoration step, Optional<HolderSet<Biome>> biomes, Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> replacements) {
		throw new UnsupportedOperationException();
	}

	// the preset's vegetation densities on all biomes: after the "replace" modifiers, by the order of the names
	@ExpectPlatform
	public static BiomeModifier density(VegetationDensity density) {
		throw new UnsupportedOperationException();
	}

	// The modifiers of the loader's own folder, and those of the other loader's folder for a namespace that has none in
	// its own: a preset datapack exported before both folders were written (PresetExport.mirrorBiomeModifiers) has only
	// the folder of the loader it was made on. By whole namespaces, not single names: a pack with its own folder stays
	// exactly as it was, whatever another pack has in the other folder, and one with both folders applies once.
	public static <T> List<T> withFallback(List<T> own, List<T> other, Function<T, Identifier> id) {
		Set<String> namespaces = new HashSet<>();
		for (T modifier : own) {
			namespaces.add(id.apply(modifier).getNamespace());
		}
		List<T> modifiers = new ArrayList<>(own);
		for (T modifier : other) {
			if (!namespaces.contains(id.apply(modifier).getNamespace())) {
				modifiers.add(modifier);
			}
		}
		return modifiers;
	}

	public static void register(String name, MapCodec<? extends BiomeModifier> value) {
		RegistryUtil.register(RTFBuiltInRegistries.BIOME_MODIFIER_TYPE, name, value);
	}
}
