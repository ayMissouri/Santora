package dev.santora.party;

import com.mojang.logging.LogUtils;
import dev.santora.core.model.Track;
import dev.santora.core.party.DriftCalculator;
import dev.santora.core.party.FollowerSnapshot;
import dev.santora.core.party.PartyMember;
import dev.santora.core.party.PartyMessage;
import dev.santora.core.party.PartySession;
import dev.santora.core.party.PublicParty;
import dev.santora.core.party.SyncAction;
import dev.santora.core.party.SyncDecision;
import dev.santora.engine.MusicEngine;
import dev.santora.engine.PartyBridge;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;

public final class PartyController implements PartyBridge {
	// replace to use your own hosted relay
	public static final String RELAY_URL = "wss://santora.up.railway.app";

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final PartyController INSTANCE = new PartyController();

	private final MusicEngine engine = MusicEngine.get();
	private final PartySession session = new PartySession();
	private final PartyConnection connection = new PartyConnection();
	private final PartyCodec codec = new PartyCodec();

	private long seq;
	private int lastQueueHash;
	private boolean hostingPublic;
	private boolean browsing;
	private boolean publicListLoaded;
	private long browseRefreshTicks;
	private long browseIdleTicks;
	private List<PublicParty> publicParties = List.of();
	private long tickCounter;
	private long lastReseekMono;
	private long lastStartSeq = Long.MIN_VALUE;
	private boolean missingHostTrack;
	private PartyConnection.Status lastStatus = PartyConnection.Status.OFFLINE;

	private PartyController() {
	}

	public static PartyController get() {
		return INSTANCE;
	}

	// UI-facing state

	public boolean inParty() {
		return session.inParty();
	}

	public boolean isHost() {
		return session.isHost();
	}

	public boolean isMember() {
		return session.isMember();
	}

	public boolean canControlPlayback() {
		return session.canControlPlayback();
	}

	public boolean canQueue() {
		return session.canQueue();
	}

	public boolean guestQueue() {
		return engine.config().partyGuestQueue();
	}

	public String code() {
		return session.code();
	}

	public String hostName() {
		return session.hostName();
	}

	public String hostId() {
		return session.hostId();
	}

	public String suggestedName() {
		return displayName();
	}

	public List<PartyMember> members() {
		return session.members();
	}

	public List<String> mirrorUpcoming() {
		return session.upcomingMirror();
	}

	public boolean missingHostTrack() {
		return missingHostTrack;
	}

	public boolean hostingPublic() {
		return hostingPublic;
	}

	public List<PublicParty> publicParties() {
		return publicParties;
	}

	public boolean publicListLoaded() {
		return publicListLoaded;
	}

	public String serverScope() {
		return ServerIdentity.scope();
	}

	public String serverAddress() {
		return ServerIdentity.address();
	}

	public boolean connecting() {
		return connection.status() == PartyConnection.Status.CONNECTING;
	}

	public boolean connectionError() {
		return connection.status() == PartyConnection.Status.ERROR;
	}

	public String lastError() {
		return connection.lastError();
	}

	// UI actions

	public void createParty(boolean makePublic) {
		String url = relayUrl();
		if (url.isEmpty()) {
			LOGGER.warn("[Santora] no relay URL configured");
			return;
		}
		leaveQuietly();
		hostingPublic = makePublic;
		connection.start(url, codec.create(displayName(), makePublic, ServerIdentity.key()), false);
	}

	public void browsePublic() {
		if (session.inParty()) {
			return;
		}
		browseIdleTicks = 0;
		if (browsing) {
			return;
		}
		browsing = true;
		publicParties = List.of();
		publicListLoaded = false;
		browseRefreshTicks = 0;
		requestPublicList();
	}

	public void refreshPublicList() {
		if (session.inParty()) {
			return;
		}
		if (!browsing) {
			browsePublic();
			return;
		}
		browseIdleTicks = 0;
		browseRefreshTicks = 0;
		requestPublicList();
	}

	public void stopBrowsing() {
		if (!browsing) {
			return;
		}
		browsing = false;
		publicParties = List.of();
		publicListLoaded = false;
		if (!session.inParty()) {
			connection.stop();
			lastStatus = PartyConnection.Status.OFFLINE;
		}
	}

	public void joinParty(String code) {
		String url = relayUrl();
		if (url.isEmpty() || code == null || code.isBlank()) {
			return;
		}
		leaveQuietly();
		String normalized = code.trim().toUpperCase(Locale.ROOT);
		connection.start(url, codec.join(normalized, displayName()), true);
	}

	/** Members learn this from the next now-playing broadcast, so nudge one out right away. */
	public void setGuestQueue(boolean allowed) {
		engine.config().setPartyGuestQueue(allowed);
		if (session.isHost()) {
			sendNow();
		}
	}

