package dev.santora.core.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SantoraConfigTest {

	@Test
	void delayRangeKeepsMinBelowMax() {
		SantoraConfig config = new SantoraConfig();
		config.setDelayMaxMillis(60_000);
		config.setDelayMinMillis(20_000);
		assertEquals(20_000, config.delayMinMillis());
		assertEquals(60_000, config.delayMaxMillis());

		config.setDelayMinMillis(90_000);
		assertEquals(90_000, config.delayMaxMillis());

		config.setDelayMaxMillis(10_000);
		assertEquals(10_000, config.delayMinMillis());
	}

	@Test
	void delayIsClampedToTheAllowedRange() {
		SantoraConfig config = new SantoraConfig();
		config.setDelayMaxMillis(SantoraConfig.DELAY_MAX_MILLIS + 1);
		assertEquals(SantoraConfig.DELAY_MAX_MILLIS, config.delayMaxMillis());
		config.setDelayMinMillis(-5);
		assertEquals(0, config.delayMinMillis());
	}

	@Test
	void delayIsPickedInsideTheRange() {
		SantoraConfig config = new SantoraConfig();
		config.setCrossfadeOn(false);
		config.setDelayMaxMillis(60_000);
		config.setDelayMinMillis(20_000);

		assertEquals(20_000, config.delayMillisAt(0f));
		assertEquals(40_000, config.delayMillisAt(0.5f));
		assertEquals(60_000, config.delayMillisAt(1f));
		assertEquals(20_000, config.delayMillisAt(-1f));
		assertEquals(60_000, config.delayMillisAt(2f));
	}

	@Test
	void crossfadeSkipsTheDelay() {
		SantoraConfig config = new SantoraConfig();
		config.setDelayMaxMillis(60_000);
		config.setDelayMinMillis(20_000);
		assertEquals(0, config.delayMillisAt(0.5f));
	}

	@Test
	void crossfadeNeedsBothTheToggleAndADuration() {
		SantoraConfig config = new SantoraConfig();
		assertTrue(config.crossfadeEnabled());

		config.setCrossfadeOn(false);
		assertFalse(config.crossfadeEnabled());

		config.setCrossfadeOn(true);
		config.setCrossfadeMillis(0);
		assertFalse(config.crossfadeEnabled());
	}
}
