package dev.santora.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MusicLibraryTest {

	private static Track track(String path, String title, String artist) {
		return new Track(path, "music.test." + title, title, artist, List.of());
	}

	private static final MusicLibrary LIBRARY = new MusicLibrary(
			List.of(
					track("minecraft:music/game/sweden", "Sweden", "C418"),
					track("minecraft:music/game/wet_hands", "Wet Hands", "C418"),
					track("minecraft:music/nether/rubedo", "Rubedo", "Lena Raine"),
					track("minecraft:music/game/pigstep", "Pigstep", "Lena Raine")),
			List.of(), List.of(), List.of());

	@Test
	void matchesTitleIgnoringCase() {
		List<Track> hits = LIBRARY.search("SWEDEN");
		assertEquals(1, hits.size());
		assertEquals("Sweden", hits.get(0).title());
	}

	@Test
	void matchesArtist() {
		List<Track> hits = LIBRARY.search("lena");
		assertEquals(2, hits.size());
	}

	@Test
	void matchesFileNameWords() {
		List<Track> hits = LIBRARY.search("wet_hands");
		assertEquals(1, hits.size());
		assertEquals("Wet Hands", hits.get(0).title());
	}

	@Test
	void allWordsMustMatch() {
		assertEquals(1, LIBRARY.search("lena pigstep").size());
		assertTrue(LIBRARY.search("lena sweden").isEmpty());
	}

	@Test
	void blankQueryMatchesEverything() {
		assertEquals(4, LIBRARY.search("   ").size());
	}

	@Test
	void noMatchesGivesEmptyList() {
		assertTrue(LIBRARY.search("beethoven").isEmpty());
	}
}
