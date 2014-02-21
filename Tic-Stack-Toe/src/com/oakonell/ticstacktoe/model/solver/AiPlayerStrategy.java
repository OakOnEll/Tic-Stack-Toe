package com.oakonell.ticstacktoe.model.solver;

import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.PlayerStrategy;

public abstract class AiPlayerStrategy extends PlayerStrategy {
	public static final int EASY_AI = 1;
	public static final int MEDIUM_AI = 2;
	public static final int HARD_AI = 3;
	public static final int RANDOM_AI = -1;

	protected AiPlayerStrategy(boolean isBlack) {
		super(isBlack);
	}

	public static Player createWhitePlayer(String whiteName, boolean isBlack,
			int aiDepth) {
		Player whitePlayer;
		if (aiDepth == RANDOM_AI) {
			whitePlayer = RandomAI.createPlayer(whiteName, false);
		} else {
			whitePlayer = MinMaxAI.createPlayer(whiteName, false, aiDepth);
		}
		return whitePlayer;
	}

}
