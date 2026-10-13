package raccoonman.reterraforged.world.worldgen.surface.rule;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import raccoonman.reterraforged.platform.RegistryUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.surface.rule.StrataRule.Strata;

public class RTFSurfaceRules {

	public static void bootstrap() {
		register("strata", StrataRule.CODEC);
	}
	
	public static StrataRule strata(Identifier name, Holder<Noise> selector, List<Strata> strata, int iterations) {
		return new StrataRule(name, selector, strata, iterations);
	}
	
	public static void register(String name, MapCodec<? extends MaterialRule> value) {
		RegistryUtil.register(BuiltInRegistries.MATERIAL_RULE_TYPE, name, value);
	}
}
