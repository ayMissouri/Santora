package dev.santora.compat;

import com.mojang.blaze3d.platform.InputConstants;
import dev.santora.Santora;
import dev.santora.engine.VanillaMusicGain;
import dev.santora.platform.SantoraPlatform;
import dev.santora.ui.MenuAccess;
import dev.santora.ui.NowPlayingOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundSource;

import java.util.List;
import java.util.Optional;

public final class SantoraEntrypoint implements ClientModInitializer, SantoraPlatform {

	private KeyMapping openKey;
	private SystemToast.SystemToastId toastId;

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(Santora.MOD_ID, path);
	}

	@Override
	public void onInitializeClient() {
		Santora.init(this);
		HudElementRegistry.addLast(id("now_playing"),
				(gfx, delta) -> NowPlayingOverlay.renderHud(new ExtractorCanvas(gfx)));
	}

	@Override
	public Screen createPlayerScreen(Screen parent) {
		return new SantoraScreen(parent);
	}

	@Override
	public void openScreen(Screen screen) {
		Minecraft.getInstance().setScreen(screen);
	}

	@Override
	public Screen currentScreen() {
		return Minecraft.getInstance().screen;
	}

	@Override
	public List<AbstractWidget> widgetsOf(Screen screen) {
		return Screens.getWidgets(screen);
	}

	@Override
	public KeyMapping registerOpenKey() {
		openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.santora.open",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_M,
				KeyMapping.Category.register(id("keys"))));
		return openKey;
	}

	@Override
	public void installOpenKeyListener(Screen screen) {
		ScreenKeyboardEvents.allowKeyPress(screen).register((current, event) ->
				!(openKey.matches(event) && MenuAccess.openKeyPressed(current)));
	}

	@Override
	public void setClipboard(String text) {
		Minecraft.getInstance().keyboardHandler.setClipboard(text);
	}

	@Override
	public String getClipboard() {
		return Minecraft.getInstance().keyboardHandler.getClipboard();
	}

	@Override
	public void showToast(String title, String message) {
		if (toastId == null) {
			toastId = new SystemToast.SystemToastId(TOAST_MILLIS);
		}
		SystemToast.add(Minecraft.getInstance().getToastManager(), toastId,
				Component.literal(title), Component.literal(message));
	}

	@Override
	public boolean playMusic(SoundInstance instance) {
		return Minecraft.getInstance().getSoundManager().play(instance) != SoundEngine.PlayResult.NOT_STARTED;
	}

	@Override
	public void refreshMusicGains() {
		Minecraft mc = Minecraft.getInstance();
		if (mc != null) {
			mc.getSoundManager().refreshCategoryVolume(SoundSource.MUSIC);
		}
	}

	@Override
	public void resetMusicCategoryGain() {
		Minecraft mc = Minecraft.getInstance();
		if (mc != null) {
			mc.getSoundManager().updateCategoryVolume(SoundSource.MUSIC, 1.0f);
		}
		VanillaMusicGain.reset();
	}

	@Override
	public Optional<Resource> findResource(String id) {
		Identifier texture = Identifier.tryParse(id);
		if (texture == null) {
			return Optional.empty();
		}
		return Minecraft.getInstance().getResourceManager().getResource(texture);
	}
}
