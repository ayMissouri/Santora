package dev.santora.core.play;

public enum RepeatMode {
	OFF,
	ALL,
	ONE;

	public RepeatMode nextMode() {
		return switch (this) {
			case OFF -> ALL;
			case ALL -> ONE;
			case ONE -> OFF;
		};
	}
}
