package dev.santora.ui;

import dev.santora.core.config.SantoraConfig;
import dev.santora.core.model.MusicContext;

public final class Theme {

	private Theme() {
	}

	public static int SCRIM = 0xC8050810;
	public static int WINDOW = 0xFF141926;
	public static int RAIL = 0xFF0F131E;
	public static int TOP_BAR = 0xFF0F131E;
	public static int DECK = 0xFF1A2133;
	public static int DIVIDER = 0xFF2A3247;
	public static int FRAME = 0xFF39425C;
	public static int INPUT_BG = 0xFF0C0F17;
	public static int EMPTY_ART = 0xFF1B2130;

	public static final int ROW_HOVER = 0x14FFFFFF;
	public static final int ROW_SELECTED = 0x26FFFFFF;
	public static int MENU_SELECTED = 0x26E3A44C;

	public static final int TEXT_PRIMARY = 0xFFF4F0E6;
	public static final int TEXT_SECONDARY = 0xFFA8B0C0;
	public static final int TEXT_MUTED = 0xFF667080;

	public static int ACCENT = 0xFFE3A44C;
	public static int ACCENT_DIM = 0xFF9C6F26;
	public static int ON_ACCENT = 0xFF221708;
	public static int PROGRESS_TRACK = 0xFF2C3448;

	public static void refresh(SantoraConfig config) {
		int base = 0xFF000000 | config.menuBackground();
		int accent = 0xFF000000 | config.menuAccent();
		int alpha = 255 * config.menuOpacity() / 100;

		SCRIM = argb(0xC8 * config.menuOpacity() / 100, blend(base, 0xFF000000, 0.72f));
		WINDOW = argb(alpha, base);
		RAIL = argb(alpha, blend(base, 0xFF000000, 0.25f));
		TOP_BAR = RAIL;
		DECK = argb(alpha, blend(base, 0xFFFFFFFF, 0.04f));
		DIVIDER = blend(base, 0xFFFFFFFF, 0.10f);
		FRAME = blend(base, 0xFFFFFFFF, 0.17f);
		INPUT_BG = blend(base, 0xFF000000, 0.40f);
		EMPTY_ART = blend(base, 0xFFFFFFFF, 0.03f);
		PROGRESS_TRACK = blend(base, 0xFFFFFFFF, 0.11f);

		ACCENT = accent;
		ACCENT_DIM = blend(accent, 0xFF000000, 0.35f);
		ON_ACCENT = blend(accent, 0xFF000000, 0.85f);
		MENU_SELECTED = argb(0x26, accent);
	}

	public static int argb(int alpha, int rgb) {
		int a = alpha < 0 ? 0 : (alpha > 255 ? 255 : alpha);
		return (a << 24) | (rgb & 0xFFFFFF);
	}

	public static final int RAIL_WIDTH = 78;
	public static final int TOP_BAR_HEIGHT = 24;
	public static final int DECK_HEIGHT = 44;
	public static final int PROGRESS_STRIP_HEIGHT = 3;
	public static final int ROW_HEIGHT = 22;
	public static final int MENU_ROW_HEIGHT = 16;
	public static final int PADDING = 10;
	public static final int ART_SIZE = 52;

	public static final int TILE_TARGET_WIDTH = 84;
	public static final int TILE_TEXT_HEIGHT = 25;
	public static final int GRID_GAP = 8;

	public static int artColor(MusicContext context) {
		return switch (context) {
			case MENU -> 0xFF3A5A8C;
			case CREATIVE -> 0xFF8C6A3A;
			case OVERWORLD -> 0xFF3A7A4A;
			case NETHER -> 0xFF8C3A3A;
			case END -> 0xFF5A3A8C;
			case UNDERWATER -> 0xFF3A7A8C;
			case SWAMP -> 0xFF5A6A3A;
			case DISCS -> 0xFF8C3A6A;
			case OTHER -> 0xFF4A4A4A;
		};
	}

	public static int artColorFor(String key) {
		int hash = key.hashCode();
		float hue = Math.abs(hash % 360) / 360.0f;
		return 0xFF000000 | hsvToRgb(hue, 0.45f, 0.55f);
	}

	private static int hsvToRgb(float h, float s, float v) {
		int i = (int) (h * 6);
		float f = h * 6 - i;
		float p = v * (1 - s);
		float q = v * (1 - f * s);
		float t = v * (1 - (1 - f) * s);

		float r;
		float g;
		float b;
		switch (i % 6) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
	}

	public static int blend(int base, int overlay, float alpha) {
		float a = alpha < 0 ? 0 : (alpha > 1 ? 1 : alpha);
		int br = (base >> 16) & 0xFF;
		int bg = (base >> 8) & 0xFF;
		int bb = base & 0xFF;
		int or = (overlay >> 16) & 0xFF;
		int og = (overlay >> 8) & 0xFF;
		int ob = overlay & 0xFF;
		int r = (int) (br + (or - br) * a);
		int g = (int) (bg + (og - bg) * a);
		int b = (int) (bb + (ob - bb) * a);
		return (base & 0xFF000000) | (r << 16) | (g << 8) | b;
	}

	public static String formatTime(long millis) {
		long totalSeconds = Math.max(0, millis) / 1000;
		return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
	}
}
