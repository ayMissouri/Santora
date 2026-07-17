package dev.santora.ui;

import dev.santora.core.config.SantoraConfig;
import dev.santora.core.model.Track;
import dev.santora.engine.MusicEngine;

import java.util.OptionalDouble;

public final class NowPlayingOverlay {

	public static final int WIDTH = 150;
	public static final int HEIGHT = 40;
	private static final int MARGIN = 4;
	private static final int ART_SIZE = 24;

	private NowPlayingOverlay() {
	}

	public static int x(SantoraConfig config, int screenWidth) {
		return MARGIN + Math.round(config.overlayX() * usable(screenWidth, WIDTH));
	}

	public static int y(SantoraConfig config, int screenHeight) {
		return MARGIN + Math.round(config.overlayY() * usable(screenHeight, HEIGHT));
	}

	public static void position(SantoraConfig config, int x, int y, int screenWidth, int screenHeight) {
		config.setOverlayPos(
				fraction(x, usable(screenWidth, WIDTH)),
				fraction(y, usable(screenHeight, HEIGHT)));
	}

	private static int usable(int screen, int size) {
		return Math.max(0, screen - size - MARGIN * 2);
	}

	private static float fraction(int pos, int usable) {
		return usable == 0 ? 0f : (pos - MARGIN) / (float) usable;
	}

	public static void renderHud(SantoraCanvas canvas) {
		MusicEngine engine = MusicEngine.get();
		SantoraConfig config = engine.config();
		if (!config.overlayOn() || !engine.isManualMode()) {
			return;
		}
		if (engine.currentTrack() == null && engine.delayRemainingMillis() < 0) {
			return;
		}
		renderCard(canvas, x(config, canvas.width()), y(config, canvas.height()), false);
	}
	
	public static void renderCard(SantoraCanvas canvas, int x, int y, boolean opaque) {
		MusicEngine engine = MusicEngine.get();
		SantoraConfig config = engine.config();
		Track track = engine.currentTrack();

		int cardBase = 0xFF000000 | config.hudBackground();
		int alpha = opaque ? 255 : 255 * config.hudOpacity() / 100;

		canvas.fill(x, y, x + WIDTH, y + HEIGHT, Theme.argb(alpha, cardBase));
		canvas.outline(x, y, WIDTH, HEIGHT,
				Theme.argb(alpha, Theme.blend(cardBase, 0xFFFFFFFF, 0.17f)));

		int artX = x + 6;
		int artY = y + 6;
		if (track != null) {
			AlbumArt.drawTrack(canvas, track, artX, artY, ART_SIZE);
			drawStateIcon(canvas, engine, artX, artY);
		} else {
			canvas.fill(artX, artY, artX + ART_SIZE, artY + ART_SIZE,
					Theme.argb(alpha, Theme.blend(cardBase, 0xFFFFFFFF, 0.03f)));
		}

		int textX = artX + ART_SIZE + 6;
		int textMax = x + WIDTH - 6 - textX;
		long waiting = engine.delayRemainingMillis();
		String title = track != null ? track.title()
				: waiting >= 0 ? "Next track in " + (waiting + 999) / 1000 + "s"
				: "Now Playing";
		String sub = track != null ? track.artist()
				: waiting >= 0 ? "" : "Santora";
		canvas.text(canvas.ellipsize(title, textMax), textX, y + 9, Theme.TEXT_PRIMARY, false);
		canvas.text(canvas.ellipsize(sub, textMax), textX, y + 20, Theme.TEXT_SECONDARY, false);

		renderProgress(canvas, engine, track, x, y, cardBase, alpha);
	}

	private static void drawStateIcon(SantoraCanvas canvas, MusicEngine engine, int artX, int artY) {
		if (engine.isPaused()) {
			int cx = artX + ART_SIZE / 2;
			int cy = artY + ART_SIZE / 2;
			iconBar(canvas, cx - 4, cy - 4, cx - 1, cy + 5);
			iconBar(canvas, cx + 1, cy - 4, cx + 4, cy + 5);
			return;
		}
		long t = engine.elapsedMillis() / 120;
		int x = artX + (ART_SIZE - 8) / 2;
		int bottom = artY + ART_SIZE / 2 + 4;
		for (int i = 0; i < 3; i++) {
			int phase = (int) ((t + i * 2) % 6);
			int h = 2 + (phase < 3 ? phase : 6 - phase);
			iconBar(canvas, x + i * 3, bottom - h, x + i * 3 + 2, bottom);
		}
	}

	private static void iconBar(SantoraCanvas canvas, int x1, int y1, int x2, int y2) {
		canvas.fill(x1 + 1, y1 + 1, x2 + 1, y2 + 1, 0x80000000);
		canvas.fill(x1, y1, x2, y2, 0xF2FFFFFF);
	}

	private static void renderProgress(SantoraCanvas canvas, MusicEngine engine, Track track,
			int x, int y, int cardBase, int alpha) {
		int stripY = y + HEIGHT - 4;
		canvas.fill(x + 1, stripY, x + WIDTH - 1, stripY + 3,
				Theme.argb(alpha, Theme.blend(cardBase, 0xFFFFFFFF, 0.11f)));

		if (track == null) {
			return;
		}
		OptionalDouble duration = engine.durationSeconds(track);
		if (duration.isPresent() && duration.getAsDouble() > 0) {
			float pct = (float) (engine.elapsedMillis() / (duration.getAsDouble() * 1000.0));
			pct = pct < 0 ? 0 : (pct > 1 ? 1 : pct);
			canvas.fill(x + 1, stripY, x + 1 + (int) ((WIDTH - 2) * pct), stripY + 3,
					0xFF000000 | engine.config().hudAccent());
		}
	}
}
