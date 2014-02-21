package com.oakonell.ticstacktoe.ui.local;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler.OnLocalMatchUpdateListener;
import com.oakonell.ticstacktoe.ui.game.GameFragment;
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
				.getName(), game.getType());
		// TODO, keep track of score, and switch first player...
	}

	public void startGame(String blackName, String whiteName, GameType type) {
		Player blackPlayer = HumanStrategy.createPlayer(blackName, true);
		Player whitePlayer = HumanStrategy.createPlayer(whiteName, false);

		Tracker myTracker = EasyTracker.getTracker();
		myTracker.sendEvent(
				getContext().getString(R.string.an_start_game_cat),
				getContext().getString(
						R.string.an_start_pass_n_play_game_action), type + "",
				0L);

		final Game game = new Game(type, GameMode.PASS_N_PLAY, blackPlayer,
				whitePlayer, blackPlayer);
		final ScoreCard score = new ScoreCard(0, 0, 0);

		DatabaseHandler db = new DatabaseHandler(getContext());
		setMatchInfo(new PassNPlayMatchInfo(TurnBasedMatch.MATCH_STATUS_ACTIVE,
				TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN, blackName, whiteName,
				game));
		db.insertMatch(getMatchInfo(), new OnLocalMatchUpdateListener() {
			@Override
			public void onUpdateSuccess(LocalMatchInfo matchInfo) {
				GameFragment gameFragment = getMainActivity().getGameFragment();
				if (gameFragment == null) {
					gameFragment = new GameFragment();

					FragmentManager manager = getMainActivity()
							.getSupportFragmentManager();
					FragmentTransaction transaction = manager
							.beginTransaction();
					transaction.replace(R.id.main_frame, gameFragment,
							MainActivity.FRAG_TAG_GAME);
					transaction.addToBackStack(null);
					transaction.commit();
				}
				gameFragment.startGame(game, score, null, false);

			}

			@Override
			public void onUpdateFailure() {
				// TODO Auto-generated method stub

			}
		});
	}

}
