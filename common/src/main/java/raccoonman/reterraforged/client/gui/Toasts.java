package raccoonman.reterraforged.client.gui;

import raccoonman.reterraforged.RTFCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId;
import net.minecraft.network.chat.Component;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;

public final class Toasts {

	public static void notify(String message, Component description, SystemToastId id) {
		Minecraft mc = Minecraft.getInstance();
		SystemToast.add(mc.getToasts(), id, Component.translatable(message), description);
	}
	
	public static void tryOrToast(String errorMessage, ThrowingRunnable r) {
		try {
			r.run();
		} catch(Exception e) {
			// errorMessage is the toast's translation key
			RTFCommon.LOGGER.error("{}", Component.translatable(errorMessage).getString(), e);
			String message = e.getMessage();
			Component messageComponent;
			if(message != null) {
				messageComponent = Component.literal(message);
			} else {
				messageComponent = Component.translatable(RTFTranslationKeys.NO_ERROR_MESSAGE);
			}
			
			notify(errorMessage, messageComponent, SystemToastId.PACK_LOAD_FAILURE);
		}
	}
	
	public interface ThrowingRunnable {
		void run() throws Exception;
	}
}
