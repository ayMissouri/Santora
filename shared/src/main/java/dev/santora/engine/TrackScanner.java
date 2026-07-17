package dev.santora.engine;

import dev.santora.core.model.LibraryBuilder;
import dev.santora.core.model.MusicLibrary;
import dev.santora.core.model.RawSound;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.resources.Identifier;
import dev.santora.mixin.WeighedSoundEventsAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TrackScanner {

	public record ScanResult(MusicLibrary library, Map<String, PlayableSound> playable) {
		public static final ScanResult EMPTY = new ScanResult(MusicLibrary.EMPTY, Map.of());
	}

	private TrackScanner() {
	}

	public static ScanResult scan(SoundManager soundManager) {
		List<RawSound> raw = new ArrayList<>();
		Map<String, PlayableSound> playable = new HashMap<>();

		for (Identifier eventId : soundManager.getAvailableSounds()) {
			if (!isMusicEvent(eventId)) {
				continue;
			}

			WeighedSoundEvents event = soundManager.getSoundEvent(eventId);
			if (event == null) {
				continue;
			}

			for (Sound sound : flatten(event, new HashSet<>())) {
				String soundPath = sound.getLocation().toString();
				raw.add(new RawSound(eventId.toString(), soundPath));
				playable.putIfAbsent(soundPath, new PlayableSound(eventId, sound));
			}
		}

		MusicLibrary library = LibraryBuilder.build(raw, TrackScanner::translate);
		return new ScanResult(library, Map.copyOf(playable));
	}

	private static String translate(String key) {
		return I18n.get(key);
	}

	private static boolean isMusicEvent(Identifier eventId) {
		String path = eventId.getPath();
		return path.startsWith("music.") || path.startsWith("music_disc.");
	}

	private static List<Sound> flatten(WeighedSoundEvents event, Set<Object> seen) {
		List<Sound> out = new ArrayList<>();
		if (!seen.add(event)) {
			return out;
		}

		for (Weighted<Sound> entry : ((WeighedSoundEventsAccessor) event).santora$list()) {
			if (entry instanceof Sound sound) {
				out.add(sound);
			} else if (entry instanceof WeighedSoundEvents nested) {
				out.addAll(flatten(nested, seen));
			}
		}
		return out;
	}
}
