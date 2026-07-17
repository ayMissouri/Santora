package dev.santora.ui;

import dev.santora.Santora;
import dev.santora.core.config.SantoraConfig;
import dev.santora.core.model.Album;
import dev.santora.core.model.AlbumKind;
import dev.santora.core.model.MusicContext;
import dev.santora.core.model.MusicLibrary;
import dev.santora.core.model.Playlists;
import dev.santora.core.model.Track;
import dev.santora.core.play.RepeatMode;
import dev.santora.engine.MusicEngine;
import net.minecraft.client.input.CharacterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

public final class SantoraUi {

	private static final int MAX_WIDTH = 460;
	private static final int MAX_HEIGHT = 260;

	private final MusicEngine engine = MusicEngine.get();

	private enum View {
		SEARCH("Search"),
		ALBUMS("Albums"),
		ARTISTS("Artists"),
		UPDATES("Updates"),
		PLAYLISTS("Playlists"),
		QUEUE("Queue"),
		SETTINGS("Settings");

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
	private static int settingsScroll;
	private static String searchQuery = "";

	private List<Track> searchResults = List.of();
	private String searchResultsQuery;
	private MusicLibrary searchResultsLibrary;

	private static final int DRAG_THRESHOLD = 4;
	private static final int DRAG_EDGE = 14;
	private static final int DRAG_SCROLL_STEP = 3;

	private static final int MENU_WIDTH = 120;
	private static final int MENU_ITEM_HEIGHT = 13;
	private static final int MENU_PAD = 3;
	private static final int NAME_INPUT_MAX = 24;
	private static final int SEARCH_INPUT_MAX = 28;

	private int dragIndex = -1;
	private int dragArmY;
	private boolean dragging;

	private static final int SETTINGS_ROW_HEIGHT = 28;
	private static final int SLIDER_WIDTH = 110;
	private static final int VALUE_WIDTH = 46;

	private static final int ROW_PLAYBACK_HEADER = 0;
	private static final int ROW_CROSSFADE = 1;
	private static final int ROW_FADE = 2;
	private static final int ROW_DELAY = 3;
	private static final int ROW_VOLUME = 4;
	private static final int ROW_RESUME = 5;
	private static final int ROW_MENU_HEADER = 6;
	private static final int ROW_MENU_BG = 7;
	private static final int ROW_MENU_ACCENT = 8;
	private static final int ROW_MENU_OPACITY = 9;
	private static final int ROW_HUD_HEADER = 10;
	private static final int ROW_HUD_ON = 11;
	private static final int ROW_HUD_POS = 12;
	private static final int ROW_HUD_BG = 13;
	private static final int ROW_HUD_ACCENT = 14;
	private static final int ROW_HUD_OPACITY = 15;
	private static final int SETTINGS_ROW_COUNT = 16;

	private static final int SLIDER_NONE = 0;
	private static final int SLIDER_CROSSFADE = 1;
	private static final int SLIDER_DELAY_MIN = 2;
	private static final int SLIDER_DELAY_MAX = 3;
	private static final int SLIDER_VOLUME = 4;
	private static final int SLIDER_MENU_OPACITY = 5;
	private static final int SLIDER_HUD_OPACITY = 6;
	private static final int SLIDER_DECK_VOLUME = 7;

	private static final int DECK_VOLUME_WIDTH = 45;

	private static final int SWATCH_SIZE = 12;
	private static final int SWATCH_GAP = 4;

	private static final int[] BACKGROUND_CHOICES = {
			0x141926, // midnight (default)
			0x17181C, // charcoal
			0x0B0C10, // black
			0x0F1B2E, // ocean
			0x122019, // pine
			0x1D1426, // plum
			0x241318, // wine
	};

	private static final int[] ACCENT_CHOICES = {
			0xE3A44C, // amber (default)
			0xE06A4D, // ember
			0xE06C8A, // rose
			0xA47CE8, // violet
			0x58A6E8, // sky
			0x4FC4B7, // teal
			0x53C487, // emerald
			0xC9CFDA, // silver
	};

	private int activeSlider = SLIDER_NONE;

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
	private int screenW;
	private int screenH;

	private boolean movingOverlay;
	private boolean overlayDragging;
	private int overlayGrabDX;
	private int overlayGrabDY;

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

	private Rect railStatusRect() {
		Rect rail = railRect();
		return new Rect(rail.x() + 8, rail.bottom() - 28, rail.w() - 16, 11);
	}

	private int gridCols() {
		int usable = mainRect().w() - Theme.PADDING * 2;
		return Math.max(2, (usable + Theme.GRID_GAP) / (Theme.TILE_TARGET_WIDTH + Theme.GRID_GAP));
	}

	private int tileWidth() {
		int cols = gridCols();
		int usable = mainRect().w() - Theme.PADDING * 2;
		return (usable - (cols - 1) * Theme.GRID_GAP) / cols;
	}

	private Rect tileRect(int index) {
		Rect main = mainRect();
		int cols = gridCols();
		int tileW = tileWidth();
		int tileH = tileW + Theme.TILE_TEXT_HEIGHT;
		int col = index % cols;
		int row = index / cols;
		return new Rect(
				main.x() + Theme.PADDING + col * (tileW + Theme.GRID_GAP),
				main.y() + Theme.PADDING + row * (tileH + Theme.GRID_GAP) - gridScroll,
				tileW, tileH);
	}

