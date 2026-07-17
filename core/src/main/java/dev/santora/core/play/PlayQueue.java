package dev.santora.core.play;

import dev.santora.core.model.Track;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class PlayQueue {

	private static final int MAX_HISTORY = 256;

	private List<Track> context = List.of();
	private String contextId = "";

	private final List<Integer> order = new ArrayList<>();
	private int cursor = -1;

	private final List<Track> userQueue = new ArrayList<>();
	private final Deque<Track> history = new ArrayDeque<>();

	private Track current;
	private boolean shuffle;
	private RepeatMode repeat = RepeatMode.OFF;

	private final Random random;

	public PlayQueue() {
		this(new Random());
	}

	public PlayQueue(Random random) {
		this.random = random;
	}

	public void setContext(String contextId, List<Track> tracks, int startIndex) {
		this.contextId = contextId == null ? "" : contextId;
		this.context = List.copyOf(tracks);
		rebuildOrder();
		cursor = -1;

		if (startIndex >= 0 && startIndex < context.size()) {
			int at = order.indexOf(startIndex);
			if (shuffle) {
				// Put the song the player clicked at the front so it plays first.
				if (at > 0) {
					Collections.swap(order, 0, at);
				}
			} else {
				cursor = at - 1;
			}
		}
	}

	public String contextId() {
		return contextId;
	}

	public List<Track> context() {
		return context;
	}

	private void rebuildOrder() {
		order.clear();
		for (int i = 0; i < context.size(); i++) {
			order.add(i);
		}
		if (shuffle) {
			Collections.shuffle(order, random);
		}
	}

	public Optional<Track> next() {
		if (repeat == RepeatMode.ONE && current != null) {
			return Optional.of(current);
		}

		if (!userQueue.isEmpty()) {
			Track track = userQueue.remove(0);
			pushHistory();
			current = track;
			return Optional.of(track);
		}

		if (order.isEmpty()) {
			return Optional.empty();
		}

		if (cursor + 1 >= order.size()) {
			if (repeat != RepeatMode.ALL) {
				return Optional.empty();
			}
			if (shuffle) {
				Collections.shuffle(order, random);
			}
			cursor = -1;
		}

		cursor++;
		pushHistory();
		current = context.get(order.get(cursor));
		return Optional.of(current);
	}

	public Optional<Track> previous() {
		if (history.isEmpty()) {
			return Optional.empty();
		}
		Track track = history.pop();
		current = track;
		int index = context.indexOf(track);
		if (index >= 0) {
			int at = order.indexOf(index);
			if (at >= 0) {
				cursor = at;
			}
		}
		return Optional.of(track);
	}

	private void pushHistory() {
		if (current != null) {
			history.push(current);
			while (history.size() > MAX_HISTORY) {
				history.removeLast();
			}
		}
	}

	public boolean hasPrevious() {
		return !history.isEmpty();
	}

	public Track current() {
		return current;
	}

	public void setCurrent(Track track) {
		this.current = track;
	}

	public void clearHistory() {
		history.clear();
	}

	public void enqueue(Track track) {
		userQueue.add(track);
	}

	public void enqueueNext(Track track) {
		userQueue.add(0, track);
	}

	public List<Track> userQueue() {
		return List.copyOf(userQueue);
	}

	public void clearQueue() {
		userQueue.clear();
	}

	/** Empties the queue but the current track keeps playing. */
	public void clearUpcoming() {
		userQueue.clear();
		context = List.of();
		contextId = "";
		order.clear();
		cursor = -1;
	}

	/** Resets the queue when the mode is switched. */
	public void reset() {
		clearUpcoming();
		history.clear();
		current = null;
	}

	public void removeFromQueue(int index) {
		if (index >= 0 && index < userQueue.size()) {
			userQueue.remove(index);
		}
	}

	public void moveInQueue(int from, int to) {
		if (from < 0 || from >= userQueue.size() || to < 0 || to >= userQueue.size() || from == to) {
			return;
		}
		userQueue.add(to, userQueue.remove(from));
	}

	public void moveUpcoming(int from, int to) {
		int base = cursor + 1;
		int size = order.size() - base;
		if (from < 0 || from >= size || to < 0 || to >= size || from == to) {
			return;
		}
		order.add(base + to, order.remove(base + from));
	}

	public boolean shuffle() {
		return shuffle;
	}

	/** Only shuffles the songs that have not played yet. */
	public void setShuffle(boolean shuffle) {
		if (this.shuffle == shuffle) {
			return;
		}
		this.shuffle = shuffle;

		if (current == null || order.isEmpty()) {
			rebuildOrder();
			return;
		}

		List<Integer> played = new ArrayList<>(order.subList(0, Math.max(cursor + 1, 0)));
		List<Integer> remaining = new ArrayList<>(order.subList(Math.max(cursor + 1, 0), order.size()));

		if (shuffle) {
			Collections.shuffle(remaining, random);
		} else {
			Collections.sort(remaining);
		}

		order.clear();
		order.addAll(played);
		order.addAll(remaining);
	}

	public RepeatMode repeat() {
		return repeat;
	}

	public void setRepeat(RepeatMode repeat) {
		this.repeat = repeat;
	}

	public int upcomingCount() {
		return userQueue.size() + Math.max(0, order.size() - cursor - 1);
	}

	public List<Track> upcoming(int limit) {
		List<Track> out = new ArrayList<>(userQueue);
		for (int i = cursor + 1; i < order.size() && out.size() < limit; i++) {
			out.add(context.get(order.get(i)));
		}
		return out.size() > limit ? out.subList(0, limit) : out;
	}
}
