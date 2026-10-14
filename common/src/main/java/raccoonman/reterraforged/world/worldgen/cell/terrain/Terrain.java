package raccoonman.reterraforged.world.worldgen.cell.terrain;

import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;

public class Terrain implements ITerrain.Delegate {
    private int id;
    private String name;
    private TerrainCategory type;
    private ITerrain delegate;
    // the delegate's erosion modifier, fixed once the terrain is made: the erosion filter asks for it at every step of
    // every droplet, and following the delegate there each time was a noticeable part of the generation time
    private final float erosionModifier;
    
    Terrain(int id, String name, Terrain terrain) {
        this(id, name, terrain.getCategory(), terrain);
    }
    
    Terrain(int id, String name, TerrainCategory type) {
        this(id, name, type, type);
    }
    
    Terrain(int id, String name, TerrainCategory type, ITerrain delegate) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.delegate = delegate;
        this.erosionModifier = delegate.erosionModifier();
    }
    
    public int getId() {
        return this.id;
    }
    
    public String getName() {
        return this.name;
    }
    
    public TerrainCategory getCategory() {
        return this.type;
    }
    
    public float getRenderHue() {
        return NoiseUtil.valCoord2D(this.name.hashCode(), 0, 0);
    }
    
    @Override
    public float erosionModifier() {
        return this.erosionModifier;
    }
    
    @Override
    public ITerrain getDelegate() {
        return this.delegate;
    }
    
    @Override
    public String toString() {
        return this.getName();
    }
    
    public Terrain withId(int id) {
        ITerrain delegate = (this.delegate instanceof Terrain) ? this.delegate : this;
        return new Terrain(id, this.name, this.type, delegate);
    }
}
