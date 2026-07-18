package dev.santora.party;

import dev.santora.core.party.PartyMember;
import dev.santora.core.party.PartyMessage;

import java.util.List;

sealed interface RelayEvent {

	record Created(String code, String self) implements RelayEvent {
	}

	record Joined(String code, String self, String host, List<PartyMember> members) implements RelayEvent {
	}

	record PeerJoin(PartyMember member) implements RelayEvent {
	}

	record PeerLeave(String id) implements RelayEvent {
	}

	record Message(String from, long serverTime, PartyMessage payload) implements RelayEvent {
	}

	record ErrorEvent(String reason) implements RelayEvent {
	}

	record Ignored() implements RelayEvent {
	}
}
