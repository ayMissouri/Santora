package dev.santora.ui;

import dev.santora.Santora;
import dev.santora.core.model.Album;
import dev.santora.core.model.AlbumKind;
import dev.santora.core.model.MusicContext;
import dev.santora.core.model.MusicLibrary;
import dev.santora.core.model.Playlists;
import dev.santora.core.model.Track;
import dev.santora.core.play.RepeatMode;
import dev.santora.engine.MusicEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

public final class SantoraUi {

	private static final int MAX_WIDTH = 460;
	private static final int MAX_HEIGHT = 260;

	private final MusicEngine engine = MusicEngine.get();

	private static final Identifier ABSENT = Identifier.fromNamespaceAndPath("santora", "absent");

	private final Map<String, Identifier> artCache = new HashMap<>();

	private enum View {
		ALBUMS("Albums"),
		ARTISTS("Artists"),
		UPDATES("Updates"),
		PLAYLISTS("Playlists"),
		QUEUE("Queue");

		final String label;

		View(String label) {
			this.label = label;
		}
	}

	private static View view = View.ALBUMS;
	private static View queueReturnView = View.ALBUMS;
	private static String openAlbumId = "";

	private static int gridScroll;
	private static int trackScroll;

	private static final int DRAG_THRESHOLD = 4;
	private static final int DRAG_EDGE = 14;
	private static final int DRAG_SCROLL_STEP = 3;

	private static final int MENU_WIDTH = 120;
	private static final int MENU_ITEM_HEIGHT = 13;
	private static final int MENU_PAD = 3;
	private static final int NAME_INPUT_MAX = 24;

	private int dragIndex = -1;
	private int dragArmY;
	private boolean dragging;

	private record MenuItem(String label, Runnable action) {
	}

	private List<MenuItem> menuItems;
	private int menuX;
	private int menuY;
	private int menuDrawX;
	private int menuDrawY;

	private boolean naming;
	private final StringBuilder nameInput = new StringBuilder();
	private String namingTrackPath;

	private int winX;
	private int winY;
	private int winW;
	private int winH;

	private boolean closeFlag;

	private record Rect(int x, int y, int w, int h) {

		int right() {
			return x + w;
		}

		int bottom() {
			return y + h;
		}

		boolean contains(int px, int py) {
			return contains(px, py, 0);
		}

		boolean contains(int px, int py, int grow) {
			return px >= x - grow && px < x + w + grow && py >= y - grow && py < y + h + grow;
		}
	}

	private int contentTop() {
		return winY + Theme.TOP_BAR_HEIGHT;
	}

	private int contentBottom() {
		return winY + winH - Theme.DECK_HEIGHT;
	}

	private Rect railRect() {
		return new Rect(winX, contentTop(), Theme.RAIL_WIDTH, contentBottom() - contentTop());
	}

	private Rect mainRect() {
		return new Rect(winX + Theme.RAIL_WIDTH, contentTop(),
				winW - Theme.RAIL_WIDTH, contentBottom() - contentTop());
	}

	private Rect deckRect() {
		return new Rect(winX, contentBottom(), winW, Theme.DECK_HEIGHT);
	}

	private Rect closeRect() {
		return new Rect(winX + winW - 18, winY + 6, 12, 12);
	}

	private Rect menuRect(int index) {
		return new Rect(winX, contentTop() + 8 + index * (Theme.MENU_ROW_HEIGHT + 2),
				Theme.RAIL_WIDTH - 1, Theme.MENU_ROW_HEIGHT);
	}

	private int gridCols() {
		int usable = mainRect().w() - Theme.PADDING * 2;
		return Math.max(2, (usable + Theme.GRID_GAP) / (Theme.TILE_TARGET_WIDTH + Theme.GRID_GAP));
	}

	private Rect tileRect(int index) {
		Rect main = mainRect();
		int cols = gridCols();
		int usable = main.w() - Theme.PADDING * 2;
		int tileW = (usable - (cols - 1) * Theme.GRID_GAP) / cols;
		int col = index % cols;
		int row = index / cols;
		return new Rect(
				main.x() + Theme.PADDING + col * (tileW + Theme.GRID_GAP),
				main.y() + Theme.PADDING + row * (Theme.TILE_HEIGHT + Theme.GRID_GAP) - gridScroll,
				tileW, Theme.TILE_HEIGHT);
	}

	private int gridContentHeight(int count) {
		int cols = gridCols();
		int rows = (count + cols - 1) / cols;
		return rows == 0 ? 0
				: Theme.PADDING * 2 + rows * Theme.TILE_HEIGHT + (rows - 1) * Theme.GRID_GAP;
	}

	private Rect backRect() {
		Rect main = mainRect();
		return new Rect(main.x() + Theme.PADDING, main.y() + 5, 64, 13);
	}

	private Rect headerArtRect() {
		Rect main = mainRect();
		return new Rect(main.x() + Theme.PADDING, main.y() + 22, Theme.ART_SIZE, Theme.ART_SIZE);
	}

	private Rect playButtonRect() {
		Rect art = headerArtRect();
		return new Rect(art.right() + Theme.PADDING, art.bottom() - 14, 44, 14);
	}

	private Rect shuffleButtonRect() {
		Rect play = playButtonRect();
		return new Rect(play.right() + 6, play.y(), 54, 14);
	}

	private int listTop() {
		return view == View.QUEUE ? mainRect().y() + 24 : headerArtRect().bottom() + 8;
	}

	private Rect listRect() {
		Rect main = mainRect();
		int top = listTop();
		return new Rect(main.x(), top, main.w(), main.bottom() - top);
	}

	private int deckCenterY() {
		return deckRect().y() + Theme.PROGRESS_STRIP_HEIGHT
				+ (Theme.DECK_HEIGHT - Theme.PROGRESS_STRIP_HEIGHT) / 2;
	}

	private Rect deckNextRect() {
		Rect deck = deckRect();
		return new Rect(deck.right() - 12 - 9, deckCenterY() - 5, 9, 9);
	}

	private Rect deckPlayRect() {
		Rect next = deckNextRect();
		return new Rect(next.x() - 9 - 15, deckCenterY() - 8, 15, 15);
	}

