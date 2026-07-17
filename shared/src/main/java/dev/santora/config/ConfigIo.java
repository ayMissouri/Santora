package dev.santora.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import dev.santora.core.config.SantoraConfig;
import dev.santora.core.play.RepeatMode;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigIo {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private ConfigIo() {
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("santora.json");
	}

	public static void load(SantoraConfig config) {
		Path path = configPath();
		if (!Files.exists(path)) {
			save(config);
			return;
		}

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

			config.setCrossfadeMillis(getInt(json, "crossfade_millis", config.crossfadeMillis()));
			config.setCrossfadeOn(getBool(json, "crossfade_on", config.crossfadeMillis() > 0));
			int legacyDelay = getInt(json, "track_delay_millis", 0);
			config.setDelayMinMillis(getInt(json, "track_delay_min_millis", legacyDelay));
			config.setDelayMaxMillis(getInt(json, "track_delay_max_millis", legacyDelay));
			config.setVolume(getFloat(json, "volume", config.volume()));
			config.setShuffle(getBool(json, "shuffle", config.shuffle()));
			config.setRepeat(parseRepeat(getString(json, "repeat", config.repeat().name())));
			config.setHideVanillaToast(getBool(json, "hide_vanilla_toast", config.hideVanillaToast()));
			config.setOverlayOn(getBool(json, "overlay_on", config.overlayOn()));
			config.setOverlayPos(getFloat(json, "overlay_x", config.overlayX()),
					getFloat(json, "overlay_y", config.overlayY()));
			config.setResumeOnLaunch(getBool(json, "resume_on_launch", config.resumeOnLaunch()));
			config.setWasManual(getBool(json, "was_manual", config.wasManual()));
			config.setLastContextId(getString(json, "last_context_id", config.lastContextId()));
			config.setLastTrackPath(getString(json, "last_track_path", config.lastTrackPath()));
		} catch (Exception e) {
			LOGGER.warn("[Santora] could not read {}; using defaults", path, e);
		}
	}

	public static void save(SantoraConfig config) {
		JsonObject json = new JsonObject();
		json.addProperty("crossfade_on", config.crossfadeOn());
		json.addProperty("crossfade_millis", config.crossfadeMillis());
		json.addProperty("track_delay_min_millis", config.delayMinMillis());
		json.addProperty("track_delay_max_millis", config.delayMaxMillis());
		json.addProperty("volume", config.volume());
		json.addProperty("shuffle", config.shuffle());
		json.addProperty("repeat", config.repeat().name());
		json.addProperty("hide_vanilla_toast", config.hideVanillaToast());
		json.addProperty("overlay_on", config.overlayOn());
		json.addProperty("overlay_x", config.overlayX());
		json.addProperty("overlay_y", config.overlayY());
		json.addProperty("resume_on_launch", config.resumeOnLaunch());
		json.addProperty("was_manual", config.wasManual());
		json.addProperty("last_context_id", config.lastContextId());
		json.addProperty("last_track_path", config.lastTrackPath());

		Path path = configPath();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(json, writer);
			}
		} catch (IOException e) {
			LOGGER.warn("[Santora] could not write {}", path, e);
		}
	}

	private static RepeatMode parseRepeat(String value) {
		try {
			return RepeatMode.valueOf(value);
		} catch (IllegalArgumentException e) {
			return RepeatMode.OFF;
		}
	}

	private static int getInt(JsonObject json, String key, int fallback) {
		try {
			return json.has(key) ? json.get(key).getAsInt() : fallback;
		} catch (RuntimeException e) {
			return fallback;
		}
	}

	private static float getFloat(JsonObject json, String key, float fallback) {
		try {
			return json.has(key) ? json.get(key).getAsFloat() : fallback;
		} catch (RuntimeException e) {
			return fallback;
		}
	}

	private static boolean getBool(JsonObject json, String key, boolean fallback) {
		try {
			return json.has(key) ? json.get(key).getAsBoolean() : fallback;
		} catch (RuntimeException e) {
			return fallback;
		}
	}

	private static String getString(JsonObject json, String key, String fallback) {
		try {
			return json.has(key) ? json.get(key).getAsString() : fallback;
		} catch (RuntimeException e) {
			return fallback;
		}
	}
}
