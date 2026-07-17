package dev.santora.ui;

import dev.santora.core.model.MusicUpdate;
import dev.santora.core.model.Track;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class AlbumArt {

	private record Ref(Identifier texture, float u0, float u1, float v0, float v1) {
	}

	private static final Ref ABSENT = new Ref(null, 0, 0, 0, 0);
	private static final Map<String, Ref> CACHE = new HashMap<>();

	private AlbumArt() {
	}

	public static boolean draw(SantoraCanvas canvas, String artKey, int x, int y, int w, int h) {
		Ref art = resolve(artKey);
		if (art == null) {
			return false;
		}
		int size = Math.min(w, h);
		canvas.fill(x, y, x + w, y + h, Theme.INPUT_BG);
		canvas.blit(art.texture(), x + (w - size) / 2, y + (h - size) / 2, size, size,
				art.u0(), art.u1(), art.v0(), art.v1());
		return true;
	}

	public static void drawTrack(SantoraCanvas canvas, Track track, int x, int y, int size) {
		if (draw(canvas, MusicUpdate.of(track.soundPath()).artKey(), x, y, size, size)) {
			return;
		}
		int base = Theme.artColorFor(track.soundPath());
		canvas.fillGradient(x, y, x + size, y + size, base, Theme.blend(base, 0xFF000000, 0.45f));
	}

	private static Ref resolve(String artKey) {
		if (artKey == null) {
			return null;
		}
		Ref cached = CACHE.computeIfAbsent(artKey, key -> {
			Identifier tex = Identifier.tryParse(key);
			if (tex == null) {
				return ABSENT;
			}
			Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(tex);
			if (resource.isEmpty()) {
				return ABSENT;
			}
			float aspect = pngAspect(resource.get());
			float uSpan = aspect > 1 ? 1f / aspect : 1f;
			float vSpan = aspect < 1 ? aspect : 1f;
			float u0 = (1f - uSpan) / 2f;
			float v0 = (1f - vSpan) / 2f;
			return new Ref(tex, u0, u0 + uSpan, v0, v0 + vSpan);
		});
		return cached == ABSENT ? null : cached;
	}

	private static float pngAspect(Resource resource) {
		try (InputStream in = resource.open()) {
			byte[] head = in.readNBytes(24);
			if (head.length < 24 || head[12] != 'I' || head[13] != 'H' || head[14] != 'D' || head[15] != 'R') {
				return 1f;
			}
			int wPx = ((head[16] & 0xFF) << 24) | ((head[17] & 0xFF) << 16) | ((head[18] & 0xFF) << 8) | (head[19] & 0xFF);
			int hPx = ((head[20] & 0xFF) << 24) | ((head[21] & 0xFF) << 16) | ((head[22] & 0xFF) << 8) | (head[23] & 0xFF);
			return wPx > 0 && hPx > 0 ? (float) wPx / hPx : 1f;
		} catch (IOException e) {
			return 1f;
		}
	}
}
