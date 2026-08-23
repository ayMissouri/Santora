package dev.santora.platform;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public interface SantoraPlatform {

	Screen createPlayerScreen(Screen parent);

	void openScreen(Screen screen);

	Screen currentScreen();
	
	List<AbstractWidget> widgetsOf(Screen screen);

	KeyMapping registerKeyMapping(KeyMapping mapping);

	void setClipboard(String text);

	String getClipboard();

	ToastManager toastManager();

	final class Holder {
		private static SantoraPlatform instance;

		private Holder() {
		}

		public static void set(SantoraPlatform platform) {
			instance = platform;
		}

		public static SantoraPlatform get() {
			if (instance == null) {
				throw new IllegalStateException(
						"Santora platform not initialised; the version entrypoint must call Santora.init() first");
			}
			return instance;
		}
	}
}
