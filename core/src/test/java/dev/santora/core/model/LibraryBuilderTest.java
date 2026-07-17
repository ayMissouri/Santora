package dev.santora.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryBuilderTest {

	private static final Map<String, String> LANG = Map.of(
			"music.game.sweden", "C418 - Sweden",
			"music.game.haggstrom", "C418 - Haggstrom",
			"music.game.nether.warmth", "C418 - Warmth",
			"music.game.os_piano", "Amos Roddy - O's Piano",
			"music.menu.mutation", "C418 - Mutation",
			"music.game.creative.aria_math", "C418 - Aria Math",
			"jukebox_song.minecraft.13", "C418 - 13",
			"jukebox_song.minecraft.pigstep", "Lena Raine - Pigstep"
	);

	private static MusicLibrary build(List<RawSound> raw) {
		return LibraryBuilder.build(raw, key -> LANG.getOrDefault(key, key));
	}

	@Test
	void splitsVanillaArtistTitleConvention() {
		MusicLibrary lib = build(List.of(new RawSound("minecraft:music.game", "minecraft:music/game/sweden")));
		Track sweden = lib.tracks().get(0);
		assertEquals("Sweden", sweden.title());
		assertEquals("C418", sweden.artist());
	}

	@Test
	void splitsOnFirstSeparatorOnly() {
		MusicLibrary lib = build(List.of(new RawSound("minecraft:music.game", "minecraft:music/game/os_piano")));
		Track track = lib.tracks().get(0);
		assertEquals("Amos Roddy", track.artist());
		assertEquals("O's Piano", track.title());
	}

	@Test
	void accentedArtistNamesKeepTheirLettersInSlugs() {
		MusicLibrary lib = LibraryBuilder.build(
				List.of(new RawSound("minecraft:music_disc.5", "minecraft:records/5")),
				key -> "Samuel Åberg - 5");
		Album album = lib.artistAlbums().get(0);
		assertEquals("artist:samuel_aberg", album.id(), "Å should become a, not vanish");
		assertEquals("santora:textures/album/artist/samuel_aberg.png", album.artKey());
	}

	@Test
	void fallsBackToPrettifiedFileNameWhenTranslationMissing() {
		MusicLibrary lib = build(List.of(new RawSound("minecraft:music.game", "minecraft:music/game/mice_on_venus")));
		Track track = lib.tracks().get(0);
		assertEquals("Mice On Venus", track.title());
		assertEquals("", track.artist());
	}

	@Test
	void deduplicatesFileReachableFromManyEvents() {
		List<RawSound> raw = List.of(
				new RawSound("minecraft:music.game", "minecraft:music/game/haggstrom"),
				new RawSound("minecraft:music.overworld.forest", "minecraft:music/game/haggstrom"),
				new RawSound("minecraft:music.overworld.desert", "minecraft:music/game/haggstrom"),
				new RawSound("minecraft:music.overworld.jungle", "minecraft:music/game/haggstrom"));

		MusicLibrary lib = build(raw);
		assertEquals(1, lib.size(), "one file must yield exactly one track");
		assertEquals(4, lib.tracks().get(0).eventIds().size(), "but it should remember every event");
	}

	@Test
	void classifiesContextByLongestPrefixNotShortest() {
		assertEquals(MusicContext.CREATIVE, MusicContext.of("minecraft:music/game/creative/aria_math"));
		assertEquals(MusicContext.NETHER, MusicContext.of("minecraft:music/game/nether/warmth"));
		assertEquals(MusicContext.END, MusicContext.of("minecraft:music/game/end/the_end"));
		assertEquals(MusicContext.UNDERWATER, MusicContext.of("minecraft:music/game/water/axolotl"));
		assertEquals(MusicContext.SWAMP, MusicContext.of("minecraft:music/game/swamp/aerie"));
		assertEquals(MusicContext.OVERWORLD, MusicContext.of("minecraft:music/game/sweden"));
		assertEquals(MusicContext.MENU, MusicContext.of("minecraft:music/menu/mutation"));
		assertEquals(MusicContext.DISCS, MusicContext.of("minecraft:records/pigstep"));
	}

	@Test
	void unknownPathsGoToOtherRatherThanBeingDropped() {
		assertEquals(MusicContext.OTHER, MusicContext.of("somemod:tunes/whatever"));
		MusicLibrary lib = build(List.of(new RawSound("somemod:music.custom", "somemod:tunes/whatever")));
		assertEquals(1, lib.size(), "third-party music must still show up");
	}

	@Test
	void groupsTracksByUpdateInReleaseOrder() {
		MusicLibrary lib = build(List.of(
				new RawSound("minecraft:music.nether.crimson_forest", "minecraft:music/game/nether/crimson_forest/chrysopoeia"),
				new RawSound("minecraft:music.game", "minecraft:music/game/sweden"),
				new RawSound("minecraft:music.under_water", "minecraft:music/game/water/axolotl")));

		List<String> titles = lib.updateAlbums().stream().map(Album::title).toList();
		assertEquals(List.of("Classic", "Aquatic Update", "Nether Update"), titles,
				"the grid should read as a timeline, not alphabetically");
		assertEquals("1.16 · 1 track", lib.updateAlbums().get(2).subtitle());
	}

	@Test
	void groupsByArtistFromVanillaTranslations() {
		MusicLibrary lib = build(List.of(
				new RawSound("minecraft:music.game", "minecraft:music/game/sweden"),
				new RawSound("minecraft:music.game", "minecraft:music/game/haggstrom"),
				new RawSound("minecraft:music.game", "minecraft:music/game/os_piano")));

		Album c418 = lib.artistAlbums().stream().filter(a -> a.title().equals("C418")).findFirst().orElseThrow();
		assertEquals(2, c418.trackCount());
		assertTrue(lib.artistAlbums().stream().anyMatch(a -> a.title().equals("Amos Roddy")));
	}

	@Test
	void discsBelongToTheUpdateThatAddedThem() {
		MusicLibrary lib = build(List.of(
				new RawSound("minecraft:music_disc.pigstep", "minecraft:records/pigstep"),
				new RawSound("minecraft:music.nether.crimson_forest", "minecraft:music/game/nether/crimson_forest/chrysopoeia")));

		Album nether = lib.updateAlbums().stream()
				.filter(a -> a.id().equals("update:nether")).findFirst().orElseThrow();
		assertEquals(2, nether.trackCount(), "Pigstep shipped with the Nether Update, same as Chrysopoeia");
	}

	@Test
	void unknownAndModdedTracksLandInOtherInsteadOfVanishing() {
		MusicLibrary lib = build(List.of(
				new RawSound("somemod:music.custom", "somemod:tunes/whatever"),
				new RawSound("minecraft:music.game", "minecraft:music/game/some_future_song")));

		Album other = lib.updateAlbums().stream()
				.filter(a -> a.id().equals("update:other")).findFirst().orElseThrow();
		assertEquals(2, other.trackCount());
		assertEquals(other, lib.updateAlbums().get(lib.updateAlbums().size() - 1), "Other always sorts last");
	}

	@Test
	void updateLookupIgnoresTheFolderSinceMojangMovesFilesAround() {
		assertEquals(MusicUpdate.NETHER, MusicUpdate.of("minecraft:music/game/nether/crimson_forest/chrysopoeia"));
		assertEquals(MusicUpdate.NETHER, MusicUpdate.of("minecraft:music/game/chrysopoeia"));
		assertEquals(MusicUpdate.OTHER, MusicUpdate.of("somemod:music/game/sweden"),
				"a modded copy of a vanilla file name must not join the vanilla playlist");
	}

	@Test
	void noFileNameIsClaimedByTwoUpdates() {
		Map<String, MusicUpdate> seen = new java.util.HashMap<>();
		for (MusicUpdate update : MusicUpdate.values()) {
			for (String fileName : update.fileNames()) {
				MusicUpdate previous = seen.put(fileName, update);
				assertTrue(previous == null, fileName + " is listed under both " + previous + " and " + update);
			}
		}
	}

	@Test
	void derivesVanillaTranslationKeyLikeToShortLanguageKey() {
		assertEquals("music.game.sweden", LibraryBuilder.translationKeyOf("minecraft:music/game/sweden"));
		assertEquals("music.game.nether.warmth", LibraryBuilder.translationKeyOf("minecraft:music/game/nether/warmth"));
	}

	@Test
	void discsUseTheJukeboxSongKeyNamespaceNotTheFilePath() {
		assertEquals("jukebox_song.minecraft.13", LibraryBuilder.translationKeyOf("minecraft:records/13"));
		assertEquals("jukebox_song.minecraft.pigstep", LibraryBuilder.translationKeyOf("minecraft:records/pigstep"));
		assertEquals("jukebox_song.somemod.custom", LibraryBuilder.translationKeyOf("somemod:records/custom"));
	}

	@Test
	void discsResolveRealTitlesAndArtists() {
		MusicLibrary lib = build(List.of(
				new RawSound("minecraft:music_disc.13", "minecraft:records/13"),
				new RawSound("minecraft:music_disc.pigstep", "minecraft:records/pigstep")));

		Track thirteen = lib.trackByPath("minecraft:records/13").orElseThrow();
		assertEquals("13", thirteen.title());
		assertEquals("C418", thirteen.artist(), "discs must not lose their composer");

		Track pigstep = lib.trackByPath("minecraft:records/pigstep").orElseThrow();
		assertEquals("Pigstep", pigstep.title());
		assertEquals("Lena Raine", pigstep.artist());
	}
}
