package com.oakonell.ticstacktoe.ui.local;

import android.content.Context;
import android.net.Uri;

import com.oakonell.ticstacktoe.GameContext;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.ui.game.HumanStrategy;

public class PassNPlayMatchInfo extends LocalMatchInfo {

	public PassNPlayMatchInfo(long id, GameType type , int matchStatus, int turnStatus,
			String blackName, String whiteName, long lastUpdated,
			String fileName, ScoreCard score, long rematchId, int winner) {
		super(id, type, matchStatus, turnStatus, blackName, whiteName, lastUpdated,
				fileName, score, rematchId, winner);
	}

	public PassNPlayMatchInfo(int matchStatus, int turnStatus,
			String blackName, String whiteName, Game game, ScoreCard score) {
		super(matchStatus, turnStatus, blackName, whiteName, game, score);
	}

	@Override
	public void accept(LocalMatchVisitor visitor) {
		visitor.visitPassNPlay(this);
	}

	@Override
	public Uri getIconImageUri() {
		return Uri.parse("android.resource://com.oakonell.ticstacktoe/"
				+ R.drawable.pass_n_play_icon_27531);
	}

	@Override
	public AbstractLocalStrategy createStrategy(GameContext context) {
		PassNPlayGameStrategy listener = new PassNPlayGameStrategy(context,
				this);
		return listener;
	}

	protected Player createWhitePlayerStrategy() {
		return HumanStrategy.createPlayer(getWhiteName(), false);
	}

	protected CharSequence whiteWon(Context context) {
		return context.getString(R.string.player_won, getWhiteName());
	}

	protected CharSequence blackWon(Context context) {
		return context.getString(R.string.player_won, getBlackName());
	}
}
