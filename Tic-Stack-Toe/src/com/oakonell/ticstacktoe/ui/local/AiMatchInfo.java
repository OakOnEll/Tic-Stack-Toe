package com.oakonell.ticstacktoe.ui.local;

import android.net.Uri;

import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.solver.AiPlayerStrategy;
import com.oakonell.ticstacktoe.ui.game.SoundManager;

public class AiMatchInfo extends LocalMatchInfo {

	private int aiLevel;

	public AiMatchInfo(long id, int matchStatus, int turnStatus,
			String blackName, String whiteName, int aiLevel, long lastUpdated,
			String fileName, ScoreCard score, long rematchId, int winner) {
		super(id, matchStatus, turnStatus, blackName, whiteName, lastUpdated,
				fileName, score, rematchId, winner);
		this.aiLevel = aiLevel;
	}

	public AiMatchInfo(int matchStatus, int turnStatus, String blackName,
			String whiteName, int aiLevel, long currentTimeMillis, Game game,
			ScoreCard score, long rematchId, int winner) {
		super(matchStatus, turnStatus, blackName, whiteName, game, score);
		this.aiLevel = aiLevel;
	}

	@Override
	public void accept(LocalMatchVisitor visitor) {
		visitor.visitAi(this);
	}

	public int getWhiteAILevel() {
		return aiLevel;
	}

	@Override
	public Uri getIconImageUri() {
		return AiPlayerStrategy.getAiImageUri(aiLevel);
	}

	@Override
	public AbstractLocalStrategy createStrategy(MainActivity activity,
			SoundManager soundManager) {
		AiGameStrategy listener = new AiGameStrategy(activity,
				activity.getGameHelper(), soundManager, aiLevel, this);
		return listener;
	}

	@Override
	protected Player createWhitePlayerStrategy() {
		return AiPlayerStrategy.createWhitePlayer(getWhiteName(), false,
				aiLevel);
	}

	protected CharSequence whiteWon() {
		return "You LOST!";
	}

	protected CharSequence blackWon() {
		return "You WON!";
	}
}
