package raccoonman.reterraforged.world.worldgen.cell.rivermap.wetland;

import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.cell.rivermap.fade.RiverTerrainFade;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil.Vec2f;
import raccoonman.reterraforged.world.worldgen.noise.module.Line;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.util.Boundsf;

public class Wetland {
    private Vec2f a;
    private Vec2f b;
    private float radius;
    private float radius2;
    private float bed;
    private float banks;
    private float moundMin;
    private float moundMax;
    private float moundVariance;
    private Noise moundShape;
    private Noise moundHeight;
    private Noise terrainEdge;

    private float fadeStartHeight;
    private float fadeEndHeight;
    // the wetlands of ReTerraForged 0.0.6 (MiscellaneousSettings.originalGeneration); the 1.21.1 port rewrote them
    // (lower banks, other mounds, fading out on high ground and in mountains)
    private boolean original;

    public Wetland(int seed, Vec2f a, Vec2f b, float radius, Levels levels, boolean original) {
        this.a = a;
        this.b = b;
        this.radius = radius;
        this.radius2 = radius * radius;
        this.original = original;
        if (original) {
            this.bed = levels.water(-1) - 0.5F / levels.worldHeight;
            this.banks = levels.ground(3);
            this.moundMin = levels.water(1);
            this.moundMax = levels.water(2);
            this.moundVariance = this.moundMax - this.moundMin;

            Noise moundShape = Noises.perlin(++seed, 10, 1);
            moundShape = Noises.clamp(moundShape, 0.3F, 0.6F);
            moundShape = Noises.map(moundShape, 0.0F, 1.0F);
            this.moundShape = moundShape;

            Noise moundHeight = Noises.simplex(++seed, 20, 1);
            moundHeight = Noises.clamp(moundHeight, 0.0F, 0.3F);
            moundHeight = Noises.map(moundHeight, 0.0F, 1.0F);
            this.moundHeight = moundHeight;

            Noise terrainEdge = Noises.perlin(++seed, 8, 1);
            terrainEdge = Noises.clamp(terrainEdge, 0.2F, 0.8F);
            terrainEdge = Noises.map(terrainEdge, 0.0F, 0.9F);
            this.terrainEdge = terrainEdge;
            return;
        }
        this.bed = levels.water(-1) - 0.5F / (float)levels.scale(1.0F);
        this.banks = levels.water(1);
        this.moundMin = levels.water(0);
        this.moundMax = levels.water(1);
        this.moundVariance = this.moundMax - this.moundMin;

        this.moundShape = Noises.simplex(seed + 1, 10, 1);
        this.moundHeight = Noises.simplex(seed + 2, 20, 1);
        this.terrainEdge = Noises.perlin(seed + 3, 15, 1);

        this.fadeStartHeight = levels.scale(300);
        this.fadeEndHeight = levels.scale(360);
    }

    // x, z: the warped position (the wetland's shape), rx, rz: the position itself
    public void apply(Cell cell, float x, float z, float rx, float rz) {
        if (this.original) {
            this.applyOriginal(cell, x, z, rx, rz);
            return;
        }
        float heightFade = RiverTerrainFade.heightFade(cell.height, this.fadeStartHeight, this.fadeEndHeight);
        float valleyFade = RiverTerrainFade.valleyFade(cell, heightFade);
        float poolsFade = RiverTerrainFade.bedFade(cell, heightFade);

        float dist2 = distToLineSq(x, z, this.a.x(), this.a.y(), this.b.x(), this.b.y());

        if (dist2 > this.radius2) {
            return;
        }
        float dist = (float)Math.sqrt(dist2);
        float valleyAlpha = 1.0F - dist / this.radius;
        valleyAlpha *= valleyFade;

        if (valleyAlpha <= 0.0F) {
            return;
        }

        float bankHeight = this.banks;
        float bedHeight = this.bed;

        if (RiverTerrainFade.isMountain(cell)) {
            float wetlandThreshold = this.banks + (2.0F / 1184.0F);

            if (cell.height > wetlandThreshold) {
                float delta = cell.height - wetlandThreshold;
                float blendSlope = 10.0F;

                float blendFactor = delta * blendSlope;
                blendFactor = Math.min(1.0F, Math.max(0.0F, blendFactor));

                bankHeight = NoiseUtil.lerp(bankHeight, cell.height, blendFactor);
                bedHeight = NoiseUtil.lerp(bedHeight, cell.height, blendFactor);

                if (valleyAlpha > 0.05F) {
                    cell.erosionMask = true;
                }
            }
        }

        float rawShape = this.moundShape.compute(x, z, 0);
        float shapeVal = NoiseUtil.clamp(rawShape, 0.0F, 1.0F);

        float poolsAlpha = valleyAlpha * shapeVal * poolsFade;
        poolsAlpha = NoiseUtil.clamp(poolsAlpha, 0.0F, 1.0F);

        if (cell.height > bedHeight && cell.height <= bankHeight) {
            cell.height = NoiseUtil.lerp(cell.height, bedHeight, poolsAlpha);
        }
        if (poolsAlpha >= 1.0F) {
            cell.erosionMask = true;
        }

        float edgeVal = NoiseUtil.clamp(this.terrainEdge.compute(x, z, 0), 0.2F, 0.8F);
        if (dist > 0.65F && poolsAlpha > edgeVal) {
            cell.terrain = TerrainType.WETLAND;
        }

        if (cell.height >= bedHeight && cell.height < this.moundMax) {
            float shapeAlpha = shapeVal * poolsAlpha;

            float rawHeight = this.moundHeight.compute(x, z, 0);
            float heightVal = NoiseUtil.clamp(rawHeight, 0.0F, 1.0F);

            float mounds = this.moundMin + heightVal * this.moundVariance;
            cell.height = NoiseUtil.lerp(cell.height, mounds, shapeAlpha);
        }
        cell.riverMask = Math.min(cell.riverMask, 1.0F - valleyAlpha);
    }

