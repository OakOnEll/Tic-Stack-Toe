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
import com.oakonell.ticstacktoe.model.State;
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

	protected AiMatchInfo createNewMatchInfo(String blackName,
			String whiteName, final Game game, ScoreCard score) {
		return new AiMatchInfo(TurnBasedMatch.MATCH_STATUS_ACTIVE,
				TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN, blackName, whiteName,
				aiDepth, System.currentTimeMillis(), game, score);
	}

	protected GameMode getGameMode() {
		return GameMode.AI;
	}

	@Override
	protected Player createWhitePlayer(String whiteName) {
		return AiPlayerStrategy.createWhitePlayer(whiteName, false, aiDepth);
	}

	protected void acceptNonHumanPlayerMove(final PlayerStrategy currentStrategy) {
		AsyncTask<Void, Void, State> aiMove = new AsyncTask<Void, Void, State>() {
			@Override
			protected State doInBackground(Void... params) {
				AbstractMove move = currentStrategy.move(getGame());
				State state = applyNonHumanMove(move);
				updateGame();
				return state;
			}

			@Override
			protected void onPostExecute(final State state) {
				getGameFragment().hideStatusText();
				getGameFragment().animateMove(state.getLastMove(), state);
			}
		};
		aiMove.execute((Void) null);
	}

}
