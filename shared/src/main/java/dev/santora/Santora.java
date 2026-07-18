package dev.santora;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.santora.config.ConfigIo;
import dev.santora.config.PlaylistIo;
import dev.santora.engine.MusicEngine;
import dev.santora.party.PartyController;
import dev.santora.platform.SantoraPlatform;
import dev.santora.ui.Theme;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public final class Santora {

	public static final String MOD_ID = "santora";
	private static final Logger LOGGER = LogUtils.getLogger();

	private static KeyMapping openKey;
	private static boolean libraryLoaded;

	private Santora() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static void init(SantoraPlatform platform) {
		SantoraPlatform.Holder.set(platform);
		ConfigIo.load(MusicEngine.get().config());
		PlaylistIo.load(MusicEngine.get().playlists());
		Theme.refresh(MusicEngine.get().config());
		MusicEngine.get().setPartyBridge(PartyController.get());

		// this is to support both game versions.
		openKey = platform.registerKeyMapping(new KeyMapping(
				"key.santora.open",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_M,
				KeyMapping.Category.register(id("keys"))));

		ClientTickEvents.END_CLIENT_TICK.register(Santora::onClientTick);

		LOGGER.info("[Santora] initialised");
	}

	private static void onClientTick(Minecraft mc) {
		tryLoadLibrary(mc);

		while (openKey.consumeClick()) {
			SantoraPlatform platform = SantoraPlatform.Holder.get();
			if (platform.currentScreen() == null) {
				platform.openScreen(platform.createPlayerScreen());
			}
		}

		PartyController.get().drainInbound();
		MusicEngine.get().tick();
		PartyController.get().tickRole();
	}

	private static void tryLoadLibrary(Minecraft mc) {
		if (libraryLoaded) {
			return;
		}
		SoundManager soundManager = mc.getSoundManager();
		if (soundManager == null || soundManager.getAvailableSounds().isEmpty()) {
			return;
		}
		MusicEngine.get().reload(soundManager);
		libraryLoaded = true;
		restoreLastState();
	}

	private static void restoreLastState() {
		MusicEngine engine = MusicEngine.get();
		if (!engine.config().resumeOnLaunch() || !engine.config().wasManual()) {
			return;
		}
		engine.albumById(engine.config().lastContextId())
				.ifPresent(album -> engine.playAlbum(album, 0));
	}

	public static void invalidateLibrary() {
		libraryLoaded = false;
	}

	public static void saveConfig() {
		ConfigIo.save(MusicEngine.get().config());
	}

	public static void savePlaylists() {
		PlaylistIo.save(MusicEngine.get().playlists());
	}

	public static void copyToClipboard(String text) {
		SantoraPlatform.Holder.get().setClipboard(text);
	}
}
