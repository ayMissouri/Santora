package dev.santora.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import dev.santora.core.model.Playlists;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PlaylistIo {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private PlaylistIo() {
	}

	private static Path playlistPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("santora_playlists.json");
	}

	public static void load(Playlists playlists) {
		Path path = playlistPath();
		if (!Files.exists(path)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

			List<String> favorites = readStrings(json.get("favorites"));
			List<Playlists.Playlist> lists = new ArrayList<>();
			if (json.get("playlists") instanceof JsonArray array) {
				for (JsonElement element : array) {
					if (!(element instanceof JsonObject entry)) {
						continue;
					}
					String id = entry.has("id") ? entry.get("id").getAsString() : "";
					String name = entry.has("name") ? entry.get("name").getAsString() : "";
					if (!id.isEmpty()) {
						lists.add(new Playlists.Playlist(id, name, readStrings(entry.get("tracks"))));
					}
				}
			}
			playlists.restore(favorites, lists);
		} catch (Exception e) {
			LOGGER.warn("[Santora] could not read {}; starting without playlists", path, e);
		}
	}

	public static void save(Playlists playlists) {
		JsonObject json = new JsonObject();

		JsonArray favorites = new JsonArray();
		playlists.favoritePaths().forEach(favorites::add);
		json.add("favorites", favorites);

		JsonArray lists = new JsonArray();
		for (Playlists.Playlist playlist : playlists.all()) {
			JsonObject entry = new JsonObject();
			entry.addProperty("id", playlist.id());
			entry.addProperty("name", playlist.name());
			JsonArray tracks = new JsonArray();
			playlist.trackPaths().forEach(tracks::add);
			entry.add("tracks", tracks);
			lists.add(entry);
		}
		json.add("playlists", lists);

		Path path = playlistPath();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(json, writer);
			}
		} catch (IOException e) {
			LOGGER.warn("[Santora] could not write {}", path, e);
		}
	}

	private static List<String> readStrings(JsonElement element) {
		List<String> out = new ArrayList<>();
		if (element instanceof JsonArray array) {
			for (JsonElement item : array) {
				try {
					out.add(item.getAsString());
				} catch (RuntimeException e) {}
			}
		}
		return out;
	}
}
