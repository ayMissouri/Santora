package dev.santora.compat;

import dev.santora.ui.SantoraScreenBase;
import net.minecraft.client.gui.GuiGraphics;

public final class SantoraScreen extends SantoraScreenBase {

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
