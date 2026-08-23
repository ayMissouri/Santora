package dev.santora.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

public final class GuiGraphicsCanvas extends GuiGraphicsCanvasBase {

	public GuiGraphicsCanvas(GuiGraphics gfx) {
		super(gfx);
	}

	@Override
	public void blit(String texture, int x, int y, int w, int h, float u0, float u1, float v0, float v1) {
		Identifier id = Identifier.tryParse(texture);
		if (id != null) {
			gfx.blit(id, x, y, x + w, y + h, u0, u1, v0, v1);
		}
	}
}
