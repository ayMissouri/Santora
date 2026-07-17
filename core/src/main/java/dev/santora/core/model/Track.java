package dev.santora.core.model;

import java.util.List;
import java.util.Objects;

public record Track(
		String soundPath,
		String translationKey,
		String title,
		String artist,
		List<String> eventIds
) {
	public Track {
		Objects.requireNonNull(soundPath, "soundPath");
		Objects.requireNonNull(translationKey, "translationKey");
		Objects.requireNonNull(title, "title");
		Objects.requireNonNull(artist, "artist");
		eventIds = List.copyOf(eventIds);
	}

	public static Track fromTranslation(String soundPath, String translationKey, String translated,
			String fallbackTitle, List<String> eventIds) {
		String title = fallbackTitle;
		String artist = "";

		boolean usable = translated != null && !translated.isBlank() && !translated.equals(translationKey);
		if (usable) {
			int split = translated.indexOf(" - ");
			if (split > 0) {
				artist = translated.substring(0, split).trim();
				title = translated.substring(split + 3).trim();
			} else {
				title = translated.trim();
			}
		}

		return new Track(soundPath, translationKey, title, artist, eventIds);
	}

	public String fileName() {
		int slash = soundPath.lastIndexOf('/');
		return slash < 0 ? soundPath : soundPath.substring(slash + 1);
	}

	public String displayLine() {
		return artist.isEmpty() ? title : artist + " · " + title;
	}
}
