package dev.santora.core.update;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModVersionTest {

	@Test
	void readsAPlainVersion() {
		assertEquals("1.13.1", ModVersion.parse("1.13.1").orElseThrow().toString());
	}

	@Test
	void ignoresTheReleaseTagPrefix() {
		assertEquals(ModVersion.parse("1.13.1"), ModVersion.parse("v1.13.1"));
	}

	@Test
	void ignoresBuildMetadata() {
		assertEquals(ModVersion.parse("1.13.1"), ModVersion.parse("1.13.1+mc1.21.11"));
	}

	@Test
	void missingPartsCountAsZero() {
		assertEquals(ModVersion.parse("1.13.0"), ModVersion.parse("1.13"));
	}

	@Test
	void comparesEachNumberInTurn() {
		assertTrue(ModVersion.isNewer("1.14.0", "1.13.1"));
		assertTrue(ModVersion.isNewer("1.13.2", "1.13.1"));
		assertTrue(ModVersion.isNewer("2.0.0", "1.99.99"));
		assertFalse(ModVersion.isNewer("1.13.1", "1.13.1"));
		assertFalse(ModVersion.isNewer("1.9.0", "1.13.1"));
	}

	@Test
	void doubleDigitsBeatSingleDigits() {
		assertTrue(ModVersion.isNewer("1.10.0", "1.9.0"));
	}

	@Test
	void aPreReleaseSitsBelowTheRelease() {
		assertTrue(ModVersion.isNewer("1.14.0", "1.14.0-beta.1"));
		assertFalse(ModVersion.isNewer("1.14.0-beta.1", "1.14.0"));
		assertTrue(ModVersion.parse("1.14.0-beta.1").orElseThrow().isPreRelease());
	}

	@Test
	void preReleasesCompareInOrder() {
		assertTrue(ModVersion.isNewer("1.14.0-beta.2", "1.14.0-beta.1"));
		assertTrue(ModVersion.isNewer("1.14.0-beta", "1.14.0-alpha"));
		assertTrue(ModVersion.isNewer("1.14.0-beta.1", "1.14.0-beta"));
	}

	@Test
	void nonsenseIsNotAVersion() {
		assertEquals(Optional.empty(), ModVersion.parse(null));
		assertEquals(Optional.empty(), ModVersion.parse(""));
		assertEquals(Optional.empty(), ModVersion.parse("latest"));
		assertFalse(ModVersion.isNewer("latest", "1.13.1"));
		assertFalse(ModVersion.isNewer("1.14.0", "${version}"));
	}
}
