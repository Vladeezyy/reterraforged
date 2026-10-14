package raccoonman.reterraforged.world.worldgen.biome.modifier.neoforge;

import java.util.Map;
import java.util.Optional;

import com.mojang.datafixers.util.Pair;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import raccoonman.reterraforged.platform.RegistryUtil;
import raccoonman.reterraforged.registries.RTFBuiltInRegistries;
import raccoonman.reterraforged.world.worldgen.biome.modifier.BiomeModifier;
import raccoonman.reterraforged.world.worldgen.biome.modifier.Filter;
import raccoonman.reterraforged.world.worldgen.biome.modifier.Order;

public class BiomeModifiersImpl {
	
	public static void bootstrap() {
		register("add", AddModifier.CODEC);
		register("replace", ReplaceModifier.CODEC);
	}

	// NeoForge's registries: the 1.20 Forge version used Forge's "forge:biome_modifier", which NeoForge does not have,
	// so on NeoForge the preset's modifiers (its trees and other features) were left out of the preset datapack and
	// the biomes kept the vanilla trees
	public static String registryNamespace() {
		return "neoforge";
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Registry<MapCodec<? extends BiomeModifier>> typeRegistry() {
		return (Registry) NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS;
	}

	public static BiomeModifier add(Order order, GenerationStep.Decoration step, Optional<Pair<Filter.Behavior, HolderSet<Biome>>> biomes, HolderSet<PlacedFeature> features) {
		return new AddModifier(order, step, biomes.map((p) -> new Filter(p.getSecond(), p.getFirst())), features);
	}
	
	public static BiomeModifier replace(GenerationStep.Decoration step, Optional<HolderSet<Biome>> biomes, Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> replacements) {
		return new ReplaceModifier(step, biomes, replacements);
	}
	
	public static void register(String name, MapCodec<? extends BiomeModifier> value) {
		RegistryUtil.register(RTFBuiltInRegistries.BIOME_MODIFIER_TYPE, name, value);
	}
}
