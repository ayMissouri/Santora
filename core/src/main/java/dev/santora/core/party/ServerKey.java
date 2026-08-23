package dev.santora.core.party;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class ServerKey {

	public static final String SINGLEPLAYER = "singleplayer";

	private static final String DEFAULT_PORT = ":25565";
	private static final int KEY_LENGTH = 16;

	private ServerKey() {
	}

	public static String normalize(String address) {
		if (address == null) {
			return SINGLEPLAYER;
		}
		String value = address.trim().toLowerCase(Locale.ROOT);
		if (value.endsWith(DEFAULT_PORT)) {
			value = value.substring(0, value.length() - DEFAULT_PORT.length());
		}
		while (value.endsWith(".")) {
			value = value.substring(0, value.length() - 1);
		}
		return value.isEmpty() ? SINGLEPLAYER : value;
	}

	public static String of(String address) {
		return digest(normalize(address));
	}

	private static String digest(String value) {
		byte[] bytes;
		try {
			bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			return String.format("%016x", (long) value.hashCode() & 0xFFFFFFFFL);
		}
		StringBuilder out = new StringBuilder(KEY_LENGTH);
		for (int i = 0; i < KEY_LENGTH / 2; i++) {
			out.append(String.format("%02x", bytes[i]));
		}
		return out.toString();
	}
}
