package com.oakonell.ticstacktoe.ui.local;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.GameContext;
import com.oakonell.ticstacktoe.GameStrategy;
import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.State;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler.OnLocalMatchUpdateListener;
import com.oakonell.ticstacktoe.ui.game.GameFragment;
import com.oakonell.ticstacktoe.ui.game.HumanStrategy;

public abstract class AbstractLocalStrategy extends GameStrategy {
	private LocalMatchInfo matchInfo;

	public AbstractLocalStrategy(GameContext context) {
		super(context);
	}

	public AbstractLocalStrategy(GameContext context,
			LocalMatchInfo localMatchInfo) {
		super(context);
		this.setMatchInfo(localMatchInfo);
	}

	@Override
	public void leaveRoom() {
		saveToDB();
	}

	public void onActivityPause(MainActivity mainActivity) {
		saveToDB();
	}

	private void saveToDB() {
		DatabaseHandler db = new DatabaseHandler(getContext());
		db.updateMatch(getMatchInfo(), new OnLocalMatchUpdateListener() {
			@Override
			public void onUpdateSuccess(LocalMatchInfo matchInfo) {

			}

			@Override
			public void onUpdateFailure() {
				showAlert("Error updating match to DB");
			}
		});
	}

	public void showAlert(String message) {
		(new AlertDialog.Builder(getContext())).setMessage(message)
				.setNeutralButton(android.R.string.ok, null).create().show();
	}

	@Override
	public void sendHumanMove() {
		updateGame();
	}

	protected void updateGame() {
		State state = getGame().getBoard().getState();
		if (state.isOver()) {
			matchInfo.setMatchStatus(TurnBasedMatch.MATCH_STATUS_COMPLETE);
			Player winner = state.getWinner();
			if (winner == null) {
				// draw
			} else if (winner.isBlack()) {
				matchInfo.setWinner(1);
			} else {
				matchInfo.setWinner(-1);
			}
			matchInfo.setScoreCard(getScore());
			saveToDB();
			return;
		}
		getMatchInfo()
				.setTurnStatus(
						getGame().getCurrentPlayer().isBlack() ? TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN
								: TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN);
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
		getGameFragment().leaveGame();
		getMenuFragment().leaveRoom();
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

		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setTitle(title);
		builder.setMessage(R.string.play_again);
		builder.setCancelable(false);

		builder.setNegativeButton(R.string.no, cancelListener);
		builder.setPositiveButton(R.string.yes, playAgainListener);

		AlertDialog dialog = builder.create();

		dialog.show();

	}

	private void playAgain() {
		Game game = getMatchInfo().readGame(getContext());
		boolean firstIsBlack = game.getCurrentPlayer().isBlack();
		startGame(firstIsBlack, game.getBlackPlayer().getName(), game
				.getWhitePlayer().getName(), game.getType(), getMatchInfo()
				.getScoreCard());
		// TODO, keep track of score, and switch first player...
	}

	@Override
	public void onActivityResume(MainActivity activity) {
		// do nothing
	}

	@Override
	public void onSignInSuccess(MainActivity activity) {
		// nothing to do, no dependency on being signed in
	}

	@Override
	public void onSignInFailed(SherlockFragmentActivity mainActivity) {
		// no worries
	}

	public void showFromMenu() {
		GameFragment gameFragment = GameFragment.createFragment();
		Game game = getMatchInfo().readGame(getContext());
		setGame(game);
		ScoreCard score = getMatchInfo().getScoreCard();
		setScore(score);

		gameFragment.startGame(null, true);

		FragmentManager manager = getActivity().getSupportFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		transaction.replace(R.id.main_frame, gameFragment,
				GameContext.FRAG_TAG_GAME);
		transaction.addToBackStack(null);
		transaction.commit();

	}

	protected LocalMatchInfo getMatchInfo() {
		return matchInfo;
	}

	protected void setMatchInfo(LocalMatchInfo matchInfo) {
		this.matchInfo = matchInfo;
		setGame(matchInfo.readGame(getContext()));
	}

	public void startGame(String blackName, String whiteName, GameType type,
			final ScoreCard score) {
		startGame(true, blackName, whiteName, type, score);
	}

	public void startGame(boolean blackFirst, String blackName,
			String whiteName, GameType type, final ScoreCard score) {
		Player whitePlayer = createWhitePlayer(whiteName);
		GameMode gameMode = getGameMode();

		Tracker myTracker = EasyTracker.getTracker();
		myTracker.sendEvent(getContext().getString(R.string.an_start_game_cat),
				getContext().getString(R.string.an_start_ai_game_action), type
						+ "", 0L);

		Player blackPlayer = HumanStrategy.createPlayer(blackName, true);
		Player firstPlayer = blackPlayer;
		if (!blackFirst) {
			firstPlayer = whitePlayer;
		}
		final Game game = new Game(type, gameMode, blackPlayer, whitePlayer,
				firstPlayer);
		setGame(game);
		setScore(score);
		LocalMatchInfo theMatchInfo = createNewMatchInfo(blackName, whiteName,
				game, score);
		if (!blackFirst) {
			theMatchInfo
					.setTurnStatus(TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN);
		}
		DatabaseHandler db = new DatabaseHandler(getContext());
		matchInfo = theMatchInfo;
		db.insertMatch(getMatchInfo(), new OnLocalMatchUpdateListener() {
			@Override
			public void onUpdateSuccess(LocalMatchInfo matchInfo) {
				GameFragment gameFragment = getGameFragment();
				if (gameFragment == null) {
					gameFragment = GameFragment.createFragment();

					FragmentManager manager = getActivity()
							.getSupportFragmentManager();
					FragmentTransaction transaction = manager
							.beginTransaction();
					transaction.replace(R.id.main_frame, gameFragment,
							GameContext.FRAG_TAG_GAME);
					transaction.addToBackStack(null);
					transaction.commit();
				}
				gameFragment.startGame(null, false);

			}

			@Override
			public void onUpdateFailure() {
				showAlert("Error inserting match into the DB");
			}
		});
	}

	protected abstract LocalMatchInfo createNewMatchInfo(String blackName,
			String whiteName, final Game game, ScoreCard score);

	protected abstract Player createWhitePlayer(String whiteName);

	@Override
	protected String getMatchId() {
		return getMatchInfo().getId() + "";
	}

}
