package raccoonman.reterraforged.mixin.terrablender;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.data.worldgen.preset.settings.CaveSettings.CompatibilityMode;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.platform.ModLoaderUtil;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.terrablender.TBCompat;
import raccoonman.reterraforged.world.worldgen.terrablender.TBTargetPoint;
import terrablender.api.RegionType;
import terrablender.api.Regions;

@Mixin(
	value = Climate.ParameterList.class,
	priority = 1001
)
class MixinParameterList<T> {
	private static final float UNDERGROUND_DEPTH_THRESHOLD = 0.2F;
	private static final int CAVE_BIOME_COMPAT_Y_MAX = 48;
	private static final int UNDERGROUND_COMPAT_REGION_SCALE = 20;
	private static final int UNDERGROUND_COMPAT_REGION_WEIGHT = 4;
	private static final ResourceLocation DARKER_DEPTHS_REGION = ResourceLocation.fromNamespaceAndPath("darkerdepths", "overworld");
	private static final ResourceLocation DEEPER_DARKER_REGION = ResourceLocation.fromNamespaceAndPath("deeperdarker", "overworld");

	// ReTerraForged gives the biome tables a temperature and humidity of its own biome types (cell.biome.type.BiomeType),
	// the middle of one vanilla band each and only some of the pairs: none on land is warm (level 3) and neutral (level
	// 2), which suits its own choice of vanilla biomes, but other mods' regions put biomes there and at other missing
	// pairs: Biomes O' Plenty's woodland, old growth woodland, grassland, orchard, pasture, tundra and more never
	// generated (39 of 61 kinds against 55 in a vanilla world). Their trees are searched with the temperature and
	// humidity moved by up to about one band by two noises, patches a few hundred blocks across; vanilla's region is
	// unchanged.
	private static final float REGION_CLIMATE_SCALE = 1.0F / 700.0F, REGION_CLIMATE_SHIFT = 0.35F;
	private SimplexNoise regionTemperatureNoise, regionHumidityNoise;
	private int maxIndex;
	private CompatibilityMode caveCompatibilityMode = CompatibilityMode.AUTO;
	private int darkerDepthsIndex = -1;
	private int deeperDarkerIndex = -1;
	private AtomicLongArray originalIndexCounts;
	private AtomicLongArray rtfIndexCounts;
	private AtomicLongArray directIndexCounts;
	private final AtomicLong diagnosticTotal = new AtomicLong();
	private final AtomicLong diagnosticOriginalPath = new AtomicLong();
	private final AtomicLong diagnosticRtfPath = new AtomicLong();
	private final AtomicLong diagnosticDirectPath = new AtomicLong();
	private final AtomicLong diagnosticDirectOverrides = new AtomicLong();
	private final AtomicLong diagnosticUnderground = new AtomicLong();

	@Inject(
		at = @At("HEAD"),
		method = "initializeForTerraBlender",
		require = 1	
	)
    public void initializeForTerraBlender(RegistryAccess registryAccess, RegionType regionType, long seed, CallbackInfo callback) {
    	this.maxIndex = Regions.getCount(regionType) - 1;
		this.regionTemperatureNoise = regionType == RegionType.OVERWORLD ? new SimplexNoise(RandomSource.create(seed ^ 0x52544654L)) : null;
		this.regionHumidityNoise = regionType == RegionType.OVERWORLD ? new SimplexNoise(RandomSource.create(seed ^ 0x52544648L)) : null;
		this.caveCompatibilityMode = getCaveCompatibilityMode(registryAccess);
		if(regionType == RegionType.OVERWORLD && hasUndergroundBiomeCompatTarget()) {
			this.originalIndexCounts = new AtomicLongArray(this.maxIndex + 1);
			this.rtfIndexCounts = new AtomicLongArray(this.maxIndex + 1);
			this.directIndexCounts = new AtomicLongArray(this.maxIndex + 1);
			this.darkerDepthsIndex = getRegionIndex(regionType, DARKER_DEPTHS_REGION);
			this.deeperDarkerIndex = getRegionIndex(regionType, DEEPER_DARKER_REGION);
			RTFCommon.LOGGER.info(
				"TerraBlender diagnostic enabled: maxIndex={}, darkerdepths={}, deeperdarker={}, caveCompatibilityMode={}, directUndergroundCompat={}",
				this.maxIndex,
				this.darkerDepthsIndex,
				this.deeperDarkerIndex,
				this.caveCompatibilityMode,
				this.isDirectUndergroundCompatEnabled()
			);
		}
//
//    	registryAccess.lookup(RTFRegistries.PRESET).flatMap((registry) -> {
//    		return registry.get(Preset.KEY);
//    	}).ifPresent((holder) -> {
//    		Preset preset = holder.value();
//        	TBCompat.setSurfaceRules(preset, (defaultRules) -> {
//        		return RTFSurfaceRuleData.overworld(preset, registryAccess.lookupOrThrow(Registries.DENSITY_FUNCTION), registryAccess.lookupOrThrow(RTFRegistries.NOISE), defaultRules);
//            });
//    	});
    }

