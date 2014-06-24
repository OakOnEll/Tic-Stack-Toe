package com.oakonell.ticstacktoe.ui.local;

import android.content.Context;
import android.net.Uri;

import com.oakonell.ticstacktoe.GameContext;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.solver.AILevel;
import com.oakonell.ticstacktoe.model.solver.AiPlayerStrategy;

public class AiMatchInfo extends LocalMatchInfo {

	private AILevel aiLevel;
	private boolean isRanked;

	public AiMatchInfo(long id, GameType type , int matchStatus, int turnStatus,
			String blackName, String whiteName, AILevel aiLevel,
			long lastUpdated, String fileName, ScoreCard score, long rematchId,
			int winner, boolean isRanked) {
		super(id, type, matchStatus, turnStatus, blackName, whiteName, lastUpdated,
				fileName, score, rematchId, winner);
		this.aiLevel = aiLevel;
		this.isRanked = isRanked;
	}

	public AiMatchInfo(int matchStatus, int turnStatus, String blackName,
			String whiteName, AILevel aiLevel, long currentTimeMillis,
			Game game, ScoreCard score, boolean isRanked) {
		super(matchStatus, turnStatus, blackName, whiteName, game, score);
		this.aiLevel = aiLevel;
		this.isRanked = isRanked;
	}

	@Override
	public void accept(LocalMatchVisitor visitor) {
		visitor.visitAi(this);
	}

	public AILevel getWhiteAILevel() {
		return aiLevel;
	}

	@Override
	public Uri getIconImageUri() {
		return AiPlayerStrategy.getAiImageUri(aiLevel);
	}

	@Override
	public AbstractLocalStrategy createStrategy(GameContext context) {
		AiGameStrategy listener = new AiGameStrategy(context, aiLevel, this);
		return listener;
	}

	@Override
	protected Player createWhitePlayerStrategy() {
		return AiPlayerStrategy.createThePlayer(getWhiteName(), false, aiLevel);
	}

	protected CharSequence whiteWon(Context context) {
		return context.getString(R.string.you_lost);
	}

	protected CharSequence blackWon(Context context) {
		return context.getString(R.string.you_won);
	}

	public boolean isRanked() {
		return isRanked;
	}

}
