package raccoonman.reterraforged.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import raccoonman.reterraforged.RTFCommon;

public final class RTFBlockTags {
	public static final TagKey<Block> SOIL = resolve("soil");
	public static final TagKey<Block> ROCK = resolve("rock");
	public static final TagKey<Block> CLAY = resolve("clay");
	public static final TagKey<Block> SEDIMENT = resolve("sediment");
	public static final TagKey<Block> ERODIBLE = resolve("erodible");
	// the stones ores generate in: "Ore Compatible Stone Only" keeps the strata rock to these
	public static final TagKey<Block> ORE_COMPATIBLE = resolve("ore_compatible");
	
    private static TagKey<Block> resolve(String path) {
    	return TagKey.create(Registries.BLOCK, RTFCommon.location(path));
    }
}