	public void leaveParty() {
		if (session.inParty()) {
			connection.send(codec.leave());
		}
		leaveQuietly();
	}

	@Override
	public void onPlaybackChanged() {
		if (session.isHost()) {
			sendNow();
		}
	}

	@Override
	public void onMemberEnqueue(Track track, boolean next) {
		if (session.isMember() && session.canQueue() && track != null) {
			connection.send(codec.message(session.hostId(),
					codec.enqueue(new PartyMessage.RequestEnqueue(track.soundPath(), next))));
		}
	}

	public void drainInbound() {
		reactToConnectionStatus();
		String raw;
		while ((raw = connection.poll()) != null) {
			handle(codec.decode(raw));
		}
	}

	public void tickRole() {
		if (session.isHost()) {
			tickHost();
		} else if (session.isMember()) {
			tickFollower();
		} else {
			tickBrowse();
		}
	}

	private void reactToConnectionStatus() {
		PartyConnection.Status status = connection.status();
		if (status == lastStatus) {
			return;
		}
		lastStatus = status;
		if (status == PartyConnection.Status.ERROR && session.isHost()) {
			LOGGER.info("[Santora] party ended: lost the relay connection");
			leaveQuietly();
		}
	}

	private void handle(RelayEvent event) {
		if (event instanceof RelayEvent.Created created) {
			session.onCreated(created.code(), created.self(), displayName());
			engine.setFollower(false);
			missingHostTrack = false;
			lastQueueHash = 0;
		} else if (event instanceof RelayEvent.Joined joined) {
			session.onJoined(joined.code(), joined.self(), displayName(), joined.host(), joined.members());
			engine.setFollower(true);
			engine.setManualMode(true);
			lastStartSeq = Long.MIN_VALUE;
			missingHostTrack = false;
		} else if (event instanceof RelayEvent.PeerJoin peer) {
			session.onPeerJoin(peer.member());
			if (session.isHost()) {
				sendWelcome(peer.member().id());
			}
		} else if (event instanceof RelayEvent.PeerLeave peer) {
			if (session.onPeerLeave(peer.id())) {
				engine.setFollower(false);
				missingHostTrack = false;
				connection.stop();
				lastStatus = PartyConnection.Status.OFFLINE;
			}
		} else if (event instanceof RelayEvent.Rooms rooms) {
			if (browsing) {
				publicParties = rooms.parties();
				publicListLoaded = true;
			}
		} else if (event instanceof RelayEvent.Message message) {
			handlePayload(message.from(), message.payload());
		} else if (event instanceof RelayEvent.ErrorEvent error) {
			LOGGER.warn("[Santora] party error: {}", error.reason());
			if (!session.inParty()) {
				connection.stop();
			}
		}
	}

	private void handlePayload(String from, PartyMessage payload) {
		if (payload instanceof PartyMessage.RequestEnqueue request) {
			if (session.isHost() && engine.config().partyGuestQueue()) {
				engine.library().trackByPath(request.soundPath())
						.ifPresent(track -> engine.requestEnqueue(track, request.next()));
			}
			return;
		}
		if (!session.isMember() || !from.equals(session.hostId())) {
			return;
		}
		if (payload instanceof PartyMessage.NowPlaying now) {
			session.onNow(now, monoMillis());
		} else if (payload instanceof PartyMessage.QueueSnapshot queue) {
			session.onQueue(queue);
		} else if (payload instanceof PartyMessage.Welcome welcome) {
			session.onWelcome(welcome, monoMillis());
		}
	}

	// Browsing public parties

	private void tickBrowse() {
		if (!browsing) {
			return;
		}
		browseIdleTicks++;
		if (browseIdleTicks > PartyProtocol.BROWSE_IDLE_TICKS) {
			stopBrowsing();
			return;
		}
		browseRefreshTicks++;
		if (browseRefreshTicks >= PartyProtocol.BROWSE_REFRESH_TICKS) {
			browseRefreshTicks = 0;
			requestPublicList();
		}
	}

	private void requestPublicList() {
		String url = relayUrl();
		if (url.isEmpty()) {
			return;
		}
		String frame = codec.list(ServerIdentity.key());
		if (connection.status() == PartyConnection.Status.OFFLINE) {
			connection.start(url, frame, true);
			return;
		}
		// Already up, or coming back up: the frame goes out now, or on the next open.
		connection.setOpenFrame(frame);
		if (connection.status() == PartyConnection.Status.CONNECTED) {
			connection.send(frame);
		}
	}

	// Host broadcasting

