package dev.santora.core.model;

public enum MusicContext {
	MENU("music/menu/", "Menu", "menu"),
	CREATIVE("music/game/creative/", "Creative", "creative"),
	END("music/game/end/", "The End", "end"),
	NETHER("music/game/nether/", "The Nether", "nether"),
	SWAMP("music/game/swamp/", "Swamp", "swamp"),
	UNDERWATER("music/game/water/", "Underwater", "underwater"),
	DISCS("records/", "Music Discs", "discs"),
	OVERWORLD("music/game/", "Overworld", "overworld"),
	OTHER("", "Other", "other");

	private final String prefix;
	private final String displayName;
	private final String id;

	MusicContext(String prefix, String displayName, String id) {
		this.prefix = prefix;
		this.displayName = displayName;
		this.id = id;
	}

	public String prefix() {
		return prefix;
	}

	public String displayName() {
		return displayName;
	}

	public String id() {
		return id;
	}

	public static MusicContext of(String soundPath) {
		String path = stripNamespace(soundPath);
		for (MusicContext context : values()) {
			if (context != OTHER && path.startsWith(context.prefix)) {
				return context;
			}
		}
		return OTHER;
	}

	private static String stripNamespace(String soundPath) {
		int colon = soundPath.indexOf(':');
		return colon < 0 ? soundPath : soundPath.substring(colon + 1);
	}
}
