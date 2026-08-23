package dev.santora.compat;

import dev.santora.Santora;
import dev.santora.platform.SantoraPlatform;
import dev.santora.ui.NowPlayingOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public final class SantoraEntrypoint implements ClientModInitializer, SantoraPlatform {

	@Override
	public void onInitializeClient() {
		Santora.init(this);
		HudElementRegistry.addLast(Santora.id("now_playing"),
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
	public KeyMapping registerKeyMapping(KeyMapping mapping) {
		return KeyMappingHelper.registerKeyMapping(mapping);
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
	public ToastManager toastManager() {
		return Minecraft.getInstance().getToastManager();
	}
}
