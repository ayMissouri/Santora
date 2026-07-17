package dev.santora.ui;

import net.minecraft.resources.Identifier;

public interface SantoraCanvas {

	int width();

	int height();

	void fill(int x1, int y1, int x2, int y2, int argb);

	void fillGradient(int x1, int y1, int x2, int y2, int topArgb, int bottomArgb);

	void outline(int x, int y, int w, int h, int argb);

	void text(String text, int x, int y, int argb, boolean shadow);

	default void textCentered(String text, int centerX, int y, int argb) {
		text(text, centerX - textWidth(text) / 2, y, argb, false);
	}

	int textWidth(String text);

	int lineHeight();

	String ellipsize(String text, int maxWidth);

	void pushScissor(int x, int y, int w, int h);

	void popScissor();

	void blit(Identifier texture, int x, int y, int w, int h);
}
