package dev.santora.platform;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;

public interface SantoraPlatform {

	Screen createPlayerScreen();

	void openScreen(Screen screen);

	Screen currentScreen();

	KeyMapping registerKeyMapping(KeyMapping mapping);

	void setClipboard(String text);

	String getClipboard();

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
