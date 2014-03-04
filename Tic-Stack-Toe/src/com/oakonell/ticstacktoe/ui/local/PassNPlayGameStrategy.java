package com.oakonell.ticstacktoe.ui.local;

import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.GameContext;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.PlayerStrategy;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.ui.game.HumanStrategy;

public class PassNPlayGameStrategy extends AbstractLocalStrategy {

	public PassNPlayGameStrategy(GameContext context) {
		super(context);
	}

	public PassNPlayGameStrategy(GameContext context,
			PassNPlayMatchInfo localMatchInfo) {
		super(context, localMatchInfo);
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
