package dev.santora.core.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum MusicUpdate {

	CLASSIC("classic", "Classic", "2009", Set.of(
			"minecraft", "clark", "sweden")),

	ALPHA("alpha", "Alpha", "2010", Set.of(
			"subwoofer_lullaby", "living_mice", "haggstrom", "danny", "key",
			"oxygene", "dry_hands", "wet_hands", "mice_on_venus",
			"13", "cat")),

	ADVENTURE("adventure", "Adventure Update", "1.0", Set.of(
			"blocks", "chirp", "far", "mall", "mellohi", "stal", "strad", "ward", "11")),

	PRETTY_SCARY("pretty_scary", "Pretty Scary Update", "1.4", Set.of(
			"wait")),

	HORSE("horse", "Horse Update", "1.6", Set.of(
			"mutation", "moog_city_2", "beginning_2", "floating_trees",
			"biome_fest", "blind_spots", "haunt_muskie", "aria_math", "dreiton", "taswell",
			"concrete_halls", "dead_voxel", "warmth", "ballad_of_the_cats",
			"the_end", "boss", "alpha")),

	AQUATIC("aquatic", "Update Aquatic", "1.13", Set.of(
			"axolotl", "dragon_fish", "shuniji")),

	NETHER("nether", "Nether Update", "1.16", Set.of(
			"chrysopoeia", "rubedo", "so_below", "pigstep")),

	CAVES_AND_CLIFFS("caves_and_cliffs", "Caves & Cliffs", "1.18", Set.of(
			"stand_tall", "left_to_bloom", "wending", "infinite_amethyst", "an_ordinary_day",
			"floating_dream", "comforting_memories", "one_more_day",
			"otherside")),

	WILD("wild", "The Wild Update", "1.19", Set.of(
			"aerie", "firebugs", "labyrinthine", "ancestry", "5")),

	TRAILS_AND_TALES("trails_and_tales", "Trails & Tales", "1.20", Set.of(
			"a_familiar_room", "bromeliad", "crescent_dunes", "echo_in_the_wind", "relic")),

	TRICKY_TRIALS("tricky_trials", "Tricky Trials", "1.21", Set.of(
			"featherfall", "deeper", "eld_unknown", "endless",
			"komorebi", "pokopoko", "yakusoku", "puzzlebox", "watcher",
			"creator", "creator_music_box", "precipice")),

	CHASE_THE_SKIES("chase_the_skies", "Chase the Skies", "1.21.6", Set.of(
			"below_and_above", "broken_clocks", "fireflies", "os_piano", "lilypad",
			"tears", "lava_chicken")),

	CHAOS_CUBED("chaos_cubed", "Chaos Cubed", "26.2", Set.of(
			"ebb", "home", "memories", "nightly", "shores", "bounce")),

	// Any modded music or future updates will go here.
	OTHER("other", "Other", "", Set.of());

	private static final Map<String, MusicUpdate> BY_FILE_NAME = new HashMap<>();

	static {
		for (MusicUpdate update : values()) {
			for (String fileName : update.fileNames) {
				BY_FILE_NAME.put(fileName, update);
			}
		}
	}

	private final String id;
	private final String displayName;
	private final String versionLabel;
	private final Set<String> fileNames;

	MusicUpdate(String id, String displayName, String versionLabel, Set<String> fileNames) {
		this.id = id;
		this.displayName = displayName;
		this.versionLabel = versionLabel;
		this.fileNames = fileNames;
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return displayName;
	}

	public String versionLabel() {
		return versionLabel;
	}

	public Set<String> fileNames() {
		return fileNames;
	}

	public static MusicUpdate of(String soundPath) {
		int colon = soundPath.indexOf(':');
		String namespace = colon < 0 ? "minecraft" : soundPath.substring(0, colon);
		if (!namespace.equals("minecraft")) {
			return OTHER;
		}
		String path = colon < 0 ? soundPath : soundPath.substring(colon + 1);
		int slash = path.lastIndexOf('/');
		String fileName = slash < 0 ? path : path.substring(slash + 1);
		return BY_FILE_NAME.getOrDefault(fileName, OTHER);
	}
}
