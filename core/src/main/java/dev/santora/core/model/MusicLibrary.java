package dev.santora.core.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public record MusicLibrary(
		List<Track> tracks,
		List<Album> contextAlbums,
		List<Album> artistAlbums,
		List<Album> playlistAlbums
) {
	public static final MusicLibrary EMPTY = new MusicLibrary(List.of(), List.of(), List.of(), List.of());

	public MusicLibrary {
		tracks = List.copyOf(tracks);
		contextAlbums = List.copyOf(contextAlbums);
		artistAlbums = List.copyOf(artistAlbums);
		playlistAlbums = List.copyOf(playlistAlbums);
	}

	public int size() {
		return tracks.size();
	}

	public boolean isEmpty() {
		return tracks.isEmpty();
	}

	public List<Album> allAlbums() {
		return java.util.stream.Stream.of(contextAlbums, artistAlbums, playlistAlbums)
				.flatMap(List::stream)
				.toList();
	}

	public Optional<Album> albumById(String id) {
		return allAlbums().stream().filter(a -> a.id().equals(id)).findFirst();
	}

	public Optional<Track> trackByPath(String soundPath) {
		return tracks.stream().filter(t -> t.soundPath().equals(soundPath)).findFirst();
	}

	private Map<String, Track> byPath() {
		return tracks.stream().collect(Collectors.toMap(Track::soundPath, Function.identity()));
	}

	public List<Track> resolveAll(List<String> soundPaths) {
		Map<String, Track> index = byPath();
		return soundPaths.stream().map(index::get).filter(java.util.Objects::nonNull).toList();
	}
}
