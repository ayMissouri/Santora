package dev.santora.party;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class PartyConnection {

	private static final Logger LOGGER = LogUtils.getLogger();

	enum Status {
		OFFLINE, CONNECTING, CONNECTED, ERROR
	}

	private final ConcurrentLinkedQueue<String> inbound = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<String> outbound = new ConcurrentLinkedQueue<>();
	private final AtomicBoolean sending = new AtomicBoolean(false);
	private final AtomicBoolean reconnectPending = new AtomicBoolean(false);

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "Santora-Party");
		t.setDaemon(true);
		return t;
	});

	private volatile Status status = Status.OFFLINE;
	private volatile String lastError = "";
	private volatile boolean active;
	private volatile boolean autoReconnect;
	private volatile String url = "";
	private volatile String openFrame = "";
	private volatile WebSocket socket;
	private volatile int attempt;
	private volatile int epoch;

	private HttpClient client;

	Status status() {
		return status;
	}

	String lastError() {
		return lastError;
	}

	String poll() {
		return inbound.poll();
	}

	void start(String url, String openFrame, boolean autoReconnect) {
		epoch++;
		this.url = url;
		this.openFrame = openFrame;
		this.autoReconnect = autoReconnect;
		this.active = true;
		this.attempt = 0;
		this.lastError = "";
		inbound.clear();
		outbound.clear();
		connect();
	}

	void setOpenFrame(String frame) {
		this.openFrame = frame;
	}

	void stop() {
		epoch++;
		active = false;
		autoReconnect = false;
		status = Status.OFFLINE;
		WebSocket s = socket;
		socket = null;
		if (s != null) {
			try {
				s.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
			} catch (RuntimeException ignored) {
			}
		}
		inbound.clear();
		outbound.clear();
	}

	void send(String frame) {
		if (!active || frame == null) {
			return;
		}
		outbound.add(frame);
		pump();
	}

	private void connect() {
		if (!active) {
			return;
		}
		final int gen = epoch;
		status = Status.CONNECTING;
		URI uri;
		try {
			uri = URI.create(url);
		} catch (RuntimeException e) {
			fail("Bad relay URL");
			return;
		}
		if (client == null) {
			client = HttpClient.newHttpClient();
		}
		client.newWebSocketBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.buildAsync(uri, new Listener(gen))
				.whenComplete((ws, error) -> {
					if (gen != epoch) {
						if (ws != null) {
							ws.abort();
						}
						return;
					}
					if (error != null || ws == null) {
						fail(error == null ? "Could not reach the relay" : error.getMessage());
						scheduleReconnect();
					} else {
						socket = ws;
						attempt = 0;
						status = Status.CONNECTED;
						lastError = "";
						if (!openFrame.isEmpty()) {
							send(openFrame);
						}
					}
				});
	}

	private void pump() {
		if (!sending.compareAndSet(false, true)) {
			return;
		}
		WebSocket s = socket;
		String frame = s == null ? null : outbound.poll();
		if (frame == null) {
			sending.set(false);
			if (s != null && !outbound.isEmpty()) {
				pump();
			}
			return;
		}
		s.sendText(frame, true).whenComplete((result, error) -> {
			sending.set(false);
			if (error != null) {
				fail(error.getMessage());
				scheduleReconnect();
			} else {
				pump();
			}
		});
	}

	private void fail(String message) {
		status = Status.ERROR;
		lastError = message == null ? "Connection error" : message;
		LOGGER.warn("[Santora] party connection: {}", lastError);
	}

	private void scheduleReconnect() {
		if (!active || !autoReconnect) {
			return;
		}
		if (!reconnectPending.compareAndSet(false, true)) {
			return;
		}
		final int gen = epoch;
		long delaySeconds = Math.min(30L, 1L << Math.min(attempt, 5));
		attempt++;
		scheduler.schedule(() -> {
			reconnectPending.set(false);
			if (gen == epoch) {
				connect();
			}
		}, delaySeconds, TimeUnit.SECONDS);
	}

	private final class Listener implements WebSocket.Listener {

		private final int gen;
		private final StringBuilder buffer = new StringBuilder();

		Listener(int gen) {
			this.gen = gen;
		}

		@Override
		public void onOpen(WebSocket webSocket) {
			webSocket.request(1);
		}

		@Override
		public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
			if (gen != epoch) {
				return null;
			}
			buffer.append(data);
			if (last) {
				inbound.add(buffer.toString());
				buffer.setLength(0);
			}
			webSocket.request(1);
			return null;
		}

		@Override
		public void onError(WebSocket webSocket, Throwable error) {
			if (gen == epoch && active) {
				fail(error == null ? "Connection error" : error.getMessage());
				scheduleReconnect();
			}
		}

		@Override
		public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
			if (gen == epoch && active) {
				status = Status.ERROR;
				scheduleReconnect();
			}
			return null;
		}
	}
}
