package dev.santora.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class GuiGraphicsCanvas extends GuiGraphicsCanvasBase {
	private static final int UV_SCALE = 4096;

	public GuiGraphicsCanvas(GuiGraphics gfx) {
		super(gfx);
	}

	@Override
	public void blit(String texture, int x, int y, int w, int h, float u0, float u1, float v0, float v1) {
		ResourceLocation id = ResourceLocation.tryParse(texture);
		if (id == null) {
			return;
		}
		gfx.blit(id, x, y, w, h,
				u0 * UV_SCALE, v0 * UV_SCALE,
				Math.round((u1 - u0) * UV_SCALE), Math.round((v1 - v0) * UV_SCALE),
				UV_SCALE, UV_SCALE);
	}
}
