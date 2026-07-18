package dev.santora.core.party;

public record SyncDecision(SyncAction action, String soundPath, long targetMillis) {

	private static final SyncDecision NONE = new SyncDecision(SyncAction.NONE, "", 0);
	private static final SyncDecision STOP = new SyncDecision(SyncAction.STOP, "", 0);
	private static final SyncDecision PAUSE = new SyncDecision(SyncAction.PAUSE, "", 0);
	private static final SyncDecision RESUME = new SyncDecision(SyncAction.RESUME, "", 0);

	public SyncDecision {
		soundPath = soundPath == null ? "" : soundPath;
		targetMillis = Math.max(0, targetMillis);
	}

	public static SyncDecision none() {
		return NONE;
	}

	public static SyncDecision stop() {
		return STOP;
	}

	public static SyncDecision pause() {
		return PAUSE;
	}

	public static SyncDecision resume() {
		return RESUME;
	}

	public static SyncDecision start(String soundPath, long targetMillis) {
		return new SyncDecision(SyncAction.START, soundPath, targetMillis);
	}

	public static SyncDecision reseek(String soundPath, long targetMillis) {
		return new SyncDecision(SyncAction.RESEEK, soundPath, targetMillis);
	}

	public static SyncDecision missing(String soundPath) {
		return new SyncDecision(SyncAction.MISSING, soundPath, 0);
	}
}
