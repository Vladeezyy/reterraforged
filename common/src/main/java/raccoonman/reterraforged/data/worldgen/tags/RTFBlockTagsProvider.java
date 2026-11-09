package raccoonman.reterraforged.data.worldgen.tags;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.tags.RTFBlockTags;

public class RTFBlockTagsProvider extends IntrinsicHolderTagsProvider<Block> {
	private final Preset preset;
	
	public RTFBlockTagsProvider(Preset preset, PackOutput packOutput, CompletableFuture<Provider> completableFuture) {
		super(packOutput, Registries.BLOCK, completableFuture, (block) -> block.builtInRegistryHolder().key());

		this.preset = preset;
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
		this.tag(RTFBlockTags.SOIL).add(Blocks.DIRT, Blocks.COARSE_DIRT);
		this.tag(RTFBlockTags.CLAY).add(Blocks.CLAY);
		this.tag(RTFBlockTags.SEDIMENT).add(Blocks.SAND, Blocks.GRAVEL);
		// the vanilla blocks of the dirt tag by name (#40): other mods put their own surface blocks in it, and the erosion
		// turned them to the strata's rock on slopes, Regions Unexplored's chalk grass on its Chalk Cliffs. A datapack may
		// add a mod's block to this tag
		this.tag(RTFBlockTags.ERODIBLE).add(Blocks.SNOW_BLOCK).add(Blocks.POWDER_SNOW).add(Blocks.GRAVEL).add(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.MYCELIUM, Blocks.ROOTED_DIRT, Blocks.MOSS_BLOCK, Blocks.PALE_MOSS_BLOCK, Blocks.MUD, Blocks.MUDDY_MANGROVE_ROOTS);
		
		// the rock of the strata: vanilla's ore-bearing stones, and the overworld stones of other mods as optional entries
		// (nothing changes without them); "Ore Compatible Stone Only" keeps those ores generate in (StrataRule.Strata)
		this.tag(RTFBlockTags.ROCK).add(Blocks.GRANITE, Blocks.ANDESITE, Blocks.STONE, Blocks.DIORITE);
		// 1.21.6+: the appender takes registered blocks only, ids of other mods' blocks go to the raw builder
		this.getOrCreateRawBuilder(RTFBlockTags.ROCK)
			.addOptionalElement(Identifier.fromNamespaceAndPath("quark", "jasper"))
			.addOptionalElement(Identifier.fromNamespaceAndPath("quark", "limestone"))
			.addOptionalElement(Identifier.fromNamespaceAndPath("quark", "shale"));
		this.tag(RTFBlockTags.ORE_COMPATIBLE).addOptionalTag(BlockTags.STONE_ORE_REPLACEABLES).addOptionalTag(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
	}
}