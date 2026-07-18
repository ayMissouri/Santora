package dev.santora.core.party;

public final class DriftCalculator {

	public static final long DRIFT_THRESHOLD_MS = 800;
	public static final long MIN_RESEEK_INTERVAL_MS = 3_000;

	private DriftCalculator() {
	}

	// Position of the hosts track.
	public static long project(long hostPositionMillis, boolean hostPaused, long sinceReceiptMillis) {
		long advanced = hostPaused ? 0 : Math.max(0, sinceReceiptMillis);
		return Math.max(0, hostPositionMillis + advanced);
	}

	public static SyncDecision decide(
			PartyMessage.NowPlaying host,
			boolean hostTrackAvailable,
			long hostTrackDurationMillis,
			long sinceReceiptMillis,
			FollowerSnapshot local,
			long sinceLastReseekMillis) {

		if (host == null || host.soundPath().isEmpty()) {
			return local.playing() ? SyncDecision.stop() : SyncDecision.none();
		}
		if (!hostTrackAvailable) {
			return SyncDecision.missing(host.soundPath());
		}

		long expected = project(host.positionMillis(), host.paused(), sinceReceiptMillis);
		boolean sameTrack = host.soundPath().equals(local.soundPath());
		boolean pastEnd = hostTrackDurationMillis > 0 && expected >= hostTrackDurationMillis;

		if (!sameTrack || !local.playing()) {
			if (sameTrack && pastEnd) {
				return SyncDecision.none();
			}
			return SyncDecision.start(host.soundPath(), expected);
		}

		if (host.paused() && !local.paused()) {
			return SyncDecision.pause();
		}
		if (!host.paused() && local.paused()) {
			return SyncDecision.resume();
		}
		if (host.paused() || pastEnd) {
			return SyncDecision.none();
		}

		boolean canReseek = sinceLastReseekMillis >= MIN_RESEEK_INTERVAL_MS;
		if (canReseek && Math.abs(expected - local.elapsedMillis()) > DRIFT_THRESHOLD_MS) {
			return SyncDecision.reseek(host.soundPath(), expected);
		}
		return SyncDecision.none();
	}
}
