package dev.santora.ui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.StringUtil;

public abstract class SantoraInputScreen extends SantoraScreenBase {

	protected SantoraInputScreen(Screen parent) {
		super(parent);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (ui.mouseClicked((int) mouseX, (int) mouseY, button)) {
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (ui.mouseDragged((int) mouseX, (int) mouseY, button)) {
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (ui.mouseReleased((int) mouseX, (int) mouseY, button)) {
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (ui.mouseScrolled((int) mouseX, (int) mouseY, scrollY)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (ui.keyPressed(keyCode, modifiers)) {
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (ui.charTyped(String.valueOf(chr), StringUtil.isAllowedChatCharacter(chr))) {
			return true;
		}
		return super.charTyped(chr, modifiers);
	}
}