	private Rect deckPrevRect() {
		Rect play = deckPlayRect();
		return new Rect(play.x() - 9 - 9, deckCenterY() - 5, 9, 9);
	}

	private Rect deckRepeatRect() {
		Rect prev = deckPrevRect();
		return new Rect(prev.x() - 12 - 26, deckCenterY() - 6, 26, 11);
	}

	private Rect deckShuffleRect() {
		Rect repeat = deckRepeatRect();
		return new Rect(repeat.x() - 4 - 28, deckCenterY() - 6, 28, 11);
	}

	public void render(SantoraCanvas canvas, int mouseX, int mouseY) {
		winW = Math.min(canvas.width() - 20, MAX_WIDTH);
		winH = Math.min(canvas.height() - 20, MAX_HEIGHT);
		winX = (canvas.width() - winW) / 2;
		winY = (canvas.height() - winH) / 2;

		canvas.fill(0, 0, canvas.width(), canvas.height(), Theme.SCRIM);
		canvas.fill(winX, winY, winX + winW, winY + winH, Theme.WINDOW);

		renderTopBar(canvas, mouseX, mouseY);
		renderRail(canvas, mouseX, mouseY);
		renderMain(canvas, mouseX, mouseY);
		renderDeck(canvas, mouseX, mouseY);

		canvas.outline(winX, winY, winW, winH, Theme.FRAME);

		renderContextMenu(canvas, mouseX, mouseY);
		renderNamingModal(canvas);
	}

	// Top bar
	private void renderTopBar(SantoraCanvas canvas, int mouseX, int mouseY) {
		int h = Theme.TOP_BAR_HEIGHT;
		canvas.fill(winX, winY, winX + winW, winY + h, Theme.TOP_BAR);
		canvas.fill(winX, winY + h - 1, winX + winW, winY + h, Theme.DIVIDER);

		drawNoteIcon(canvas, winX + 10, winY + 7, Theme.ACCENT);
		canvas.text("SANTORA", winX + 24, winY + (h - canvas.lineHeight()) / 2 + 1,
				Theme.TEXT_PRIMARY, false);

		Rect close = closeRect();
		boolean hover = close.contains(mouseX, mouseY, 2);
		canvas.textCentered("x", close.x() + close.w() / 2, close.y() + 2,
				hover ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);
	}

	private void drawNoteIcon(SantoraCanvas canvas, int x, int y, int color) {
		canvas.fill(x, y + 6, x + 3, y + 9, color);
		canvas.fill(x + 6, y + 5, x + 9, y + 8, color);
		canvas.fill(x + 2, y + 1, x + 3, y + 7, color);
		canvas.fill(x + 8, y, x + 9, y + 6, color);
		canvas.fill(x + 2, y, x + 9, y + 2, color);
	}

	// Left menu
	private void renderRail(SantoraCanvas canvas, int mouseX, int mouseY) {
		Rect rail = railRect();
		canvas.fill(rail.x(), rail.y(), rail.right(), rail.bottom(), Theme.RAIL);
		canvas.fill(rail.right() - 1, rail.y(), rail.right(), rail.bottom(), Theme.DIVIDER);

		View[] views = View.values();
		for (int i = 0; i < views.length; i++) {
			Rect row = menuRect(i);
			boolean selected = view == views[i];
			boolean hover = row.contains(mouseX, mouseY);

			if (selected) {
				canvas.fill(row.x(), row.y(), row.right(), row.bottom(), Theme.MENU_SELECTED);
				canvas.fill(row.x(), row.y(), row.x() + 2, row.bottom(), Theme.ACCENT);
			} else if (hover) {
				canvas.fill(row.x(), row.y(), row.right(), row.bottom(), Theme.ROW_HOVER);
			}

			int textY = row.y() + (row.h() - canvas.lineHeight()) / 2 + 1;
			canvas.text(views[i].label, row.x() + 12, textY,
					selected ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY, false);

			if (views[i] == View.QUEUE) {
				int queued = engine.queue().upcomingCount();
				if (queued > 0) {
					String count = String.valueOf(queued);
					canvas.text(count, row.right() - 6 - canvas.textWidth(count), textY,
							Theme.TEXT_MUTED, false);
				}
			}
		}

		renderRailStatus(canvas, rail);
	}

	private void renderRailStatus(SantoraCanvas canvas, Rect rail) {
		int y = rail.bottom() - 26;
		canvas.fill(rail.x() + 8, y - 5, rail.right() - 8, y - 4, Theme.DIVIDER);

		boolean manual = engine.isManualMode();
		int color = manual ? Theme.ACCENT : Theme.TEXT_MUTED;
		canvas.fill(rail.x() + 10, y + 2, rail.x() + 14, y + 6, color);
		canvas.text(manual ? "MANUAL" : "VANILLA", rail.x() + 18, y, color, false);

		canvas.text(engine.library().size() + " tracks", rail.x() + 10, y + 12,
				Theme.TEXT_MUTED, false);
	}

	// Main area
	private Album openAlbum() {
		return openAlbumId.isEmpty() ? null
				: engine.albumById(openAlbumId).orElse(null);
	}

	private List<Album> browseAlbums() {
		MusicLibrary library = engine.library();
		return switch (view) {
			case ALBUMS -> library.contextAlbums();
			case ARTISTS -> library.artistAlbums();
			case UPDATES -> library.updateAlbums();
			case PLAYLISTS -> engine.playlistAlbums();
			case QUEUE -> List.of();
		};
	}

	private int gridTileCount(List<Album> albums) {
		return albums.size() + (view == View.PLAYLISTS ? 1 : 0);
	}

	private List<Track> visibleTracks() {
		if (view == View.QUEUE) {
			List<Track> rows = new ArrayList<>();
			Track current = engine.currentTrack();
			if (current != null) {
				rows.add(current);
			}
			rows.addAll(engine.queue().upcoming(64));
			return rows;
		}
		Album album = openAlbum();
		return album == null ? List.of() : album.tracks();
	}