	private int gridContentHeight(int count) {
		int cols = gridCols();
		int rows = (count + cols - 1) / cols;
		return rows == 0 ? 0
				: Theme.PADDING * 2 + rows * (tileWidth() + Theme.TILE_TEXT_HEIGHT) + (rows - 1) * Theme.GRID_GAP;
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
		if (view == View.QUEUE) {
			return mainRect().y() + 24;
		}
		if (view == View.SEARCH) {
			return searchFieldRect().bottom() + 5;
		}
		return headerArtRect().bottom() + 8;
	}

	private Rect searchFieldRect() {
		Rect main = mainRect();
		return new Rect(main.x() + Theme.PADDING, main.y() + 6, main.w() - Theme.PADDING * 2, 15);
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
		return new Rect(prev.x() - 12 - 11, deckCenterY() - 5, 11, 10);
	}

	private Rect deckShuffleRect() {
		Rect repeat = deckRepeatRect();
		return new Rect(repeat.x() - 10 - 11, deckCenterY() - 5, 11, 10);
	}

	private Rect deckVolumeRect() {
		Rect shuffle = deckShuffleRect();
		return new Rect(shuffle.x() - 10 - DECK_VOLUME_WIDTH, deckCenterY() - 4,
				DECK_VOLUME_WIDTH, 9);
	}

	private int deckTimeRight() {
		return deckVolumeRect().x() - 18;
	}

	public void render(SantoraCanvas canvas, int mouseX, int mouseY) {
		screenW = canvas.width();
		screenH = canvas.height();
		winW = Math.min(canvas.width() - 20, MAX_WIDTH);
		winH = Math.min(canvas.height() - 20, MAX_HEIGHT);
		winX = (canvas.width() - winW) / 2;
		winY = (canvas.height() - winH) / 2;

		if (movingOverlay) {
			renderOverlayMove(canvas, mouseX, mouseY);
			return;
		}

		canvas.fill(0, 0, canvas.width(), canvas.height(), Theme.SCRIM);
		canvas.fill(winX, winY, winX + winW, winY + winH, Theme.WINDOW);

		renderTopBar(canvas, mouseX, mouseY);
		renderRail(canvas, mouseX, mouseY);
		renderMain(canvas, mouseX, mouseY);
		renderDeck(canvas, mouseX, mouseY);

		canvas.outline(winX, winY, winW, winH, Theme.FRAME);

		renderTooltips(canvas, mouseX, mouseY);
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

		renderRailStatus(canvas, rail, mouseX, mouseY);
	}

	private void renderRailStatus(SantoraCanvas canvas, Rect rail, int mouseX, int mouseY) {
		int y = rail.bottom() - 26;
		canvas.fill(rail.x() + 8, y - 5, rail.right() - 8, y - 4, Theme.DIVIDER);

		boolean manual = engine.isManualMode();
		Rect status = railStatusRect();
		boolean hover = manual && status.contains(mouseX, mouseY, 2);
		if (hover) {
			canvas.fill(status.x() - 2, status.y() - 1, status.right() + 2, status.bottom() + 1,
					Theme.ROW_HOVER);
		}

		int color = manual ? Theme.ACCENT : Theme.TEXT_MUTED;
		canvas.fill(rail.x() + 10, y + 2, rail.x() + 14, y + 6, color);
		canvas.text(manual ? "MANUAL" : "VANILLA", rail.x() + 18, y,
				hover ? Theme.TEXT_PRIMARY : color, false);

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
			case SEARCH, QUEUE, SETTINGS -> List.of();
		};
	}

	private int gridTileCount(List<Album> albums) {
		return albums.size() + (view == View.PLAYLISTS ? 1 : 0);
	}

	private List<Track> searchTracks() {
		MusicLibrary library = engine.library();
		if (!searchQuery.equals(searchResultsQuery) || library != searchResultsLibrary) {
			searchResults = library.search(searchQuery);
			searchResultsQuery = searchQuery;
			searchResultsLibrary = library;
		}
		return searchResults;
	}

