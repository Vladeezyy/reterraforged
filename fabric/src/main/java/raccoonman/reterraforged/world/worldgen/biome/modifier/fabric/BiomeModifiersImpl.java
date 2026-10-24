package raccoonman.reterraforged.world.worldgen.biome.modifier.fabric;

import java.util.Map;
import java.util.Optional;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.platform.RegistryUtil;
import raccoonman.reterraforged.registries.RTFBuiltInRegistries;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.world.worldgen.biome.modifier.BiomeModifier;
import raccoonman.reterraforged.world.worldgen.biome.modifier.Filter;
import raccoonman.reterraforged.world.worldgen.biome.modifier.Order;

public class BiomeModifiersImpl {
	// The data registry of the modifiers is "forge:biome_modifier", so Forge biome modifiers that other mods ship in
	// their Fabric jars (data/<ns>/forge/biome_modifier, e.g. YUNG's Better Dungeons: "forge:remove_features") land in
	// it too. A type unknown here would fail the whole registry and with it world creation (stuck on "Preparing for
	// world creation..."), so such modifiers are read as Dummy, which does nothing.
	public static final Codec<BiomeModifier> DATA_CODEC = new Codec<>() {

		@Override
		public <T> DataResult<Pair<BiomeModifier, T>> decode(DynamicOps<T> ops, T input) {
			Optional<ResourceLocation> type = ops.get(input, "type").flatMap(ops::getStringValue).result().map(ResourceLocation::tryParse);
			if(type.isPresent() && !RTFBuiltInRegistries.BIOME_MODIFIER_TYPE.containsKey(type.get())) {
				RTFCommon.LOGGER.debug("Ignoring biome modifier of unknown type {}", type.get());
				return DataResult.success(Pair.of(new Dummy(), input));
			}
			return BiomeModifier.CODEC.decode(ops, input);
		}

		@Override
		public <T> DataResult<T> encode(BiomeModifier input, DynamicOps<T> ops, T prefix) {
			return BiomeModifier.CODEC.encode(input, ops, prefix);
		}
	};

	public static void bootstrap() {
		register("add", AddModifier.CODEC);
		register("replace", ReplaceModifier.CODEC);
	}
	
	public static String registryNamespace() {
		return "forge";
	}

	public static Registry<MapCodec<? extends BiomeModifier>> typeRegistry() {
		return RegistryUtil.createRegistry(RTFRegistries.BIOME_MODIFIER_TYPE);
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

	private record Dummy() implements BiomeModifier	{
		
		@Override
		public MapCodec<Dummy> codec() {
			return makeCodec();
		}
		
		public static MapCodec<Dummy> makeCodec() {
			return MapCodec.unit(Dummy::new);
		}
	};
}
