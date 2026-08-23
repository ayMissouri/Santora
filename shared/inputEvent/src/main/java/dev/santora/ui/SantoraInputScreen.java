package dev.santora.ui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public abstract class SantoraInputScreen extends SantoraScreenBase {

	protected SantoraInputScreen(Screen parent) {
		super(parent);
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
		if (ui.keyPressed(event.key(), event.modifiers())) {
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (ui.charTyped(event.codepointAsString(), event.isAllowedChatCharacter())) {
			return true;
		}
		return super.charTyped(event);
	}
}
