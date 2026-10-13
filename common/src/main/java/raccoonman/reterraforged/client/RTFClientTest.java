package raccoonman.reterraforged.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetConfigScreen;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetListPage.PresetEntry;
import raccoonman.reterraforged.client.gui.widget.WidgetList;

/**
 * Client check for tools/clienttest.sh, only with -Drtf.clienttest=&lt;world folder name&gt;: opens the preset screen
 * (with the live preview) and takes a screenshot, then loads that world (a copy of the server test world), flies to
 * the highest mountain, a desert and a river nearby, takes a screenshot at each and quits. Called every client tick.
 */
public final class RTFClientTest {
	private static final String WORLD = System.getProperty("rtf.clienttest");
	// tools/showcase.sh (pictures for the README, with shaders): evening sun and lower cameras
	private static final boolean SHOWCASE = Boolean.getBoolean("rtf.showcase");
	// a button press as if made with the keyboard (Enter)
	private static final KeyEvent PRESS = new KeyEvent(InputConstants.KEY_RETURN, 0, 0);
	// tools/clienttest.sh with BIOMES="biomesoplenty:lavender_field,...": one more screenshot in each of these biomes
	private static final String BIOMES = System.getProperty("rtf.clientbiomes", "");
	// SPOTS="3260,1160;3608,922": a screenshot looking down at each of these places (pits found by "rtf holes")
	private static final String SPOTS = System.getProperty("rtf.clientspots", "");
	private static final Set<String> CONFIRM_KEYS = Set.of("gui.proceed", "gui.yes", "gui.continue", "selectWorld.backupJoinSkipButton");
	private static int step;
	private static int wait;
	private static int shot;
	private static float[][] spots;
	private static CompletableFuture<float[][]> search;

	private RTFClientTest() {
	}

	public static void tick(Minecraft mc) {
		if (WORLD == null || WORLD.isEmpty()) return;
		try {
			// tools/clienttest.sh <loader> gui|widgets: a world made through the screens with mouse clicks (RTFGuiTest),
			// every widget of the settings pages used (RTFWidgetTest)
			if (WORLD.equals("gui")) RTFGuiTest.tick(mc);
			else if (WORLD.equals("widgets")) RTFWidgetTest.tick(mc);
			else run(mc);
		} catch (Throwable t) {
			RTFCommon.LOGGER.error("[RTF clienttest] failed", t);
			mc.stop();
		}
	}

	private static void run(Minecraft mc) {
		if (wait > 0) {
			wait--;
			return;
		}
		switch (step) {
			case 0 -> {
				if (mc.gui.screen() instanceof TitleScreen title) {
					mc.options.renderDistance().set(16);
					CreateWorldScreen.openFresh(mc, () -> mc.gui.setScreen(title));
					next(60);
				}
			}
			case 1 -> {
				if (mc.gui.screen() instanceof CreateWorldScreen create) {
					mc.gui.setScreen(new PresetConfigScreen(create));
					next(40);
				}
			}
			case 2 -> {
				// select the first preset ("Default") and go to its settings page with the live preview
				if (mc.gui.screen() instanceof PresetConfigScreen screen) {
					if (selectFirstPreset(screen)) screen.nextButton.onPress(PRESS);
					else RTFCommon.LOGGER.warn("[RTF clienttest] no preset in the list");
				}
				next(200);
			}
			case 3 -> {
				shoot(mc, "preset-screen");
				mc.gui.setScreen(new TitleScreen());
				next(20);
			}
			case 4 -> {
				mc.createWorldOpenFlows().openWorld(WORLD, () -> {
					RTFCommon.LOGGER.error("[RTF clienttest] could not open world {}", WORLD);
					mc.stop();
				});
				next(20);
			}
			case 5 -> {
				// "experimental settings" / backup questions when loading the world: answer yes
				if (mc.player == null) {
					confirm(mc.gui.screen());
					wait = 10;
					return;
				}
				if (search == null) {
					// searched on the server thread, the client keeps rendering meanwhile
					MinecraftServer server = mc.getSingleplayerServer();
					search = server.submit(() -> findSpots(server));
				}
				if (!search.isDone()) return;
				spots = search.join();
				command(mc, "gamemode spectator");
				command(mc, SHOWCASE ? "time set 10500" : "time set 6000");
				command(mc, "weather clear");
				// the time stops: doDaylightCycle up to 1.21.10, advance_time since 1.21.11 (the other one just fails)
				command(mc, "gamerule doDaylightCycle false");
				command(mc, "gamerule advance_time false");
				if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
				mc.options.cloudStatus().set(CloudStatus.OFF);
				next(20);
			}
			default -> {
				int i = step - 6;
				if (i < spots.length * 2) {
					float[] spot = spots[i / 2];
					if (i % 2 == 0) {
						command(mc, String.format(java.util.Locale.ROOT, "tp @s %.1f %.1f %.1f %.1f %.1f", spot[0], spot[1], spot[2], spot[3], spot[4]));
						next(Integer.getInteger("rtf.showcase.wait", 500)); // chunks around the new place are generated and rendered (far ones too, with Distant Horizons)
					} else {
						shoot(mc, names[i / 2]);
						next(20);
					}
				} else {
					RTFCommon.LOGGER.info("[RTF clienttest] done, {} screenshots", shot);
					mc.stop();
				}
			}
		}
	}