	// the point findValuePositional looks up, for getUniqueness below: a @Redirect of its getUniqueness call needs the
	// call's owner, which Loom's remapper left in Mojang names for this method of TerraBlender's (1.21.10+ jars have no
	// refmap), so on Fabric 1.21.10 and 1.21.11 the preset's biome regions were never used
	private static final ThreadLocal<Climate.TargetPoint> REGION_POINT = new ThreadLocal<>();

	@Inject(
		method = "findValuePositional",
		at = @At("HEAD"),
		require = 0
	)
	private void findValuePositionalHead(Climate.TargetPoint targetPoint, int x, int y, int z, CallbackInfoReturnable<T> callback) {
		REGION_POINT.set(targetPoint);
	}

	@Inject(
		method = "findValuePositional",
		at = @At("RETURN"),
		require = 0
	)
	private void findValuePositionalReturn(Climate.TargetPoint targetPoint, int x, int y, int z, CallbackInfoReturnable<T> callback) {
		REGION_POINT.remove();
	}

	// the region of the point findValuePositional looks up: the preset's biome region at the surface, TerraBlender's
	// own choice underground and for points without one (null when not one of findValuePositional's)
	private Integer presetRegion(Climate.TargetPoint targetPoint, int x, int y, int z) {
		if(!((Object) targetPoint instanceof TBTargetPoint tbTargetPoint) || Double.isNaN(tbTargetPoint.getUniqueness())) {
			return null;
		}
		float depth = Climate.unquantizeCoord(targetPoint.depth());
		if(depth >= UNDERGROUND_DEPTH_THRESHOLD || (hasUndergroundBiomeCompatTarget() && QuartPos.toBlock(y) < CAVE_BIOME_COMPAT_Y_MAX)) {
			return null;
		}
		int selected = NoiseUtil.round(this.maxIndex * (float) tbTargetPoint.getUniqueness());
		this.recordDiagnosticSelection(selected, false, false);
		if(selected != 0 && this.regionTemperatureNoise != null && !TBCompat.probing) {
			double bx = QuartPos.toBlock(x) * REGION_CLIMATE_SCALE, bz = QuartPos.toBlock(z) * REGION_CLIMATE_SCALE;
			float temperature = Climate.unquantizeCoord(targetPoint.temperature()) + REGION_CLIMATE_SHIFT * (float) this.regionTemperatureNoise.getValue(bx, bz);
			float humidity = Climate.unquantizeCoord(targetPoint.humidity()) + REGION_CLIMATE_SHIFT * (float) this.regionHumidityNoise.getValue(bx, bz);
			tbTargetPoint.setRegionClimate(Climate.quantizeCoord(Math.max(-1.0F, Math.min(1.0F, temperature))), Climate.quantizeCoord(Math.max(-1.0F, Math.min(1.0F, humidity))));
		}
		return selected;
	}

