package dev.santora.core.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

public final class LibraryBuilder {

	private static final String RECORDS_PREFIX = "records/";

	private LibraryBuilder() {
	}

	public static MusicLibrary build(List<RawSound> raw, UnaryOperator<String> translate) {
		Map<String, List<String>> eventsByPath = new TreeMap<>();
		for (RawSound sound : raw) {
			eventsByPath.computeIfAbsent(sound.soundPath(), k -> new ArrayList<>()).add(sound.eventId());
		}

		List<Track> tracks = new ArrayList<>(eventsByPath.size());
		for (Map.Entry<String, List<String>> entry : eventsByPath.entrySet()) {
			String soundPath = entry.getKey();
			String key = translationKeyOf(soundPath);
			List<String> events = entry.getValue().stream().distinct().sorted().toList();
			tracks.add(Track.fromTranslation(soundPath, key, translate.apply(key),
					prettify(lastSegment(soundPath)), events));
		}
		tracks.sort(Comparator.comparing(Track::title, String.CASE_INSENSITIVE_ORDER));

		return new MusicLibrary(tracks, contextAlbums(tracks), artistAlbums(tracks), playlistAlbums(tracks));
	}

	private static List<Album> contextAlbums(List<Track> tracks) {
		Map<MusicContext, List<Track>> byContext = new LinkedHashMap<>();
		for (MusicContext context : MusicContext.values()) {
			byContext.put(context, new ArrayList<>());
		}
		for (Track track : tracks) {
			byContext.get(MusicContext.of(track.soundPath())).add(track);
		}

		List<Album> albums = new ArrayList<>();
		byContext.forEach((context, list) -> {
			if (!list.isEmpty()) {
				albums.add(new Album("context:" + context.id(), context.displayName(),
						plural(list.size()), AlbumKind.CONTEXT, list,
						"santora:textures/album/" + context.id() + ".png"));
			}
		});
		return albums;
	}

	private static List<Album> artistAlbums(List<Track> tracks) {
		Map<String, List<Track>> byArtist = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (Track track : tracks) {
			if (!track.artist().isEmpty()) {
				byArtist.computeIfAbsent(track.artist(), k -> new ArrayList<>()).add(track);
			}
		}

		List<Album> albums = new ArrayList<>();
		byArtist.forEach((artist, list) -> albums.add(new Album("artist:" + slug(artist), artist,
				plural(list.size()), AlbumKind.ARTIST, list, null)));
		return albums;
	}

	private static List<Album> playlistAlbums(List<Track> tracks) {
		Map<String, List<Track>> byEvent = new TreeMap<>();
		for (Track track : tracks) {
			for (String event : track.eventIds()) {
				byEvent.computeIfAbsent(event, k -> new ArrayList<>()).add(track);
			}
		}

		List<Album> albums = new ArrayList<>();
		byEvent.forEach((event, list) -> {
			// Music discs already get their own album, so we skip making a one-song playlist for every disc.
			if (stripNamespace(event).startsWith("music_disc.")) {
				return;
			}
			if (list.isEmpty()) {
				return;
			}
			albums.add(new Album("event:" + event, playlistName(event), plural(list.size()),
					AlbumKind.PLAYLIST, list, null));
		});
		albums.sort(Comparator.comparing(Album::title, String.CASE_INSENSITIVE_ORDER));
		return albums;
	}

	static String translationKeyOf(String soundPath) {
		String namespace = namespaceOf(soundPath);
		String path = stripNamespace(soundPath);

		if (path.startsWith(RECORDS_PREFIX)) {
			return "jukebox_song." + namespace + "." + path.substring(RECORDS_PREFIX.length());
		}
		return path.replace('/', '.');
	}

	static String playlistName(String eventId) {
		String path = stripNamespace(eventId);
		for (String prefix : new String[] { "music.overworld.", "music.nether.", "music." }) {
			if (path.startsWith(prefix)) {
				return prettify(path.substring(prefix.length()).replace('.', '_'));
			}
		}
		return prettify(path.replace('.', '_'));
	}

	static String prettify(String raw) {
		String[] words = raw.replace('_', ' ').trim().split("\\s+");
		StringBuilder out = new StringBuilder();
		for (String word : words) {
			if (word.isEmpty()) {
				continue;
			}
			if (!out.isEmpty()) {
				out.append(' ');
			}
			out.append(Character.toUpperCase(word.charAt(0)));
			if (word.length() > 1) {
				out.append(word.substring(1));
			}
		}
		return out.isEmpty() ? raw : out.toString();
	}

	private static String plural(int count) {
		return count + (count == 1 ? " track" : " tracks");
	}

	private static String slug(String value) {
		return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
	}

	private static String lastSegment(String path) {
		int slash = path.lastIndexOf('/');
		return slash < 0 ? stripNamespace(path) : path.substring(slash + 1);
	}

	private static String stripNamespace(String id) {
		int colon = id.indexOf(':');
		return colon < 0 ? id : id.substring(colon + 1);
	}

	private static String namespaceOf(String id) {
		int colon = id.indexOf(':');
		return colon < 0 ? "minecraft" : id.substring(0, colon);
	}
}
