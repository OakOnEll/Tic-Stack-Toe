package com.oakonell.ticstacktoe.ui.local;

import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.ui.game.HumanStrategy;
import com.oakonell.ticstacktoe.ui.game.SoundManager;

public class PassNPlayMatchInfo extends LocalMatchInfo {

	public PassNPlayMatchInfo(long id, int matchStatus, int turnStatus,
			String blackName, String whiteName, long lastUpdated,
			String fileName) {
		super(id, matchStatus, turnStatus, blackName, whiteName, lastUpdated,
				fileName);
	}

	public PassNPlayMatchInfo(int matchStatus, int turnStatus,
			String blackName, String whiteName, Game game) {
		super(matchStatus, turnStatus, blackName, whiteName, game);
	}

	@Override
	public void accept(LocalMatchVisitor visitor) {
		visitor.visitPassNPlay(this);
	}

	@Override
	public AbstractLocalStrategy createStrategy(MainActivity activity,
			SoundManager soundManager) {
		PassNPlayGameStrategy listener = new PassNPlayGameStrategy(activity,
				soundManager, this);
		return listener;
	}

	protected Player createWhitePlayerStrategy() {
		return HumanStrategy.createPlayer(getWhiteName(), false);
	}

}
