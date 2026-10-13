package raccoonman.reterraforged.world.worldgen.biome.modifier.neoforge;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import raccoonman.reterraforged.world.worldgen.biome.modifier.BiomeModifier;

interface ForgeBiomeModifier extends BiomeModifier, net.neoforged.neoforge.common.world.BiomeModifier {
	// 26.3: NeoForge passes the registries too
	void modify(RegistryAccess registries, Holder<Biome> biome, net.neoforged.neoforge.common.world.BiomeModifier.Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder);

	MapCodec<? extends ForgeBiomeModifier> codec();
}
