package raccoonman.reterraforged.data.worldgen.tags;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.tags.RTFBlockTags;

public class RTFBlockTagsProvider extends TagsProvider<Block> {
	private Preset preset;
	
	public RTFBlockTagsProvider(Preset preset, PackOutput packOutput, CompletableFuture<Provider> completableFuture) {
		super(packOutput, Registries.BLOCK, completableFuture);

		this.preset = preset;
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
		this.tag(RTFBlockTags.SOIL).add(key(Blocks.DIRT), key(Blocks.COARSE_DIRT));
		this.tag(RTFBlockTags.CLAY).add(key(Blocks.CLAY));
		this.tag(RTFBlockTags.SEDIMENT).add(key(Blocks.SAND), key(Blocks.GRAVEL));
		this.tag(RTFBlockTags.ERODIBLE).add(key(Blocks.SNOW_BLOCK)).add(key(Blocks.POWDER_SNOW)).add(key(Blocks.GRAVEL)).addOptionalTag(BlockTags.SUBSTRATE_OVERWORLD);
		
		// the rock of the strata: vanilla's ore-bearing stones, and the overworld stones of other mods as optional entries
		// (nothing changes without them); "Ore Compatible Stone Only" keeps those ores generate in (StrataRule.Strata)
		this.tag(RTFBlockTags.ROCK).add(key(Blocks.GRANITE), key(Blocks.ANDESITE), key(Blocks.STONE), key(Blocks.DIORITE))
			.addOptional(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("quark", "jasper")))
			.addOptional(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("quark", "limestone")))
			.addOptional(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("quark", "shale")));
		this.tag(RTFBlockTags.ORE_COMPATIBLE).addOptionalTag(BlockTags.STONE_ORE_REPLACEABLES).addOptionalTag(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
	}

	// 26.2: tag appenders take registry keys
	private static ResourceKey<Block> key(Block block) {
		return block.builtInRegistryHolder().key();
	}
}
