package dev.santora.core.party;

import dev.santora.core.play.RepeatMode;

import java.util.List;

public sealed interface PartyMessage {

	record NowPlaying(String soundPath, long positionMillis, boolean paused,
			boolean shuffle, RepeatMode repeat, long seq) implements PartyMessage {
	}

	record QueueSnapshot(List<String> upcoming, String contextId, long seq) implements PartyMessage {
		public QueueSnapshot {
			upcoming = List.copyOf(upcoming);
		}
	}

	record Welcome(NowPlaying now, QueueSnapshot queue) implements PartyMessage {
	}

	record RequestEnqueue(String soundPath, boolean next) implements PartyMessage {
	}
}
