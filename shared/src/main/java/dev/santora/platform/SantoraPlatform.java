package dev.santora.platform;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.server.packs.resources.Resource;

import java.util.List;
import java.util.Optional;

public interface SantoraPlatform {

	Screen createPlayerScreen(Screen parent);

	void openScreen(Screen screen);

	Screen currentScreen();

	List<AbstractWidget> widgetsOf(Screen screen);

	KeyMapping registerOpenKey();

	void installOpenKeyListener(Screen screen);

	void setClipboard(String text);

	String getClipboard();

	long TOAST_MILLIS = 10_000L;

	void showToast(String title, String message);

	boolean playMusic(SoundInstance instance);

	void refreshMusicGains();

	void resetMusicCategoryGain();

	Optional<Resource> findResource(String id);

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
