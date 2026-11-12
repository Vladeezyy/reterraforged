package raccoonman.reterraforged.data.worldgen.preset;

import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;

public class PresetNoiseData {

	// the world's seed in the Perlin noises of the data (strata, biome edges of the Perlin types) from generation version 1:
	// a preset of an existing world writes the same data as before
	public static final int SEEDED_PERLIN_VERSION = 1;

	public static void bootstrap(Preset preset, BootstrapContext<Noise> ctx) {
		Noises.withSeededPerlin(preset.world().properties.fixes(SEEDED_PERLIN_VERSION), () -> {
			PresetTerrainNoise.bootstrap(preset, ctx);
			PresetClimateNoise.bootstrap(preset, ctx);
			PresetSurfaceNoise.bootstrap(preset, ctx);
			PresetStrataNoise.bootstrap(preset, ctx);
			return null;
		});
	}
	
	public static Noise getNoise(HolderGetter<Noise> noiseLookup, ResourceKey<Noise> key) {
		return new Noises.HolderHolder(noiseLookup.getOrThrow(key));
	}
	
	public static Noise registerAndWrap(BootstrapContext<Noise> ctx, ResourceKey<Noise> key, Noise noise) {
		return new Noises.HolderHolder(ctx.register(key, noise));
	}
	
	public static ResourceKey<Noise> createKey(String name) {
        return ResourceKey.create(RTFRegistries.NOISE, RTFCommon.location(name));
	}
}
