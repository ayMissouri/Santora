package dev.santora.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class GuiGraphicsCanvas extends GuiGraphicsCanvasBase {

	public GuiGraphicsCanvas(GuiGraphics gfx) {
		super(gfx);
	}

	@Override
	public void blit(String texture, int x, int y, int w, int h, float u0, float u1, float v0, float v1) {
		ResourceLocation id = ResourceLocation.tryParse(texture);
		if (id != null) {
			gfx.blit(id, x, y, x + w, y + h, u0, u1, v0, v1);
		}
	}
}
