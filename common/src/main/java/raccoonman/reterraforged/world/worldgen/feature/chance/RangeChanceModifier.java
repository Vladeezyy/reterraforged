package raccoonman.reterraforged.world.worldgen.feature.chance;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;

public abstract class RangeChanceModifier implements ChanceModifier {
	protected final float from;
    protected final float to;
	protected final boolean exclusive;

    public RangeChanceModifier(float from, float to, boolean exclusive) {
        this.from = from;
        this.to = to;
        this.exclusive = exclusive;
    }
    
    protected abstract float getValue(GeneratorContext generatorContext, ChanceContext chanceCtx, FeaturePlaceContext<?> placeCtx);
    
    @Override
	public float getChance(ChanceContext chanceCtx, FeaturePlaceContext<?> placeCtx) {
    	GeneratorContext generatorContext = generatorContext(placeCtx);
    	// a dimension without ReTerraForged terrain (a mod's or a datapack's) with a biome whose trees the preset replaced:
    	// the biome modifiers change the biome in every dimension. The chance stays as it is there; this threw, and the
    	// server stopped with a "Feature placement" crash
    	if(generatorContext == null) {
    		return 1.0F;
    	}
        return this.apply(this.getValue(generatorContext, chanceCtx, placeCtx));
    }

    private float apply(float value) {
    	float max = this.exclusive ? 0 : 1;
    	float range = Math.abs(max - this.from);
        if (this.from < this.to) {
            if (value <= this.from) {
                return 0F;
            }
            if (value >= this.to) {
                return max;
            }
            return (value - this.from) / range;
        } else if (this.from > this.to) {
            if (value <= this.to) {
                return max;
            }
            if (value >= this.from) {
                return 0F;
            }
            return 1 - ((value - this.to) / range);
        }
        return 0F;
    }

    /** The cell of the world's tile at the feature's origin (only ReTerraForged worlds place these modifiers) */
    protected static Cell cellAt(GeneratorContext generatorContext, BlockPos pos) {
        int x = pos.getX();
        int z = pos.getZ();
        int chunkX = SectionPos.blockToSectionCoord(x);
        int chunkZ = SectionPos.blockToSectionCoord(z);
        Tile.Chunk chunk = generatorContext.cache.provideAtChunk(chunkX, chunkZ).getChunkReader(chunkX, chunkZ);
        return chunk.getCell(x, z);
    }

    @Nullable
    private static GeneratorContext generatorContext(FeaturePlaceContext<?> placeCtx) {
        if((Object) placeCtx.level().getLevel().getChunkSource().randomState() instanceof RTFRandomState rtfRandomState) {
            return rtfRandomState.generatorContext();
        }
        return null;
    }
}
