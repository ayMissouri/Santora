package dev.santora.core.party;

public record FollowerSnapshot(String soundPath, boolean playing, boolean paused, long elapsedMillis) {

	public FollowerSnapshot {
		soundPath = soundPath == null ? "" : soundPath;
	}

	public static FollowerSnapshot idle() {
		return new FollowerSnapshot("", false, false, 0);
	}
}
