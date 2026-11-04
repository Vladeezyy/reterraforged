package raccoonman.reterraforged.world.worldgen.feature.template.decorator;

import com.mojang.serialization.MapCodec;
import raccoonman.reterraforged.platform.RegistryUtil;
import raccoonman.reterraforged.registries.RTFBuiltInRegistries;

public class TemplateDecorators {

	public static void bootstrap() {
		register("tree", TreeDecorator.CODEC);
		register("bee_nest", BeeNestDecorator.CODEC);
	}
	
	

	// a bee nest with the given chance (Minecraft's BeehiveDecorator finds no place on most template trees)
	public static TemplateDecorator<TreeContext> beeNest(float probability) {
		return new BeeNestDecorator(probability);
	}

	// one of Minecraft's tree decorators on a template tree
	public static TemplateDecorator<TreeContext> tree(net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator decorator) {
		return new TreeDecorator(decorator, decorator);
	}

	private static void register(String name, MapCodec<? extends TemplateDecorator<?>> placement) {
		RegistryUtil.register(RTFBuiltInRegistries.TEMPLATE_DECORATOR_TYPE, name, placement);
	}
}
