package dev.santora.compat;

import dev.santora.ui.SantoraCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

public final class GuiGraphicsCanvas implements SantoraCanvas {

	private final GuiGraphics gfx;
	private final Font font;

	public GuiGraphicsCanvas(GuiGraphics gfx) {
		this.gfx = gfx;
		this.font = Minecraft.getInstance().font;
	}

	@Override
	public int width() {
		return gfx.guiWidth();
	}

	@Override
	public int height() {
		return gfx.guiHeight();
	}

	@Override
	public void fill(int x1, int y1, int x2, int y2, int argb) {
		gfx.fill(x1, y1, x2, y2, argb);
	}

	@Override
	public void fillGradient(int x1, int y1, int x2, int y2, int topArgb, int bottomArgb) {
		gfx.fillGradient(x1, y1, x2, y2, topArgb, bottomArgb);
	}

	@Override
	public void outline(int x, int y, int w, int h, int argb) {
		gfx.renderOutline(x, y, w, h, argb);
	}

	@Override
	public void text(String text, int x, int y, int argb, boolean shadow) {
		gfx.drawString(font, text, x, y, argb, shadow);
	}

	@Override
	public int textWidth(String text) {
		return font.width(text);
	}

	@Override
	public int lineHeight() {
		return font.lineHeight;
	}

	@Override
	public String ellipsize(String text, int maxWidth) {
		if (font.width(text) <= maxWidth) {
			return text;
		}
		return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
	}

	@Override
	public void pushScissor(int x, int y, int w, int h) {
		gfx.enableScissor(x, y, x + w, y + h);
	}

	@Override
	public void popScissor() {
		gfx.disableScissor();
	}

	@Override
	public void blit(Identifier texture, int x, int y, int w, int h, float u0, float u1, float v0, float v1) {
		gfx.blit(texture, x, y, x + w, y + h, u0, u1, v0, v1);
	}
}
