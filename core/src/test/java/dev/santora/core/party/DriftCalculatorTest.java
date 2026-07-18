package dev.santora.core.party;

import dev.santora.core.play.RepeatMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DriftCalculatorTest {

	private static final String TRACK = "minecraft:music.game";
	private static final long NO_DURATION = 0;
	private static final long FRESH = 0;
	private static final long RESEEK_READY = DriftCalculator.MIN_RESEEK_INTERVAL_MS;

	private static PartyMessage.NowPlaying host(String path, long pos, boolean paused) {
		return new PartyMessage.NowPlaying(path, pos, paused, false, RepeatMode.OFF, 1);
	}

	private static FollowerSnapshot playing(String path, long elapsed) {
		return new FollowerSnapshot(path, true, false, elapsed);
	}

	@Test
	void projectAdvancesWhilePlayingAndFreezesWhenPaused() {
		assertEquals(6_000, DriftCalculator.project(5_000, false, 1_000));
		assertEquals(5_000, DriftCalculator.project(5_000, true, 1_000));
		assertEquals(0, DriftCalculator.project(-100, false, 0), "position never goes negative");
	}

	@Test
	void hostPlayingNothingStopsOrStaysQuiet() {
		PartyMessage.NowPlaying silent = host("", 0, false);
		assertEquals(SyncAction.STOP,
				DriftCalculator.decide(silent, true, NO_DURATION, FRESH, playing(TRACK, 1_000), RESEEK_READY).action());
		assertEquals(SyncAction.NONE,
				DriftCalculator.decide(silent, true, NO_DURATION, FRESH, FollowerSnapshot.idle(), RESEEK_READY).action());
	}

	@Test
	void unavailableTrackReportsMissing() {
		SyncDecision decision = DriftCalculator.decide(
				host(TRACK, 2_000, false), false, NO_DURATION, FRESH, FollowerSnapshot.idle(), RESEEK_READY);
		assertEquals(SyncAction.MISSING, decision.action());
		assertEquals(TRACK, decision.soundPath());
	}

	@Test
	void differentTrackStartsAtProjectedPosition() {
		SyncDecision decision = DriftCalculator.decide(
				host(TRACK, 2_000, false), true, NO_DURATION, 500, playing("minecraft:music.creative", 40_000), RESEEK_READY);
		assertEquals(SyncAction.START, decision.action());
		assertEquals(TRACK, decision.soundPath());
		assertEquals(2_500, decision.targetMillis(), "should start where the host is now, not where it was");
	}

	@Test
	void lostSameTrackRestartsUnlessItAlreadyEnded() {
		FollowerSnapshot stopped = new FollowerSnapshot(TRACK, false, false, 0);
		assertEquals(SyncAction.START,
				DriftCalculator.decide(host(TRACK, 2_000, false), true, 180_000, FRESH, stopped, RESEEK_READY).action());
		assertEquals(SyncAction.NONE,
				DriftCalculator.decide(host(TRACK, 179_000, false), true, 2_000, FRESH, stopped, RESEEK_READY).action());
	}

	@Test
	void pauseAndResumeFollowTheHost() {
		assertEquals(SyncAction.PAUSE,
				DriftCalculator.decide(host(TRACK, 3_000, true), true, NO_DURATION, FRESH, playing(TRACK, 3_000), RESEEK_READY).action());
		FollowerSnapshot pausedLocal = new FollowerSnapshot(TRACK, true, true, 3_000);
		assertEquals(SyncAction.RESUME,
				DriftCalculator.decide(host(TRACK, 3_000, false), true, NO_DURATION, FRESH, pausedLocal, RESEEK_READY).action());
	}

	@Test
	void smallDriftIsLeftAlone() {
		SyncDecision decision = DriftCalculator.decide(
				host(TRACK, 10_000, false), true, NO_DURATION, FRESH, playing(TRACK, 10_400), RESEEK_READY);
		assertEquals(SyncAction.NONE, decision.action(), "400ms is under the threshold");
	}

	@Test
	void largeDriftReseeksOnlyAfterTheCooldown() {
		PartyMessage.NowPlaying h = host(TRACK, 10_000, false);
		FollowerSnapshot drifted = playing(TRACK, 12_000); // 2s behind projection
		assertEquals(SyncAction.NONE,
				DriftCalculator.decide(h, true, NO_DURATION, FRESH, drifted, 500).action(),
				"too soon after the last correction");
		SyncDecision ready = DriftCalculator.decide(h, true, NO_DURATION, FRESH, drifted, RESEEK_READY);
		assertEquals(SyncAction.RESEEK, ready.action());
		assertEquals(10_000, ready.targetMillis());
	}

	@Test
	void noReseekPastTheEndOfTheTrack() {
		SyncDecision decision = DriftCalculator.decide(
				host(TRACK, 179_500, false), true, 180_000, 2_000, playing(TRACK, 170_000), RESEEK_READY);
		assertEquals(SyncAction.NONE, decision.action(), "the track is about to end; don't restart it");
	}
}