    // ReTerraForged 0.0.6: the distance from the warped position, the noises at the position itself
    private void applyOriginal(Cell cell, float x, float z, float rx, float rz) {
        if (cell.height < this.bed) {
            return;
        }
        float t = Line.distanceOnLine(x, z, this.a.x(), this.a.y(), this.b.x(), this.b.y());
        float d2 = getDistance2(x, z, this.a.x(), this.a.y(), this.b.x(), this.b.y(), t);
        if (d2 > this.radius2) {
            return;
        }
        float dist = 1.0F - d2 / this.radius2;
        if (dist <= 0.0F) {
            return;
        }
        float valleyAlpha = NoiseUtil.map(dist, 0.0F, 0.65F, 0.65F);
        if (cell.height > this.banks) {
            cell.height = NoiseUtil.lerp(cell.height, this.banks, valleyAlpha);
        }
        float poolsAlpha = NoiseUtil.map(dist, 0.65F, 0.7F, 0.050000012F);
        if (cell.height > this.bed && cell.height <= this.banks) {
            cell.height = NoiseUtil.lerp(cell.height, this.bed, poolsAlpha);
        }
        if (poolsAlpha >= 1.0F) {
            cell.erosionMask = true;
        }
        if (dist > 0.65F && poolsAlpha > this.terrainEdge.compute(rx, rz, 0)) {
            cell.terrain = TerrainType.WETLAND;
        }
        if (cell.height >= this.bed && cell.height < this.moundMax) {
            float shapeAlpha = this.moundShape.compute(rx, rz, 0) * poolsAlpha;
            float mounds = this.moundMin + this.moundHeight.compute(rx, rz, 0) * this.moundVariance;
            cell.height = NoiseUtil.lerp(cell.height, mounds, shapeAlpha);
        }
        cell.riverMask = Math.min(cell.riverMask, 1.0F - valleyAlpha);
    }

    public void recordBounds(Boundsf.Builder builder) {
        builder.record(Math.min(this.a.x(), this.b.x()) - this.radius, Math.min(this.a.y(), this.b.y()) - this.radius);
        builder.record(Math.max(this.a.x(), this.b.x()) + this.radius, Math.max(this.a.y(), this.b.y()) + this.radius);
    }

    private static float getDistance2(float x, float y, float ax, float ay, float bx, float by, float t) {
        if (t <= 0.0f) {
            return Line.distSq(x, y, ax, ay);
        }
        if (t >= 1.0f) {
            return Line.distSq(x, y, bx, by);
        }
        float px = ax + t * (bx - ax);
        float py = ay + t * (by - ay);
        return Line.distSq(x, y, px, py);
    }

    private static float distToLineSq(float px, float py, float l1x, float l1y, float l2x, float l2y) {
        float l2_l1_x = l2x - l1x;
        float l2_l1_y = l2y - l1y;
        float lengthSq = l2_l1_x * l2_l1_x + l2_l1_y * l2_l1_y;
        if (lengthSq == 0.0F) {
            float dx = px - l1x;
            float dy = py - l1y;
            return dx * dx + dy * dy;
        }

        float t = ((px - l1x) * l2_l1_x + (py - l1y) * l2_l1_y) / lengthSq;
        t = Math.max(0.0F, Math.min(1.0F, t));

        float projectionX = l1x + t * l2_l1_x;
        float projectionY = l1y + t * l2_l1_y;

        float dx = px - projectionX;
        float dy = py - projectionY;
        return dx * dx + dy * dy;
    }
}
