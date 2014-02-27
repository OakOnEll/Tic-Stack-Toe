package com.oakonell.ticstacktoe.ui.local;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.GameStrategy;
import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.AbstractMove;
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

public abstract class AbstractLocalStrategy extends GameStrategy {
	private LocalMatchInfo matchInfo;

	public AbstractLocalStrategy(MainActivity mainActivity,
			SoundManager soundManager) {
		super(mainActivity, soundManager);

	}

	public AbstractLocalStrategy(MainActivity mainActivity,
			LocalMatchInfo localMatchInfo, SoundManager soundManager) {
		super(mainActivity, soundManager);
		this.setMatchInfo(localMatchInfo);
	}

	@Override
	public void leaveRoom() {
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
	public void sendMove(Game game, AbstractMove lastMove, ScoreCard score) {
		// TODO Auto-generated method stub
		// write the game to the DB, or wait until leave...??
		if (game.getBoard().getState().isOver()) {
			matchInfo.setMatchStatus(TurnBasedMatch.MATCH_STATUS_COMPLETE);
			Player winner = game.getBoard().getState().getWinner();
			if (winner == null) {
				// draw
			} else if (winner.isBlack()) {
				matchInfo.setWinner(1);
			} else {
				matchInfo.setWinner(-1);
			}
			matchInfo.setScoreCard(score);
			saveToDB();
			return;
		}
		getMatchInfo()
				.setTurnStatus(
						getMatchInfo().getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN ? TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN
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
		getMainActivity().getGameFragment().leaveGame();
		getMainActivity().getMenuFragment().leaveRoom();
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

	abstract protected void playAgain();

	@Override
	public void onResume(MainActivity activity) {
		// do nothing
	}

	@Override
	public void onFragmentResume() {
		// do nothing
	}

	@Override
	public void onSignInSuccess(MainActivity activity) {
		// nothing to do, no dependency on being signed in
	}

	@Override
	public void onSignInFailed(MainActivity mainActivity) {
		// no worries
	}

	public void showFromMenu() {
		final ScoreCard score = new ScoreCard(0, 0, 0);
		GameFragment gameFragment = GameFragment.createFragment(this);
		gameFragment.startGame(getMatchInfo().readGame(getContext()), score,
				null, true);

		FragmentManager manager = getMainActivity().getSupportFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		transaction.replace(R.id.main_frame, gameFragment,
				MainActivity.FRAG_TAG_GAME);
		transaction.addToBackStack(null);
		transaction.commit();

	}

	protected LocalMatchInfo getMatchInfo() {
		return matchInfo;
	}

	protected void setMatchInfo(LocalMatchInfo matchInfo) {
		this.matchInfo = matchInfo;
	}

	public void startGame(String blackName, String whiteName, GameType type,
			final ScoreCard score) {
		Player whitePlayer = createWhitePlayer(whiteName);
		GameMode gameMode = getGameMode();

		Tracker myTracker = EasyTracker.getTracker();
		myTracker.sendEvent(getContext().getString(R.string.an_start_game_cat),
				getContext().getString(R.string.an_start_ai_game_action), type
						+ "", 0L);

		Player blackPlayer = HumanStrategy.createPlayer(blackName, true);
		final Game game = new Game(type, gameMode, blackPlayer, whitePlayer,
				blackPlayer);
		LocalMatchInfo theMatchInfo = createMatchInfo(blackName, whiteName,
				game, score);

		DatabaseHandler db = new DatabaseHandler(getContext());
		matchInfo = theMatchInfo;
		db.insertMatch(getMatchInfo(), new OnLocalMatchUpdateListener() {
			@Override
			public void onUpdateSuccess(LocalMatchInfo matchInfo) {
				GameFragment gameFragment = getMainActivity().getGameFragment();
				if (gameFragment == null) {
					gameFragment = GameFragment
							.createFragment(AbstractLocalStrategy.this);

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

	protected abstract LocalMatchInfo createMatchInfo(String blackName,
			String whiteName, final Game game, ScoreCard score);

	protected abstract GameMode getGameMode();

	protected abstract Player createWhitePlayer(String whiteName);
}