	private void renderMain(SantoraCanvas canvas, int mouseX, int mouseY) {
		Rect main = mainRect();

		if (engine.library().isEmpty()) {
			canvas.textCentered("Indexing music...", main.x() + main.w() / 2,
					main.y() + main.h() / 2 - 4, Theme.TEXT_MUTED);
			return;
		}

		if (view == View.QUEUE) {
			renderQueueHeader(canvas, main);
			renderTrackList(canvas, mouseX, mouseY);
		} else if (openAlbum() == null) {
			renderGrid(canvas, mouseX, mouseY);
		} else {
			renderAlbumDetail(canvas, mouseX, mouseY);
			renderTrackList(canvas, mouseX, mouseY);
		}
	}

	// Album grid
	private void renderGrid(SantoraCanvas canvas, int mouseX, int mouseY) {
		Rect main = mainRect();
		List<Album> albums = browseAlbums();
		int tileCount = gridTileCount(albums);

		if (tileCount == 0) {
			canvas.textCentered("No albums here", main.x() + main.w() / 2,
					main.y() + main.h() / 2 - 4, Theme.TEXT_MUTED);
			return;
		}

		gridScroll = clamp(gridScroll, 0, Math.max(0, gridContentHeight(tileCount) - main.h()));

		canvas.pushScissor(main.x(), main.y(), main.w(), main.h());
		for (int i = 0; i < albums.size(); i++) {
			Rect tile = tileRect(i);
			if (tile.bottom() < main.y() || tile.y() > main.bottom()) {
				continue;
			}
			Album album = albums.get(i);
			boolean hover = tile.contains(mouseX, mouseY) && main.contains(mouseX, mouseY);

			drawAlbumArt(canvas, album, tile.x(), tile.y(), tile.w(), Theme.TILE_ART_HEIGHT);
			if (hover) {
				canvas.fill(tile.x(), tile.y(), tile.right(), tile.y() + Theme.TILE_ART_HEIGHT,
						Theme.ROW_HOVER);
				canvas.outline(tile.x(), tile.y(), tile.w(), Theme.TILE_ART_HEIGHT, Theme.ACCENT);
			}

			canvas.text(canvas.ellipsize(album.title(), tile.w() - 2),
					tile.x() + 1, tile.y() + Theme.TILE_ART_HEIGHT + 4,
					hover ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY, false);
			canvas.text(album.trackCount() + (album.trackCount() == 1 ? " track" : " tracks"),
					tile.x() + 1, tile.y() + Theme.TILE_ART_HEIGHT + 14, Theme.TEXT_MUTED, false);
		}
		if (view == View.PLAYLISTS) {
			renderNewPlaylistTile(canvas, albums.size(), mouseX, mouseY);
		}
		canvas.popScissor();
	}

	private void renderNewPlaylistTile(SantoraCanvas canvas, int index, int mouseX, int mouseY) {
		Rect main = mainRect();
		Rect tile = tileRect(index);
		if (tile.bottom() < main.y() || tile.y() > main.bottom()) {
			return;
		}
		boolean hover = tile.contains(mouseX, mouseY) && main.contains(mouseX, mouseY);

		canvas.fill(tile.x(), tile.y(), tile.right(), tile.y() + Theme.TILE_ART_HEIGHT, 0xFF10141F);
		canvas.outline(tile.x(), tile.y(), tile.w(), Theme.TILE_ART_HEIGHT,
				hover ? Theme.ACCENT : Theme.DIVIDER);
		canvas.textCentered("+", tile.x() + tile.w() / 2,
				tile.y() + Theme.TILE_ART_HEIGHT / 2 - canvas.lineHeight() / 2,
				hover ? Theme.ACCENT : Theme.TEXT_SECONDARY);
		canvas.text("New Playlist", tile.x() + 1, tile.y() + Theme.TILE_ART_HEIGHT + 4,
				hover ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY, false);
	}

	// Album detail
	private void renderAlbumDetail(SantoraCanvas canvas, int mouseX, int mouseY) {
		Album album = openAlbum();
		Rect main = mainRect();
		Rect back = backRect();

		boolean backHover = back.contains(mouseX, mouseY, 2);
		canvas.text("< " + view.label, back.x(), back.y() + 2,
				backHover ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY, false);

		Rect art = headerArtRect();
		drawAlbumArt(canvas, album, art.x(), art.y(), art.w(), art.h());

		int textX = art.right() + Theme.PADDING;
		int textMax = main.right() - Theme.PADDING - textX;
		canvas.text(canvas.ellipsize(album.title(), textMax), textX, art.y() + 2,
				Theme.TEXT_PRIMARY, false);
		canvas.text(canvas.ellipsize(album.subtitle(), textMax), textX,
				art.y() + 2 + canvas.lineHeight() + 3, Theme.TEXT_SECONDARY, false);

		drawButton(canvas, mouseX, mouseY, playButtonRect(), "Play", true);
		drawButton(canvas, mouseX, mouseY, shuffleButtonRect(), "Shuffle", false);
	}

	private void renderQueueHeader(SantoraCanvas canvas, Rect main) {
		canvas.text("Queue", main.x() + Theme.PADDING, main.y() + 7, Theme.TEXT_PRIMARY, false);
		String queued = engine.queue().upcomingCount() + " up next";
		canvas.text(queued, main.right() - Theme.PADDING - canvas.textWidth(queued),
				main.y() + 7, Theme.TEXT_SECONDARY, false);
	}

