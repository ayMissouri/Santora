package dev.santora.party;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.santora.core.party.PartyMember;
import dev.santora.core.party.PartyMessage;
import dev.santora.core.play.RepeatMode;

import java.util.ArrayList;
import java.util.List;

final class PartyCodec {

	private final Gson gson = new Gson();

	String create(String name) {
		JsonObject o = envelope(PartyProtocol.T_CREATE);
		o.addProperty("v", PartyProtocol.VERSION);
		o.addProperty("name", name);
		return gson.toJson(o);
	}

	String join(String code, String name) {
		JsonObject o = envelope(PartyProtocol.T_JOIN);
		o.addProperty("v", PartyProtocol.VERSION);
		o.addProperty("code", code);
		o.addProperty("name", name);
		return gson.toJson(o);
	}

	String leave() {
		return gson.toJson(envelope(PartyProtocol.T_LEAVE));
	}

	String message(String to, JsonObject data) {
		JsonObject o = envelope(PartyProtocol.T_MSG);
		o.addProperty("to", to);
		o.add("data", data);
		return gson.toJson(o);
	}

	JsonObject now(PartyMessage.NowPlaying n) {
		JsonObject o = new JsonObject();
		o.addProperty("t", PartyProtocol.T_NOW);
		o.addProperty("path", n.soundPath());
		o.addProperty("pos", n.positionMillis());
		o.addProperty("paused", n.paused());
		o.addProperty("shuffle", n.shuffle());
		o.addProperty("repeat", n.repeat().name());
		o.addProperty("seq", n.seq());
		return o;
	}

	JsonObject queue(PartyMessage.QueueSnapshot q) {
		JsonObject o = new JsonObject();
		o.addProperty("t", PartyProtocol.T_QUEUE);
		JsonArray arr = new JsonArray();
		q.upcoming().forEach(arr::add);
		o.add("upcoming", arr);
		o.addProperty("ctx", q.contextId());
		o.addProperty("seq", q.seq());
		return o;
	}

	JsonObject welcome(PartyMessage.Welcome w) {
		JsonObject o = new JsonObject();
		o.addProperty("t", PartyProtocol.T_WELCOME);
		if (w.now() != null) {
			o.add("now", now(w.now()));
		}
		if (w.queue() != null) {
			o.add("queue", queue(w.queue()));
		}
		return o;
	}

	JsonObject enqueue(PartyMessage.RequestEnqueue e) {
		JsonObject o = new JsonObject();
		o.addProperty("t", PartyProtocol.T_ENQ);
		o.addProperty("path", e.soundPath());
		o.addProperty("next", e.next());
		return o;
	}

	// Inbound

	RelayEvent decode(String json) {
		JsonObject o = parse(json);
		if (o == null) {
			return new RelayEvent.Ignored();
		}
		return switch (str(o, "t", "")) {
			case PartyProtocol.T_CREATED -> new RelayEvent.Created(str(o, "code", ""), str(o, "self", ""));
			case PartyProtocol.T_JOINED -> new RelayEvent.Joined(
					str(o, "code", ""), str(o, "self", ""), str(o, "host", ""), members(o.get("members")));
			case PartyProtocol.T_PEER_JOIN -> {
				PartyMember member = member(o.get("member"));
				yield member == null ? new RelayEvent.Ignored() : new RelayEvent.PeerJoin(member);
			}
			case PartyProtocol.T_PEER_LEAVE -> new RelayEvent.PeerLeave(str(o, "id", ""));
			case PartyProtocol.T_ERROR -> new RelayEvent.ErrorEvent(str(o, "reason", "error"));
			case PartyProtocol.T_MSG -> {
				PartyMessage payload = payload(o.get("data"));
				yield payload == null
						? new RelayEvent.Ignored()
						: new RelayEvent.Message(str(o, "from", ""), lng(o, "ts", 0), payload);
			}
			default -> new RelayEvent.Ignored();
		};
	}

