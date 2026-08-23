package dev.santora.engine;

import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import java.util.concurrent.CompletableFuture;

public class SantoraSoundInstance extends AbstractSoundInstance implements TickableSoundInstance, FabricSoundInstance {

	private final Sound forcedSound;
	private boolean stopped;
	private long seekMillis;

	public SantoraSoundInstance(ResourceLocation eventId, Sound forcedSound) {
		super(eventId, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
		this.forcedSound = forcedSound;

		this.sound = forcedSound;

		this.volume = 1.0f;
		this.pitch = 1.0f;
		this.relative = true;
		this.attenuation = SoundInstance.Attenuation.NONE;
		this.looping = false;
		this.delay = 0;
	}

	@Override
	public WeighedSoundEvents resolve(SoundManager soundManager) {
		WeighedSoundEvents events = soundManager.getSoundEvent(this.location);
		this.sound = this.forcedSound;
		return events;
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}

	@Override
	public boolean isStopped() {
		return stopped;
	}

	@Override
	public void tick() {}

	public void setGain(float gain) {
		this.volume = gain < 0f ? 0f : (gain > 1f ? 1f : gain);
	}

	public float gain() {
		return this.volume;
	}

	public void markStopped() {
		this.stopped = true;
	}

	public Sound forcedSound() {
		return forcedSound;
	}

	public void setSeekMillis(long millis) {
		this.seekMillis = millis < 0 ? 0 : millis;
	}

	public long seekMillis() {
		return seekMillis;
	}

	@Override
	public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, ResourceLocation id, boolean repeatInstantly) {
		CompletableFuture<AudioStream> stream = loader.getStream(id, repeatInstantly);
		if (seekMillis <= 0) {
			return stream;
		}
		long skip = seekMillis;
		return stream.thenApply(delegate -> new SeekingAudioStream(delegate, skip));
	}
}
