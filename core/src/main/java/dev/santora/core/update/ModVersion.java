package dev.santora.core.update;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ModVersion implements Comparable<ModVersion> {

	private final List<Integer> numbers;
	private final List<String> preRelease;

	private ModVersion(List<Integer> numbers, List<String> preRelease) {
		this.numbers = List.copyOf(numbers);
		this.preRelease = List.copyOf(preRelease);
	}

	public static Optional<ModVersion> parse(String raw) {
		if (raw == null) {
			return Optional.empty();
		}

		String text = raw.trim();
		if (text.startsWith("v") || text.startsWith("V")) {
			text = text.substring(1);
		}

		int plus = text.indexOf('+');
		if (plus >= 0) {
			text = text.substring(0, plus);
		}

		String pre = "";
		int dash = text.indexOf('-');
		if (dash >= 0) {
			pre = text.substring(dash + 1);
			text = text.substring(0, dash);
		}

		List<Integer> numbers = new ArrayList<>();
		for (String segment : text.split("[.]", -1)) {
			Integer number = leadingNumber(segment);
			if (number == null) {
				break;
			}
			numbers.add(number);
		}
		if (numbers.isEmpty()) {
			return Optional.empty();
		}

		List<String> parts = pre.isEmpty() ? List.of() : List.of(pre.split("[.]", -1));
		return Optional.of(new ModVersion(numbers, parts));
	}

	public static boolean isNewer(String candidate, String current) {
		Optional<ModVersion> found = parse(candidate);
		Optional<ModVersion> running = parse(current);
		return found.isPresent() && running.isPresent() && found.get().isNewerThan(running.get());
	}

	public boolean isNewerThan(ModVersion other) {
		return compareTo(other) > 0;
	}

	public boolean isPreRelease() {
		return !preRelease.isEmpty();
	}

	@Override
	public int compareTo(ModVersion other) {
		int size = Math.max(numbers.size(), other.numbers.size());
		for (int i = 0; i < size; i++) {
			int cmp = Integer.compare(number(i), other.number(i));
			if (cmp != 0) {
				return cmp;
			}
		}
		return comparePreRelease(preRelease, other.preRelease);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ModVersion other && compareTo(other) == 0;
	}

	@Override
	public int hashCode() {
		int hash = 1;
		for (int i = 0; i < 3; i++) {
			hash = hash * 31 + number(i);
		}
		return hash * 31 + preRelease.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < numbers.size(); i++) {
			if (i > 0) {
				text.append('.');
			}
			text.append(numbers.get(i));
		}
		if (!preRelease.isEmpty()) {
			text.append('-').append(String.join(".", preRelease));
		}
		return text.toString();
	}

	private int number(int index) {
		return index < numbers.size() ? numbers.get(index) : 0;
	}

	private static int comparePreRelease(List<String> a, List<String> b) {
		if (a.isEmpty() || b.isEmpty()) {
			return Boolean.compare(a.isEmpty(), b.isEmpty());
		}
		int size = Math.min(a.size(), b.size());
		for (int i = 0; i < size; i++) {
			int cmp = compareIdentifier(a.get(i), b.get(i));
			if (cmp != 0) {
				return cmp;
			}
		}
		return Integer.compare(a.size(), b.size());
	}

	private static int compareIdentifier(String a, String b) {
		Integer left = wholeNumber(a);
		Integer right = wholeNumber(b);
		if (left != null && right != null) {
			return Integer.compare(left, right);
		}
		if (left != null) {
			return -1;
		}
		if (right != null) {
			return 1;
		}
		return a.compareTo(b);
	}

	private static Integer leadingNumber(String segment) {
		int end = 0;
		while (end < segment.length() && Character.isDigit(segment.charAt(end))) {
			end++;
		}
		return end == 0 ? null : toInt(segment.substring(0, end));
	}

	private static Integer wholeNumber(String text) {
		for (int i = 0; i < text.length(); i++) {
			if (!Character.isDigit(text.charAt(i))) {
				return null;
			}
		}
		return text.isEmpty() ? null : toInt(text);
	}

	private static Integer toInt(String digits) {
		try {
			return Integer.valueOf(digits);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
