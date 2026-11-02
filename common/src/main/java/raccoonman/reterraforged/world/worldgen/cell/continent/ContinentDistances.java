package raccoonman.reterraforged.world.worldgen.cell.continent;

import raccoonman.reterraforged.world.worldgen.util.PosUtil;

/** How far the rivers of a continent run from its center (the simple and advanced continents) */
public final class ContinentDistances {

	private ContinentDistances() {
	}

	/** From the center (cx, cz) along (dx, dz) to the shallow ocean: halves the way to the next continent's cell */
	public static float toOcean(SimpleContinent continent, float shallowOcean, int cx, int cz, float dx, float dz) {
		float high = continent.getDistanceToEdge(cx, cz, dx, dz);
		float low = 0.0F;
		for (int i = 0; i < 50; ++i) {
			float mid = (low + high) / 2.0F;
			float x = cx + dx * mid;
			float z = cz + dz * mid;
			float edge = continent.getEdgeValue(x, z);
			if (edge > shallowOcean) {
				low = mid;
			} else {
				high = mid;
			}
			if (high - low < 10.0F) {
				break;
			}
		}
		return high;
	}

	/** From the center (cx, cz) along (dx, dz) to where another continent's cell begins */
	public static float toEdge(Continent continent, int continentScale, int cx, int cz, float dx, float dz) {
		float distance = (float) (continentScale * 4);
		for (int i = 0; i < 10; ++i) {
			float x = cx + dx * distance;
			float z = cz + dz * distance;
			long centerPos = continent.getNearestCenter(x, z);
			int conX = PosUtil.unpackLeft(centerPos);
			int conZ = PosUtil.unpackRight(centerPos);
			distance += distance;
			if (conX != cx || conZ != cz) {
				float low = 0.0F;
				float high = distance;
				for (int j = 0; j < 50; ++j) {
					float mid = (low + high) / 2.0F;
					float px = cx + dx * mid;
					float pz = cz + dz * mid;
					centerPos = continent.getNearestCenter(px, pz);
					conX = PosUtil.unpackLeft(centerPos);
					conZ = PosUtil.unpackRight(centerPos);
					if (conX == cx && conZ == cz) {
						low = mid;
					} else {
						high = mid;
					}
					if (high - low < 50.0F) {
						break;
					}
				}
				return high;
			}
		}
		return distance;
	}
}
