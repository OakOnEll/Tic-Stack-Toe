package com.oakonell.ticstacktoe;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler.OnLocalMatchUpdateListener;
import com.oakonell.ticstacktoe.model.solver.MinMaxAI;
import com.oakonell.ticstacktoe.model.solver.RandomAI;
import com.oakonell.ticstacktoe.ui.game.AbstractGameFragment;
import com.oakonell.ticstacktoe.ui.game.GameFragment;
import com.oakonell.ticstacktoe.ui.game.HumanStrategy;
import com.oakonell.ticstacktoe.ui.menu.LocalMatchInfo;

public class AiListener implements GameListener {

	private MainActivity mainActivity;

	private LocalMatchInfo matchInfo;

	private int aiDepth;

	public AiListener(MainActivity mainActivity) {
		this.mainActivity = mainActivity;
	}

	public AiListener(MainActivity mainActivity, LocalMatchInfo localMatchInfo) {
		this(mainActivity);
		this.matchInfo = localMatchInfo;
	}

	@Override
	public void leaveRoom() {
		saveToDB();
	}

	private void saveToDB() {
		DatabaseHandler db = new DatabaseHandler(mainActivity);
		db.updateMatch(matchInfo, new OnLocalMatchUpdateListener() {
			@Override
			public void onUpdateSuccess(LocalMatchInfo matchInfo) {

			}

			@Override
			public void onUpdateFailure() {
				mainActivity.getGameHelper().showAlert(
						"Error updating match to DB");
			}
		});
	}

	@Override
	public void sendMove(Game game, AbstractMove lastMove, ScoreCard score) {
		// TODO Auto-generated method stub
		// write the game to the DB, or wait until leave...??
		if (game.getBoard().getState().isOver()) {
			matchInfo.setMatchStatus(TurnBasedMatch.MATCH_STATUS_COMPLETE);
			saveToDB();
			return;
		}
		matchInfo
				.setTurnStatus(matchInfo.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN ? TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN
						: TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN);
	}

	@Override
	public void backFromWaitingRoom() {
		// do nothing
	}

	@Override
	public boolean warnToLeave() {
		return false;
	}

	public void leaveGame() {
		mainActivity.getGameFragment().leaveGame();
		mainActivity.getMenuFragment().leaveRoom();
	}

	public void playAgain() {
		Game game = matchInfo.readGame(mainActivity);

		startGame(game.getBlackPlayer().getName(), game.getWhitePlayer()
				.getName(), game.getType(), aiDepth);
		// TODO, keep track of score, and switch first player...
	}

	@Override
	public void promptToPlayAgain(String winner, String title) {
		OnClickListener cancelListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				leaveGame();
			}

		};
		OnClickListener playAgainListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				playAgain();
			}

		};

		AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
		builder.setTitle(title);
		builder.setMessage(R.string.play_again);
		builder.setCancelable(false);

		builder.setNegativeButton(R.string.no, cancelListener);
		builder.setPositiveButton(R.string.yes, playAgainListener);

		AlertDialog dialog = builder.create();

		dialog.show();

	}

	@Override
	public void onResume(MainActivity activity) {
		// do nothing
	}

	@Override
	public void onFragmentResume() {
		// do nothing
	}

	@Override
	public void reassociate(MainActivity activity) {
		// nothing to do, no dependency on being signed in
	}

	@Override
	public void onSignInFailed(MainActivity mainActivity) {
		// no worries
	}

	@Override
	public void showSettings(AbstractGameFragment fragment) {
		fragment.showFullSettingsPreference();
	}

	@Override
	public boolean shouldKeepScreenOn() {
		return false;
	}

	@Override
	public ChatHelper getChatHelper() {
		return null;
	}

	public void startGame(String blackName, String whiteName, GameType type,
			int aiDepth) {
		this.aiDepth = aiDepth;
		Player blackPlayer = HumanStrategy.createPlayer(blackName, true);

		Player whitePlayer;
		if (aiDepth < 0) {
			whitePlayer = RandomAI.createPlayer(whiteName, false);
		} else {
			whitePlayer = MinMaxAI.createPlayer(whiteName, false, aiDepth);
		}

		Tracker myTracker = EasyTracker.getTracker();
		myTracker.sendEvent(mainActivity.getString(R.string.an_start_game_cat),
				mainActivity.getString(R.string.an_start_ai_game_action), type
						+ "", 0L);

		final Game game = new Game(type, GameMode.AI, blackPlayer, whitePlayer,
				blackPlayer);

		final ScoreCard score = new ScoreCard(0, 0, 0);

		DatabaseHandler db = new DatabaseHandler(mainActivity);
		matchInfo = new LocalMatchInfo(TurnBasedMatch.MATCH_STATUS_ACTIVE,
				TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN, blackName, whiteName, aiDepth,
				System.currentTimeMillis(), game);
		db.insertMatch(matchInfo, new OnLocalMatchUpdateListener() {
			@Override
			public void onUpdateSuccess(LocalMatchInfo matchInfo) {
				GameFragment gameFragment = mainActivity.getGameFragment();
				if (gameFragment == null) {
					gameFragment = new GameFragment();

					FragmentManager manager = mainActivity
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

	public void showFromMenu() {
		final ScoreCard score = new ScoreCard(0, 0, 0);
		GameFragment gameFragment = new GameFragment();
		gameFragment.startGame(matchInfo.readGame(mainActivity), score, null,
				true);

		FragmentManager manager = mainActivity.getSupportFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		transaction.replace(R.id.main_frame, gameFragment,
				MainActivity.FRAG_TAG_GAME);
		transaction.addToBackStack(null);
		transaction.commit();

	}
}
