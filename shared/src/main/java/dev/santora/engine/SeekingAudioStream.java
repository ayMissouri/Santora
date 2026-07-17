package dev.santora.engine;

import net.minecraft.client.sounds.AudioStream;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class SeekingAudioStream implements AudioStream {

	private final AudioStream delegate;
	private long bytesToSkip;

	public SeekingAudioStream(AudioStream delegate, long skipMillis) {
		this.delegate = delegate;
		AudioFormat format = delegate.getFormat();
		long frames = (long) (skipMillis / 1000.0 * format.getSampleRate());
		this.bytesToSkip = frames * format.getFrameSize();
	}

	@Override
	public AudioFormat getFormat() {
		return delegate.getFormat();
	}

	@Override
	public ByteBuffer read(int size) throws IOException {
		while (bytesToSkip > 0) {
			ByteBuffer buffer = delegate.read(size);
			if (buffer == null || !buffer.hasRemaining()) {
				bytesToSkip = 0;
				return buffer;
			}
			int remaining = buffer.remaining();
			if (remaining <= bytesToSkip) {
				bytesToSkip -= remaining;
				continue;
			}
			buffer.position(buffer.position() + (int) bytesToSkip);
			bytesToSkip = 0;
			return buffer;
		}
		return delegate.read(size);
	}

	@Override
	public void close() throws IOException {
		delegate.close();
	}
}
