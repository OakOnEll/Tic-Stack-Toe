package com.oakonell.ticstacktoe.model;

public class PlayerStrategy {
	private boolean isBlack;

	protected PlayerStrategy(boolean isBlack) {
		this.isBlack = isBlack;
	}

	public boolean isHuman() {
		return false;
	}

	public boolean isAI() {
		return false;
	}

	public boolean isBlack() {
		return isBlack;
	}

	public AbstractMove move(Game game) {
		return null;
	}

}
