package dev.santora.core.party;

import dev.santora.core.play.RepeatMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartySessionTest {

	private static PartyMessage.NowPlaying now(String path, long seq) {
		return new PartyMessage.NowPlaying(path, 0, false, false, RepeatMode.OFF, seq);
	}

	private static PartyMessage.QueueSnapshot queue(List<String> upcoming, long seq) {
		return new PartyMessage.QueueSnapshot(upcoming, "album:x", seq);
	}

	@Test
	void startsSolo() {
		PartySession session = new PartySession();
		assertEquals(PartyRole.SOLO, session.role());
		assertFalse(session.inParty());
		assertTrue(session.canControlPlayback());
	}

	@Test
	void hostingTakesControl() {
		PartySession session = new PartySession();
		session.onCreated("ABCDEF", "h1", "Alice");
		assertTrue(session.isHost());
		assertTrue(session.canControlPlayback());
		assertEquals("ABCDEF", session.code());
		assertEquals(1, session.memberCount());
		assertEquals("Alice", session.hostName());
	}

	@Test
	void joiningGivesUpControlAndKnowsTheHost() {
		PartySession session = new PartySession();
		session.onJoined("ABCDEF", "m2", "Bob", "h1", List.of(new PartyMember("h1", "Alice")));
		assertTrue(session.isMember());
		assertFalse(session.canControlPlayback());
		assertEquals("Alice", session.hostName());
		assertEquals(2, session.memberCount(), "roster plus ourselves");
	}

	@Test
	void peerJoinIsIdempotent() {
		PartySession session = new PartySession();
		session.onCreated("ABCDEF", "h1", "Alice");
		session.onPeerJoin(new PartyMember("m2", "Bob"));
		session.onPeerJoin(new PartyMember("m2", "Bob"));
		assertEquals(2, session.memberCount());
	}

	@Test
	void aPeerLeavingJustShrinksTheRoster() {
		PartySession session = new PartySession();
		session.onCreated("ABCDEF", "h1", "Alice");
		session.onPeerJoin(new PartyMember("m2", "Bob"));
		assertFalse(session.onPeerLeave("m2"));
		assertEquals(1, session.memberCount());
	}

	@Test
	void hostLeavingEndsThePartyForMembers() {
		PartySession session = new PartySession();
		session.onJoined("ABCDEF", "m2", "Bob", "h1", List.of(new PartyMember("h1", "Alice")));
		assertTrue(session.onPeerLeave("h1"), "host left -> party over");
		assertEquals(PartyRole.SOLO, session.role());
	}

	@Test
	void nowUpdatesDropWhenStale() {
		PartySession session = new PartySession();
		session.onJoined("ABCDEF", "m2", "Bob", "h1", List.of(new PartyMember("h1", "Alice")));
		assertTrue(session.onNow(now("a", 5), 1_000));
		assertEquals("a", session.lastNow().soundPath());
		assertFalse(session.onNow(now("b", 5), 2_000), "same seq is stale");
		assertFalse(session.onNow(now("b", 3), 2_000), "older seq is stale");
		assertEquals("a", session.lastNow().soundPath());
		assertTrue(session.onNow(now("c", 6), 3_000));
		assertEquals("c", session.lastNow().soundPath());
		assertEquals(3_000, session.lastNowReceiptMillis());
	}

	@Test
	void queueAndNowTrackSeparateSequences() {
		PartySession session = new PartySession();
		session.onJoined("ABCDEF", "m2", "Bob", "h1", List.of(new PartyMember("h1", "Alice")));
		assertTrue(session.onNow(now("a", 10), 1_000));
		assertTrue(session.onQueue(queue(List.of("x", "y"), 4)));
		assertEquals(List.of("x", "y"), session.upcomingMirror());
		assertFalse(session.onQueue(queue(List.of("z"), 4)), "same queue seq is stale");
		assertTrue(session.onQueue(queue(List.of("z"), 5)));
		assertEquals(List.of("z"), session.upcomingMirror());
	}

	@Test
	void welcomeCatchesAJoinerUp() {
		PartySession session = new PartySession();
		session.onJoined("ABCDEF", "m2", "Bob", "h1", List.of(new PartyMember("h1", "Alice")));
		session.onWelcome(new PartyMessage.Welcome(now("a", 2), queue(List.of("x"), 2)), 500);
		assertEquals("a", session.lastNow().soundPath());
		assertEquals(List.of("x"), session.upcomingMirror());
	}
}
