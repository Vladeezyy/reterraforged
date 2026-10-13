package raccoonman.reterraforged.world.worldgen.surface.rule;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.material.rule.RuleEvaluator;
import org.jetbrains.annotations.Nullable;
import raccoonman.reterraforged.mixin.MaterialRuleContextAccessor;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.surface.RTFSurfaceSystem;

public record StrataRule(Identifier name, Holder<Noise> selector, List<Strata> strata, int iterations) implements MaterialRule {
	public static final MapCodec<StrataRule> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Identifier.CODEC.fieldOf("name").forGetter(StrataRule::name),
		Noise.CODEC.fieldOf("selector").forGetter(StrataRule::selector),
		Strata.CODEC.listOf().fieldOf("strata").forGetter(StrataRule::strata),
		Codec.INT.fieldOf("iterations").forGetter(StrataRule::iterations)
	).apply(instance, StrataRule::new));
	
	public StrataRule {
		strata = ImmutableList.copyOf(strata);
	}
	
	@Override
	public RuleEvaluator compile(MaterialRuleContext ctx) {
		MaterialRuleContextAccessor accessor = (MaterialRuleContextAccessor) (Object) ctx;
		ChunkAccess chunk = RTFSurfaceSystem.BUILDING_CHUNK.get();
		if(chunk != null && accessor.reterraforged$system() instanceof RTFSurfaceSystem rtfSurfaceSystem && (Object) accessor.reterraforged$randomState() instanceof RTFRandomState rtfRandomState && rtfRandomState.generatorContext() != null) {
			return new Source(chunk, rtfRandomState.seed(this.selector.value()), rtfSurfaceSystem.getOrCreateStrata(this.name, this::generateStrata));
		} else {
			return (x, y, z) -> null;
		}
	}

	@Override
	public MapCodec<StrataRule> codec() {
		return CODEC;
	}
	
	private List<List<Layer>> generateStrata(RandomSource random) {
        List<List<Layer>> layers = new ArrayList<>();
		for(int i = 0; i < this.iterations; i++) {
			List<Layer> layer = new ArrayList<>();
			for(Strata strata : this.strata) {
				layer.addAll(strata.generateLayers(random));
			}
			layers.add(layer);
		}
        return layers;
	}
	
	public record Strata(TagKey<Block> materials, Holder<Noise> noise, int attempts, int minLayers, int maxLayers, float minDepth, float maxDepth) {
		public static final Codec<Strata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			TagKey.hashedCodec(Registries.BLOCK).fieldOf("materials").forGetter(Strata::materials),
			Noise.CODEC.fieldOf("noise").forGetter(Strata::noise),
			Codec.INT.fieldOf("attempts").forGetter(Strata::attempts),
			Codec.INT.fieldOf("min_layers").forGetter(Strata::minLayers),
			Codec.INT.fieldOf("max_layers").forGetter(Strata::maxLayers),
			Codec.FLOAT.fieldOf("min_depth").forGetter(Strata::minDepth),
			Codec.FLOAT.fieldOf("max_depth").forGetter(Strata::maxDepth)			
		).apply(instance, Strata::new));
		
		public List<Layer> generateLayers(RandomSource random) {
			int lastIndex = -1;
	        int layers = this.minLayers + NoiseUtil.round(random.nextFloat() * (this.maxLayers - this.minLayers));
	        List<Layer> result = new ArrayList<>();
	        List<Holder<Block>> materials = ImmutableList.copyOf(BuiltInRegistries.BLOCK.getTagOrEmpty(this.materials));
	        
	        int seed = random.nextInt();
	        for (int i = 0; i < layers; i++) {
	            int attempts = this.attempts;
	            int index = random.nextInt(materials.size());
	            while (--attempts >= 0 && index == lastIndex) {
	                index = random.nextInt(materials.size());
	            }
	            if (index != lastIndex) {
	                lastIndex = index;
	                BlockState material = materials.get(index).value().defaultBlockState();
	                float depth = this.minDepth + random.nextFloat() * (this.maxDepth - this.minDepth);
	                result.add(new Layer(material, Noises.shiftSeed(Noises.mul(this.noise.value(), depth), random.nextInt()), seed));
	            }
	        }
	        return result;
		}
	}
	
	// this has to be public so that SurfaceSystemExtension can access it
	// should be private otherwise
	public record Layer(BlockState material, Noise depth, int seed) {
	
		public float computeDepth(float x, float z) {
			return this.depth.compute(x, z, this.seed);
		}
	}
	
	private class Source implements RuleEvaluator {
		private ChunkAccess chunk;
		private Noise selector;
		private List<List<Layer>> strata;
		private List<Layer> layers;
		private float[] depthBuffer;
		private int lastX, lastZ;
		private boolean initialized;
		
		public Source(ChunkAccess chunk, Noise selector, List<List<Layer>> strata) {
			this.chunk = chunk;
			this.selector = selector;
			this.strata = strata;
		}
		
        @Nullable
		@Override
		public BlockState tryApply(int x, int y, int z) {
        	if(!this.initialized || this.lastX != x || this.lastZ != z) {
        		this.initBuffer(x, z);
        		this.lastX = x;
        		this.lastZ = z;
        		this.initialized = true;
        	}
        	
        	Layer last = null;
        	for(int i = 0; i < this.layers.size(); i++) {
        		Layer layer = last = this.layers.get(i);
        		if(y > this.depthBuffer[i]) {
        			return layer.material();
        		}
        	}
        	
        	return last != null ? last.material() : null;
		}
		
		private void initBuffer(int x, int z) {
        	this.layers = this.selectLayers(x, z);
			int layerCount = this.layers.size();
			
	        if (this.depthBuffer == null || this.depthBuffer.length < layerCount) {
	            this.depthBuffer = new float[layerCount];
	        }
	        
            int localX = x & 0xF;
            int localZ = z & 0xF;
            int height = this.chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, localZ);

            float sum = 0.0F;
            for(int i = 0; i < layerCount; i++) {
            	Layer layer = this.layers.get(i);
            	float depth = layer.computeDepth(x, z);
            	sum += depth;
            	this.depthBuffer[i] = depth;
            }
            
            int y = height;
            for(int i = 0; i < layerCount; i++) {
            	this.depthBuffer[i] = y -= Math.round((this.depthBuffer[i] / sum) * height);
            }
		}
		
		private List<Layer> selectLayers(int x, int z) {
			float selector = this.selector.compute(x, z, 0);
	        int index = (int) (selector * this.strata.size());
	        index = Math.min(this.strata.size() - 1, index);
	        return this.strata.get(index);
	    }
	}
}
