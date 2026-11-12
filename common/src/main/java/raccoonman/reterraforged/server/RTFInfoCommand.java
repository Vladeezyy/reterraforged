package raccoonman.reterraforged.server;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import raccoonman.reterraforged.RTFCommon;

/**
 * "/reterraforged info": the world's block of the log in the chat, for an operator writing a bug report. Not "rtf", the
 * self tests' command (registered only with -Drtf.selftest=true). Shows the seed: level 2 has /seed anyway.
 */
public final class RTFInfoCommand {

	private RTFInfoCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(RTFCommon.MOD_ID).requires((source) -> source.hasPermission(2)).then(Commands.literal("info").executes((ctx) -> {
			WorldInfo info = WorldInfo.current();
			if (info == null) {
				ctx.getSource().sendFailure(Component.literal(WorldInfo.LOG_PREFIX + "no details: the overworld has not loaded"));
				return 0;
			}
			// one message: the lines stay together in the chat and in the server's log
			ctx.getSource().sendSuccess(() -> Component.literal(String.join("\n", info.chatLines())), false);
			return 1;
		})));
	}
}
