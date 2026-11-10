package raccoonman.reterraforged.world.worldgen.feature.template;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.server.RTFMinecraftServer;
import raccoonman.reterraforged.world.worldgen.feature.template.TemplateFeature.Config;
import raccoonman.reterraforged.world.worldgen.feature.template.decorator.DecoratorConfig;
import raccoonman.reterraforged.world.worldgen.feature.template.decorator.TemplateDecorator;
import raccoonman.reterraforged.world.worldgen.feature.template.decorator.TemplateDecorators;
import raccoonman.reterraforged.world.worldgen.feature.template.decorator.TreeContext;
import raccoonman.reterraforged.world.worldgen.feature.template.paste.Paste;
import raccoonman.reterraforged.world.worldgen.feature.template.paste.PasteConfig;
import raccoonman.reterraforged.world.worldgen.feature.template.paste.PasteType;
import raccoonman.reterraforged.world.worldgen.feature.template.placement.TemplatePlacement;
import raccoonman.reterraforged.world.worldgen.feature.template.template.Dimensions;
import raccoonman.reterraforged.world.worldgen.feature.template.template.FeatureTemplate;
import raccoonman.reterraforged.world.worldgen.feature.template.template.TemplateContext;

public class TemplateFeature extends Feature<Config<?>> {

	public TemplateFeature(Codec<Config<?>> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<Config<?>> ctx) {
		RandomSource random = ctx.random();
		Config<?> config = ctx.config();
		
		Mirror mirror = nextMirror(random);
		Rotation rotation = nextRotation(random);
        return paste(ctx.level(), random, ctx.origin(), mirror, rotation, config, FeatureTemplate.WORLD_GEN);
	}

    public static <T extends TemplateContext> boolean paste(WorldGenLevel world, RandomSource rand, BlockPos pos, Mirror mirror, Rotation rotation, Config<T> config, PasteType pasteType) {
        return paste(world, rand, pos, mirror, rotation, config, pasteType, false);
    }

    public static <T extends TemplateContext> boolean paste(WorldGenLevel world, RandomSource rand, BlockPos pos, Mirror mirror, Rotation rotation, Config<T> config, PasteType pasteType, boolean modified) {
        if (config.templates().isEmpty()) {
            RTFCommon.LOGGER.warn("Empty template list for config");
            return false;
        }
        
        if(world.getServer() instanceof RTFMinecraftServer rtfMinecraftServer) {
	        DecoratorConfig<T> decoratorConfig = config.decorator();
	        
	        Identifier templateName = nextTemplate(config.templates, rand);
	        FeatureTemplate template = rtfMinecraftServer.getFeatureTemplateManager().load(templateName);
	        
	        Dimensions dimensions = template.getDimensions(mirror, rotation);
	        TemplatePlacement<T> placement = config.placement();
	        if (!placement.canPlaceAt(world, pos, dimensions)) {
	            return false;
	        }
	
	        Paste paste = pasteType.get(template);
	        T buffer = placement.createContext();
	        if (paste.apply(world, buffer, pos, mirror, rotation, placement, config.paste())) {
	            Identifier biome = world.getBiome(pos).unwrapKey().map(ResourceKey::identifier).orElse(null);
	            for (TemplateDecorator<T> decorator : decoratorConfig.getDecorators(biome)) {
	                decorator.apply(world, buffer, rand, modified);
	            }
	            return true;
	        }
	
	        return false;
        } else {
        	throw new IllegalStateException();
        }
    }

    // a tree grown from a sapling (#41), as the tree is pasted in generation: a 2 x 2 one with its trunk on the four
    // saplings whose north-west one is at pos, and a bee nest with the given chance, as Minecraft's tree grown next to
    // flowers has
    public static <T extends TemplateContext> boolean growSapling(ServerLevel level, RandomSource rand, BlockPos pos, Config<T> config, boolean twoByTwo, float beeNest) {
        if (config.templates().isEmpty() || !(level.getServer() instanceof RTFMinecraftServer rtfMinecraftServer)) {
            return false;
        }
        Mirror mirror = nextMirror(rand);
        Rotation rotation = nextRotation(rand);
        FeatureTemplate template = rtfMinecraftServer.getFeatureTemplateManager().load(nextTemplate(config.templates, rand));
        if (twoByTwo) {
            BlockPos corner = template.trunkCorner(mirror, rotation);
            if (corner != null) {
                pos = pos.subtract(corner);
            }
        }

        TemplatePlacement<T> placement = new SaplingPlacement<>(config.placement());
        if (!placement.canPlaceAt(level, pos, template.getDimensions(mirror, rotation))) {
            return false;
        }
        T buffer = placement.createContext();
        if (!FeatureTemplate.WORLD_GEN.get(template).apply(level, buffer, pos, mirror, rotation, placement, config.paste())) {
            return false;
        }
        Identifier biome = level.getBiome(pos).unwrapKey().map(ResourceKey::identifier).orElse(null);
        for (TemplateDecorator<T> decorator : config.decorator().getDecorators(biome)) {
            decorator.apply(level, buffer, rand, true);
        }
        if (beeNest > 0.0F && buffer instanceof TreeContext tree) {
            TemplateDecorators.beeNest(beeNest).apply(level, tree, rand, true);
        }
        return true;
    }

	private static Identifier nextTemplate(List<Identifier> templates, RandomSource random) {
        return templates.get(random.nextInt(templates.size()));
    }

    private static Mirror nextMirror(RandomSource random) {
        return Mirror.values()[random.nextInt(Mirror.values().length)];
    }

    private static Rotation nextRotation(RandomSource random) {
        return Rotation.values()[random.nextInt(Rotation.values().length)];
    }
    
	public record Config<T extends TemplateContext>(List<Identifier> templates, TemplatePlacement<T> placement, PasteConfig paste, DecoratorConfig<T> decorator) implements FeatureConfiguration {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public static final Codec<Config<?>> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.listOf().fieldOf("templates").forGetter(Config::templates),
			TemplatePlacement.CODEC.fieldOf("placement").forGetter(Config::placement),
			PasteConfig.CODEC.fieldOf("paste").forGetter(Config::paste),
			DecoratorConfig.CODEC.fieldOf("decorator").forGetter(Config::decorator)
		).apply(instance, (templates, placement, paste, decorator) -> new Config(templates, placement, paste, decorator)));
	}

    // the placement of a tree grown from a sapling: what it may replace is vanilla's (the player's saplings and flowers stay)
    private record SaplingPlacement<T extends TemplateContext>(TemplatePlacement<T> placement) implements TemplatePlacement<T> {

        @Override
        public boolean canPlaceAt(LevelAccessor world, BlockPos pos, Dimensions dimensions) {
            return this.placement.canPlaceAt(world, pos, dimensions);
        }

        @Override
        public boolean canReplaceAt(LevelAccessor world, BlockPos pos) {
            return this.placement.canReplaceAt(world, pos) && BlockUtils.canSaplingTreeReplace(world, pos);
        }

        @Override
        public T createContext() {
            return this.placement.createContext();
        }

        @Override
        public MapCodec<? extends TemplatePlacement<T>> codec() {
            throw new UnsupportedOperationException();
        }
    }
}
