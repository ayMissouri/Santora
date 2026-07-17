package dev.santora.engine;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.logging.LogUtils;
import dev.santora.core.audio.OggDuration;
import dev.santora.core.config.SantoraConfig;
import dev.santora.core.model.Album;
import dev.santora.core.model.MusicLibrary;
import dev.santora.core.model.Playlists;
import dev.santora.core.model.Track;
import dev.santora.core.play.Fade;
import dev.santora.core.play.PlayQueue;
import dev.santora.core.play.RepeatMode;
import dev.santora.mixin.MusicManagerAccessor;
import dev.santora.mixin.SoundEngineAccessor;
import dev.santora.mixin.SoundManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public final class MusicEngine {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final MusicEngine INSTANCE = new MusicEngine();
	private static final int START_GRACE_TICKS = 5;

	private static final class Voice {
		final Track track;
		final SantoraSoundInstance instance;
		final long startedAtMs;
		int ticksAlive;

		Voice(Track track, SantoraSoundInstance instance, long startedAtMs) {
			this.track = track;
			this.instance = instance;
			this.startedAtMs = startedAtMs;
		}
	}

	private final SantoraConfig config = new SantoraConfig();
	private final PlayQueue queue = new PlayQueue();
	private final Playlists playlists = new Playlists();

	private MusicLibrary library = MusicLibrary.EMPTY;
	private Map<String, PlayableSound> playable = Map.of();

	private List<Album> playlistAlbums = List.of();
	private int playlistAlbumsRevision = -1;
	private MusicLibrary playlistAlbumsLibrary;

	private final Map<String, Double> durations = new ConcurrentHashMap<>();
	private final Executor durationExecutor = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "Santora-Durations");
		t.setDaemon(true);
		return t;
	});

	private boolean manualMode;
	private Voice current;
	private Voice outgoing;
	private long fadeStartMs;

	private boolean paused;
	private long pausedAtMs;
	private long pausedTotalMs;

	private long resumeAtMs;

	private MusicEngine() {
	}

	public static MusicEngine get() {
		return INSTANCE;
	}

	// Library
	public void reload(SoundManager soundManager) {
		TrackScanner.ScanResult result = TrackScanner.scan(soundManager);
		this.library = result.library();
		this.playable = result.playable();
		LOGGER.info("[Santora] indexed {} music tracks across {} albums",
				library.size(), library.allAlbums().size());
		prefetchDurations();
	}

	public MusicLibrary library() {
		return library;
	}

	public SantoraConfig config() {
		return config;
	}

	public PlayQueue queue() {
		return queue;
	}

	public Playlists playlists() {
		return playlists;
	}

	public List<Album> playlistAlbums() {
		if (playlistAlbumsRevision != playlists.revision() || playlistAlbumsLibrary != library) {
			playlistAlbums = playlists.toAlbums(library);
			playlistAlbumsRevision = playlists.revision();
			playlistAlbumsLibrary = library;
		}
		return playlistAlbums;
	}

	public Optional<Album> albumById(String id) {
		Optional<Album> album = library.albumById(id);
		if (album.isPresent()) {
			return album;
		}
		return playlistAlbums().stream().filter(a -> a.id().equals(id)).findFirst();
	}

	public boolean isManualMode() {
		return manualMode;
	}

	public void setManualMode(boolean manual) {
		if (this.manualMode == manual) {
			return;
		}
		this.manualMode = manual;

		Minecraft mc = Minecraft.getInstance();
		if (mc == null) {
			return;
		}

		// Reset the default game music volume whenever control changes.
		if (manual) {
			mc.getMusicManager().stopPlaying();
			resetMusicCategoryGain(mc);
		} else {
			stopAllVoices();
			paused = false;
			resumeAtMs = 0;
			queue.reset();
			resetMusicCategoryGain(mc);
		}

		config.setWasManual(manual);
	}

	private void resetMusicCategoryGain(Minecraft mc) {
		mc.getSoundManager().updateCategoryVolume(SoundSource.MUSIC, 1.0f);
		((MusicManagerAccessor) mc.getMusicManager()).santora$setCurrentGain(1.0f);
	}

	// Playback controls
	public void playAlbum(Album album, int index) {
		setManualMode(true);
		queue.setContext(album.id(), album.tracks(), index);
		queue.clearHistory();
		advance(true);
	}

	public void playTrack(Track track) {
		setManualMode(true);
		startTrack(track, config.crossfadeEnabled());
	}

	public void shuffleAll() {
		setManualMode(true);
		queue.setShuffle(true);
		config.setShuffle(true);
		queue.setContext("library", library.tracks(), -1);
		queue.clearHistory();
		advance(true);
	}

	public void togglePlayPause() {
		if (current == null) {
			if (manualMode) {
				advance(true);
			}
			return;
		}
		setPaused(!paused);
	}

	public void setPaused(boolean value) {
		if (this.paused == value || current == null) {
			return;
		}
		this.paused = value;

		if (value) {
			pausedAtMs = now();
			setChannelPaused(current, true);
		} else {
			pausedTotalMs += now() - pausedAtMs;
			setChannelPaused(current, false);
		}
	}

	public boolean isPaused() {
		return paused;
	}

	public void next() {
		advance(true);
	}

	public void previous() {
		if (current != null && elapsedMillis() > 3_000) {
			restartCurrent();
			return;
		}
		Optional<Track> prev = queue.previous();
		if (prev.isPresent()) {
			startTrack(prev.get(), config.crossfadeEnabled());
		} else {
			restartCurrent();
		}
	}

	private void restartCurrent() {
		if (current != null) {
			startTrack(current.track, false);
		}
	}

	public void stop() {
		stopAllVoices();
		queue.setCurrent(null);
	}

	public void setShuffle(boolean shuffle) {
		queue.setShuffle(shuffle);
		config.setShuffle(shuffle);
	}

	public void cycleRepeat() {
		RepeatMode mode = queue.repeat().nextMode();
		queue.setRepeat(mode);
		config.setRepeat(mode);
	}

	public void setVolume(float volume) {
		config.setVolume(volume);
		// Mid-crossfade the fade tick reapplies gains anyway.
		if (current != null && outgoing == null) {
			current.instance.setGain(config.volume());
		}
	}

	public void tick() {
		if (!manualMode || paused) {
			return;
		}

		long now = now();
		tickCrossfade(now);

		if (current == null && resumeAtMs > 0) {
			if (now >= resumeAtMs) {
				resumeAtMs = 0;
				advance(false);
			}
			return;
		}

		if (current == null) {
			return;
		}

		current.ticksAlive++;

		if (current.ticksAlive > START_GRACE_TICKS && !isActive(current)) {
			onTrackEnded();
			return;
		}

		if (shouldPreemptForCrossfade(now)) {
			advance(false);
		}
	}

	private void tickCrossfade(long now) {
		if (outgoing == null) {
			return;
		}

		float t = Fade.progress(now - fadeStartMs, config.crossfadeMillis());
		outgoing.instance.setGain(Fade.outgoing(t) * config.volume());
		if (current != null) {
			current.instance.setGain(Fade.incoming(t) * config.volume());
		}

		if (t >= 1.0f) {
			stopVoice(outgoing);
			outgoing = null;
		}
	}

	private boolean shouldPreemptForCrossfade(long now) {
		if (!config.crossfadeEnabled() || outgoing != null) {
			return false;
		}
		Double seconds = durations.get(current.track.soundPath());
		if (seconds == null) {
			return false;
		}
		long remaining = (long) (seconds * 1000.0) - elapsedMillis();
		return remaining <= config.crossfadeMillis();
	}

	private void onTrackEnded() {
		current = null;
		int gap = config.delayMillisAt(ThreadLocalRandom.current().nextFloat());
		if (gap > 0) {
			resumeAtMs = now() + gap;
		} else {
			advance(false);
		}
	}

	private void advance(boolean userInitiated) {
		Optional<Track> next = queue.next();
		if (next.isEmpty()) {
			stopAllVoices();
			return;
		}
		startTrack(next.get(), config.crossfadeEnabled() && (userInitiated || current != null));
	}

	private void startTrack(Track track, boolean crossfade) {
		PlayableSound sound = playable.get(track.soundPath());
		if (sound == null) {
			LOGGER.warn("[Santora] no playable sound for {}", track.soundPath());
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		SantoraSoundInstance instance = new SantoraSoundInstance(sound.eventId(), sound.sound());

		if (crossfade && current != null) {
			if (outgoing != null) {
				stopVoice(outgoing);
			}
			outgoing = current;
			fadeStartMs = now();
			instance.setGain(0.0f);
		} else {
			stopAllVoices();
			instance.setGain(config.volume());
		}

		SoundEngine.PlayResult result = mc.getSoundManager().play(instance);
		if (result == SoundEngine.PlayResult.NOT_STARTED) {
			LOGGER.warn("[Santora] sound engine refused {}", track.soundPath());
			current = null;
			return;
		}

		current = new Voice(track, instance, now());
		queue.setCurrent(track);
		paused = false;
		pausedTotalMs = 0;
		resumeAtMs = 0;

		config.setLastTrackPath(track.soundPath());
		config.setLastContextId(queue.contextId());
	}

	private boolean isActive(Voice voice) {
		return Minecraft.getInstance().getSoundManager().isActive(voice.instance);
	}

	private void stopVoice(Voice voice) {
		if (voice == null) {
			return;
		}
		voice.instance.markStopped();
		Minecraft.getInstance().getSoundManager().stop(voice.instance);
	}

	private void stopAllVoices() {
		stopVoice(outgoing);
		stopVoice(current);
		outgoing = null;
		current = null;
	}

	// Pausing and resuming
	public void reassertPause() {
		if (paused && current != null) {
			setChannelPaused(current, true);
		}
	}

	private void setChannelPaused(Voice voice, boolean pause) {
		SoundEngine engine = ((SoundManagerAccessor) Minecraft.getInstance().getSoundManager())
				.santora$soundEngine();
		ChannelAccess.ChannelHandle handle =
				((SoundEngineAccessor) engine).santora$instanceToChannel().get(voice.instance);

		if (handle == null) {
			return;
		}
		handle.execute(pause ? Channel::pause : Channel::unpause);
	}

	// Now playing info
	public Track currentTrack() {
		return current == null ? null : current.track;
	}

	public long delayRemainingMillis() {
		if (current != null || resumeAtMs == 0) {
			return -1;
		}
		return Math.max(0, resumeAtMs - now());
	}

	// Since minecraft doesnt show the current song position, I just measure it manually.
	public long elapsedMillis() {
		if (current == null) {
			return 0;
		}
		long base = (paused ? pausedAtMs : now()) - current.startedAtMs;
		return Math.max(0, base - pausedTotalMs);
	}

	public OptionalDouble durationSeconds(Track track) {
		Double seconds = track == null ? null : durations.get(track.soundPath());
		return seconds == null ? OptionalDouble.empty() : OptionalDouble.of(seconds);
	}

	private void prefetchDurations() {
		List<Track> tracks = library.tracks();
		Map<String, PlayableSound> snapshot = playable;

		durationExecutor.execute(() -> {
			Minecraft mc = Minecraft.getInstance();
			int found = 0;
			for (Track track : tracks) {
				if (durations.containsKey(track.soundPath())) {
					continue;
				}
				PlayableSound sound = snapshot.get(track.soundPath());
				if (sound == null) {
					continue;
				}
				Optional<Resource> resource = mc.getResourceManager().getResource(sound.sound().getPath());
				if (resource.isEmpty()) {
					continue;
				}
				try (InputStream in = resource.get().open()) {
					OptionalDouble seconds = OggDuration.readSeconds(in);
					if (seconds.isPresent()) {
						durations.put(track.soundPath(), seconds.getAsDouble());
						found++;
					}
				} catch (Exception e) {
					LOGGER.debug("[Santora] could not read duration for {}", track.soundPath(), e);
				}
			}
			LOGGER.info("[Santora] read durations for {}/{} tracks", found, tracks.size());
		});
	}

	private static long now() {
		return System.currentTimeMillis();
	}
}