	private List<Track> visibleTracks() {
		if (view == View.SEARCH) {
			return searchTracks();
		}
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

		if (view == View.SETTINGS) {
			renderSettings(canvas, mouseX, mouseY);
			return;
		}

		if (engine.library().isEmpty()) {
			canvas.textCentered("Indexing music...", main.x() + main.w() / 2,
					main.y() + main.h() / 2 - 4, Theme.TEXT_MUTED);
			return;
		}

		if (view == View.SEARCH) {
			renderSearchField(canvas);
			renderTrackList(canvas, mouseX, mouseY);
		} else if (view == View.QUEUE) {
			renderQueueHeader(canvas, main, mouseX, mouseY);
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

			drawAlbumArt(canvas, album, tile.x(), tile.y(), tile.w(), tile.w());
			if (hover) {
				canvas.fill(tile.x(), tile.y(), tile.right(), tile.y() + tile.w(),
						Theme.ROW_HOVER);
				canvas.outline(tile.x(), tile.y(), tile.w(), tile.w(), Theme.ACCENT);
			}

			canvas.text(canvas.ellipsize(album.title(), tile.w() - 2),
					tile.x() + 1, tile.y() + tile.w() + 4,
					hover ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY, false);
			canvas.text(album.trackCount() + (album.trackCount() == 1 ? " track" : " tracks"),
					tile.x() + 1, tile.y() + tile.w() + 14, Theme.TEXT_MUTED, false);
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

		canvas.fill(tile.x(), tile.y(), tile.right(), tile.y() + tile.w(), Theme.INPUT_BG);
		canvas.outline(tile.x(), tile.y(), tile.w(), tile.w(),
				hover ? Theme.ACCENT : Theme.DIVIDER);
		canvas.textCentered("+", tile.x() + tile.w() / 2,
				tile.y() + tile.w() / 2 - canvas.lineHeight() / 2,
				hover ? Theme.ACCENT : Theme.TEXT_SECONDARY);
		canvas.text("New Playlist", tile.x() + 1, tile.y() + tile.w() + 4,
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

	private void renderSearchField(SantoraCanvas canvas) {
		Rect field = searchFieldRect();
		canvas.fill(field.x(), field.y(), field.right(), field.bottom(), Theme.INPUT_BG);
		canvas.outline(field.x(), field.y(), field.w(), field.h(), Theme.DIVIDER);

		int textY = field.y() + (field.h() - canvas.lineHeight()) / 2 + 1;
		boolean caret = System.currentTimeMillis() / 400 % 2 == 0;
		canvas.text(searchQuery + (caret ? "_" : ""), field.x() + 4, textY,
				Theme.TEXT_PRIMARY, false);
		if (searchQuery.isEmpty()) {
			canvas.text("Search songs...", field.x() + 4 + canvas.textWidth("_") + 2, textY,
					Theme.TEXT_MUTED, false);
			return;
		}

		int found = searchTracks().size();
		String count = found == 1 ? "1 match" : found + " matches";
		canvas.text(count, field.right() - 4 - canvas.textWidth(count), textY,
				Theme.TEXT_MUTED, false);
	}

	private Rect clearQueueRect() {
		Rect main = mainRect();
		return new Rect(main.right() - Theme.PADDING - 40, main.y() + 4, 40, 14);
	}

	private void renderQueueHeader(SantoraCanvas canvas, Rect main, int mouseX, int mouseY) {
		canvas.text("Queue", main.x() + Theme.PADDING, main.y() + 7, Theme.TEXT_PRIMARY, false);

		int right = main.right() - Theme.PADDING;
		if (engine.queue().upcomingCount() > 0) {
			Rect clear = clearQueueRect();
			drawButton(canvas, mouseX, mouseY, clear, "Clear", false);
			right = clear.x() - 8;
		}
		String queued = engine.queue().upcomingCount() + " up next";
		canvas.text(queued, right - canvas.textWidth(queued), main.y() + 7,
				Theme.TEXT_SECONDARY, false);
	}

	// Settings
	private Rect settingsRowRect(int row) {
		Rect main = mainRect();
		return new Rect(main.x() + Theme.PADDING,
				main.y() + 24 + row * SETTINGS_ROW_HEIGHT - settingsScroll,
				main.w() - Theme.PADDING * 2, SETTINGS_ROW_HEIGHT);
	}

	private Rect settingsSliderRect(int row) {
		Rect rowRect = settingsRowRect(row);
		int x = rowRect.right() - VALUE_WIDTH - 6 - SLIDER_WIDTH;
		return new Rect(x, rowRect.y() + (rowRect.h() - 9) / 2, SLIDER_WIDTH, 9);
	}

	private Rect settingsSwitchRect(int row) {
		Rect rowRect = settingsRowRect(row);
		return new Rect(rowRect.right() - 22, rowRect.y() + (rowRect.h() - 12) / 2, 22, 12);
	}

	private int settingsContentHeight() {
		return 24 + SETTINGS_ROW_COUNT * SETTINGS_ROW_HEIGHT + Theme.PADDING;
	}

	private void renderSettings(SantoraCanvas canvas, int mouseX, int mouseY) {
		Rect main = mainRect();
		SantoraConfig config = engine.config();
		settingsScroll = clamp(settingsScroll, 0, Math.max(0, settingsContentHeight() - main.h()));

		canvas.pushScissor(main.x(), main.y(), main.w(), main.h());
		canvas.text("Settings", main.x() + Theme.PADDING, main.y() + 7 - settingsScroll,
				Theme.TEXT_PRIMARY, false);

		renderSettingsHeader(canvas, ROW_PLAYBACK_HEADER, "PLAYBACK");

		renderSwitchRow(canvas, ROW_CROSSFADE, "Crossfade", "Blend each track into the next",
				config.crossfadeOn(), mouseX, mouseY);

		renderSliderRow(canvas, ROW_FADE, "Fade duration", "How long the blend lasts",
				config.crossfadeMillis() / (float) SantoraConfig.CROSSFADE_MAX_MILLIS,
				formatSeconds(config.crossfadeMillis()),
				!config.crossfadeOn(), SLIDER_CROSSFADE, mouseX, mouseY);

		renderDelayRow(canvas, ROW_DELAY, mouseX, mouseY);

		renderSliderRow(canvas, ROW_VOLUME, "Volume", "Only affects Santora's playback",
				config.volume(), Math.round(config.volume() * 100) + "%",
				false, SLIDER_VOLUME, mouseX, mouseY);

		renderSwitchRow(canvas, ROW_RESUME, "Resume on launch", "Keep playing where you left off",
				config.resumeOnLaunch(), mouseX, mouseY);

		renderSettingsHeader(canvas, ROW_MENU_HEADER, "MENU LOOK");

		renderSwatchRow(canvas, ROW_MENU_BG, "Background", "Base color of this window",
				BACKGROUND_CHOICES, config.menuBackground(), mouseX, mouseY);

		renderSwatchRow(canvas, ROW_MENU_ACCENT, "Accent", "Highlights, buttons and progress",
				ACCENT_CHOICES, config.menuAccent(), mouseX, mouseY);

		renderSliderRow(canvas, ROW_MENU_OPACITY, "Background opacity", "Let the game show through",
				(config.menuOpacity() - SantoraConfig.MENU_OPACITY_MIN)
						/ (float) (100 - SantoraConfig.MENU_OPACITY_MIN),
				config.menuOpacity() + "%", false, SLIDER_MENU_OPACITY, mouseX, mouseY);

		renderSettingsHeader(canvas, ROW_HUD_HEADER, "NOW PLAYING HUD");

		renderSwitchRow(canvas, ROW_HUD_ON, "Show HUD", "Show the current track in-game",
				config.overlayOn(), mouseX, mouseY);

		Rect move = overlayMoveRect();
		renderSettingLabels(canvas, ROW_HUD_POS, "Position", "Drag the card anywhere on screen",
				move.x());
		drawButton(canvas, mouseX, mouseY, move, "Move", false);

		renderSwatchRow(canvas, ROW_HUD_BG, "Background", "Base color of the card",
				BACKGROUND_CHOICES, config.hudBackground(), mouseX, mouseY);

		renderSwatchRow(canvas, ROW_HUD_ACCENT, "Accent", "Color of the progress bar",
				ACCENT_CHOICES, config.hudAccent(), mouseX, mouseY);

		renderSliderRow(canvas, ROW_HUD_OPACITY, "Background opacity", "0% leaves just text and art",
				config.hudOpacity() / 100f, config.hudOpacity() + "%",
				false, SLIDER_HUD_OPACITY, mouseX, mouseY);

		canvas.popScissor();
	}

	private void renderSettingsHeader(SantoraCanvas canvas, int row, String label) {
		Rect rowRect = settingsRowRect(row);
		canvas.text(label, rowRect.x(), rowRect.bottom() - canvas.lineHeight() - 3,
				Theme.ACCENT, false);
		canvas.fill(rowRect.x(), rowRect.bottom() - 1, rowRect.right(), rowRect.bottom(),
				Theme.DIVIDER);
	}

	private Rect swatchRect(int row, int count, int index) {
		Rect rowRect = settingsRowRect(row);
		int width = count * SWATCH_SIZE + (count - 1) * SWATCH_GAP;
		return new Rect(rowRect.right() - width + index * (SWATCH_SIZE + SWATCH_GAP),
				rowRect.y() + (rowRect.h() - SWATCH_SIZE) / 2, SWATCH_SIZE, SWATCH_SIZE);
	}

	private void renderSwatchRow(SantoraCanvas canvas, int row, String label, String sub,
			int[] choices, int selected, int mouseX, int mouseY) {
		renderSettingLabels(canvas, row, label, sub, swatchRect(row, choices.length, 0).x());

		for (int i = 0; i < choices.length; i++) {
			Rect swatch = swatchRect(row, choices.length, i);
			canvas.fill(swatch.x(), swatch.y(), swatch.right(), swatch.bottom(),
					0xFF000000 | choices[i]);
			if (choices[i] == selected) {
				canvas.outline(swatch.x() - 1, swatch.y() - 1, swatch.w() + 2, swatch.h() + 2,
						Theme.TEXT_PRIMARY);
			} else if (swatch.contains(mouseX, mouseY, 1)) {
				canvas.outline(swatch.x(), swatch.y(), swatch.w(), swatch.h(), Theme.TEXT_SECONDARY);
			}
		}
	}

	private int pickSwatch(int row, int[] choices, int mouseX, int mouseY) {
		for (int i = 0; i < choices.length; i++) {
			if (swatchRect(row, choices.length, i).contains(mouseX, mouseY, 1)) {
				return choices[i];
			}
		}
		return -1;
	}

	private Rect overlayMoveRect() {
		Rect rowRect = settingsRowRect(ROW_HUD_POS);
		return new Rect(rowRect.right() - 40, rowRect.y() + (rowRect.h() - 14) / 2, 40, 14);
	}

	private void renderSettingLabels(SantoraCanvas canvas, int row, String label, String sub,
			int controlX) {
		Rect rowRect = settingsRowRect(row);
		int max = controlX - 6 - rowRect.x();
		canvas.text(canvas.ellipsize(label, max), rowRect.x(), rowRect.y() + 4,
				Theme.TEXT_PRIMARY, false);
		canvas.text(canvas.ellipsize(sub, max), rowRect.x(), rowRect.y() + 15,
				Theme.TEXT_MUTED, false);
	}

	private void renderSwitchRow(SantoraCanvas canvas, int row, String label, String sub,
			boolean on, int mouseX, int mouseY) {
		Rect toggle = settingsSwitchRect(row);
		renderSettingLabels(canvas, row, label, sub, toggle.x());

		boolean hover = toggle.contains(mouseX, mouseY, 2);
		int track = on ? Theme.ACCENT : Theme.PROGRESS_TRACK;
		if (hover) {
			track = Theme.blend(track, 0xFFFFFFFF, 0.12f);
		}
		canvas.fill(toggle.x(), toggle.y(), toggle.right(), toggle.bottom(), track);
		int knobX = on ? toggle.right() - 10 : toggle.x() + 2;
		canvas.fill(knobX, toggle.y() + 2, knobX + 8, toggle.bottom() - 2,
				on ? Theme.ON_ACCENT : Theme.TEXT_SECONDARY);
	}

	private void renderSliderRow(SantoraCanvas canvas, int row, String label, String sub,
			float t, String value, boolean dimmed, int slider, int mouseX, int mouseY) {
		Rect track = settingsSliderRect(row);
		renderSettingLabels(canvas, row, label, sub, track.x());

		int cy = track.y() + track.h() / 2;
		canvas.fill(track.x(), cy - 1, track.right(), cy + 1, Theme.PROGRESS_TRACK);
		int handleX = handleX(track, t);
		canvas.fill(track.x(), cy - 1, handleX, cy + 1, dimmed ? Theme.ACCENT_DIM : Theme.ACCENT);
		boolean hover = activeSlider == slider
				|| (activeSlider == SLIDER_NONE && track.contains(mouseX, mouseY, 3));
		canvas.fill(handleX, track.y(), handleX + 3, track.bottom(),
				hover ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);

		renderSettingValue(canvas, row, value);
	}

	private void renderDelayRow(SantoraCanvas canvas, int row, int mouseX, int mouseY) {
		SantoraConfig config = engine.config();
		Rect track = settingsSliderRect(row);
		boolean fading = config.crossfadeEnabled();
		renderSettingLabels(canvas, row, "Delay between tracks",
				fading ? "Off while crossfade is on" : "Waits a random time in this range",
				track.x());

		float tMin = config.delayMinMillis() / (float) SantoraConfig.DELAY_MAX_MILLIS;
		float tMax = config.delayMaxMillis() / (float) SantoraConfig.DELAY_MAX_MILLIS;
		int cy = track.y() + track.h() / 2;
		canvas.fill(track.x(), cy - 1, track.right(), cy + 1, Theme.PROGRESS_TRACK);

		int minX = handleX(track, tMin);
		int maxX = handleX(track, tMax);
		canvas.fill(minX, cy - 1, maxX + 3, cy + 1, fading ? Theme.ACCENT_DIM : Theme.ACCENT);

		int picked = activeSlider != SLIDER_NONE ? activeSlider
				: track.contains(mouseX, mouseY, 3) ? pickDelayHandle(track, mouseX) : SLIDER_NONE;
		canvas.fill(minX, track.y(), minX + 3, track.bottom(),
				picked == SLIDER_DELAY_MIN ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);
		canvas.fill(maxX, track.y(), maxX + 3, track.bottom(),
				picked == SLIDER_DELAY_MAX ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);

		renderSettingValue(canvas, row, formatDelayRange(config));
	}

	private void renderSettingValue(SantoraCanvas canvas, int row, String value) {
		Rect rowRect = settingsRowRect(row);
		canvas.text(value, rowRect.right() - canvas.textWidth(value),
				rowRect.y() + (rowRect.h() - canvas.lineHeight()) / 2 + 1, Theme.TEXT_SECONDARY, false);
	}

	private static int handleX(Rect track, float t) {
		float clamped = t < 0f ? 0f : (t > 1f ? 1f : t);
		return track.x() + Math.round((track.w() - 3) * clamped);
	}

	private static float sliderT(Rect track, int mouseX) {
		float t = (mouseX - track.x()) / (float) (track.w() - 3);
		return t < 0f ? 0f : (t > 1f ? 1f : t);
	}

	private static String formatSeconds(int millis) {
		if (millis == 0) {
			return "Off";
		}
		return millis % 1000 == 0 ? millis / 1000 + "s"
				: String.format(java.util.Locale.ROOT, "%.1fs", millis / 1000f);
	}

	private static String formatDelayRange(SantoraConfig config) {
		int min = config.delayMinMillis() / 1000;
		int max = config.delayMaxMillis() / 1000;
		if (max == 0) {
			return "Off";
		}
		return min == max ? max + "s" : min + "-" + max + "s";
	}

	private int pickDelayHandle(Rect track, int mouseX) {
		SantoraConfig config = engine.config();
		float t = sliderT(track, mouseX);
		float tMin = config.delayMinMillis() / (float) SantoraConfig.DELAY_MAX_MILLIS;
		float tMax = config.delayMaxMillis() / (float) SantoraConfig.DELAY_MAX_MILLIS;
		if (t < tMin) {
			return SLIDER_DELAY_MIN;
		}
		if (t > tMax) {
			return SLIDER_DELAY_MAX;
		}
		return t - tMin <= tMax - t ? SLIDER_DELAY_MIN : SLIDER_DELAY_MAX;
	}

	private void clickSettings(int mouseX, int mouseY) {
		SantoraConfig config = engine.config();

		if (settingsSwitchRect(ROW_CROSSFADE).contains(mouseX, mouseY, 2)) {
			config.setCrossfadeOn(!config.crossfadeOn());
			return;
		}
		if (settingsSwitchRect(ROW_RESUME).contains(mouseX, mouseY, 2)) {
			config.setResumeOnLaunch(!config.resumeOnLaunch());
			return;
		}
		if (settingsSwitchRect(ROW_HUD_ON).contains(mouseX, mouseY, 2)) {
			config.setOverlayOn(!config.overlayOn());
			return;
		}
		if (overlayMoveRect().contains(mouseX, mouseY, 2)) {
			movingOverlay = true;
			return;
		}
		if (clickSwatches(mouseX, mouseY)) {
			return;
		}

		if (settingsSliderRect(ROW_FADE).contains(mouseX, mouseY, 3)) {
			activeSlider = SLIDER_CROSSFADE;
		} else if (settingsSliderRect(ROW_DELAY).contains(mouseX, mouseY, 3)) {
			activeSlider = pickDelayHandle(settingsSliderRect(ROW_DELAY), mouseX);
		} else if (settingsSliderRect(ROW_VOLUME).contains(mouseX, mouseY, 3)) {
			activeSlider = SLIDER_VOLUME;
		} else if (settingsSliderRect(ROW_MENU_OPACITY).contains(mouseX, mouseY, 3)) {
			activeSlider = SLIDER_MENU_OPACITY;
		} else if (settingsSliderRect(ROW_HUD_OPACITY).contains(mouseX, mouseY, 3)) {
			activeSlider = SLIDER_HUD_OPACITY;
		} else {
			return;
		}
		applySlider(activeSlider, mouseX);
	}

	private boolean clickSwatches(int mouseX, int mouseY) {
		SantoraConfig config = engine.config();

		int color = pickSwatch(ROW_MENU_BG, BACKGROUND_CHOICES, mouseX, mouseY);
		if (color >= 0) {
			config.setMenuBackground(color);
			Theme.refresh(config);
			return true;
		}
		color = pickSwatch(ROW_MENU_ACCENT, ACCENT_CHOICES, mouseX, mouseY);
		if (color >= 0) {
			config.setMenuAccent(color);
			Theme.refresh(config);
			return true;
		}
		color = pickSwatch(ROW_HUD_BG, BACKGROUND_CHOICES, mouseX, mouseY);
		if (color >= 0) {
			config.setHudBackground(color);
			return true;
		}
		color = pickSwatch(ROW_HUD_ACCENT, ACCENT_CHOICES, mouseX, mouseY);
		if (color >= 0) {
			config.setHudAccent(color);
			return true;
		}
		return false;
	}

	private void applySlider(int slider, int mouseX) {
		SantoraConfig config = engine.config();
		switch (slider) {
			case SLIDER_CROSSFADE -> config.setCrossfadeMillis(snap(
					sliderT(settingsSliderRect(ROW_FADE), mouseX)
							* SantoraConfig.CROSSFADE_MAX_MILLIS, 500));
			case SLIDER_DELAY_MIN -> config.setDelayMinMillis(snap(
					sliderT(settingsSliderRect(ROW_DELAY), mouseX)
							* SantoraConfig.DELAY_MAX_MILLIS, 5_000));
			case SLIDER_DELAY_MAX -> config.setDelayMaxMillis(snap(
					sliderT(settingsSliderRect(ROW_DELAY), mouseX)
							* SantoraConfig.DELAY_MAX_MILLIS, 5_000));
			case SLIDER_VOLUME -> engine.setVolume(
					snap(sliderT(settingsSliderRect(ROW_VOLUME), mouseX) * 100, 5) / 100f);
			case SLIDER_MENU_OPACITY -> {
				int span = 100 - SantoraConfig.MENU_OPACITY_MIN;
				config.setMenuOpacity(snap(SantoraConfig.MENU_OPACITY_MIN
						+ sliderT(settingsSliderRect(ROW_MENU_OPACITY), mouseX) * span, 5));
				Theme.refresh(config);
			}
			case SLIDER_HUD_OPACITY -> config.setHudOpacity(snap(
					sliderT(settingsSliderRect(ROW_HUD_OPACITY), mouseX) * 100, 5));
			case SLIDER_DECK_VOLUME -> engine.setVolume(
					snap(sliderT(deckVolumeRect(), mouseX) * 100, 5) / 100f);
			default -> { }
		}
	}

	private static int snap(float value, int step) {
		return Math.round(value / step) * step;
	}

	// Placing the now playing overlay
	private Rect overlayCardRect() {
		SantoraConfig config = engine.config();
		return new Rect(NowPlayingOverlay.x(config, screenW), NowPlayingOverlay.y(config, screenH),
				NowPlayingOverlay.WIDTH, NowPlayingOverlay.HEIGHT);
	}

	private Rect overlayDoneRect() {
		return new Rect(screenW / 2 - 26, screenH - 34, 52, 16);
	}

	private void renderOverlayMove(SantoraCanvas canvas, int mouseX, int mouseY) {
		canvas.fill(0, 0, screenW, screenH, 0x66050810);

		canvas.textCentered("Drag the card where you want it", screenW / 2, 14, Theme.TEXT_PRIMARY);
		canvas.textCentered("Esc or Done when you're happy", screenW / 2, 26, Theme.TEXT_MUTED);

		Rect card = overlayCardRect();
		NowPlayingOverlay.renderCard(canvas, card.x(), card.y(), true);
		if (overlayDragging || card.contains(mouseX, mouseY)) {
			canvas.outline(card.x() - 1, card.y() - 1, card.w() + 2, card.h() + 2, Theme.ACCENT);
		}

		drawButton(canvas, mouseX, mouseY, overlayDoneRect(), "Done", true);
	}

	private void exitOverlayMove() {
		movingOverlay = false;
		overlayDragging = false;
		Santora.saveConfig();
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
				} else if (view != View.SEARCH) {
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
			String message = view == View.QUEUE ? "Queue is empty"
					: view == View.SEARCH ? "No matches"
					: "No tracks";
			canvas.textCentered(message, list.x() + list.w() / 2, list.y() + 20, Theme.TEXT_MUTED);
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
			canvas.fill(x, y, x + w, y + h, Theme.EMPTY_ART);
			return;
		}

		if (AlbumArt.draw(canvas, album.artKey(), x, y, w, h)) {
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
			AlbumArt.drawTrack(canvas, track, deck.x() + 8, artY, artSize);
		} else {
			canvas.fill(deck.x() + 8, artY, deck.x() + 8 + artSize, artY + artSize, Theme.EMPTY_ART);
		}

		int textX = deck.x() + 8 + artSize + 6;
		int textMax = Math.max(40, deckTimeRight() - 68 - textX);
		long waiting = engine.delayRemainingMillis();
		String title = track != null ? canvas.ellipsize(track.title(), textMax)
				: waiting >= 0 ? "Next track in " + (waiting + 999) / 1000 + "s"
				: "Nothing playing";
		canvas.text(title, textX, deck.y() + 11, Theme.TEXT_PRIMARY, false);
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

		Rect repeat = deckRepeatRect();
		RepeatMode mode = engine.queue().repeat();
		drawRepeatIcon(canvas, repeat.x(), cy,
				toggleColor(repeat, mouseX, mouseY, mode != RepeatMode.OFF),
				mode == RepeatMode.ONE);

		Rect shuffle = deckShuffleRect();
		drawShuffleIcon(canvas, shuffle.x(), cy,
				toggleColor(shuffle, mouseX, mouseY, engine.queue().shuffle()));

		renderDeckVolume(canvas, cy, mouseX, mouseY);

		if (track != null) {
			OptionalDouble duration = engine.durationSeconds(track);
			String total = duration.isPresent()
					? Theme.formatTime((long) (duration.getAsDouble() * 1000))
					: "--:--";
			String time = Theme.formatTime(engine.elapsedMillis()) + " / " + total;
			canvas.text(time, deckTimeRight() - canvas.textWidth(time), cy - 4,
					Theme.TEXT_MUTED, false);
		}
	}

	private void renderDeckVolume(SantoraCanvas canvas, int cy, int mouseX, int mouseY) {
		Rect vol = deckVolumeRect();
		float volume = engine.config().volume();

		drawSpeakerIcon(canvas, vol.x() - 12, cy,
				volume == 0f ? Theme.TEXT_MUTED : Theme.TEXT_SECONDARY);

		canvas.fill(vol.x(), cy - 1, vol.right(), cy + 1, Theme.PROGRESS_TRACK);
		int handleX = handleX(vol, volume);
		canvas.fill(vol.x(), cy - 1, handleX, cy + 1, Theme.ACCENT);
		boolean hover = activeSlider == SLIDER_DECK_VOLUME
				|| (activeSlider == SLIDER_NONE && vol.contains(mouseX, mouseY, 3));
		canvas.fill(handleX, vol.y(), handleX + 3, vol.bottom(),
				hover ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);
	}

	private void drawSpeakerIcon(SantoraCanvas canvas, int x, int cy, int color) {
		canvas.fill(x, cy - 2, x + 2, cy + 3, color);
		for (int i = 0; i < 3; i++) {
			canvas.fill(x + 2 + i, cy - 2 - i, x + 3 + i, cy + 3 + i, color);
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

	private void renderTooltips(SantoraCanvas canvas, int mouseX, int mouseY) {
		if (menuItems != null || naming) {
			return;
		}
		if (deckVolumeRect().contains(mouseX, mouseY, 3)) {
			drawTooltip(canvas, "Volume " + Math.round(engine.config().volume() * 100) + "%",
					deckVolumeRect());
		} else if (deckShuffleRect().contains(mouseX, mouseY, 2)) {
			drawTooltip(canvas, "Shuffle", deckShuffleRect());
		} else if (deckRepeatRect().contains(mouseX, mouseY, 2)) {
			String label = switch (engine.queue().repeat()) {
				case OFF -> "Repeat";
				case ALL -> "Repeat all";
				case ONE -> "Repeat one";
			};
			drawTooltip(canvas, label, deckRepeatRect());
		} else if (engine.isManualMode() && railStatusRect().contains(mouseX, mouseY, 2)) {
			drawTooltip(canvas, "Back to vanilla music", railStatusRect());
		}
	}

	private void drawTooltip(SantoraCanvas canvas, String text, Rect anchor) {
		int w = canvas.textWidth(text) + 8;
		int h = canvas.lineHeight() + 5;
		int x = clamp(anchor.x() + anchor.w() / 2 - w / 2, winX + 2, winX + winW - w - 2);
		int y = anchor.y() - h - 4;
		canvas.fill(x, y, x + w, y + h, Theme.DECK);
		canvas.outline(x, y, w, h, Theme.FRAME);
		canvas.text(text, x + 4, y + 3, Theme.TEXT_PRIMARY, false);
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

	private int toggleColor(Rect rect, int mouseX, int mouseY, boolean on) {
		boolean hover = rect.contains(mouseX, mouseY, 2);
		return on ? Theme.ACCENT : (hover ? Theme.TEXT_SECONDARY : Theme.TEXT_MUTED);
	}

	private void drawShuffleIcon(SantoraCanvas canvas, int x, int cy, int color) {
		canvas.fill(x, cy - 3, x + 2, cy - 2, color);
		canvas.fill(x, cy + 3, x + 2, cy + 4, color);
		for (int i = 0; i < 7; i++) {
			canvas.fill(x + 2 + i, cy - 3 + i, x + 3 + i, cy - 2 + i, color);
			canvas.fill(x + 2 + i, cy + 3 - i, x + 3 + i, cy + 4 - i, color);
		}
		drawTriangleRight(canvas, x + 9, cy - 3, 3, color);
		drawTriangleRight(canvas, x + 9, cy + 3, 3, color);
	}

	private void drawRepeatIcon(SantoraCanvas canvas, int x, int cy, int color, boolean one) {
		canvas.fill(x + 1, cy - 4, x + 10, cy - 3, color);
		canvas.fill(x, cy - 3, x + 1, cy + 3, color);
		canvas.fill(x + 10, cy - 3, x + 11, cy + 3, color);
		canvas.fill(x + 1, cy + 3, x + 3, cy + 4, color);
		canvas.fill(x + 6, cy + 3, x + 10, cy + 4, color);
		drawTriangleLeft(canvas, x + 4, cy + 3, 3, color);
		if (one) {
			canvas.fill(x + 6, cy - 2, x + 7, cy + 2, color);
			canvas.fill(x + 5, cy - 1, x + 6, cy, color);
		}
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
		if (movingOverlay) {
			Rect card = overlayCardRect();
			if (button == 0 && card.contains(mouseX, mouseY)) {
				overlayDragging = true;
				overlayGrabDX = mouseX - card.x();
				overlayGrabDY = mouseY - card.y();
			} else if (button == 0 && overlayDoneRect().contains(mouseX, mouseY, 2)) {
				exitOverlayMove();
			}
			return true;
		}

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

		boolean listVisible = view == View.QUEUE || view == View.SEARCH || openAlbum() != null;
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
		canvas.fill(field.x(), field.y(), field.right(), field.bottom(), Theme.INPUT_BG);
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
		if (railStatusRect().contains(mouseX, mouseY, 2)) {
			if (engine.isManualMode()) {
				engine.setManualMode(false);
			}
			return true;
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
		settingsScroll = 0;
	}

	private boolean clickMain(int mouseX, int mouseY) {
		Rect main = mainRect();
		if (!main.contains(mouseX, mouseY)) {
			return false;
		}

		if (view == View.SETTINGS) {
			clickSettings(mouseX, mouseY);
			return true;
		}

		if (engine.library().isEmpty()) {
			return true;
		}

		if (view == View.QUEUE && engine.queue().upcomingCount() > 0
				&& clearQueueRect().contains(mouseX, mouseY)) {
			engine.queue().clearUpcoming();
			return true;
		}

		if (view == View.SEARCH || view == View.QUEUE) {
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
					dragIndex = upIndex;
					dragArmY = mouseY;
					dragging = false;
				} else if (view == View.SEARCH) {
					engine.playTrack(tracks.get(i));
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

	private int queueRowOffset() {
		return engine.currentTrack() != null ? 1 : 0;
	}

	public boolean mouseDragged(int mouseX, int mouseY, int button) {
		if (movingOverlay) {
			if (overlayDragging) {
				NowPlayingOverlay.position(engine.config(),
						mouseX - overlayGrabDX, mouseY - overlayGrabDY, screenW, screenH);
			}
			return true;
		}
		if (button == 0 && activeSlider != SLIDER_NONE) {
			applySlider(activeSlider, mouseX);
			return true;
		}
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
		if (movingOverlay) {
			overlayDragging = false;
			return true;
		}
		if (button == 0 && activeSlider != SLIDER_NONE) {
			activeSlider = SLIDER_NONE;
			return true;
		}
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
			cancelDrag();
			return;
		}
		dragIndex = clamp(dragIndex, 0, upcomingCount - 1);

		Rect list = listRect();
		int row = (mouseY - list.y() + trackScroll) / Theme.ROW_HEIGHT;
		int target = clamp(row - base, 0, upcomingCount - 1);

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
		if (deckVolumeRect().contains(mouseX, mouseY, 3)) {
			activeSlider = SLIDER_DECK_VOLUME;
			applySlider(activeSlider, mouseX);
		} else if (deckPrevRect().contains(mouseX, mouseY, 3)) {
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
		if (naming || movingOverlay) {
			return true;
		}
		closeMenu();

		Rect main = mainRect();
		if (!main.contains(mouseX, mouseY)) {
			return false;
		}
		int step = (int) (-amount * 16);

		if (view == View.SETTINGS) {
			int max = Math.max(0, settingsContentHeight() - main.h());
			settingsScroll = clamp(settingsScroll + step, 0, max);
		} else if (view != View.QUEUE && view != View.SEARCH && openAlbum() == null) {
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
		if (movingOverlay) {
			if (keyCode == 256) { // escape
				exitOverlayMove();
			}
			return true;
		}

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

		if (view == View.SEARCH) {
			if (keyCode == 259 && !searchQuery.isEmpty()) { // backspace
				searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
				trackScroll = 0;
				return true;
			}
			if (keyCode == 256 && !searchQuery.isEmpty()) { // escape clears first
				searchQuery = "";
				trackScroll = 0;
				return true;
			}
			return false;
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
		if (movingOverlay) {
			return true;
		}
		if (naming) {
			if (event.isAllowedChatCharacter() && nameInput.length() < NAME_INPUT_MAX) {
				nameInput.append(event.codepointAsString());
			}
			return true;
		}
		if (view == View.SEARCH && menuItems == null) {
			if (event.isAllowedChatCharacter() && searchQuery.length() < SEARCH_INPUT_MAX) {
				searchQuery += event.codepointAsString();
				trackScroll = 0;
			}
			return true;
		}
		return false;
	}

	public void onClose() {
		Santora.saveConfig();
		Santora.savePlaylists();
	}

	private static int clamp(int value, int min, int max) {
		return value < min ? min : (value > max ? max : value);
	}
}
