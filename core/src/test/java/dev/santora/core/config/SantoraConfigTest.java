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
	void overlayPositionStaysOnScreen() {
		SantoraConfig config = new SantoraConfig();
		config.setOverlayPos(0.25f, 0.75f);
		assertEquals(0.25f, config.overlayX());
		assertEquals(0.75f, config.overlayY());

		config.setOverlayPos(-0.5f, 1.5f);
		assertEquals(0f, config.overlayX());
		assertEquals(1f, config.overlayY());
	}

	@Test
	void appearanceValuesAreClamped() {
		SantoraConfig config = new SantoraConfig();

		config.setMenuOpacity(5);
		assertEquals(SantoraConfig.MENU_OPACITY_MIN, config.menuOpacity());
		config.setMenuOpacity(150);
		assertEquals(100, config.menuOpacity());

		config.setHudOpacity(-5);
		assertEquals(0, config.hudOpacity());
		config.setHudOpacity(150);
		assertEquals(100, config.hudOpacity());

		config.setMenuAccent(0xFF123456);
		assertEquals(0x123456, config.menuAccent());
		config.setHudBackground(0xFF0B0C10);
		assertEquals(0x0B0C10, config.hudBackground());
	}

	@Test
	void colorsRoundTripThroughHex() {
		assertEquals("#E3A44C", SantoraConfig.formatColor(0xE3A44C));
		assertEquals("#0B0C10", SantoraConfig.formatColor(0x0B0C10));

		assertEquals(0xE3A44C, SantoraConfig.parseColor("#E3A44C", 0));
		assertEquals(0xE3A44C, SantoraConfig.parseColor("e3a44c", 0));
		assertEquals(0x111111, SantoraConfig.parseColor("nope", 0x111111));
		assertEquals(0x111111, SantoraConfig.parseColor("#12345", 0x111111));
		assertEquals(0x111111, SantoraConfig.parseColor(null, 0x111111));
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
