package dev.santora.compat;

import dev.santora.Santora;
import dev.santora.platform.SantoraPlatform;
import dev.santora.ui.NowPlayingOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class SantoraEntrypoint implements ClientModInitializer, SantoraPlatform {

	@Override
	public void onInitializeClient() {
		Santora.init(this);
		HudElementRegistry.addLast(Santora.id("now_playing"),
				(gfx, delta) -> NowPlayingOverlay.renderHud(new ExtractorCanvas(gfx)));
	}

	@Override
	public Screen createPlayerScreen() {
		return new SantoraScreen();
	}

	@Override
	public void openScreen(Screen screen) {
		Minecraft.getInstance().gui.setScreen(screen);
	}

	@Override
	public Screen currentScreen() {
		return Minecraft.getInstance().gui.screen();
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
}
