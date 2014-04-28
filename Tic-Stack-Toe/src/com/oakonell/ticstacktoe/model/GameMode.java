package com.oakonell.ticstacktoe.model;

/**
 * This designates the game player mode
 *
 */
public enum GameMode {
	PASS_N_PLAY(0), AI(1), TURN_BASED(2), ONLINE(3), TUTORIAL(4);
	private final int value;

	private GameMode(int val) {
		this.value = val;
	}

	public int getVal() {
		return value;
	}

	public static GameMode fromValue(int value) {
		for (GameMode each : GameMode.values()) {
			if (each.getVal() == value)
				return each;
		}
		return null;
	}
}
