package dev.santora.core.play;

import dev.santora.core.model.Track;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayQueueTest {

	private static Track track(String name) {
		return new Track("minecraft:music/game/" + name, "music.game." + name, name, "C418", List.of());
	}

	private static final List<Track> ALBUM = List.of(track("a"), track("b"), track("c"), track("d"));

	private static PlayQueue seeded() {
		return new PlayQueue(new Random(1234L));
	}

	@Test
	void playsContextInOrder() {
		PlayQueue q = seeded();
		q.setContext("album", ALBUM, -1);
		assertEquals("a", q.next().orElseThrow().title());
		assertEquals("b", q.next().orElseThrow().title());
		assertEquals("c", q.next().orElseThrow().title());
		assertEquals("d", q.next().orElseThrow().title());
		assertTrue(q.next().isEmpty(), "should stop at the end with repeat off");
	}

	@Test
	void startsAtClickedTrack() {
		PlayQueue q = seeded();
		q.setContext("album", ALBUM, 2);
		assertEquals("c", q.next().orElseThrow().title());
		assertEquals("d", q.next().orElseThrow().title());
	}

	@Test
	void userQueueJumpsTheLineAndIsConsumed() {
		PlayQueue q = seeded();
		q.setContext("album", ALBUM, -1);
		assertEquals("a", q.next().orElseThrow().title());

		q.enqueue(track("zz"));
		assertEquals("zz", q.next().orElseThrow().title(), "queued track plays before context resumes");
		assertEquals("b", q.next().orElseThrow().title(), "context resumes where it left off");
		assertTrue(q.userQueue().isEmpty(), "queued track should be consumed once played");
	}

	@Test
	void repeatOneReplaysWithoutDrainingTheUserQueue() {
		PlayQueue q = seeded();
		q.setContext("album", ALBUM, -1);
		q.next();
		q.enqueue(track("queued"));
		q.setRepeat(RepeatMode.ONE);

		assertEquals("a", q.next().orElseThrow().title());
		assertEquals("a", q.next().orElseThrow().title());
		assertEquals(1, q.userQueue().size(), "repeat-one must not silently eat the queue");
	}

	@Test
	void repeatAllWraps() {
		PlayQueue q = seeded();
		q.setContext("album", ALBUM, -1);
		q.setRepeat(RepeatMode.ALL);
		for (int i = 0; i < 4; i++) {
			q.next();
		}
		assertTrue(q.next().isPresent(), "should wrap rather than stop");
	}

	@Test
	void previousWalksActualListeningHistoryNotContextOrder() {
		PlayQueue q = seeded();
		q.setContext("album", ALBUM, -1);
		q.setShuffle(true);

		List<String> heard = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			heard.add(q.next().orElseThrow().title());
		}
		assertEquals(heard.get(1), q.previous().orElseThrow().title());
	}

	@Test
	void previousIsEmptyAtStart() {
		PlayQueue q = seeded();
		q.setContext("album", ALBUM, -1);
		assertFalse(q.hasPrevious());
		assertTrue(q.previous().isEmpty());
	}

	@Test
	void shufflePlaysEveryTrackExactlyOnce() {
		PlayQueue q = seeded();
		q.setContext("album", ALBUM, -1);
		q.setShuffle(true);

		Set<String> seen = new HashSet<>();
		for (int i = 0; i < ALBUM.size(); i++) {
			assertTrue(seen.add(q.next().orElseThrow().title()), "shuffle must not repeat a track early");
		}
		assertEquals(ALBUM.size(), seen.size(), "shuffle must not drop tracks");
		assertTrue(q.next().isEmpty());
	}

	@Test
	void togglingShuffleOffRestoresTrueAlbumOrderForTheRemainder() {
		PlayQueue q = seeded();
		q.setContext("album", ALBUM, 0);
		q.setShuffle(true);
		q.next();
		q.setShuffle(false);

		List<String> rest = new ArrayList<>();
		q.next().ifPresent(t -> rest.add(t.title()));
		q.next().ifPresent(t -> rest.add(t.title()));
		List<String> sorted = new ArrayList<>(rest);
		sorted.sort(String::compareTo);
		assertEquals(sorted, rest, "remaining tracks should be in album order once shuffle is off");
	}

	@Test
	void queueReorderAndClear() {
		PlayQueue q = seeded();
		q.enqueue(track("x"));
		q.enqueue(track("y"));
		q.enqueue(track("z"));

		q.moveInQueue(2, 0);
		assertEquals(List.of("z", "x", "y"), q.userQueue().stream().map(Track::title).toList());

		q.removeFromQueue(1);
		assertEquals(List.of("z", "y"), q.userQueue().stream().map(Track::title).toList());

		q.clearQueue();
		assertTrue(q.userQueue().isEmpty());
	}

	@Test
	void queueReorderIgnoresOutOfRangeRatherThanThrowing() {
		PlayQueue q = seeded();
		q.enqueue(track("x"));
		q.moveInQueue(5, 0);
		q.moveInQueue(0, 5);
		q.removeFromQueue(99);
		assertEquals(1, q.userQueue().size(), "a bad drag must not throw into the render loop");
	}

	@Test
	void upcomingReorderChangesContextPlayOrder() {
		PlayQueue q = seeded();
		q.setContext("album", ALBUM, -1);
		q.next();

		q.moveUpcoming(1, 0);
		assertEquals(List.of("c", "b", "d"),
				q.upcoming(10).stream().map(Track::title).toList());
		assertEquals("c", q.next().orElseThrow().title());
		assertEquals("b", q.next().orElseThrow().title());
		assertEquals("d", q.next().orElseThrow().title());
	}

	@Test
	void upcomingReorderIgnoresOutOfRangeRatherThanThrowing() {
		PlayQueue q = seeded();
		q.setContext("album", ALBUM, -1);
		q.next();
		q.moveUpcoming(-1, 0);
		q.moveUpcoming(0, 99);
		q.moveUpcoming(99, 0);
		assertEquals("b", q.next().orElseThrow().title());
	}

	@Test
	void emptyContextIsSafe() {
		PlayQueue q = seeded();
		q.setContext("empty", List.of(), -1);
		assertTrue(q.next().isEmpty());
		q.setRepeat(RepeatMode.ALL);
		assertTrue(q.next().isEmpty(), "repeat-all on an empty context must not spin forever");
	}
}