	// Song list
	private void renderTrackList(SantoraCanvas canvas, int mouseX, int mouseY) {
		if (dragging) {
			tickDragScroll(mouseY);
		}

		Rect list = listRect();
		List<Track> tracks = visibleTracks();
		Track playing = engine.currentTrack();

		trackScroll = clamp(trackScroll, 0, Math.max(0, tracks.size() * Theme.ROW_HEIGHT - list.h()));

		int queueBase = queueRowOffset();

		canvas.pushScissor(list.x(), list.y(), list.w(), list.h());
		int rowY = list.y() - trackScroll;
		int index = 1;

		for (Track track : tracks) {
			int rowH = Theme.ROW_HEIGHT;
			if (rowY + rowH >= list.y() && rowY <= list.bottom()) {
				boolean isPlaying = playing != null && playing.soundPath().equals(track.soundPath());
				boolean hover = list.contains(mouseX, mouseY)
						&& mouseY >= rowY && mouseY < rowY + rowH;
				boolean reorderable = view == View.QUEUE && index - 1 >= queueBase;
				boolean dragged = dragging && reorderable && index - 1 - queueBase == dragIndex;

				if (dragged) {
					canvas.fill(list.x(), rowY, list.right(), rowY + rowH, Theme.ROW_SELECTED);
					canvas.fill(list.x(), rowY, list.x() + 2, rowY + rowH, Theme.ACCENT);
				} else if (hover) {
					canvas.fill(list.x(), rowY, list.right(), rowY + rowH, Theme.ROW_HOVER);
				}

				int textY = rowY + (rowH - canvas.lineHeight()) / 2;

				if (isPlaying) {
					drawEqBars(canvas, list.x() + Theme.PADDING, rowY + rowH / 2 + 4, Theme.ACCENT);
				} else if (reorderable && (hover || dragged)) {
					drawGrip(canvas, list.x() + Theme.PADDING, rowY + rowH / 2,
							dragged ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);
				} else {
					canvas.text(String.valueOf(index), list.x() + Theme.PADDING, textY,
							Theme.TEXT_MUTED, false);
				}

				int heartX = list.right() - Theme.PADDING - 46;
				int titleX = list.x() + Theme.PADDING + 16;
				int titleMax = heartX - 4 - titleX;

				canvas.text(canvas.ellipsize(track.title(), titleMax), titleX, textY - 4,
						isPlaying ? Theme.ACCENT : Theme.TEXT_PRIMARY, false);
				canvas.text(canvas.ellipsize(track.artist(), titleMax), titleX, textY + 5,
						Theme.TEXT_MUTED, false);

				if (engine.playlists().isFavorite(track.soundPath())) {
					drawHeart(canvas, heartX, rowY + rowH / 2 - 3, 1, Theme.ACCENT);
				}

				OptionalDouble seconds = engine.durationSeconds(track);
				String time = seconds.isPresent()
						? Theme.formatTime((long) (seconds.getAsDouble() * 1000))
						: "--:--";
				canvas.text(time, list.right() - Theme.PADDING - canvas.textWidth(time), textY,
						Theme.TEXT_MUTED, false);
			}
			rowY += Theme.ROW_HEIGHT;
			index++;
		}

		if (tracks.isEmpty()) {
			canvas.textCentered(view == View.QUEUE ? "Queue is empty" : "No tracks",
					list.x() + list.w() / 2, list.y() + 20, Theme.TEXT_MUTED);
		}
		canvas.popScissor();
	}

	private void drawGrip(SantoraCanvas canvas, int x, int cy, int color) {
		for (int i = -1; i <= 1; i++) {
			canvas.fill(x, cy + i * 3 - 1, x + 9, cy + i * 3, color);
		}
	}

	private void drawHeart(SantoraCanvas canvas, int x, int y, int s, int color) {
		canvas.fill(x + s, y, x + 3 * s, y + s, color);
		canvas.fill(x + 4 * s, y, x + 6 * s, y + s, color);
		canvas.fill(x, y + s, x + 7 * s, y + 3 * s, color);
		canvas.fill(x + s, y + 3 * s, x + 6 * s, y + 4 * s, color);
		canvas.fill(x + 2 * s, y + 4 * s, x + 5 * s, y + 5 * s, color);
		canvas.fill(x + 3 * s, y + 5 * s, x + 4 * s, y + 6 * s, color);
	}

	private void drawEqBars(SantoraCanvas canvas, int x, int bottomY, int color) {
		long t = engine.elapsedMillis() / 120;
		for (int i = 0; i < 3; i++) {
			int phase = (int) ((t + i * 2) % 6);
			int h = 2 + (phase < 3 ? phase : 6 - phase);
			int barX = x + i * 3;
			canvas.fill(barX, bottomY - h, barX + 2, bottomY, color);
		}
	}

	// Album art
	private void drawAlbumArt(SantoraCanvas canvas, Album album, int x, int y, int w, int h) {
		if (album == null) {
			canvas.fill(x, y, x + w, y + h, 0xFF1B2130);
			return;
		}

		Identifier art = artTexture(album);
		if (art != null) {
			int size = Math.min(w, h);
			canvas.fill(x, y, x + w, y + h, 0xFF0C0F17);
			canvas.blit(art, x + (w - size) / 2, y + (h - size) / 2, size, size);
			return;
		}

		boolean favorites = album.id().equals(Playlists.FAVORITES_ID);
		int base = album.kind() == AlbumKind.CONTEXT
				? Theme.artColor(contextOf(album))
				: favorites ? 0xFF8C3A55 : Theme.artColorFor(album.id());
		canvas.fillGradient(x, y, x + w, y + h, base, Theme.blend(base, 0xFF000000, 0.45f));
		canvas.outline(x, y, w, h, 0x33FFFFFF);

		if (favorites) {
			int s = Math.max(1, Math.min(w, h) / 16);
			drawHeart(canvas, x + (w - 7 * s) / 2, y + (h - 6 * s) / 2, s, 0xAAFFFFFF);
			return;
		}
		String initials = album.title().isEmpty() ? "?" : album.title().substring(0, 1).toUpperCase();
		canvas.textCentered(initials, x + w / 2, y + h / 2 - canvas.lineHeight() / 2, 0x66FFFFFF);
	}

	private Identifier artTexture(Album album) {
		if (album.artKey() == null) {
			return null;
		}
		Identifier cached = artCache.computeIfAbsent(album.id(), id -> {
			Identifier tex = Identifier.tryParse(album.artKey());
			if (tex == null) {
				return ABSENT;
			}
			return Minecraft.getInstance().getResourceManager().getResource(tex).isPresent() ? tex : ABSENT;
		});
		return cached == ABSENT ? null : cached;
	}

	private MusicContext contextOf(Album album) {
		for (MusicContext c : MusicContext.values()) {
			if (album.id().equals("context:" + c.id())) {
				return c;
			}
		}
		return MusicContext.OTHER;
	}

