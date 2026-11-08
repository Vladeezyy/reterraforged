package raccoonman.reterraforged.world.worldgen.terrablender;

import java.util.List;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.Regions;

/**
 * Which TerraBlender region a biome cell of a ReTerraForged world belongs to. Only touched where TerraBlender is loaded
 * (its mixins, the biome mod test).
 *
 * TerraBlender's uniqueness here is the biome cell's id (TBNoiseRouterData.UNIQUENESS), and the same id also picks the
 * cell's temperature and humidity among its biome type's pairs (BiomeType.getTemperature). A region read straight off
 * the id (round(id * last index)) therefore only ever had one end of each pair list: vanilla's region, index 0, which
 * also holds the biomes Lithostitched injects into vanilla's table (Regions Unexplored 0.6.3), got the first pair of
 * every type, the last region the last pairs. It also gave the first and the last region half a share each and ignored
 * the regions' weights. With Regions Unexplored and Biomes O' Plenty 24 kinds of Regions Unexplored's 47 generated (41
 * without BoP). Now the region is a hash of the id, chosen by the regions' weights as TerraBlender's own layer does.
 */
public final class TBRegions {

	private TBRegions() {
	}

	// the share of the regions' weights up to and including each index (TerraBlender's InitialLayer: Region.getWeight(),
	// only regions with a biome of this registry, the ones TerraBlender has a tree for); the last region with a weight
	// ends at infinity, so rounding never reaches one without; null if none has a weight
	public static float[] bounds(RegistryAccess registryAccess, RegionType regionType) {
		int count = Regions.getCount(regionType);
		if(count <= 0) {
			return null;
		}
		Registry<Biome> biomes = registryAccess.registryOrThrow(Registries.BIOME);
		List<Region> regions = Regions.get(regionType);
		float[] weights = new float[count];
		for(Region region : regions) {
			int index = Regions.getIndex(regionType, region.getName());
			if(index >= 0 && index < count && region.getType() == regionType && (index == 0 || hasBiomes(region, biomes))) {
				weights[index] = Math.max(0, region.getWeight());
			}
		}
		float total = 0.0F;
		int last = -1;
		for(int i = 0; i < count; i++) {
			total += weights[i];
			if(weights[i] > 0.0F) {
				last = i;
			}
		}
		if(last < 0) {
			return null;
		}
		float[] bounds = new float[count];
		float sum = 0.0F;
		for(int i = 0; i < count; i++) {
			sum += weights[i];
			bounds[i] = i >= last ? Float.POSITIVE_INFINITY : sum / total;
		}
		return bounds;
	}

	private static boolean hasBiomes(Region region, Registry<Biome> biomes) {
		boolean[] found = { false };
		region.addBiomes(biomes, (pair) -> {
			if(!found[0] && biomes.getHolder(pair.getSecond()).isPresent()) {
				found[0] = true;
			}
		});
		return found[0];
	}

	// the region of a biome cell's id (0..1): a hash of the id against the bounds above
	public static int region(float id, float[] bounds) {
		if(bounds == null) {
			return 0;
		}
		float roll = roll(id);
		for(int i = 0; i < bounds.length; i++) {
			if(roll < bounds[i]) {
				return i;
			}
		}
		return 0;
	}

	// 0..1, independent of the id's own value (murmur3's finalizer on its bits)
	private static float roll(float id) {
		int h = Float.floatToIntBits(id) ^ 0x52544652;
		h ^= h >>> 16;
		h *= 0x85EBCA6B;
		h ^= h >>> 13;
		h *= 0xC2B2AE35;
		h ^= h >>> 16;
		return (h >>> 8) * 0x1.0p-24F;
	}
}
