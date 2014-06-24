package com.oakonell.ticstacktoe.ui.local.tutorial;

import android.content.Context;
import android.net.Uri;

import com.oakonell.ticstacktoe.GameContext;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.solver.AILevel;
import com.oakonell.ticstacktoe.ui.local.LocalMatchInfo;

/**
 * This class holds the MatchInfo for a tutorial game. It is not actually long
 * lived- not stored in the DB.
 */
public class TutorialMatchInfo extends LocalMatchInfo {

	public TutorialMatchInfo(long id, int matchStatus, int turnStatus,
			String blackName, String whiteName, AILevel aiLevel,
			long lastUpdated, String fileName, ScoreCard score, long rematchId,
			int winner) {
		super(id, GameType.JUNIOR, matchStatus, turnStatus, blackName, whiteName, lastUpdated,
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

	protected CharSequence whiteWon(Context context) {
		return context.getString(R.string.you_lost);
	}

	protected CharSequence blackWon(Context context) {
		return context.getString(R.string.you_won);
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