	// Playback bar
	private void renderDeck(SantoraCanvas canvas, int mouseX, int mouseY) {
		Rect deck = deckRect();
		Track track = engine.currentTrack();

		canvas.fill(deck.x(), deck.y() + Theme.PROGRESS_STRIP_HEIGHT, deck.right(), deck.bottom(),
				Theme.DECK);
		renderProgressStrip(canvas, deck, track);

		int artSize = 26;
		int artY = deck.y() + Theme.PROGRESS_STRIP_HEIGHT + 7;
		if (track != null) {
			int base = Theme.artColorFor(track.soundPath());
			canvas.fillGradient(deck.x() + 8, artY, deck.x() + 8 + artSize, artY + artSize,
					base, Theme.blend(base, 0xFF000000, 0.45f));
		} else {
			canvas.fill(deck.x() + 8, artY, deck.x() + 8 + artSize, artY + artSize, 0xFF1B2130);
		}

		int textX = deck.x() + 8 + artSize + 6;
		int textMax = Math.max(40, deckShuffleRect().x() - 64 - textX);
		canvas.text(track == null ? "Nothing playing" : canvas.ellipsize(track.title(), textMax),
				textX, deck.y() + 11, Theme.TEXT_PRIMARY, false);
		canvas.text(track == null ? "" : canvas.ellipsize(track.artist(), textMax),
				textX, deck.y() + 22, Theme.TEXT_SECONDARY, false);

		int cy = deckCenterY();

		Rect next = deckNextRect();
		drawTriangleRight(canvas, next.x(), cy, 9, transportColor(next, mouseX, mouseY, true));
		canvas.fill(next.x() + 6, cy - 4, next.x() + 8, cy + 5,
				transportColor(next, mouseX, mouseY, true));

		Rect play = deckPlayRect();
		boolean playHover = play.contains(mouseX, mouseY, 2);
		canvas.fill(play.x(), play.y(), play.right(), play.bottom(),
				playHover ? Theme.blend(Theme.ACCENT, 0xFFFFFFFF, 0.15f) : Theme.ACCENT);
		if (engine.isPaused() || track == null) {
			drawTriangleRight(canvas, play.x() + 6, cy, 7, Theme.ON_ACCENT);
		} else {
			canvas.fill(play.x() + 4, cy - 3, play.x() + 6, cy + 4, Theme.ON_ACCENT);
			canvas.fill(play.x() + 9, cy - 3, play.x() + 11, cy + 4, Theme.ON_ACCENT);
		}

		Rect prev = deckPrevRect();
		boolean prevEnabled = engine.queue().hasPrevious();
		canvas.fill(prev.x() + 1, cy - 4, prev.x() + 3, cy + 5,
				transportColor(prev, mouseX, mouseY, prevEnabled));
		drawTriangleLeft(canvas, prev.x() + 4, cy, 9, transportColor(prev, mouseX, mouseY, prevEnabled));

		drawToggle(canvas, mouseX, mouseY, deckRepeatRect(),
				engine.queue().repeat() == RepeatMode.ONE ? "RPT1" : "RPT",
				engine.queue().repeat() != RepeatMode.OFF);
		drawToggle(canvas, mouseX, mouseY, deckShuffleRect(), "SHUF", engine.queue().shuffle());

		if (track != null) {
			OptionalDouble duration = engine.durationSeconds(track);
			String total = duration.isPresent()
					? Theme.formatTime((long) (duration.getAsDouble() * 1000))
					: "--:--";
			String time = Theme.formatTime(engine.elapsedMillis()) + " / " + total;
			canvas.text(time, deckShuffleRect().x() - 8 - canvas.textWidth(time), cy - 4,
					Theme.TEXT_MUTED, false);
		}
	}

	private void renderProgressStrip(SantoraCanvas canvas, Rect deck, Track track) {
		int y = deck.y();
		canvas.fill(deck.x(), y, deck.right(), y + Theme.PROGRESS_STRIP_HEIGHT, Theme.PROGRESS_TRACK);

		if (track == null) {
			return;
		}
		OptionalDouble duration = engine.durationSeconds(track);
		if (duration.isPresent() && duration.getAsDouble() > 0) {
			float pct = (float) (engine.elapsedMillis() / (duration.getAsDouble() * 1000.0));
			pct = pct < 0 ? 0 : (pct > 1 ? 1 : pct);
			canvas.fill(deck.x(), y, deck.x() + (int) (deck.w() * pct),
					y + Theme.PROGRESS_STRIP_HEIGHT, Theme.ACCENT);
		}
	}

	private int transportColor(Rect rect, int mouseX, int mouseY, boolean enabled) {
		if (!enabled) {
			return Theme.TEXT_MUTED;
		}
		return rect.contains(mouseX, mouseY, 3) ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;
	}

