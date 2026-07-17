package dev.santora.core.audio;

import java.io.IOException;
import java.io.InputStream;
import java.util.OptionalDouble;

public final class OggDuration {

	private static final byte[] CAPTURE = { 'O', 'g', 'g', 'S' };
	private static final int HEAD_BYTES = 4096;
	private static final int TAIL_BYTES = 65536;
	private static final long NO_GRANULE = -1L;

	private OggDuration() {
	}

	public static OptionalDouble readSeconds(InputStream in) {
		try (InputStream stream = in) {
			byte[] head = readAtMost(stream, HEAD_BYTES);
			OptionalDouble sampleRate = sampleRate(head);
			if (sampleRate.isEmpty()) {
				return OptionalDouble.empty();
			}

			byte[] tail = readTail(stream, head);
			OptionalDouble granule = lastGranule(tail);
			if (granule.isEmpty()) {
				return OptionalDouble.empty();
			}

			double seconds = granule.getAsDouble() / sampleRate.getAsDouble();
			return seconds > 0 && seconds < 60 * 60 ? OptionalDouble.of(seconds) : OptionalDouble.empty();
		} catch (IOException | RuntimeException e) {
			return OptionalDouble.empty();
		}
	}

	private static OptionalDouble sampleRate(byte[] head) {
		if (!hasCaptureAt(head, 0)) {
			return OptionalDouble.empty();
		}

		int dataStart = pageDataStart(head, 0);
		if (dataStart < 0 || dataStart + 16 > head.length) {
			return OptionalDouble.empty();
		}

		if (head[dataStart] != 0x01 || !matches(head, dataStart + 1, "vorbis")) {
			return OptionalDouble.empty();
		}

		long rate = readU32LE(head, dataStart + 12);
		return rate > 0 ? OptionalDouble.of(rate) : OptionalDouble.empty();
	}

	private static int pageDataStart(byte[] buf, int pageStart) {
		int segmentCountAt = pageStart + 26;
		if (segmentCountAt >= buf.length) {
			return -1;
		}
		int segments = buf[segmentCountAt] & 0xFF;
		int start = pageStart + 27 + segments;
		return start <= buf.length ? start : -1;
	}

	private static OptionalDouble lastGranule(byte[] tail) {
		for (int i = tail.length - 27; i >= 0; i--) {
			if (!hasCaptureAt(tail, i)) {
				continue;
			}
			long granule = readI64LE(tail, i + 6);
			if (granule != NO_GRANULE && granule > 0) {
				return OptionalDouble.of(granule);
			}
		}
		return OptionalDouble.empty();
	}

	private static byte[] readTail(InputStream in, byte[] head) throws IOException {
		byte[] window = new byte[TAIL_BYTES];
		int filled = Math.min(head.length, TAIL_BYTES);
		System.arraycopy(head, head.length - filled, window, 0, filled);

		byte[] chunk = new byte[8192];
		int read;
		while ((read = in.read(chunk)) != -1) {
			if (read >= TAIL_BYTES) {
				System.arraycopy(chunk, read - TAIL_BYTES, window, 0, TAIL_BYTES);
				filled = TAIL_BYTES;
			} else if (filled + read <= TAIL_BYTES) {
				System.arraycopy(chunk, 0, window, filled, read);
				filled += read;
			} else {
				int shift = filled + read - TAIL_BYTES;
				System.arraycopy(window, shift, window, 0, filled - shift);
				System.arraycopy(chunk, 0, window, filled - shift, read);
				filled = TAIL_BYTES;
			}
		}

		if (filled == TAIL_BYTES) {
			return window;
		}
		byte[] exact = new byte[filled];
		System.arraycopy(window, 0, exact, 0, filled);
		return exact;
	}

	private static byte[] readAtMost(InputStream in, int max) throws IOException {
		byte[] buf = new byte[max];
		int filled = 0;
		while (filled < max) {
			int read = in.read(buf, filled, max - filled);
			if (read == -1) {
				break;
			}
			filled += read;
		}
		if (filled == max) {
			return buf;
		}
		byte[] exact = new byte[filled];
		System.arraycopy(buf, 0, exact, 0, filled);
		return exact;
	}

	private static boolean hasCaptureAt(byte[] buf, int offset) {
		if (offset + CAPTURE.length > buf.length) {
			return false;
		}
		for (int i = 0; i < CAPTURE.length; i++) {
			if (buf[offset + i] != CAPTURE[i]) {
				return false;
			}
		}
		return true;
	}

	private static boolean matches(byte[] buf, int offset, String text) {
		if (offset + text.length() > buf.length) {
			return false;
		}
		for (int i = 0; i < text.length(); i++) {
			if (buf[offset + i] != (byte) text.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	private static long readU32LE(byte[] buf, int offset) {
		return (buf[offset] & 0xFFL)
				| ((buf[offset + 1] & 0xFFL) << 8)
				| ((buf[offset + 2] & 0xFFL) << 16)
				| ((buf[offset + 3] & 0xFFL) << 24);
	}

	private static long readI64LE(byte[] buf, int offset) {
		if (offset + 8 > buf.length) {
			return NO_GRANULE;
		}
		long value = 0;
		for (int i = 7; i >= 0; i--) {
			value = (value << 8) | (buf[offset + i] & 0xFFL);
		}
		return value;
	}
}
