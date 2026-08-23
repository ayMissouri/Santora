package dev.santora.ui;

import dev.santora.engine.MusicEngine;
import dev.santora.platform.SantoraPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class SantoraScreenBase extends Screen {

	protected final SantoraUi ui = new SantoraUi();

	// the screen the menu was opened from.
	private final Screen parent;

	protected SantoraScreenBase(Screen parent) {
		super(Component.translatable("screen.santora.player"));
		this.parent = parent;
	}

	@Override
	public boolean isPauseScreen() {
		// Opening the menu does not pause the game.
		return parent != null && parent.isPauseScreen();
	}

	protected boolean wantsVanillaBackground() {
		return MusicEngine.get().config().menuOpacity() >= 100 || Minecraft.getInstance().level == null;
	}

	protected void draw(SantoraCanvas canvas, int mouseX, int mouseY) {
		ui.render(canvas, mouseX, mouseY);
		if (ui.consumeCloseRequest()) {
			onClose();
		}
	}

	@Override
	public void onClose() {
		ui.onClose();
		if (parent != null) {
			SantoraPlatform.Holder.get().openScreen(parent);
		} else {
			super.onClose();
		}
	}
}
