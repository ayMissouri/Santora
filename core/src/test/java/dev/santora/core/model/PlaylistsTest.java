package dev.santora.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaylistsTest {

	private static Track track(String name) {
		return new Track("minecraft:music/game/" + name, "music.game." + name, name, "C418", List.of());
	}

	private static MusicLibrary library(String... names) {
		List<Track> tracks = java.util.Arrays.stream(names).map(PlaylistsTest::track).toList();
		return new MusicLibrary(tracks, List.of(), List.of(), List.of());
	}

	private static String path(String name) {
		return "minecraft:music/game/" + name;
	}

	@Test
	void toggleFavoriteFlipsAndKeepsOrder() {
		Playlists playlists = new Playlists();
		assertTrue(playlists.toggleFavorite(path("b")));
		assertTrue(playlists.toggleFavorite(path("a")));
		assertEquals(List.of(path("b"), path("a")), playlists.favoritePaths());

		assertFalse(playlists.toggleFavorite(path("b")));
		assertEquals(List.of(path("a")), playlists.favoritePaths());
		assertFalse(playlists.isFavorite(path("b")));
		assertTrue(playlists.isFavorite(path("a")));
	}

	@Test
	void createAssignsUniqueIdsAndCleansNames() {
		Playlists playlists = new Playlists();
		String first = playlists.create("  Chill Mix  ");
		String second = playlists.create("");
		assertFalse(first.equals(second), "each playlist gets its own id");
		assertEquals("Chill Mix", playlists.byId(first).orElseThrow().name());
		assertEquals("New Playlist", playlists.byId(second).orElseThrow().name());
	}

	@Test
	void addTrackSkipsDuplicates() {
		Playlists playlists = new Playlists();
		String id = playlists.create("Mix");
		assertTrue(playlists.addTrack(id, path("a")));
		assertFalse(playlists.addTrack(id, path("a")), "same track twice is refused");
		assertEquals(List.of(path("a")), playlists.byId(id).orElseThrow().trackPaths());
	}

	@Test
	void removeTrackAndDelete() {
		Playlists playlists = new Playlists();
		String id = playlists.create("Mix");
		playlists.addTrack(id, path("a"));
		playlists.addTrack(id, path("b"));

		assertTrue(playlists.removeTrack(id, path("a")));
		assertFalse(playlists.removeTrack(id, path("a")));
		assertEquals(List.of(path("b")), playlists.byId(id).orElseThrow().trackPaths());

		playlists.delete(id);
		assertTrue(playlists.byId(id).isEmpty());
	}

	@Test
	void favoritesWorkThroughTheSharedTrackApi() {
		Playlists playlists = new Playlists();
		assertTrue(playlists.addTrack(Playlists.FAVORITES_ID, path("a")));
		assertFalse(playlists.addTrack(Playlists.FAVORITES_ID, path("a")));
		assertTrue(playlists.isFavorite(path("a")));
		assertTrue(playlists.removeTrack(Playlists.FAVORITES_ID, path("a")));
		assertFalse(playlists.isFavorite(path("a")));
	}

	@Test
	void restoreContinuesIdNumbering() {
		Playlists playlists = new Playlists();
		playlists.restore(List.of(path("a")),
				List.of(new Playlists.Playlist("playlist:7", "Old", List.of(path("b")))));

		assertEquals(List.of(path("a")), playlists.favoritePaths());
		String next = playlists.create("Fresh");
		assertEquals("playlist:8", next, "new ids continue after the highest restored id");
	}

	@Test
	void toAlbumsPutsFavoritesFirstAndHidesUnknownTracks() {
		Playlists playlists = new Playlists();
		playlists.toggleFavorite(path("a"));
		playlists.toggleFavorite(path("missing"));
		String id = playlists.create("Mix");
		playlists.addTrack(id, path("b"));

		List<Album> albums = playlists.toAlbums(library("a", "b"));
		assertEquals(2, albums.size());
		assertEquals(Playlists.FAVORITES_ID, albums.get(0).id());
		assertEquals(AlbumKind.PLAYLIST, albums.get(0).kind());
		assertEquals(1, albums.get(0).trackCount(), "unknown favorite is hidden, not shown broken");
		assertEquals("Mix", albums.get(1).title());
		assertEquals(1, albums.get(1).trackCount());
	}

	@Test
	void revisionBumpsOnEveryMutation() {
		Playlists playlists = new Playlists();
		int rev = playlists.revision();
		playlists.toggleFavorite(path("a"));
		assertTrue(playlists.revision() > rev);

		rev = playlists.revision();
		String id = playlists.create("Mix");
		playlists.addTrack(id, path("a"));
		playlists.removeTrack(id, path("a"));
		playlists.delete(id);
		assertEquals(rev + 4, playlists.revision());
	}
}
