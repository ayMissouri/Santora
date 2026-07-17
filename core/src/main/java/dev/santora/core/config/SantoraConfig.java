package dev.santora.core.config;

import dev.santora.core.play.RepeatMode;

public final class SantoraConfig {

	public static final int CROSSFADE_MAX_MILLIS = 12_000;
	public static final int DELAY_MAX_MILLIS = 300_000;

	public static final int DEFAULT_BACKGROUND = 0x141926;
	public static final int DEFAULT_ACCENT = 0xE3A44C;
	public static final int MENU_OPACITY_MIN = 30;

	private boolean crossfadeOn = true;
	private int crossfadeMillis = 3_000;
	private int delayMinMillis = 0;
	private int delayMaxMillis = 0;
	private float volume = 1.0f;
	private boolean shuffle = false;
	private RepeatMode repeat = RepeatMode.OFF;
	private boolean hideVanillaToast = true;

	private boolean overlayOn = false;
	private float overlayX = 0f;
	private float overlayY = 0f;

	private int menuBackground = DEFAULT_BACKGROUND;
	private int menuAccent = DEFAULT_ACCENT;
	private int menuOpacity = 100;
	private int hudBackground = DEFAULT_BACKGROUND;
	private int hudAccent = DEFAULT_ACCENT;
	private int hudOpacity = 90;

	private boolean resumeOnLaunch = false;
	private boolean wasManual = false;
	private String lastContextId = "";
	private String lastTrackPath = "";

	public boolean crossfadeOn() {
		return crossfadeOn;
	}

	public void setCrossfadeOn(boolean on) {
		this.crossfadeOn = on;
	}

	public int crossfadeMillis() {
		return crossfadeMillis;
	}

	public void setCrossfadeMillis(int millis) {
		this.crossfadeMillis = clamp(millis, 0, CROSSFADE_MAX_MILLIS);
	}

	public int delayMinMillis() {
		return delayMinMillis;
	}

	public void setDelayMinMillis(int millis) {
		this.delayMinMillis = clamp(millis, 0, DELAY_MAX_MILLIS);
		if (delayMaxMillis < delayMinMillis) {
			delayMaxMillis = delayMinMillis;
		}
	}

	public int delayMaxMillis() {
		return delayMaxMillis;
	}

	public void setDelayMaxMillis(int millis) {
		this.delayMaxMillis = clamp(millis, 0, DELAY_MAX_MILLIS);
		if (delayMinMillis > delayMaxMillis) {
			delayMinMillis = delayMaxMillis;
		}
	}

	public float volume() {
		return volume;
	}

	public void setVolume(float volume) {
		this.volume = volume < 0f ? 0f : (volume > 1f ? 1f : volume);
	}

	public boolean shuffle() {
		return shuffle;
	}

	public void setShuffle(boolean shuffle) {
		this.shuffle = shuffle;
	}

	public RepeatMode repeat() {
		return repeat;
	}

	public void setRepeat(RepeatMode repeat) {
		this.repeat = repeat == null ? RepeatMode.OFF : repeat;
	}

	public boolean hideVanillaToast() {
		return hideVanillaToast;
	}

	public void setHideVanillaToast(boolean hide) {
		this.hideVanillaToast = hide;
	}

	public boolean overlayOn() {
		return overlayOn;
	}

	public void setOverlayOn(boolean on) {
		this.overlayOn = on;
	}

	public float overlayX() {
		return overlayX;
	}

	public float overlayY() {
		return overlayY;
	}

	public void setOverlayPos(float x, float y) {
		this.overlayX = clamp01(x);
		this.overlayY = clamp01(y);
	}

	public int menuBackground() {
		return menuBackground;
	}

	public void setMenuBackground(int rgb) {
		this.menuBackground = rgb & 0xFFFFFF;
	}

	public int menuAccent() {
		return menuAccent;
	}

	public void setMenuAccent(int rgb) {
		this.menuAccent = rgb & 0xFFFFFF;
	}

	public int menuOpacity() {
		return menuOpacity;
	}

	public void setMenuOpacity(int percent) {
		this.menuOpacity = clamp(percent, MENU_OPACITY_MIN, 100);
	}

	public int hudBackground() {
		return hudBackground;
	}

	public void setHudBackground(int rgb) {
		this.hudBackground = rgb & 0xFFFFFF;
	}

	public int hudAccent() {
		return hudAccent;
	}

	public void setHudAccent(int rgb) {
		this.hudAccent = rgb & 0xFFFFFF;
	}

	public int hudOpacity() {
		return hudOpacity;
	}

	public void setHudOpacity(int percent) {
		this.hudOpacity = clamp(percent, 0, 100);
	}

	public static int parseColor(String value, int fallback) {
		if (value == null) {
			return fallback;
		}
		String hex = value.startsWith("#") ? value.substring(1) : value;
		if (hex.length() != 6) {
			return fallback;
		}
		try {
			return Integer.parseInt(hex, 16);
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	public static String formatColor(int rgb) {
		return String.format("#%06X", rgb & 0xFFFFFF);
	}

	public boolean resumeOnLaunch() {
		return resumeOnLaunch;
	}

	public void setResumeOnLaunch(boolean resume) {
		this.resumeOnLaunch = resume;
	}

	public boolean wasManual() {
		return wasManual;
	}

	public void setWasManual(boolean manual) {
		this.wasManual = manual;
	}

	public String lastContextId() {
		return lastContextId;
	}

	public void setLastContextId(String id) {
		this.lastContextId = id == null ? "" : id;
	}

	public String lastTrackPath() {
		return lastTrackPath;
	}

	public void setLastTrackPath(String path) {
		this.lastTrackPath = path == null ? "" : path;
	}

	public boolean crossfadeEnabled() {
		return crossfadeOn && crossfadeMillis > 0;
	}

	public int delayMillisAt(float t) {
		if (crossfadeEnabled()) {
			return 0;
		}
		float clamped = t < 0f ? 0f : (t > 1f ? 1f : t);
		return delayMinMillis + Math.round((delayMaxMillis - delayMinMillis) * clamped);
	}

	private static int clamp(int value, int min, int max) {
		return value < min ? min : (value > max ? max : value);
	}

	private static float clamp01(float value) {
		return value < 0f ? 0f : (value > 1f ? 1f : value);
	}
}
