package raccoonman.reterraforged.client;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
			run(mc);
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
				if (mc.screen instanceof TitleScreen title) {
					mc.options.renderDistance().set(16);
					CreateWorldScreen.openFresh(mc, title);
					next(60);
				}
			}
			case 1 -> {
				if (mc.screen instanceof CreateWorldScreen create) {
					mc.setScreen(new PresetConfigScreen(create));
					next(40);
				}
			}
			case 2 -> {
				// select the first preset ("Default") and go to its settings page with the live preview
				if (mc.screen instanceof PresetConfigScreen screen) {
					if (selectFirstPreset(screen)) screen.nextButton.onPress();
					else RTFCommon.LOGGER.warn("[RTF clienttest] no preset in the list");
				}
				next(200);
			}
			case 3 -> {
				shoot(mc, "preset-screen");
				mc.setScreen(new TitleScreen());
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
					confirm(mc.screen);
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
				mc.options.hideGui = true;
				mc.options.cloudStatus().set(CloudStatus.OFF);
				next(20);
			}
			default -> {
				int i = step - 6;
				if (i < spots.length * 2) {
					float[] spot = spots[i / 2];
					if (i % 2 == 0) {
						command(mc, String.format(java.util.Locale.ROOT, "tp @s %.1f %.1f %.1f %.1f %.1f", spot[0], spot[1], spot[2], spot[3], spot[4]));
						next(500); // chunks around the new place are generated and rendered
					} else {
						shoot(mc, NAMES[i / 2]);
						next(20);
					}
				} else {
					RTFCommon.LOGGER.info("[RTF clienttest] done, {} screenshots", shot);
					mc.stop();
				}
			}
		}
	}

	private static final String[] NAMES = { "mountains", "desert", "river", "overview" };

	private static void next(int ticks) {
		step++;
		wait = ticks;
	}

	private static void shoot(Minecraft mc, String name) {
		Screenshot.grab(mc.gameDirectory, "rtf-" + name + ".png", mc.getMainRenderTarget(), (message) -> {
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
				preset.onPress();
				return true;
			}
		}
		return false;
	}

	private static void confirm(Screen screen) {
		if (screen == null) return;
		for (var child : screen.children()) {
			if (child instanceof Button button && button.getMessage().getContents() instanceof TranslatableContents contents && CONFIRM_KEYS.contains(contents.getKey())) {
				RTFCommon.LOGGER.info("[RTF clienttest] pressing {} on {}", contents.getKey(), screen.getClass().getSimpleName());
				button.onPress();
				return;
			}
		}
	}

	private static void command(Minecraft mc, String command) {
		MinecraftServer server = mc.getSingleplayerServer();
		server.execute(() -> {
			ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
			server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), command);
		});
	}

	// camera spots: x, y, z, yaw, pitch
	private static float[][] findSpots(MinecraftServer server) {
		ServerLevel level = server.overworld();
		var gen = level.getChunkSource().getGenerator();
		var rs = level.getChunkSource().randomState();
		BlockPos spawn = level.getSharedSpawnPos();

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
		return new float[][] { mountains, desert != null ? desert : overview, river != null ? river : overview, overview };
	}

	private static float[] biomeSpot(ServerLevel level, BlockPos origin, ResourceKey<Biome> biome) {
		Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d((b) -> b.is(biome), origin.atY(level.getSeaLevel() + 8), 2400, 32, 4096);
		if (found == null) {
			RTFCommon.LOGGER.warn("[RTF clienttest] no {} found", biome.location());
			return null;
		}
		BlockPos pos = found.getFirst();
		var gen = level.getChunkSource().getGenerator();
		int y = gen.getBaseHeight(pos.getX(), pos.getZ(), Heightmap.Types.WORLD_SURFACE_WG, level, level.getChunkSource().randomState());
		RTFCommon.LOGGER.info("[RTF clienttest] {} at {} {} {}", biome.location(), pos.getX(), y, pos.getZ());
		return SHOWCASE ? lookAt(pos.getX(), y, pos.getZ(), 120, 18) : lookAt(pos.getX(), y, pos.getZ(), 70, 35);
	}

	// a camera south of the target, raised so it looks down at it at the given angle
	private static float[] lookAt(int x, int y, int z, int distance, float pitch) {
		float height = distance * Mth.sin(pitch * Mth.DEG_TO_RAD) / Mth.cos(pitch * Mth.DEG_TO_RAD);
		return new float[] { x + 0.5F, Math.min(y + height, 310), z + distance, 180, pitch };
	}
}
