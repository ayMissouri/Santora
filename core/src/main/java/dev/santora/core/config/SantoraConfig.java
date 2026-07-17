package dev.santora.core.config;

import dev.santora.core.play.RepeatMode;

public final class SantoraConfig {

	public static final int CROSSFADE_MAX_MILLIS = 12_000;
	public static final int DELAY_MAX_MILLIS = 300_000;

	private boolean crossfadeOn = true;
	private int crossfadeMillis = 3_000;
	private int delayMinMillis = 0;
	private int delayMaxMillis = 0;
	private float volume = 1.0f;
	private boolean shuffle = false;
	private RepeatMode repeat = RepeatMode.OFF;
	private boolean hideVanillaToast = true;

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
}
