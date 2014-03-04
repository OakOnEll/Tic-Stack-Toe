package com.oakonell.ticstacktoe.ui.local;

import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.PlayerStrategy;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.ui.game.HumanStrategy;
import com.oakonell.ticstacktoe.ui.game.SoundManager;

public class PassNPlayGameStrategy extends AbstractLocalStrategy {

	public PassNPlayGameStrategy(MainActivity mainActivity, GameHelper helper,
			SoundManager soundManager) {
		super(mainActivity, helper, soundManager);
	}

	public PassNPlayGameStrategy(MainActivity mainActivity, GameHelper helper,
			SoundManager soundManager, PassNPlayMatchInfo localMatchInfo) {
		super(mainActivity, helper, localMatchInfo, soundManager);
	}

	@Override
	protected LocalMatchInfo createNewMatchInfo(String blackName,
			String whiteName, Game game, ScoreCard score) {
		return new PassNPlayMatchInfo(TurnBasedMatch.MATCH_STATUS_ACTIVE,
				TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN, blackName, whiteName,
				game, score);
	}

	@Override
	protected GameMode getGameMode() {
		return GameMode.PASS_N_PLAY;
	}

	@Override
	protected Player createWhitePlayer(String whiteName) {
		return HumanStrategy.createPlayer(whiteName, false);
	}

	@Override
	protected void acceptNonHumanPlayerMove(PlayerStrategy currentStrategy) {
		throw new RuntimeException(
				"Shouldn't get here on a human vs human, local match");
	}

}
