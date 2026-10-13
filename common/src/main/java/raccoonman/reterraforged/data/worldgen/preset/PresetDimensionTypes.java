package raccoonman.reterraforged.data.worldgen.preset;

import com.mojang.serialization.Lifecycle;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.DimensionTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;

public final class PresetDimensionTypes {

	public static void bootstrap(Preset preset, BootstrapContext<DimensionType> ctx) {
		WorldSettings worldSettings = preset.world();
		WorldSettings.Properties properties = worldSettings.properties;
		int worldHeight = properties.worldHeight;
		int worldDepth = properties.worldDepth;
		int totalHeight = worldDepth + worldHeight;

		// vanilla's overworld (sky, fog, clouds, music, timelines... as environment attributes since 1.21.11) with the
		// preset's world height
		DimensionType vanilla = vanillaOverworld(ctx);
		ctx.register(BuiltinDimensionTypes.OVERWORLD, new DimensionType(vanilla.hasFixedTime(), vanilla.hasSkyLight(), vanilla.hasCeiling(), vanilla.hasEnderDragonFight(), vanilla.coordinateScale(), -worldDepth, totalHeight, totalHeight, vanilla.infiniburn(), vanilla.ambientLight(), vanilla.monsterSettings(), vanilla.skybox(), vanilla.cardinalLightType(), vanilla.attributes(), vanilla.timelines(), vanilla.defaultClock()));
	}

	private static DimensionType vanillaOverworld(BootstrapContext<DimensionType> ctx) {
		DimensionType[] overworld = new DimensionType[1];
		DimensionTypes.bootstrap(new BootstrapContext<DimensionType>() {

			@Override
			public Holder.Reference<DimensionType> register(ResourceKey<DimensionType> key, DimensionType value, Lifecycle lifecycle) {
				if (key == BuiltinDimensionTypes.OVERWORLD) overworld[0] = value;
				return null;
			}

			@Override
			public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> registry) {
				return ctx.lookup(registry);
			}
		});
		return overworld[0];
	}
}
