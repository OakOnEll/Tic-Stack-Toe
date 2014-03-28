package com.oakonell.ticstacktoe.model.solver;

import android.net.Uri;

import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.PlayerStrategy;

public abstract class AiPlayerStrategy extends PlayerStrategy {

	protected AiPlayerStrategy(boolean isBlack) {
		super(isBlack);
	}

	public static Uri getAiImageUri(AILevel aiDepth) {
		if (aiDepth == AILevel.RANDOM_AI) {
			return RandomAI.getImageUri();
		}
		return MinMaxAI.getImageUri(aiDepth.getValue());
	}

	public static Player createThePlayer(String whiteName, boolean isBlack,
			AILevel aiDepth) {
		Player whitePlayer;
		if (aiDepth == AILevel.RANDOM_AI) {
			whitePlayer = RandomAI.createPlayer(whiteName, isBlack);
		} else {
			whitePlayer = MinMaxAI.createPlayer(whiteName, isBlack,
					aiDepth.getValue());
		}
		return whitePlayer;
	}

}
