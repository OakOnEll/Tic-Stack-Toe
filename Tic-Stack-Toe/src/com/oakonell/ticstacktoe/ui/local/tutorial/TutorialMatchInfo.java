package com.oakonell.ticstacktoe.ui.local.tutorial;

import android.net.Uri;

import com.oakonell.ticstacktoe.GameContext;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.solver.AILevel;
import com.oakonell.ticstacktoe.ui.local.LocalMatchInfo;

public class TutorialMatchInfo extends LocalMatchInfo {

	public TutorialMatchInfo(long id, int matchStatus, int turnStatus,
			String blackName, String whiteName, AILevel aiLevel,
			long lastUpdated, String fileName, ScoreCard score, long rematchId,
			int winner) {
		super(id, matchStatus, turnStatus, blackName, whiteName, lastUpdated,
				fileName, score, rematchId, winner);
	}

	public TutorialMatchInfo(int matchStatus, int turnStatus, String blackName,
			String whiteName, long currentTimeMillis, Game game, ScoreCard score) {
		super(matchStatus, turnStatus, blackName, whiteName, game, score);
	}

	@Override
	public void accept(LocalMatchVisitor visitor) {
		visitor.visitTutorial(this);
	}

	protected CharSequence whiteWon() {
		return "You LOST!";
	}

	protected CharSequence blackWon() {
		return "You WON!";
	}

	@Override
	protected Player createWhitePlayerStrategy() {
		return TutorialGameStrategy.staticCreateWhitePlayer(getWhiteName());
	}

	@Override
	public TutorialGameStrategy createStrategy(GameContext context) {
		return new TutorialGameStrategy(context, this);
	}

	@Override
	public Uri getIconImageUri() {
		return TutorialGameStrategy.getTutorialImageUri();
	}

}