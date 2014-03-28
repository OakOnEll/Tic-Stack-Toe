package com.oakonell.ticstacktoe.server.rank;

public enum GameOutcome {
	LOSE(0, 0.0), DRAW(1, 0.5), WIN(2, 1.0);

	private int id;
	private double multiplier;

	private GameOutcome(int id, double multiplier) {
		this.id = id;
		this.multiplier = multiplier;
	}

	public int getId() {
		return id;
	}

	public double getMultiplier() {
		return multiplier;
	}

	public static GameOutcome fromId(int id) {
		for (GameOutcome each : GameOutcome.values()) {
			if (each.getId() == id)
				return each;
		}
		return null;
	}
}
