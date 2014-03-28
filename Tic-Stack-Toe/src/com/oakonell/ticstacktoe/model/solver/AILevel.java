package com.oakonell.ticstacktoe.model.solver;

public enum AILevel {
	EASY_AI(1), MEDIUM_AI(2), HARD_AI(3), RANDOM_AI(-1);

	private final int value;

	private AILevel(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static AILevel fromValue(int value) {
		for (AILevel each : AILevel.values()) {
			if (each.getValue() == value)
				return each;
		}
		return null;
	}
}
