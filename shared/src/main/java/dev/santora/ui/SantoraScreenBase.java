package dev.santora.ui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public abstract class SantoraScreenBase extends Screen {

	protected final SantoraUi ui = new SantoraUi();

	protected SantoraScreenBase() {
		super(Component.translatable("screen.santora.player"));
	}

	@Override
	public boolean isPauseScreen() {
		// Opening the menu does not pause the game.
		return false;
	}

	protected void draw(SantoraCanvas canvas, int mouseX, int mouseY) {
		ui.render(canvas, mouseX, mouseY);
		if (ui.consumeCloseRequest()) {
			onClose();
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (ui.mouseClicked((int) event.x(), (int) event.y(), event.button())) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (ui.mouseDragged((int) event.x(), (int) event.y(), event.button())) {
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (ui.mouseReleased((int) event.x(), (int) event.y(), event.button())) {
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (ui.mouseScrolled((int) mouseX, (int) mouseY, scrollY)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (ui.keyPressed(event.key())) {
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		ui.onClose();
		super.onClose();
	}
}
