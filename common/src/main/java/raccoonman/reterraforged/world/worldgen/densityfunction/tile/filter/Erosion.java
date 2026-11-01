package raccoonman.reterraforged.world.worldgen.densityfunction.tile.filter;

import java.util.function.IntFunction;

import raccoonman.reterraforged.data.worldgen.preset.settings.FilterSettings;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Size;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.util.FastRandom;

public class Erosion implements Filter {
    private final float erodeSpeed;
    private final float depositSpeed;
    private final float initialSpeed;
    private final float initialWaterVolume;
    private final int maxDropletLifetime;
    private final float minCapacity;
    private final int[][] erosionBrushIndices;
    private final float[][] erosionBrushWeights;
    private final int seed;
    private final int mapSize;
    private final Modifier modifier;
    
    public Erosion(final int seed, final int mapSize, final FilterSettings.Erosion settings, final Modifier modifier) {
        this.seed = seed;
        this.mapSize = mapSize;
        this.modifier = modifier;
        this.erodeSpeed = settings.erosionRate;
        this.depositSpeed = settings.depositeRate;
        this.initialSpeed = settings.dropletVelocity;
        this.initialWaterVolume = settings.dropletVolume;
        this.maxDropletLifetime = settings.dropletLifetime;
        // the minimum sediment capacity of a droplet: 0.01 as in TerraForged; it is larger than what small droplets
        // carry, so below a volume x velocity of 0.36 they all eroded alike. Now it shrinks with them there (every
        // built-in preset is above 0.36: its erosion is unchanged), except for presets saved before 0.0.22
        this.minCapacity = settings.legacyMinCapacity ? 0.01F : 0.01F * Math.min(1.0F, settings.dropletVelocity * settings.dropletVolume / 0.36F);
        this.erosionBrushIndices = new int[mapSize * mapSize][];
        this.erosionBrushWeights = new float[mapSize * mapSize][];
        this.initBrushes(mapSize, 4);
    }
    
    public int getSize() {
        return this.mapSize;
    }
    
    @Override
    public void apply(Filterable map, int regionX, int regionZ, int iterationsPerChunk) {
        final int chunkX = map.getBlockX() >> 4;
        final int chunkZ = map.getBlockZ() >> 4;
        final int lengthChunks = map.getBlockSize().total() >> 4;
        final int borderChunks = map.getBlockSize().border() >> 4;
        final Size size = map.getBlockSize();
        final int mapSize = size.total();
        final float maxPos = (float)(mapSize - 2);
        final Cell[] cells = map.getBacking();
        // the droplets read and change the heights of the cells: in arrays they stay in the CPU's cache (the Cell
        // objects are spread over the heap). What does not change while eroding (the terrain's strength, the mask)
        // is worked out once. The same float operations in the same order: the result is the same to the bit.
        final Scratch scratch = SCRATCH.get().of(cells.length);
        final float[] height = scratch.height;
        final float[] sediment = scratch.sediment;
        final float[] heightErosion = scratch.heightErosion;
        final float[] strength = scratch.strength;
        for (int n = 0; n < cells.length; n++) {
            final Cell cell = cells[n];
            height[n] = cell.height;
            sediment[n] = cell.sediment;
            heightErosion[n] = cell.heightErosion;
            scratch.mask[n] = cell.erosionMask;
            strength[n] = cell.erosionMask ? 0.0F : Modifier.strength(cell);
        }
        final TerrainPos gradient1 = new TerrainPos();
        final TerrainPos gradient2 = new TerrainPos();
        final FastRandom random = new FastRandom();
        for (int i = 0; i < iterationsPerChunk; ++i) {
            final long iterationSeed = NoiseUtil.seed(this.seed, i);
            for (int cz = 0; cz < lengthChunks; ++cz) {
                final int relZ = cz << 4;
                final int seedZ = chunkZ + cz - borderChunks;
                for (int cx = 0; cx < lengthChunks; ++cx) {
                    final int relX = cx << 4;
                    final int seedX = chunkX + cx - borderChunks;
                    final long chunkSeed = NoiseUtil.seed(seedX, seedZ);
                    random.seed(chunkSeed, iterationSeed);
                    float posX = (float)(relX + random.nextInt(16));
                    float posZ = (float)(relZ + random.nextInt(16));
                    posX = NoiseUtil.clamp(posX, 1.0f, maxPos);
                    posZ = NoiseUtil.clamp(posZ, 1.0f, maxPos);
                    this.applyDrop(posX, posZ, scratch, mapSize, gradient1, gradient2);
                }
            }
        }
        for (int n = 0; n < cells.length; n++) {
            final Cell cell = cells[n];
            cell.height = height[n];
            cell.sediment = sediment[n];
            cell.heightErosion = heightErosion[n];
        }
    }
    