	private static String[] names = { "mountains", "desert", "river", "overview", "village" };

	private static void next(int ticks) {
		step++;
		wait = ticks;
	}

	private static void shoot(Minecraft mc, String name) {
		Screenshot.grab(mc.gameDirectory, "rtf-" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, (message) -> {
			RTFCommon.LOGGER.info("[RTF clienttest] {}", message.getString());
		});
		shot++;
	}

	// selects the first preset in the list like a mouse click does (the next page needs the list selection)
	private static boolean selectFirstPreset(Screen screen) {
		for (GuiEventListener child : screen.children()) {
			if (child instanceof WidgetList<?> list && selectFirstPreset(list)) return true;
		}
		return false;
	}

	private static <T extends AbstractWidget> boolean selectFirstPreset(WidgetList<T> list) {
		for (WidgetList.Entry<T> entry : list.children()) {
			if (entry.getWidget() instanceof PresetEntry preset) {
				list.setSelected(entry);
				preset.onPress(PRESS);
				return true;
			}
		}
		return false;
	}

	static void confirm(Screen screen) {
		if (screen == null) return;
		for (var child : screen.children()) {
			if (child instanceof Button button && button.getMessage().getContents() instanceof TranslatableContents contents && CONFIRM_KEYS.contains(contents.getKey())) {
				RTFCommon.LOGGER.info("[RTF clienttest] pressing {} on {}", contents.getKey(), screen.getClass().getSimpleName());
				button.onPress(PRESS);
				return;
			}
		}
	}

