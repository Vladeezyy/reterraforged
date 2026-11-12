package raccoonman.reterraforged.world.worldgen.biome.modifier.neoforge;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.mojang.datafixers.util.Pair;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.platform.RegistryUtil;
import raccoonman.reterraforged.registries.RTFBuiltInRegistries;
import raccoonman.reterraforged.world.worldgen.biome.modifier.BiomeModifier;
import raccoonman.reterraforged.world.worldgen.biome.modifier.Filter;
import raccoonman.reterraforged.world.worldgen.biome.modifier.Order;
import raccoonman.reterraforged.world.worldgen.biome.modifier.VegetationDensity;

public class BiomeModifiersImpl {
	
	public static void bootstrap() {
		register("add", AddModifier.CODEC);
		register("replace", ReplaceModifier.CODEC);
		register("vegetation_density", DensityModifier.CODEC);
		NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent event) -> warnAboutFabricFolder(event.getServer()));
	}

	// A preset datapack exported on Fabric by an older version has its modifiers only in data/<ns>/forge/biome_modifier,
	// which NeoForge does not read: the world gets vanilla's trees and no erosion or swamp surface. Reading them here
	// too would need a mixin into NeoForge's application of the modifiers (it runs once, before any event), so it is
	// only said in the log; the same preset exported again has both folders (PresetExport.mirrorBiomeModifiers)
	private static void warnAboutFabricFolder(MinecraftServer server) {
		Set<String> applied = server.registryAccess().registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS).keySet().stream().map(ResourceLocation::getNamespace).collect(Collectors.toSet());
		Set<String> fabricOnly = server.getResourceManager().listResources("forge/biome_modifier", (id) -> id.getNamespace().equals(RTFCommon.MOD_ID) && id.getPath().endsWith(".json")).keySet().stream().map(ResourceLocation::getNamespace).filter((namespace) -> !applied.contains(namespace)).collect(Collectors.toSet());
		if(!fabricOnly.isEmpty()) {
			RTFCommon.LOGGER.warn("The preset datapack has its biome modifiers only for Fabric (data/{}/forge/biome_modifier): NeoForge does not apply them, so the world has no trees, erosion or swamp surface of the preset. Export the preset again with this version and replace the datapack", RTFCommon.MOD_ID);
		}
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
	
	public static BiomeModifier density(VegetationDensity density) {
		return new DensityModifier(density);
	}

	public static void register(String name, MapCodec<? extends BiomeModifier> value) {
		RegistryUtil.register(RTFBuiltInRegistries.BIOME_MODIFIER_TYPE, name, value);
	}
}