	// Playback buttons
	private void drawButton(SantoraCanvas canvas, int mouseX, int mouseY, Rect rect,
			String label, boolean primary) {
		boolean hover = rect.contains(mouseX, mouseY);
		int bg = primary
				? (hover ? Theme.blend(Theme.ACCENT, 0xFFFFFFFF, 0.15f) : Theme.ACCENT)
				: (hover ? Theme.ROW_SELECTED : Theme.ROW_HOVER);
		canvas.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), bg);
		canvas.textCentered(label, rect.x() + rect.w() / 2,
				rect.y() + (rect.h() - canvas.lineHeight()) / 2 + 1,
				primary ? Theme.ON_ACCENT : Theme.TEXT_PRIMARY);
	}

	private void drawToggle(SantoraCanvas canvas, int mouseX, int mouseY, Rect rect,
			String label, boolean on) {
		boolean hover = rect.contains(mouseX, mouseY, 2);
		int color = on ? Theme.ACCENT : (hover ? Theme.TEXT_SECONDARY : Theme.TEXT_MUTED);
		canvas.textCentered(label, rect.x() + rect.w() / 2, rect.y() + 1, color);
	}

	private void drawTriangleRight(SantoraCanvas canvas, int x, int cy, int h, int color) {
		int w = (h + 1) / 2;
		for (int i = 0; i < w; i++) {
			int half = (h - 1) / 2 - i;
			canvas.fill(x + i, cy - half, x + i + 1, cy + half + 1, color);
		}
	}

	private void drawTriangleLeft(SantoraCanvas canvas, int x, int cy, int h, int color) {
		int w = (h + 1) / 2;
		for (int i = 0; i < w; i++) {
			canvas.fill(x + i, cy - i, x + i + 1, cy + i + 1, color);
		}
	}

	public boolean mouseClicked(int mouseX, int mouseY, int button) {
		if (naming) {
			if (!namingRect().contains(mouseX, mouseY)) {
				cancelNaming();
			}
			return true;
		}

		if (menuItems != null) {
			if (menuRect().contains(mouseX, mouseY)) {
				if (button == 0) {
					runMenuItem(mouseY);
				}
				return true;
			}
			closeMenu();
			if (button != 1) {
				return new Rect(winX, winY, winW, winH).contains(mouseX, mouseY);
			}
		}

		if (button == 1) {
			return openContextMenu(mouseX, mouseY);
		}

		if (button != 0) {
			return false;
		}

		if (closeRect().contains(mouseX, mouseY, 2)) {
			closeFlag = true;
			return true;
		}

		if (clickRail(mouseX, mouseY) || clickMain(mouseX, mouseY) || clickDeck(mouseX, mouseY)) {
			return true;
		}

		return new Rect(winX, winY, winW, winH).contains(mouseX, mouseY);
	}

	// Right-click context menu
	private Rect menuRect() {
		return new Rect(menuDrawX, menuDrawY, MENU_WIDTH,
				menuItems.size() * MENU_ITEM_HEIGHT + MENU_PAD * 2);
	}

	private void showMenu(List<MenuItem> items, int x, int y) {
		menuItems = items;
		menuX = x;
		menuY = y;
		menuDrawX = x;
		menuDrawY = y;
	}

	private void closeMenu() {
		menuItems = null;
	}

	private void runMenuItem(int mouseY) {
		int offset = mouseY - menuDrawY - MENU_PAD;
		int index = offset / MENU_ITEM_HEIGHT;
		if (offset < 0 || index >= menuItems.size()) {
			return;
		}
		List<MenuItem> before = menuItems;
		menuItems.get(index).action().run();
		if (menuItems == before) {
			closeMenu();
		}
	}

	private boolean openContextMenu(int mouseX, int mouseY) {
		boolean inWindow = new Rect(winX, winY, winW, winH).contains(mouseX, mouseY);
		if (engine.library().isEmpty()) {
			return inWindow;
		}

		boolean listVisible = view == View.QUEUE || openAlbum() != null;
		if (listVisible && listRect().contains(mouseX, mouseY)) {
			List<Track> tracks = visibleTracks();
			int row = (mouseY - listRect().y() + trackScroll) / Theme.ROW_HEIGHT;
			if (row >= 0 && row < tracks.size()) {
				openTrackMenu(tracks, row, mouseX, mouseY);
			}
			return true;
		}

		if (view != View.QUEUE && openAlbum() == null && mainRect().contains(mouseX, mouseY)) {
			List<Album> albums = browseAlbums();
			for (int i = 0; i < albums.size(); i++) {
				if (tileRect(i).contains(mouseX, mouseY)) {
					openTileMenu(albums.get(i), mouseX, mouseY);
					break;
				}
			}
			return true;
		}

		if (deckRect().contains(mouseX, mouseY) && engine.currentTrack() != null) {
			openDeckMenu(engine.currentTrack(), mouseX, mouseY);
			return true;
		}

		return inWindow;
	}

	private void openTrackMenu(List<Track> tracks, int row, int mouseX, int mouseY) {
		Track track = tracks.get(row);
		String path = track.soundPath();
		Album open = openAlbum();

		List<MenuItem> items = new ArrayList<>();
		if (view != View.QUEUE && open != null) {
			items.add(new MenuItem("Play", () -> engine.playAlbum(open, row)));
		} else {
			items.add(new MenuItem("Play", () -> engine.playTrack(track)));
		}
		items.add(new MenuItem("Play next", () -> engine.queue().enqueueNext(track)));
		items.add(new MenuItem("Add to queue", () -> engine.queue().enqueue(track)));
		items.add(favoriteItem(path));
		items.add(new MenuItem("Add to playlist...", () -> showPlaylistPicker(path)));

		if (view == View.QUEUE) {
			int queueIndex = row - queueRowOffset();
			if (queueIndex >= 0 && queueIndex < engine.queue().userQueue().size()) {
				items.add(new MenuItem("Remove from queue",
						() -> engine.queue().removeFromQueue(queueIndex)));
			}
		} else if (open != null && open.kind() == AlbumKind.PLAYLIST
				&& !open.id().equals(Playlists.FAVORITES_ID)) {
			items.add(new MenuItem("Remove from playlist", () -> {
				engine.playlists().removeTrack(open.id(), path);
				Santora.savePlaylists();
			}));
		}
		showMenu(items, mouseX, mouseY);
	}

	private void openTileMenu(Album album, int mouseX, int mouseY) {
		List<MenuItem> items = new ArrayList<>();
		items.add(new MenuItem("Play", () -> engine.playAlbum(album, 0)));
		items.add(new MenuItem("Shuffle", () -> {
			engine.setShuffle(true);
			engine.playAlbum(album, -1);
		}));
		items.add(new MenuItem("Add to queue",
				() -> album.tracks().forEach(engine.queue()::enqueue)));
		if (album.kind() == AlbumKind.PLAYLIST && !album.id().equals(Playlists.FAVORITES_ID)) {
			items.add(new MenuItem("Delete playlist", () -> {
				engine.playlists().delete(album.id());
				Santora.savePlaylists();
			}));
		}
		showMenu(items, mouseX, mouseY);
	}

	private void openDeckMenu(Track track, int mouseX, int mouseY) {
		List<MenuItem> items = new ArrayList<>();
		items.add(new MenuItem("Play next", () -> engine.queue().enqueueNext(track)));
		items.add(new MenuItem("Add to queue", () -> engine.queue().enqueue(track)));
		items.add(favoriteItem(track.soundPath()));
		items.add(new MenuItem("Add to playlist...", () -> showPlaylistPicker(track.soundPath())));
		showMenu(items, mouseX, mouseY);
	}

	private MenuItem favoriteItem(String path) {
		boolean favorite = engine.playlists().isFavorite(path);
		return new MenuItem(favorite ? "Unfavorite" : "Favorite", () -> {
			engine.playlists().toggleFavorite(path);
			Santora.savePlaylists();
		});
	}

	private void showPlaylistPicker(String path) {
		List<MenuItem> items = new ArrayList<>();
		for (Playlists.Playlist playlist : engine.playlists().all()) {
			String id = playlist.id();
			items.add(new MenuItem(playlist.name(), () -> {
				engine.playlists().addTrack(id, path);
				Santora.savePlaylists();
			}));
		}
		items.add(new MenuItem("+ New playlist", () -> startNaming(path)));
		menuItems = items;
	}

	private void renderContextMenu(SantoraCanvas canvas, int mouseX, int mouseY) {
		if (menuItems == null) {
			return;
		}
		int h = menuItems.size() * MENU_ITEM_HEIGHT + MENU_PAD * 2;
		menuDrawX = clamp(menuX, winX, winX + winW - MENU_WIDTH);
		menuDrawY = clamp(menuY, winY, winY + winH - h);

		int x = menuDrawX;
		int y = menuDrawY;
		canvas.fill(x, y, x + MENU_WIDTH, y + h, Theme.DECK);
		canvas.outline(x, y, MENU_WIDTH, h, Theme.FRAME);

		for (int i = 0; i < menuItems.size(); i++) {
			int rowY = y + MENU_PAD + i * MENU_ITEM_HEIGHT;
			boolean hover = mouseX >= x && mouseX < x + MENU_WIDTH
					&& mouseY >= rowY && mouseY < rowY + MENU_ITEM_HEIGHT;
			if (hover) {
				canvas.fill(x + 1, rowY, x + MENU_WIDTH - 1, rowY + MENU_ITEM_HEIGHT, Theme.ROW_HOVER);
			}
			canvas.text(canvas.ellipsize(menuItems.get(i).label(), MENU_WIDTH - 16), x + 8,
					rowY + (MENU_ITEM_HEIGHT - canvas.lineHeight()) / 2 + 1,
					hover ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY, false);
		}
	}

	// New playlist naming
	private void startNaming(String pendingTrackPath) {
		naming = true;
		namingTrackPath = pendingTrackPath;
		nameInput.setLength(0);
	}

	private void cancelNaming() {
		naming = false;
		namingTrackPath = null;
		nameInput.setLength(0);
	}

	private void confirmNaming() {
		String name = nameInput.toString().trim();
		if (name.isEmpty()) {
			return;
		}
		String id = engine.playlists().create(name);
		if (namingTrackPath != null) {
			engine.playlists().addTrack(id, namingTrackPath);
		}
		Santora.savePlaylists();
		cancelNaming();
	}

	private Rect namingRect() {
		int w = 190;
		int h = 56;
		return new Rect(winX + (winW - w) / 2, winY + (winH - h) / 2, w, h);
	}

	private void renderNamingModal(SantoraCanvas canvas) {
		if (!naming) {
			return;
		}
		canvas.fill(winX, winY, winX + winW, winY + winH, 0x99050810);

		Rect box = namingRect();
		canvas.fill(box.x(), box.y(), box.right(), box.bottom(), Theme.WINDOW);
		canvas.outline(box.x(), box.y(), box.w(), box.h(), Theme.FRAME);
		canvas.text("New playlist", box.x() + 8, box.y() + 7, Theme.TEXT_PRIMARY, false);

		Rect field = new Rect(box.x() + 8, box.y() + 19, box.w() - 16, 14);
		canvas.fill(field.x(), field.y(), field.right(), field.bottom(), 0xFF0C0F17);
		canvas.outline(field.x(), field.y(), field.w(), field.h(), Theme.DIVIDER);

		boolean caret = System.currentTimeMillis() / 400 % 2 == 0;
		canvas.text(nameInput + (caret ? "_" : ""), field.x() + 4,
				field.y() + (field.h() - canvas.lineHeight()) / 2 + 1, Theme.TEXT_PRIMARY, false);

		canvas.text("Enter to create - Esc to cancel", box.x() + 8, box.y() + 40,
				Theme.TEXT_MUTED, false);
	}

	public boolean consumeCloseRequest() {
		boolean flag = closeFlag;
		closeFlag = false;
		return flag;
	}

	private boolean clickRail(int mouseX, int mouseY) {
		if (!railRect().contains(mouseX, mouseY)) {
			return false;
		}
		View[] views = View.values();
		for (int i = 0; i < views.length; i++) {
			if (menuRect(i).contains(mouseX, mouseY)) {
				selectView(views[i]);
				break;
			}
		}
		return true;
	}

	private void selectView(View next) {
		cancelDrag();
		closeMenu();
		if (next == View.QUEUE) {
			if (view != View.QUEUE) {
				queueReturnView = view;
				view = View.QUEUE;
				trackScroll = 0;
			}
			return;
		}
		view = next;
		openAlbumId = "";
		gridScroll = 0;
		trackScroll = 0;
	}

	private boolean clickMain(int mouseX, int mouseY) {
		Rect main = mainRect();
		if (!main.contains(mouseX, mouseY) || engine.library().isEmpty()) {
			return main.contains(mouseX, mouseY);
		}

		if (view == View.QUEUE) {
			clickTrackRows(mouseX, mouseY);
			return true;
		}

		Album open = openAlbum();
		if (open == null) {
			List<Album> albums = browseAlbums();
			for (int i = 0; i < albums.size(); i++) {
				if (tileRect(i).contains(mouseX, mouseY)) {
					openAlbumId = albums.get(i).id();
					trackScroll = 0;
					return true;
				}
			}
			if (view == View.PLAYLISTS && tileRect(albums.size()).contains(mouseX, mouseY)) {
				startNaming(null);
			}
			return true;
		}

		if (backRect().contains(mouseX, mouseY, 2)) {
			openAlbumId = "";
			return true;
		}
		if (playButtonRect().contains(mouseX, mouseY)) {
			engine.playAlbum(open, 0);
			return true;
		}
		if (shuffleButtonRect().contains(mouseX, mouseY)) {
			engine.setShuffle(true);
			engine.playAlbum(open, -1);
			return true;
		}
		clickTrackRows(mouseX, mouseY);
		return true;
	}

	private void clickTrackRows(int mouseX, int mouseY) {
		Rect list = listRect();
		if (!list.contains(mouseX, mouseY)) {
			return;
		}
		List<Track> tracks = visibleTracks();
		Album open = openAlbum();
		int rowY = list.y() - trackScroll;
		for (int i = 0; i < tracks.size(); i++) {
			if (mouseY >= rowY && mouseY < rowY + Theme.ROW_HEIGHT) {
				int upIndex = i - queueRowOffset();
				if (view == View.QUEUE && upIndex >= 0) {
					// Arm a drag; the click action runs on release if no drag happened.
					dragIndex = upIndex;
					dragArmY = mouseY;
					dragging = false;
				} else if (view == View.QUEUE || open == null) {
					engine.queue().setCurrent(tracks.get(i));
				} else {
					engine.playAlbum(open, i);
				}
				return;
			}
			rowY += Theme.ROW_HEIGHT;
		}
	}

	// Queue drag-to-reorder
	private int queueRowOffset() {
		// Row 0 of the queue view is the playing track, when there is one.
		return engine.currentTrack() != null ? 1 : 0;
	}

	public boolean mouseDragged(int mouseX, int mouseY, int button) {
		if (button != 0 || dragIndex < 0) {
			return false;
		}
		if (!dragging) {
			if (Math.abs(mouseY - dragArmY) < DRAG_THRESHOLD) {
				return true;
			}
			dragging = true;
		}
		moveDraggedTo(mouseY);
		return true;
	}

	public boolean mouseReleased(int mouseX, int mouseY, int button) {
		if (button != 0 || dragIndex < 0) {
			return false;
		}
		boolean wasClick = !dragging;
		int row = queueRowOffset() + dragIndex;
		cancelDrag();
		if (wasClick) {
			List<Track> tracks = visibleTracks();
			if (row < tracks.size()) {
				engine.queue().setCurrent(tracks.get(row));
			}
		}
		return true;
	}

	private void moveDraggedTo(int mouseY) {
		int base = queueRowOffset();
		int queuedCount = engine.queue().userQueue().size();
		int upcomingCount = visibleTracks().size() - base;
		if (upcomingCount <= 0) {
			// Playback drained the list mid-drag.
			cancelDrag();
			return;
		}
		dragIndex = clamp(dragIndex, 0, upcomingCount - 1);

		Rect list = listRect();
		int row = (mouseY - list.y() + trackScroll) / Theme.ROW_HEIGHT;
		int target = clamp(row - base, 0, upcomingCount - 1);

		// A drag stays inside the segment it started in: the user queue
		// plays before the context tracks, so rows cannot cross over.
		if (dragIndex < queuedCount) {
			target = Math.min(target, queuedCount - 1);
			if (target != dragIndex) {
				engine.queue().moveInQueue(dragIndex, target);
				dragIndex = target;
			}
		} else {
			target = Math.max(target, queuedCount);
			if (target != dragIndex) {
				engine.queue().moveUpcoming(dragIndex - queuedCount, target - queuedCount);
				dragIndex = target;
			}
		}
	}

	private void tickDragScroll(int mouseY) {
		Rect list = listRect();
		int max = Math.max(0, visibleTracks().size() * Theme.ROW_HEIGHT - list.h());
		if (mouseY < list.y() + DRAG_EDGE) {
			trackScroll = clamp(trackScroll - DRAG_SCROLL_STEP, 0, max);
		} else if (mouseY > list.bottom() - DRAG_EDGE) {
			trackScroll = clamp(trackScroll + DRAG_SCROLL_STEP, 0, max);
		} else {
			return;
		}
		moveDraggedTo(mouseY);
	}

	private void cancelDrag() {
		dragIndex = -1;
		dragging = false;
	}

	private boolean clickDeck(int mouseX, int mouseY) {
		if (!deckRect().contains(mouseX, mouseY)) {
			return false;
		}
		if (deckPrevRect().contains(mouseX, mouseY, 3)) {
			engine.previous();
		} else if (deckPlayRect().contains(mouseX, mouseY, 2)) {
			engine.togglePlayPause();
		} else if (deckNextRect().contains(mouseX, mouseY, 3)) {
			engine.next();
		} else if (deckShuffleRect().contains(mouseX, mouseY, 2)) {
			engine.setShuffle(!engine.queue().shuffle());
		} else if (deckRepeatRect().contains(mouseX, mouseY, 2)) {
			engine.cycleRepeat();
		}
		return true;
	}

	public boolean mouseScrolled(int mouseX, int mouseY, double amount) {
		if (naming) {
			return true;
		}
		closeMenu();

		Rect main = mainRect();
		if (!main.contains(mouseX, mouseY)) {
			return false;
		}
		int step = (int) (-amount * 16);

		if (view != View.QUEUE && openAlbum() == null) {
			int max = Math.max(0, gridContentHeight(gridTileCount(browseAlbums())) - main.h());
			gridScroll = clamp(gridScroll + step, 0, max);
		} else {
			Rect list = listRect();
			int max = Math.max(0, visibleTracks().size() * Theme.ROW_HEIGHT - list.h());
			trackScroll = clamp(trackScroll + step, 0, max);
		}
		return true;
	}

	public boolean keyPressed(int keyCode) {
		if (naming) {
			switch (keyCode) {
				case 257, 335 -> confirmNaming(); // enter
				case 256 -> cancelNaming(); // escape
				case 259 -> { // backspace
					if (!nameInput.isEmpty()) {
						nameInput.deleteCharAt(nameInput.length() - 1);
					}
				}
				default -> { }
			}
			return true;
		}

		if (menuItems != null && keyCode == 256) { // escape
			closeMenu();
			return true;
		}

		switch (keyCode) {
			case 32 -> { // space
				engine.togglePlayPause();
				return true;
			}
			case 81 -> { // Q
				cancelDrag();
				closeMenu();
				if (view == View.QUEUE) {
					view = queueReturnView;
				} else {
					queueReturnView = view;
					view = View.QUEUE;
				}
				trackScroll = 0;
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	public boolean charTyped(CharacterEvent event) {
		if (!naming) {
			return false;
		}
		if (event.isAllowedChatCharacter() && nameInput.length() < NAME_INPUT_MAX) {
			nameInput.append(event.codepointAsString());
		}
		return true;
	}

	public void onClose() {
		Santora.saveConfig();
		Santora.savePlaylists();
	}

	private static int clamp(int value, int min, int max) {
		return value < min ? min : (value > max ? max : value);
	}
}