    private void applyDrop(float posX, float posY, final Scratch scratch, final int mapSize, final TerrainPos gradient1, final TerrainPos gradient2) {
        final float[] cells = scratch.height;
        float dirX = 0.0f;
        float dirY = 0.0f;
        float sediment = 0.0f;
        float speed = this.initialSpeed;
        float water = this.initialWaterVolume;
        gradient1.reset();
        gradient2.reset();
        for (int lifetime = 0; lifetime < this.maxDropletLifetime; ++lifetime) {
            final int nodeX = (int)posX;
            final int nodeY = (int)posY;
            final int dropletIndex = nodeY * mapSize + nodeX;
            final float cellOffsetX = posX - nodeX;
            final float cellOffsetY = posY - nodeY;
            gradient1.at(cells, mapSize, posX, posY);
            dirX = dirX * 0.05f - gradient1.gradientX * 0.95f;
            dirY = dirY * 0.05f - gradient1.gradientY * 0.95f;
            float len = (float)Math.sqrt(dirX * dirX + dirY * dirY);
            if (Float.isNaN(len)) {
                len = 0.0f;
            }
            if (len != 0.0f) {
                dirX /= len;
                dirY /= len;
            }
            posX += dirX;
            posY += dirY;
            if ((dirX == 0.0f && dirY == 0.0f) || posX < 0.0f || posX >= mapSize - 1 || posY < 0.0f || posY >= mapSize - 1) {
                return;
            }
            final float newHeight = gradient2.at(cells, mapSize, posX, posY).height;
            final float deltaHeight = newHeight - gradient1.height;
            final float sedimentCapacity = Math.max(-deltaHeight * speed * water * 4.0f, this.minCapacity);
            if (sediment > sedimentCapacity || deltaHeight > 0.0f) {
                final float amountToDeposit = (deltaHeight > 0.0f) ? Math.min(deltaHeight, sediment) : ((sediment - sedimentCapacity) * this.depositSpeed);
                sediment -= amountToDeposit;
                this.deposit(scratch, dropletIndex, amountToDeposit * (1.0f - cellOffsetX) * (1.0f - cellOffsetY));
                this.deposit(scratch, dropletIndex + 1, amountToDeposit * cellOffsetX * (1.0f - cellOffsetY));
                this.deposit(scratch, dropletIndex + mapSize, amountToDeposit * (1.0f - cellOffsetX) * cellOffsetY);
                this.deposit(scratch, dropletIndex + mapSize + 1, amountToDeposit * cellOffsetX * cellOffsetY);
            }
            else {
                final float amountToErode = Math.min((sedimentCapacity - sediment) * this.erodeSpeed, -deltaHeight);
                final int[] brushIndices = this.erosionBrushIndices[dropletIndex];
                final float[] brushWeights = this.erosionBrushWeights[dropletIndex];
                for (int brushPointIndex = 0; brushPointIndex < brushIndices.length; ++brushPointIndex) {
                    final int nodeIndex = brushIndices[brushPointIndex];
                    final float weighedErodeAmount = amountToErode * brushWeights[brushPointIndex];
                    final float deltaSediment = Math.min(cells[nodeIndex], weighedErodeAmount);
                    this.erode(scratch, nodeIndex, deltaSediment);
                    sediment += deltaSediment;
                }
            }
            speed = (float)Math.sqrt(speed * speed + deltaHeight * 3.0f);
            water *= 0.99f;
            if (Float.isNaN(speed)) {
                speed = 0.0f;
            }
        }
    }
    