	// the tree of the region found above is searched with the region temperature and humidity (the first search; the
	// second is vanilla's tree for the region's deferred placeholders, with the point as it is)
	@ModifyArg(
		method = "findValuePositional",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Climate$RTree;search(Lnet/minecraft/world/level/biome/Climate$TargetPoint;Lnet/minecraft/world/level/biome/Climate$DistanceMetric;)Ljava/lang/Object;",
			ordinal = 0
		),
		index = 0,
		require = 0
	)
	private Climate.TargetPoint regionTargetPoint(Climate.TargetPoint point) {
		if((Object) point instanceof TBTargetPoint tbTargetPoint && tbTargetPoint.getRegionTemperature() != Long.MIN_VALUE) {
			return new Climate.TargetPoint(tbTargetPoint.getRegionTemperature(), tbTargetPoint.getRegionHumidity(), point.continentalness(), point.erosion(), point.depth(), point.weirdness());
		}
		return point;
	}

	@Inject(
		method = "getUniqueness(III)I",
		at = @At("RETURN"),
		cancellable = true,
		require = 0
	)
	private void getUniqueness(int x, int y, int z, CallbackInfoReturnable<Integer> callback) {
		Climate.TargetPoint point = REGION_POINT.get();
		Integer region = point != null ? this.presetRegion(point, x, y, z) : null;
		if(region != null) {
			callback.setReturnValue(region);
			return;
		}
		int original = callback.getReturnValue();
		int selected = this.selectUndergroundCompatRegion(original, x, y, z);
		boolean underground = this.isUndergroundCompatY(y);
		boolean overridden = selected != original;
		this.recordDirectDiagnosticSelection(selected, underground, overridden);
		if(overridden) {
			callback.setReturnValue(selected);
		}
	}

	private static boolean hasUndergroundBiomeCompatTarget() {
		return ModLoaderUtil.isLoaded("darkerdepths") || ModLoaderUtil.isLoaded("deeperdarker");
	}

	private static CompatibilityMode getCaveCompatibilityMode(RegistryAccess registryAccess) {
		return registryAccess.lookup(RTFRegistries.PRESET).flatMap((registry) -> {
			return registry.get(Preset.KEY);
		}).map((holder) -> {
			return holder.value().caves().compatibilityMode;
		}).orElse(CompatibilityMode.AUTO);
	}

	private static int getRegionIndex(RegionType regionType, ResourceLocation region) {
		try {
			return Regions.getIndex(regionType, region);
		} catch(RuntimeException exception) {
			return -1;
		}
	}

	private int selectUndergroundCompatRegion(int selected, int x, int y, int z) {
		if(!this.isDirectUndergroundCompatEnabled() || !this.isUndergroundCompatY(y)) {
			return selected;
		}
		int target = this.selectUndergroundCompatTarget(x, y, z);
		return target >= 0 ? target : selected;
	}

	private boolean isDirectUndergroundCompatEnabled() {
		return this.caveCompatibilityMode == CompatibilityMode.AUTO && this.hasUndergroundCompatTargetRegion();
	}

	private boolean hasUndergroundCompatTargetRegion() {
		return this.isValidIndex(this.darkerDepthsIndex) || this.isValidIndex(this.deeperDarkerIndex);
	}

	private boolean isValidIndex(int index) {
		return index >= 0 && index <= this.maxIndex;
	}

	private boolean isUndergroundCompatY(int y) {
		return QuartPos.toBlock(y) < CAVE_BIOME_COMPAT_Y_MAX;
	}

	private int selectUndergroundCompatTarget(int x, int y, int z) {
		int roll = undergroundCompatRoll(x, y, z);
		if(roll >= UNDERGROUND_COMPAT_REGION_WEIGHT) {
			return -1;
		}
		boolean hasDarkerDepths = this.isValidIndex(this.darkerDepthsIndex);
		boolean hasDeeperDarker = this.isValidIndex(this.deeperDarkerIndex);
		if(hasDarkerDepths && hasDeeperDarker) {
			return (roll & 1) == 0 ? this.darkerDepthsIndex : this.deeperDarkerIndex;
		}
		return hasDarkerDepths ? this.darkerDepthsIndex : this.deeperDarkerIndex;
	}

	private static int undergroundCompatRoll(int x, int y, int z) {
		int hash = x >> 4;
		hash = 31 * hash + (y >> 2);
		hash = 31 * hash + (z >> 4);
		hash ^= hash >>> 16;
		hash *= 0x7feb352d;
		hash ^= hash >>> 15;
		hash *= 0x846ca68b;
		hash ^= hash >>> 16;
		return (hash & Integer.MAX_VALUE) % UNDERGROUND_COMPAT_REGION_SCALE;
	}

	private void recordDiagnosticSelection(int selected, boolean originalPath, boolean underground) {
		AtomicLongArray originalCounts = this.originalIndexCounts;
		AtomicLongArray rtfCounts = this.rtfIndexCounts;
		if(originalCounts == null || rtfCounts == null) {
			return;
		}
		if(selected >= 0 && selected <= this.maxIndex) {
			(originalPath ? originalCounts : rtfCounts).incrementAndGet(selected);
		}
		if(originalPath) {
			this.diagnosticOriginalPath.incrementAndGet();
		} else {
			this.diagnosticRtfPath.incrementAndGet();
		}
		if(underground) {
			this.diagnosticUnderground.incrementAndGet();
		}
		long total = this.diagnosticTotal.incrementAndGet();
		if(total == 10000L || total == 100000L || total == 1000000L || total % 5000000L == 0L) {
			this.logDiagnostic(total);
		}
	}

	private void recordDirectDiagnosticSelection(int selected, boolean underground, boolean overridden) {
		AtomicLongArray directCounts = this.directIndexCounts;
		if(directCounts == null) {
			return;
		}
		if(this.isValidIndex(selected)) {
			directCounts.incrementAndGet(selected);
		}
		this.diagnosticDirectPath.incrementAndGet();
		if(overridden) {
			this.diagnosticDirectOverrides.incrementAndGet();
		}
		if(underground) {
			this.diagnosticUnderground.incrementAndGet();
		}
		long total = this.diagnosticTotal.incrementAndGet();
		if(total == 10000L || total == 100000L || total == 1000000L || total % 5000000L == 0L) {
			this.logDiagnostic(total);
		}
	}

	private void logDiagnostic(long total) {
		RTFCommon.LOGGER.info(
			"TerraBlender diagnostic: total={}, originalPath={}, rtfPath={}, directPath={}, directOverrides={}, underground={}, darkerdepthsOriginal={}, darkerdepthsRtf={}, darkerdepthsDirect={}, deeperdarkerOriginal={}, deeperdarkerRtf={}, deeperdarkerDirect={}, originalTop={}, rtfTop={}, directTop={}",
			total,
			this.diagnosticOriginalPath.get(),
			this.diagnosticRtfPath.get(),
			this.diagnosticDirectPath.get(),
			this.diagnosticDirectOverrides.get(),
			this.diagnosticUnderground.get(),
			this.countAt(this.originalIndexCounts, this.darkerDepthsIndex),
			this.countAt(this.rtfIndexCounts, this.darkerDepthsIndex),
			this.countAt(this.directIndexCounts, this.darkerDepthsIndex),
			this.countAt(this.originalIndexCounts, this.deeperDarkerIndex),
			this.countAt(this.rtfIndexCounts, this.deeperDarkerIndex),
			this.countAt(this.directIndexCounts, this.deeperDarkerIndex),
			this.formatTop(this.originalIndexCounts),
			this.formatTop(this.rtfIndexCounts),
			this.formatTop(this.directIndexCounts)
		);
	}

	private long countAt(AtomicLongArray counts, int index) {
		if(counts == null || index < 0 || index >= counts.length()) {
			return 0L;
		}
		return counts.get(index);
	}

	private String formatTop(AtomicLongArray counts) {
		if(counts == null) {
			return "[]";
		}
		int firstIndex = -1;
		int secondIndex = -1;
		int thirdIndex = -1;
		long firstCount = -1L;
		long secondCount = -1L;
		long thirdCount = -1L;
		for(int index = 0; index < counts.length(); index++) {
			long count = counts.get(index);
			if(count > firstCount) {
				thirdIndex = secondIndex;
				thirdCount = secondCount;
				secondIndex = firstIndex;
				secondCount = firstCount;
				firstIndex = index;
				firstCount = count;
			} else if(count > secondCount) {
				thirdIndex = secondIndex;
				thirdCount = secondCount;
				secondIndex = index;
				secondCount = count;
			} else if(count > thirdCount) {
				thirdIndex = index;
				thirdCount = count;
			}
		}
		return "[" + firstIndex + "=" + firstCount + ", " + secondIndex + "=" + secondCount + ", " + thirdIndex + "=" + thirdCount + "]";
	}
}
