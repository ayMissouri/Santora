package dev.santora.compat;

import dev.santora.ui.SantoraInputScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public final class SantoraScreen extends SantoraInputScreen {

	public SantoraScreen(Screen parent) {
		super(parent);
	}

	@Override
	public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
		super.render(gfx, mouseX, mouseY, partialTick);
		draw(new GuiGraphicsCanvas(gfx), mouseX, mouseY);
	}

	@Override
	public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
		if (wantsVanillaBackground()) {
			super.renderBackground(gfx, mouseX, mouseY, partialTick);
		}
	}
}