    private void initBrushes(final int size, final int radius) {
        final int[] xOffsets = new int[radius * radius * 4];
        final int[] yOffsets = new int[radius * radius * 4];
        final float[] weights = new float[radius * radius * 4];
        float weightSum = 0.0f;
        int addIndex = 0;
        for (int i = 0; i < this.erosionBrushIndices.length; ++i) {
            final int centreX = i % size;
            final int centreY = i / size;
            if (centreY <= radius || centreY >= size - radius || centreX <= radius + 1 || centreX >= size - radius) {
                weightSum = 0.0f;
                addIndex = 0;
                for (int y = -radius; y <= radius; ++y) {
                    for (int x = -radius; x <= radius; ++x) {
                        final float sqrDst = (float)(x * x + y * y);
                        if (sqrDst < radius * radius) {
                            final int coordX = centreX + x;
                            final int coordY = centreY + y;
                            if (coordX >= 0 && coordX < size && coordY >= 0 && coordY < size) {
                                final float weight = 1.0f - (float)Math.sqrt(sqrDst) / radius;
                                weightSum += weight;
                                weights[addIndex] = weight;
                                xOffsets[addIndex] = x;
                                yOffsets[addIndex] = y;
                                ++addIndex;
                            }
                        }
                    }
                }
            }
            final int numEntries = addIndex;
            this.erosionBrushIndices[i] = new int[numEntries];
            this.erosionBrushWeights[i] = new float[numEntries];
            for (int j = 0; j < numEntries; ++j) {
                this.erosionBrushIndices[i][j] = (yOffsets[j] + centreY) * size + xOffsets[j] + centreX;
                this.erosionBrushWeights[i][j] = weights[j] / weightSum;
            }
        }
    }
    
    // Modifier.modify on the arrays: getValueModifier(height) * strength * amount
    private void deposit(final Scratch scratch, final int n, final float amount) {
        if (!scratch.mask[n]) {
            final float change = this.modifier.getValueModifier(scratch.height[n]) * scratch.strength[n] * amount;
            scratch.height[n] += change;
            scratch.sediment[n] += change;
        }
    }
    
    private void erode(final Scratch scratch, final int n, final float amount) {
        if (!scratch.mask[n]) {
            final float change = this.modifier.getValueModifier(scratch.height[n]) * scratch.strength[n] * amount;
            scratch.height[n] -= change;
            scratch.heightErosion[n] -= change;
        }
    }

    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    /** The arrays of one thread, kept for the next tile of the same size */
    private static class Scratch {
        private float[] height = new float[0];
        private float[] sediment, heightErosion, strength;
        private boolean[] mask;

        private Scratch of(int length) {
            if (this.height.length != length) {
                this.height = new float[length];
                this.sediment = new float[length];
                this.heightErosion = new float[length];
                this.strength = new float[length];
                this.mask = new boolean[length];
            }
            return this;
        }
    }
    
    public static IntFunction<Erosion> factory(final GeneratorContext context) {
        return new Factory(context.seed.root(), context.preset.filters(), context.levels);
    }
    
    private static class TerrainPos
    {
        private float height;
        private float gradientX;
        private float gradientY;
        
        private TerrainPos at(final float[] heights, final int mapSize, final float posX, final float posY) {
            final int coordX = (int)posX;
            final int coordY = (int)posY;
            final float x = posX - coordX;
            final float y = posY - coordY;
            final int nodeIndexNW = coordY * mapSize + coordX;
            final float heightNW = heights[nodeIndexNW];
            final float heightNE = heights[nodeIndexNW + 1];
            final float heightSW = heights[nodeIndexNW + mapSize];
            final float heightSE = heights[nodeIndexNW + mapSize + 1];
            this.gradientX = (heightNE - heightNW) * (1.0f - y) + (heightSE - heightSW) * y;
            this.gradientY = (heightSW - heightNW) * (1.0f - x) + (heightSE - heightNE) * x;
            this.height = heightNW * (1.0f - x) * (1.0f - y) + heightNE * x * (1.0f - y) + heightSW * (1.0f - x) * y + heightSE * x * y;
            return this;
        }
        
        private void reset() {
            this.height = 0.0f;
            this.gradientX = 0.0f;
            this.gradientY = 0.0f;
        }
    }
    
    private static class Factory implements IntFunction<Erosion> {
        private static final int SEED_OFFSET = 12768;
        private final int seed;
        private final Modifier modifier;
        private final FilterSettings.Erosion settings;
        
        private Factory(final int seed, final FilterSettings filters, final Levels levels) {
            this.seed = seed + 12768;
            this.settings = filters.erosion.copy();
            this.modifier = Modifier.range(levels.ground, levels.ground(15));
        }
        
        @Override
        public Erosion apply(final int size) {
            return new Erosion(this.seed, size, this.settings, this.modifier);
        }
    }
}
