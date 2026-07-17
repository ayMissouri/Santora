package dev.santora.core.play;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FadeTest {

	@Test
	void endpointsAreExact() {
		assertEquals(1.0f, Fade.outgoing(0f), 1e-6);
		assertEquals(0.0f, Fade.outgoing(1f), 1e-6);
		assertEquals(0.0f, Fade.incoming(0f), 1e-6);
		assertEquals(1.0f, Fade.incoming(1f), 1e-6);
	}

	@Test
	void powerStaysConstantAcrossTheFade() {
		for (float t = 0f; t <= 1f; t += 0.05f) {
			float out = Fade.outgoing(t);
			float in = Fade.incoming(t);
			assertEquals(1.0f, out * out + in * in, 1e-5, "power dipped at t=" + t);
		}
	}

	@Test
	void progressClampsAndHandlesZeroDuration() {
		assertEquals(0.5f, Fade.progress(500, 1000), 1e-6);
		assertEquals(1.0f, Fade.progress(5000, 1000), 1e-6, "overshoot must clamp, not exceed 1");
		assertEquals(0.0f, Fade.progress(-100, 1000), 1e-6);
		assertEquals(1.0f, Fade.progress(0, 0), 1e-6, "zero duration means an instant hard cut");
	}

	@Test
	void gainsAreMonotonic() {
		float prevOut = Float.MAX_VALUE;
		float prevIn = -1f;
		for (float t = 0f; t <= 1f; t += 0.05f) {
			assertTrue(Fade.outgoing(t) <= prevOut + 1e-6, "outgoing must never rise");
			assertTrue(Fade.incoming(t) >= prevIn - 1e-6, "incoming must never fall");
			prevOut = Fade.outgoing(t);
			prevIn = Fade.incoming(t);
		}
	}
}
