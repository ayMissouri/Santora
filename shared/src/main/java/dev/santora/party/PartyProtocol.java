package dev.santora.party;

final class PartyProtocol {

	static final int VERSION = 1;

	// Relay envelope types
	static final String T_CREATE = "create";
	static final String T_JOIN = "join";
	static final String T_LEAVE = "leave";
	static final String T_MSG = "msg";
	static final String T_CREATED = "created";
	static final String T_JOINED = "joined";
	static final String T_PEER_JOIN = "peer_join";
	static final String T_PEER_LEAVE = "peer_leave";
	static final String T_ERROR = "error";

	// Party payload types
	static final String T_NOW = "now";
	static final String T_QUEUE = "queue";
	static final String T_WELCOME = "welcome";
	static final String T_ENQ = "enq";

	static final String BROADCAST = "*";

	static final int QUEUE_LIMIT = 128;
	static final int MAX_STRING = 256;
	static final long HEARTBEAT_TICKS = 40;

	private PartyProtocol() {
	}
}