	private static void command(Minecraft mc, String command) {
		MinecraftServer server = mc.getSingleplayerServer();
		server.execute(() -> {
			ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
			server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.OWNER), command);
		});
	}

	// camera spots: x, y, z, yaw, pitch
	private static float[][] findSpots(MinecraftServer server) {
		ServerLevel level = server.overworld();
		var gen = level.getChunkSource().getGenerator();
		var rs = level.getChunkSource().randomState();
		BlockPos spawn = level.getRespawnData().pos();

		int bestX = spawn.getX(), bestZ = spawn.getZ(), bestY = Integer.MIN_VALUE;
		for (int x = -1536; x <= 1536; x += 64) {
			for (int z = -1536; z <= 1536; z += 64) {
				int y = gen.getBaseHeight(spawn.getX() + x, spawn.getZ() + z, Heightmap.Types.WORLD_SURFACE_WG, level, rs);
				if (y > bestY) {
					bestY = y;
					bestX = spawn.getX() + x;
					bestZ = spawn.getZ() + z;
				}
			}
		}
		RTFCommon.LOGGER.info("[RTF clienttest] highest point {} {} {}", bestX, bestY, bestZ);
		float[] mountains = SHOWCASE ? lookAt(bestX, bestY, bestZ, 300, 4) : lookAt(bestX, bestY, bestZ, 160, 25);
		float[] desert = biomeSpot(level, spawn, Biomes.DESERT);
		float[] river = biomeSpot(level, spawn, Biomes.RIVER);
		int sy = gen.getBaseHeight(spawn.getX(), spawn.getZ(), Heightmap.Types.WORLD_SURFACE_WG, level, rs);
		float[] overview = { spawn.getX(), Math.min(sy + (SHOWCASE ? 40 : 80), level.getMaxY() - 9), spawn.getZ() + 160, 180, SHOWCASE ? 12 : 30 };
		// the nearest village, from above: its houses on the terrain and its streets
		float[] village = overview;
		BlockPos villagePos = level.findNearestMapStructure(StructureTags.VILLAGE, spawn, 100, false);
		if (villagePos != null) {
			int vy = gen.getBaseHeight(villagePos.getX(), villagePos.getZ(), Heightmap.Types.WORLD_SURFACE_WG, level, rs);
			RTFCommon.LOGGER.info("[RTF clienttest] village at {} {} {}", villagePos.getX(), vy, villagePos.getZ());
			village = lookAt(villagePos.getX(), vy, villagePos.getZ(), 70, 40);
		}
		if (SHOWCASE) {
			return showcaseSpots(level, spawn, mountains, overview, village);
		}
		List<float[]> list = new ArrayList<>(List.of(mountains, desert != null ? desert : overview, river != null ? river : overview, overview, village));
		List<String> named = new ArrayList<>(List.of("mountains", "desert", "river", "overview", "village"));
		for (String id : BIOMES.split(",")) {
			if (id.isBlank()) continue;
			float[] spot = biomeSpot(level, spawn, ResourceKey.create(Registries.BIOME, Identifier.parse(id.trim())));
			if (spot == null) continue;
			list.add(spot);
			named.add(id.trim().replace(':', '-'));
		}
		for (String spot : SPOTS.split(";")) {
			String[] xz = spot.trim().split(",");
			if (xz.length != 2) continue;
			int x = Integer.parseInt(xz[0]), z = Integer.parseInt(xz[1]);
			int y = gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, rs);
			list.add(lookAt(x, y, z, 40, 50));
			named.add("spot_" + x + "_" + z);
		}
		names = named.toArray(String[]::new);
		return list.toArray(float[][]::new);
	}

	// the README pictures: the landscapes of the world, each where it is nearest to the spawn
	private static float[][] showcaseSpots(ServerLevel level, BlockPos spawn, float[] mountains, float[] overview, float[] village) {
		java.util.List<String> shotNames = new java.util.ArrayList<>();
		java.util.List<float[]> shots = new java.util.ArrayList<>();
		shotNames.add("mountains");
		shots.add(mountains);
		Object[][] biomes = {
			{ "valley", Biomes.MEADOW }, { "cherry-grove", Biomes.CHERRY_GROVE }, { "flower-forest", Biomes.FLOWER_FOREST },
			{ "jungle", Biomes.JUNGLE }, { "badlands", Biomes.WOODED_BADLANDS }, { "snowy-taiga", Biomes.SNOWY_TAIGA },
			{ "swamp", Biomes.SWAMP }, { "desert", Biomes.DESERT }, { "coast", Biomes.BEACH }, { "river", Biomes.RIVER }
		};
		for (Object[] biome : biomes) {
			@SuppressWarnings("unchecked")
			float[] spot = biomeSpot(level, spawn, (ResourceKey<Biome>) biome[1]);
			if (spot != null) {
				shotNames.add((String) biome[0]);
				shots.add(spot);
			}
		}
		float[] lake = lakeSpot(level, spawn);
		if (lake != null) {
			shotNames.add("lake");
			shots.add(lake);
		}
		shotNames.add("overview");
		shots.add(overview);
		shotNames.add("village");
		shots.add(village);
		names = shotNames.toArray(String[]::new);
		return shots.toArray(float[][]::new);
	}

	// the nearest lake of the mod's rivermaps (lakes have no biome of their own)
	private static float[] lakeSpot(ServerLevel level, BlockPos spawn) {
		if (!((Object) level.getChunkSource().randomState() instanceof raccoonman.reterraforged.world.worldgen.RTFRandomState state) || state.generatorContext() == null) {
			return null;
		}
		var lookup = state.generatorContext().lookup;
		raccoonman.reterraforged.world.worldgen.cell.Cell cell = new raccoonman.reterraforged.world.worldgen.cell.Cell();
		for (int r = 0; r <= 3000; r += 48) {
			for (int dx = -r; dx <= r; dx += 48) {
				for (int dz = -r; dz <= r; dz += 48) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
					int x = spawn.getX() + dx, z = spawn.getZ() + dz;
					lookup.applyCell(cell.reset(), x, z, false);
					if (cell.terrain == raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType.LAKE) {
						int y = level.getSeaLevel();
						RTFCommon.LOGGER.info("[RTF clienttest] lake at {} {} {}", x, y, z);
						return lookAt(x, y, z, 110, 22);
					}
				}
			}
		}
		RTFCommon.LOGGER.warn("[RTF clienttest] no lake found");
		return null;
	}

	private static float[] biomeSpot(ServerLevel level, BlockPos origin, ResourceKey<Biome> biome) {
		Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d((b) -> b.is(biome), origin.atY(level.getSeaLevel() + 8), 6400, 32, 4096);
		if (found == null) {
			RTFCommon.LOGGER.warn("[RTF clienttest] no {} found", biome.identifier());
			return null;
		}
		BlockPos pos = found.getFirst();
		var gen = level.getChunkSource().getGenerator();
		int y = gen.getBaseHeight(pos.getX(), pos.getZ(), Heightmap.Types.WORLD_SURFACE_WG, level, level.getChunkSource().randomState());
		RTFCommon.LOGGER.info("[RTF clienttest] {} at {} {} {}", biome.identifier(), pos.getX(), y, pos.getZ());
		return SHOWCASE ? lookAt(pos.getX(), y, pos.getZ(), 120, 18) : lookAt(pos.getX(), y, pos.getZ(), 70, 35);
	}

	// a camera south of the target, raised so it looks down at it at the given angle
	private static float[] lookAt(int x, int y, int z, int distance, float pitch) {
		float height = distance * Mth.sin(pitch * Mth.DEG_TO_RAD) / Mth.cos(pitch * Mth.DEG_TO_RAD);
		return new float[] { x + 0.5F, Math.min(y + height, 310), z + distance, 180, pitch };
	}
}
