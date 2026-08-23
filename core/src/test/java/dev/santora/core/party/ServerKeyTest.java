package dev.santora.core.party;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerKeyTest {

	@Test
	void noServerMeansSingleplayer() {
		assertEquals(ServerKey.SINGLEPLAYER, ServerKey.normalize(null));
		assertEquals(ServerKey.SINGLEPLAYER, ServerKey.normalize(""));
		assertEquals(ServerKey.SINGLEPLAYER, ServerKey.normalize("   "));
	}

	@Test
	void sameServerTypedDifferentlyLandsInOneBucket() {
		String expected = ServerKey.of("mc.example.net");
		assertEquals(expected, ServerKey.of("MC.Example.NET"));
		assertEquals(expected, ServerKey.of("  mc.example.net  "));
		assertEquals(expected, ServerKey.of("mc.example.net:25565"));
		assertEquals(expected, ServerKey.of("mc.example.net."));
	}

	@Test
	void otherPortsAreTheirOwnBucket() {
		assertNotEquals(ServerKey.of("mc.example.net"), ServerKey.of("mc.example.net:25566"));
	}

	@Test
	void differentServersDoNotCollide() {
		assertNotEquals(ServerKey.of("mc.example.net"), ServerKey.of("play.example.net"));
		assertNotEquals(ServerKey.of("mc.example.net"), ServerKey.of(""));
	}

	@Test
	void keyIsShortHexTheRelayCanBucketOn() {
		String key = ServerKey.of("mc.example.net");
		assertEquals(16, key.length());
		assertTrue(key.matches("[0-9a-f]+"), key);
	}

	@Test
	void addressNeverAppearsInTheKey() {
		assertFalse(ServerKey.of("mc.example.net").contains("example"));
	}
}
