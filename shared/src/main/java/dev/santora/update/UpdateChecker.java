package dev.santora.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import dev.santora.Santora;
import dev.santora.core.update.ModVersion;
import dev.santora.platform.SantoraPlatform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

public final class UpdateChecker {

	public enum State {
		OFF, CHECKING, UP_TO_DATE, OUTDATED, FAILED
	}

	public static final String RELEASES_PAGE = "https://github.com/ayMissouri/Santora/releases/latest";

	private static final String LATEST_RELEASE_API =
			"https://api.github.com/repos/ayMissouri/Santora/releases/latest";

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final UpdateChecker INSTANCE = new UpdateChecker();

	private static final long TOAST_MILLIS = 10_000L;

	private final String current = readVersion();

	private volatile State state = State.OFF;
	private volatile String latest = "";
	private volatile boolean noticePending;
	private volatile int epoch;

	private HttpClient client;
	private SystemToast.SystemToastId toastId;

	private UpdateChecker() {
	}

	public static UpdateChecker get() {
		return INSTANCE;
	}

	public State state() {
		return state;
	}

	public boolean updateAvailable() {
		return state == State.OUTDATED;
	}

	public String currentVersion() {
		return current.isEmpty() ? "unknown" : current;
	}

	public String latestVersion() {
		return latest;
	}

	public void check() {
		if (state == State.CHECKING) {
			return;
		}
		epoch++;
		if (current.isEmpty()) {
			fail("Santora doesn't know its own version");
			return;
		}
		state = State.CHECKING;
		request(epoch);
	}

	public void stop() {
		epoch++;
		state = State.OFF;
		noticePending = false;
	}

	public void tick() {
		if (!noticePending) {
			return;
		}
		noticePending = false;
		SystemToast.add(SantoraPlatform.Holder.get().toastManager(), toastId(),
				Component.literal("Santora " + latest + " is out"),
				Component.literal("You're on " + current));
	}

	public String statusLine() {
		return switch (state) {
			case OFF -> "Checks are off";
			case CHECKING -> "Looking for a newer version...";
			case UP_TO_DATE -> "You're on the latest version";
			case OUTDATED -> latest + " is out, grab it from GitHub";
			case FAILED -> "Couldn't reach GitHub";
		};
	}

	private void request(int gen) {
		HttpRequest request;
		try {
			request = HttpRequest.newBuilder(URI.create(LATEST_RELEASE_API))
					.timeout(Duration.ofSeconds(15))
					.header("Accept", "application/vnd.github+json")
					.header("User-Agent", "Santora/" + current)
					.GET()
					.build();
		} catch (RuntimeException e) {
			fail("bad release URL");
			return;
		}

		client().sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenAccept(response -> accept(gen, response))
				.exceptionally(error -> {
					if (gen == epoch) {
						fail(reason(error));
					}
					return null;
				});
	}

	private void accept(int gen, HttpResponse<String> response) {
		if (gen != epoch) {
			return;
		}
		if (response.statusCode() != 200) {
			fail("GitHub answered " + response.statusCode());
			return;
		}

		String tag = "";
		try {
			JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
			if (json.has("tag_name")) {
				tag = json.get("tag_name").getAsString();
			}
		} catch (RuntimeException e) {
			fail("the release feed didn't make sense");
			return;
		}
		if (tag.isEmpty()) {
			fail("the latest release has no tag");
			return;
		}

		latest = ModVersion.parse(tag).map(ModVersion::toString).orElse(tag);
		if (ModVersion.isNewer(tag, current)) {
			state = State.OUTDATED;
			noticePending = true;
			LOGGER.info("[Santora] {} is out; this is {}", latest, current);
		} else {
			state = State.UP_TO_DATE;
		}
	}

	private void fail(String reason) {
		state = State.FAILED;
		LOGGER.warn("[Santora] could not check for updates: {}", reason);
	}

	private synchronized HttpClient client() {
		if (client == null) {
			client = HttpClient.newBuilder()
					.connectTimeout(Duration.ofSeconds(10))
					.followRedirects(HttpClient.Redirect.NORMAL)
					.executor(Executors.newSingleThreadExecutor(r -> {
						Thread thread = new Thread(r, "Santora-Updates");
						thread.setDaemon(true);
						return thread;
					}))
					.build();
		}
		return client;
	}

	private SystemToast.SystemToastId toastId() {
		if (toastId == null) {
			toastId = new SystemToast.SystemToastId(TOAST_MILLIS);
		}
		return toastId;
	}

	private static String reason(Throwable error) {
		Throwable cause = error instanceof CompletionException && error.getCause() != null
				? error.getCause()
				: error;
		return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
	}

	private static String readVersion() {
		return FabricLoader.getInstance().getModContainer(Santora.MOD_ID)
				.map(mod -> mod.getMetadata().getVersion().getFriendlyString())
				.orElse("");
	}
}
