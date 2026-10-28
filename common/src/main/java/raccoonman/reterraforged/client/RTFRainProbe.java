package raccoonman.reterraforged.client;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Rain check for the GUI test (-Drtf.clientrain=true, issue #22): after the world is made through the screens, stands on
 * the ground in the nearest plains (-Drtf.clientrainbiome=... another biome) in the rain, counts the rain sounds the
 * client plays and logs what the client and the server decide the rain by (the biome object, whether it is the one in
 * the registry, its precipitation, and with Ecliptic Seasons installed its fields and the surface biome it keeps for
 * each column). -Drtf.clientrainreset=true runs "ecliptic debug reset surface_biome_cache" before counting. Works in a
 * production client too (the released jar, -Drtf.clienttest=gui -Drtf.clientrain=true), where Ecliptic Seasons loads.
 */
final class RTFRainProbe {
	static final boolean ENABLED = Boolean.getBoolean("rtf.clientrain");
	private static final Map<String, Integer> SOUNDS = new TreeMap<>();
	private static int step;
	private static int wait;

	private RTFRainProbe() {
	}

	// true once done
	static boolean tick(Minecraft mc) {
		if (wait > 0) {
			wait--;
			return false;
		}
		switch (step) {
			case 0 -> {
				MinecraftServer server = mc.getSingleplayerServer();
				// the nearest place of the biome (-Drtf.clientrainbiome, plains by default), the spawn without one
				String biome = System.getProperty("rtf.clientrainbiome", "minecraft:plains");
				BlockPos spot = server.submit(() -> {
					ServerLevel level = server.overworld();
					var found = level.findClosestBiome3d((h) -> h.is(net.minecraft.resources.ResourceLocation.parse(biome)), level.getSharedSpawnPos(), 6400, 32, 64);
					BlockPos at = found != null ? found.getFirst() : level.getSharedSpawnPos();
					RTFGuiTest.log("rain: {} at {}", biome, found != null ? at.toShortString() : "none, the spawn");
					level.getChunk(at);
					return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, at);
				}).join();
				RTFGuiTest.command(mc, "gamemode creative");
				RTFGuiTest.command(mc, "time set 6000");
				RTFGuiTest.command(mc, String.format(Locale.ROOT, "tp @s %d %d %d 0 0", spot.getX(), spot.getY(), spot.getZ()));
				RTFGuiTest.command(mc, "weather rain 100000");
				mc.getSoundManager().addListener((sound, events, range) -> {
					String id = sound.getLocation().toString();
					if (id.contains("rain")) SOUNDS.merge(id, 1, Integer::sum);
				});
				step++;
				wait = 300;
			}
			case 1 -> {
				// -Drtf.clientrainreset=true: Ecliptic Seasons rebuilds its surface biomes of the loaded chunks first
				if (Boolean.getBoolean("rtf.clientrainreset")) {
					RTFGuiTest.command(mc, "ecliptic debug reset surface_biome_cache");
					step = 10;
					wait = 200;
					return false;
				}
				SOUNDS.clear();
				step++;
				wait = 400;
			}
			case 2 -> {
				BlockPos pos = BlockPos.containing(mc.gameRenderer.getMainCamera().getPosition());
				RTFGuiTest.log("rain: sounds in 400 ticks {}, camera {}", SOUNDS, pos.toShortString());
				RTFGuiTest.log("rain: client {}", describe(mc.level, pos));
				MinecraftServer server = mc.getSingleplayerServer();
				String serverSide = server.submit(() -> describe(server.overworld(), pos)).join();
				RTFGuiTest.log("rain: server {}", serverSide);
				RTFGuiTest.log("rain: same biome object on the client and the server: {}", mc.level.getBiome(pos).value() == server.overworld().getBiome(pos).value());
				RTFGuiTest.log("rain: client columns around (vanilla biome / ES surface biome -> ES precipitation): {}", columns(mc.level, pos));
				RTFGuiTest.log("rain: server columns around: {}", server.submit(() -> columns(server.overworld(), pos)).join());
				RTFGuiTest.shoot(mc, "rain");
				step++;
				wait = 20;
			}
			case 10 -> {
				SOUNDS.clear();
				step = 2;
				wait = 400;
			}
			default -> {
				return true;
			}
		}
		return false;
	}

	private static String describe(Level level, BlockPos pos) {
		Holder<Biome> holder = level.getBiome(pos);
		Biome biome = holder.value();
		Registry<Biome> registry = level.registryAccess().registryOrThrow(Registries.BIOME);
		Biome inRegistry = holder.unwrapKey().map(registry::get).orElse(null);
		long foreign = level.isClientSide() ? -1 : ((ServerLevel) level).getChunkSource().getGenerator().getBiomeSource().possibleBiomes().stream()
			.filter((h) -> h.unwrapKey().map(registry::get).orElse(null) != h.value()).count();
		BlockPos above = pos.above();
		Object surface = es("com.teamtea.eclipticseasons.common.core.map.MapChecker", "getSurfaceBiome", new Class<?>[] { Level.class, BlockPos.class }, level, above);
		Biome surfaceBiome = surface instanceof Holder<?> h && h.value() instanceof Biome b ? b : biome;
		Object weather = es("com.teamtea.eclipticseasons.common.core.biome.WeatherManager", "getBiomeWeather", new Class<?>[] { Level.class, Biome.class }, level, surfaceBiome);
		Object list = es("com.teamtea.eclipticseasons.common.core.biome.WeatherManager", "getBiomeList", new Class<?>[] { Level.class }, level);
		return String.format(Locale.ROOT, "biome %s, the registry's object %s (id %d), hasPrecipitation %s, precipitation at %s, raining %s, raining at %s, can see sky %s, motion blocking y %d, ES tag %s id %s; ES surface biome %s (its id %s, the registry's object %s), its weather %s, rain or snow %s, weather list size %s, small biome %s; possible biomes not from the registry %d",
			holder.getRegisteredName(), biome == inRegistry, registry.getId(biome), biome.hasPrecipitation(), biome.getPrecipitationAt(pos), level.isRaining(), level.isRainingAt(above),
			level.canSeeSky(above), level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, above).getY(), call(biome, "eclipticseasons$getBindTag"), call(biome, "eclipticseasons$getBindId"),
			surface instanceof Holder<?> h ? h.getRegisteredName() : surface, call(surfaceBiome, "eclipticseasons$getBindId"), registry.getId(surfaceBiome) >= 0, weather,
			es("com.teamtea.eclipticseasons.common.core.biome.WeatherManager", "getRainOrSnow", new Class<?>[] { Level.class, Biome.class, BlockPos.class }, level, surfaceBiome, above),
			list instanceof java.util.List<?> l ? l.size() : list, call(surfaceBiome, "eclipticseasons$isSmallBiome"), foreign);
	}

	// what the rain of the client (LevelRenderer.tickRain, 21x21 columns) is decided by, every 5 blocks, counted
	private static Map<String, Integer> columns(Level level, BlockPos center) {
		Map<String, Integer> counts = new TreeMap<>();
		for (int dx = -10; dx <= 10; dx += 5) {
			for (int dz = -10; dz <= 10; dz += 5) {
				BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, center.offset(dx, 0, dz));
				Object surface = es("com.teamtea.eclipticseasons.common.core.map.MapChecker", "getSurfaceBiome", new Class<?>[] { Level.class, BlockPos.class }, level, top);
				Biome surfaceBiome = surface instanceof Holder<?> h && h.value() instanceof Biome b ? b : level.getBiome(top).value();
				Object precipitation = es("com.teamtea.eclipticseasons.common.core.biome.WeatherManager", "getRainOrSnow", new Class<?>[] { Level.class, Biome.class, BlockPos.class }, level, surfaceBiome, top);
				String key = level.getBiome(top).getRegisteredName() + " / " + (surface instanceof Holder<?> h ? h.getRegisteredName() : surface) + " -> " + precipitation;
				counts.merge(key, 1, Integer::sum);
			}
		}
		return counts;
	}

	private static Object call(Object target, String method) {
		try {
			Method m = target.getClass().getMethod(method);
			return m.invoke(target);
		} catch (ReflectiveOperationException e) {
			return "-";
		}
	}

	// a static method of Ecliptic Seasons, "-" without it
	private static Object es(String owner, String method, Class<?>[] types, Object... args) {
		try {
			return Class.forName(owner).getMethod(method, types).invoke(null, args);
		} catch (ReflectiveOperationException | LinkageError e) {
			return "-";
		}
	}
}
