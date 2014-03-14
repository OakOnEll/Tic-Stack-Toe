package com.oakonell.ticstacktoe.model.solver;

import android.net.Uri;

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

	public static Uri getAiImageUri(int aiDepth) {
		if (aiDepth == RANDOM_AI) {
			return RandomAI.getImageUri();
		}
		return MinMaxAI.getImageUri(aiDepth);
	}

	public static Player createThePlayer(String whiteName, boolean isBlack,
			int aiDepth) {
		Player whitePlayer;
		if (aiDepth == RANDOM_AI) {
			whitePlayer = RandomAI.createPlayer(whiteName, isBlack);
		} else {
			whitePlayer = MinMaxAI.createPlayer(whiteName, isBlack, aiDepth);
		}
		return whitePlayer;
	}

}
