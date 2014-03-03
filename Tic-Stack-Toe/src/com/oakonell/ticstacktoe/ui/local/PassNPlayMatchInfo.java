package com.oakonell.ticstacktoe.ui.local;

import android.net.Uri;

import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.ui.game.HumanStrategy;
import com.oakonell.ticstacktoe.ui.game.SoundManager;

public class PassNPlayMatchInfo extends LocalMatchInfo {

	public PassNPlayMatchInfo(long id, int matchStatus, int turnStatus,
			String blackName, String whiteName, long lastUpdated,
			String fileName, ScoreCard score, long rematchId, int winner) {
		super(id, matchStatus, turnStatus, blackName, whiteName, lastUpdated,
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
	public AbstractLocalStrategy createStrategy(MainActivity activity,
			SoundManager soundManager) {
		PassNPlayGameStrategy listener = new PassNPlayGameStrategy(activity,activity.getGameHelper(),
				soundManager, this);
		return listener;
	}

	protected Player createWhitePlayerStrategy() {
		return HumanStrategy.createPlayer(getWhiteName(), false);
	}


	protected CharSequence whiteWon() {
		return getWhiteName() + " WON!";
	}

	protected CharSequence blackWon() {
		return getBlackName() + " WON!";
	}
}
