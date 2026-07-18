package dev.santora.engine;

import dev.santora.core.model.Track;

public interface PartyBridge {

	void onPlaybackChanged();

	void onMemberEnqueue(Track track, boolean next);

	PartyBridge NONE = new PartyBridge() {
		@Override
		public void onPlaybackChanged() {
		}

		@Override
		public void onMemberEnqueue(Track track, boolean next) {
		}
	};
}