	private PartyMessage payload(JsonElement element) {
		if (element == null || !element.isJsonObject()) {
			return null;
		}
		JsonObject o = element.getAsJsonObject();
		return switch (str(o, "t", "")) {
			case PartyProtocol.T_NOW -> nowPayload(o);
			case PartyProtocol.T_QUEUE -> queuePayload(o);
			case PartyProtocol.T_WELCOME -> new PartyMessage.Welcome(
					nowPayload(child(o, "now")), queuePayload(child(o, "queue")));
			case PartyProtocol.T_ENQ -> new PartyMessage.RequestEnqueue(str(o, "path", ""), bool(o, "next", false));
			default -> null;
		};
	}

	private PartyMessage.NowPlaying nowPayload(JsonObject o) {
		if (o == null) {
			return null;
		}
		return new PartyMessage.NowPlaying(str(o, "path", ""), lng(o, "pos", 0), bool(o, "paused", false),
				bool(o, "shuffle", false), repeat(str(o, "repeat", "OFF")), lng(o, "seq", -1));
	}

	private PartyMessage.QueueSnapshot queuePayload(JsonObject o) {
		if (o == null) {
			return null;
		}
		return new PartyMessage.QueueSnapshot(strings(o.get("upcoming")), str(o, "ctx", ""), lng(o, "seq", -1));
	}

	private static JsonObject envelope(String type) {
		JsonObject o = new JsonObject();
		o.addProperty("t", type);
		return o;
	}

	private static JsonObject parse(String json) {
		try {
			JsonElement element = JsonParser.parseString(json);
			return element.isJsonObject() ? element.getAsJsonObject() : null;
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static JsonObject child(JsonObject o, String key) {
		JsonElement element = o.get(key);
		return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
	}

	private PartyMember member(JsonElement element) {
		if (element == null || !element.isJsonObject()) {
			return null;
		}
		JsonObject o = element.getAsJsonObject();
		String id = str(o, "id", "");
		return id.isEmpty() ? null : new PartyMember(id, sanitize(str(o, "name", "")));
	}

	private List<PartyMember> members(JsonElement element) {
		List<PartyMember> out = new ArrayList<>();
		if (element != null && element.isJsonArray()) {
			for (JsonElement item : element.getAsJsonArray()) {
				PartyMember member = member(item);
				if (member != null) {
					out.add(member);
				}
			}
		}
		return out;
	}

	private static List<String> strings(JsonElement element) {
		List<String> out = new ArrayList<>();
		if (element != null && element.isJsonArray()) {
			JsonArray arr = element.getAsJsonArray();
			for (int i = 0; i < arr.size() && out.size() < PartyProtocol.QUEUE_LIMIT; i++) {
				try {
					String value = arr.get(i).getAsString();
					if (value.length() <= PartyProtocol.MAX_STRING) {
						out.add(value);
					}
				} catch (RuntimeException ignored) {
				}
			}
		}
		return out;
	}

	private static RepeatMode repeat(String value) {
		try {
			return RepeatMode.valueOf(value);
		} catch (IllegalArgumentException e) {
			return RepeatMode.OFF;
		}
	}

	private static String sanitize(String value) {
		String stripped = value.replaceAll("[\\p{Cntrl}]", "");
		return stripped.length() > 32 ? stripped.substring(0, 32) : stripped;
	}

	private static String str(JsonObject o, String key, String fallback) {
		try {
			if (!o.has(key) || o.get(key).isJsonNull()) {
				return fallback;
			}
			String value = o.get(key).getAsString();
			return value.length() > PartyProtocol.MAX_STRING ? value.substring(0, PartyProtocol.MAX_STRING) : value;
		} catch (RuntimeException e) {
			return fallback;
		}
	}

	private static long lng(JsonObject o, String key, long fallback) {
		try {
			return o.has(key) ? o.get(key).getAsLong() : fallback;
		} catch (RuntimeException e) {
			return fallback;
		}
	}

	private static boolean bool(JsonObject o, String key, boolean fallback) {
		try {
			return o.has(key) ? o.get(key).getAsBoolean() : fallback;
		} catch (RuntimeException e) {
			return fallback;
		}
	}
}
