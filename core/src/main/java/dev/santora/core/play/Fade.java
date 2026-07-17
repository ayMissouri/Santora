package dev.santora.core.play;

public final class Fade {

	private static final double HALF_PI = Math.PI / 2.0;

	private Fade() {
	}

	public static float outgoing(float t) {
		return (float) Math.cos(clamp(t) * HALF_PI);
	}

	public static float incoming(float t) {
		return (float) Math.sin(clamp(t) * HALF_PI);
	}

	public static float progress(long elapsedMillis, long durationMillis) {
		if (durationMillis <= 0) {
			return 1.0f;
		}
		return clamp((float) elapsedMillis / (float) durationMillis);
	}

	private static float clamp(float t) {
		return t < 0f ? 0f : (t > 1f ? 1f : t);
	}
}
