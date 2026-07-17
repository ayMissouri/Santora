package dev.santora.core.model;

import java.util.List;
import java.util.Objects;

public record Album(
		String id,
		String title,
		String subtitle,
		AlbumKind kind,
		List<Track> tracks,
		String artKey
) {
	public Album {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(title, "title");
		Objects.requireNonNull(kind, "kind");
		subtitle = subtitle == null ? "" : subtitle;
		tracks = List.copyOf(tracks);
	}

	public int trackCount() {
		return tracks.size();
	}

	public boolean isEmpty() {
		return tracks.isEmpty();
	}
}
