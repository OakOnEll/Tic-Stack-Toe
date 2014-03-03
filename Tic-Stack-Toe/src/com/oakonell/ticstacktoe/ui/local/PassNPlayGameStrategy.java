package com.oakonell.ticstacktoe.ui.local;

import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.PlayerStrategy;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.ui.game.HumanStrategy;
import com.oakonell.ticstacktoe.ui.game.SoundManager;

public class PassNPlayGameStrategy extends AbstractLocalStrategy {

	public PassNPlayGameStrategy(MainActivity mainActivity,
			SoundManager soundManager) {
		super(mainActivity, soundManager);
	}

	public PassNPlayGameStrategy(MainActivity mainActivity,
			SoundManager soundManager, PassNPlayMatchInfo localMatchInfo) {
		super(mainActivity, localMatchInfo, soundManager);
	}

	public void playAgain() {
		Game game = getMatchInfo().readGame(getContext());
		startGame(game.getBlackPlayer().getName(), game.getWhitePlayer()
				.getName(), game.getType(), getMatchInfo().getScoreCard());
		// TODO, keep track of score, and switch first player...
	}

	public void startGame(String blackName, String whiteName, GameType type,
			ScoreCard score) {
		super.startGame(blackName, whiteName, type, score);
	}

	@Override
	protected LocalMatchInfo createMatchInfo(String blackName,
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
	protected void acceptCurrentPlayerMove(PlayerStrategy currentStrategy) {
		throw new RuntimeException(
				"Shouldn't get here on a human vs human, local match");
	}

}
