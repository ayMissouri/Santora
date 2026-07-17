package dev.santora.core.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class Playlists {

	public static final String FAVORITES_ID = "playlist:favorites";
	private static final String ID_PREFIX = "playlist:";
	private static final int NAME_MAX = 32;

	public record Playlist(String id, String name, List<String> trackPaths) {
		public Playlist {
			trackPaths = List.copyOf(trackPaths);
		}
	}

	private final Set<String> favorites = new LinkedHashSet<>();
	private final List<Playlist> lists = new ArrayList<>();
	private int nextId = 1;
	private int revision;

	public int revision() {
		return revision;
	}

	public boolean isFavorite(String soundPath) {
		return favorites.contains(soundPath);
	}

	public boolean toggleFavorite(String soundPath) {
		if (!favorites.remove(soundPath)) {
			favorites.add(soundPath);
		}
		revision++;
		return favorites.contains(soundPath);
	}

	public List<String> favoritePaths() {
		return List.copyOf(favorites);
	}

	public List<Playlist> all() {
		return List.copyOf(lists);
	}

	public Optional<Playlist> byId(String id) {
		return lists.stream().filter(p -> p.id().equals(id)).findFirst();
	}

	public String create(String name) {
		String id = ID_PREFIX + nextId++;
		lists.add(new Playlist(id, cleanName(name), List.of()));
		revision++;
		return id;
	}

	public void delete(String id) {
		if (lists.removeIf(p -> p.id().equals(id))) {
			revision++;
		}
	}

	public boolean addTrack(String id, String soundPath) {
		if (FAVORITES_ID.equals(id)) {
			boolean added = favorites.add(soundPath);
			if (added) {
				revision++;
			}
			return added;
		}
		for (int i = 0; i < lists.size(); i++) {
			Playlist p = lists.get(i);
			if (!p.id().equals(id)) {
				continue;
			}
			if (p.trackPaths().contains(soundPath)) {
				return false;
			}
			List<String> paths = new ArrayList<>(p.trackPaths());
			paths.add(soundPath);
			lists.set(i, new Playlist(p.id(), p.name(), paths));
			revision++;
			return true;
		}
		return false;
	}

	public boolean removeTrack(String id, String soundPath) {
		if (FAVORITES_ID.equals(id)) {
			boolean removed = favorites.remove(soundPath);
			if (removed) {
				revision++;
			}
			return removed;
		}
		for (int i = 0; i < lists.size(); i++) {
			Playlist p = lists.get(i);
			if (!p.id().equals(id) || !p.trackPaths().contains(soundPath)) {
				continue;
			}
			List<String> paths = new ArrayList<>(p.trackPaths());
			paths.remove(soundPath);
			lists.set(i, new Playlist(p.id(), p.name(), paths));
			revision++;
			return true;
		}
		return false;
	}

	public void restore(List<String> favoritePaths, List<Playlist> playlists) {
		favorites.clear();
		favorites.addAll(favoritePaths);
		lists.clear();
		nextId = 1;
		for (Playlist p : playlists) {
			if (FAVORITES_ID.equals(p.id())) {
				continue;
			}
			lists.add(new Playlist(p.id(), cleanName(p.name()), p.trackPaths()));
			nextId = Math.max(nextId, idNumber(p.id()) + 1);
		}
		revision++;
	}

	public List<Album> toAlbums(MusicLibrary library) {
		List<Album> albums = new ArrayList<>(lists.size() + 1);
		List<Track> favoriteTracks = library.resolveAll(List.copyOf(favorites));
		albums.add(new Album(FAVORITES_ID, "Favorites", plural(favoriteTracks.size()),
				AlbumKind.PLAYLIST, favoriteTracks, null));
		for (Playlist p : lists) {
			List<Track> tracks = library.resolveAll(p.trackPaths());
			albums.add(new Album(p.id(), p.name(), plural(tracks.size()),
					AlbumKind.PLAYLIST, tracks, null));
		}
		return albums;
	}

	private static String cleanName(String name) {
		String cleaned = name == null ? "" : name.trim();
		if (cleaned.isEmpty()) {
			cleaned = "New Playlist";
		}
		return cleaned.length() > NAME_MAX ? cleaned.substring(0, NAME_MAX).trim() : cleaned;
	}

	private static int idNumber(String id) {
		try {
			return Integer.parseInt(id.substring(ID_PREFIX.length()));
		} catch (RuntimeException e) {
			return 0;
		}
	}

	private static String plural(int count) {
		return count + (count == 1 ? " track" : " tracks");
	}
}
