package com.oakonell.ticstacktoe.ui.local;

import android.os.AsyncTask;

import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.PlayerStrategy;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.solver.AiPlayerStrategy;
import com.oakonell.ticstacktoe.ui.game.SoundManager;

public class AiGameStrategy extends AbstractLocalStrategy {

	private final int aiDepth;

	public AiGameStrategy(MainActivity mainActivity, GameHelper helper,
			SoundManager soundManager, int aiDepth) {
		super(mainActivity, helper, soundManager);
		this.aiDepth = aiDepth;
	}

	public AiGameStrategy(MainActivity mainActivity, GameHelper helper,
			SoundManager soundManager, int aiDepth, AiMatchInfo matchInfo) {
		super(mainActivity, helper, matchInfo, soundManager);
		this.aiDepth = aiDepth;
	}

	public void playAgain() {
		Game game = getMatchInfo().readGame(getContext());

		startGame(game.getBlackPlayer().getName(), game.getWhitePlayer()
				.getName(), game.getType(), getMatchInfo().getScoreCard());
		// TODO, keep track of score, and switch first player...
	}

	protected AiMatchInfo createMatchInfo(String blackName, String whiteName,
			final Game game, ScoreCard score) {
		return new AiMatchInfo(TurnBasedMatch.MATCH_STATUS_ACTIVE,
				TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN, blackName, whiteName,
				aiDepth, System.currentTimeMillis(), game, score, 0, 0);
	}

	protected GameMode getGameMode() {
		return GameMode.AI;
	}

	@Override
	protected Player createWhitePlayer(String whiteName) {
		return AiPlayerStrategy.createWhitePlayer(whiteName, false, aiDepth);
	}

	protected void acceptCurrentPlayerMove(final PlayerStrategy currentStrategy) {
		// show a thinking/progress icon, suitable for network play and ai
		// thinking..
		if (!currentStrategy.isAI()) {
			return;
		}

		aiMakeMove(currentStrategy);
	}

	private void aiMakeMove(final PlayerStrategy currentStrategy) {
		AsyncTask<Void, Void, AbstractMove> aiMove = new AsyncTask<Void, Void, AbstractMove>() {
			@Override
			protected AbstractMove doInBackground(Void... params) {
				AbstractMove move = currentStrategy.move(getGame());
				// would like to save the state here, but need to modify the
				// game fragment to use already modified game...
				// move.applyToGame(getGame());

				return move;
			}

			@Override
			protected void onPostExecute(final AbstractMove move) {
				// gameFragment.startGame(state.game, state.score, waitingText,
				// showMove);
				// getMainActivity().getGameFragment().startGame(getGame(),
				// getScore(), null, true);
				getGameFragment().highlightAndMakeMove(move);
			}
		};
		aiMove.execute((Void) null);
	}

}