	private void tickHost() {
		tickCounter++;
		if (tickCounter % PartyProtocol.HEARTBEAT_TICKS == 0) {
			sendNow();
		}
		List<Track> upcoming = engine.queue().upcoming(PartyProtocol.QUEUE_LIMIT);
		String contextId = engine.queue().contextId();
		int hash = hashQueue(upcoming, contextId);
		if (hash != lastQueueHash) {
			lastQueueHash = hash;
			sendQueue(upcoming, contextId);
		}
	}

	private void sendNow() {
		connection.send(codec.message(PartyProtocol.BROADCAST, codec.now(nowPlaying())));
	}

	private void sendQueue(List<Track> upcoming, String contextId) {
		List<String> paths = new ArrayList<>(upcoming.size());
		for (Track track : upcoming) {
			paths.add(track.soundPath());
		}
		connection.send(codec.message(PartyProtocol.BROADCAST,
				codec.queue(new PartyMessage.QueueSnapshot(paths, contextId, ++seq))));
	}

	private void sendWelcome(String memberId) {
		List<Track> upcoming = engine.queue().upcoming(PartyProtocol.QUEUE_LIMIT);
		List<String> paths = new ArrayList<>(upcoming.size());
		for (Track track : upcoming) {
			paths.add(track.soundPath());
		}
		PartyMessage.QueueSnapshot queue =
				new PartyMessage.QueueSnapshot(paths, engine.queue().contextId(), ++seq);
		connection.send(codec.message(memberId, codec.welcome(new PartyMessage.Welcome(nowPlaying(), queue))));
	}

	private PartyMessage.NowPlaying nowPlaying() {
		Track track = engine.currentTrack();
		boolean shuffle = engine.queue().shuffle();
		boolean guestQueue = engine.config().partyGuestQueue();
		if (track == null) {
			return new PartyMessage.NowPlaying("", 0, false, shuffle, engine.queue().repeat(),
					guestQueue, ++seq);
		}
		return new PartyMessage.NowPlaying(track.soundPath(), engine.elapsedMillis(), engine.isPaused(),
				shuffle, engine.queue().repeat(), guestQueue, ++seq);
	}

	private void tickFollower() {
		PartyMessage.NowPlaying now = session.lastNow();
		if (now == null) {
			return;
		}
		long monoNow = monoMillis();
		Optional<Track> track = engine.library().trackByPath(now.soundPath());
		boolean available = track.isPresent();
		long durationMillis = 0;
		if (available) {
			OptionalDouble seconds = engine.durationSeconds(track.get());
			if (seconds.isPresent()) {
				durationMillis = (long) (seconds.getAsDouble() * 1000);
			}
		}

		Track current = engine.currentTrack();
		FollowerSnapshot local = new FollowerSnapshot(
				current == null ? "" : current.soundPath(),
				current != null,
				engine.isPaused(),
				engine.elapsedMillis());

		SyncDecision decision = DriftCalculator.decide(now, available, durationMillis,
				monoNow - session.lastNowReceiptMillis(), local, monoNow - lastReseekMono);
		missingHostTrack = decision.action() == SyncAction.MISSING;

		switch (decision.action()) {
			case START -> {
				if (now.seq() != lastStartSeq) {
					lastStartSeq = now.seq();
					engine.followerStart(track.get(), decision.targetMillis(), now.paused());
					lastReseekMono = monoNow;
				}
			}
			case RESEEK -> {
				engine.followerStart(track.get(), decision.targetMillis(), now.paused());
				lastReseekMono = monoNow;
			}
			case PAUSE -> engine.followerSetPaused(true);
			case RESUME -> engine.followerSetPaused(false);
			case STOP, MISSING -> engine.followerIdle();
			case NONE -> {
			}
		}
	}

	// Helpers

	private void leaveQuietly() {
		browsing = false;
		publicParties = List.of();
		publicListLoaded = false;
		hostingPublic = false;
		connection.stop();
		session.reset();
		engine.setFollower(false);
		missingHostTrack = false;
		lastStartSeq = Long.MIN_VALUE;
		lastQueueHash = 0;
		lastStatus = PartyConnection.Status.OFFLINE;
	}

	private String relayUrl() {
		return RELAY_URL == null ? "" : RELAY_URL.trim();
	}

	private String displayName() {
		String configured = engine.config().displayName();
		if (configured != null && !configured.isBlank()) {
			return configured;
		}
		try {
			return Minecraft.getInstance().getUser().getName();
		} catch (RuntimeException e) {
			return "Player";
		}
	}

	private static int hashQueue(List<Track> upcoming, String contextId) {
		int hash = contextId.hashCode();
		for (Track track : upcoming) {
			hash = hash * 31 + track.soundPath().hashCode();
		}
		return hash;
	}

	private static long monoMillis() {
		return System.nanoTime() / 1_000_000L;
	}
}
