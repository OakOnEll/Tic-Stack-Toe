package com.oakonell.ticstacktoe.ui.local;

import android.app.AlertDialog;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.SherlockFragmentActivity;
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
import com.oakonell.ticstacktoe.ui.local.RankedAIPlayAgainFragment.RankedAIPlayAgainListener;

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

	protected void saveToDB() {
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
			gameFinished();
			return;
		}
		getMatchInfo()
				.setTurnStatus(
						getGame().getCurrentPlayer().isBlack() ? TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN
								: TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN);
	}

	protected void gameFinished() {

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
		final RankedAIPlayAgainFragment playAgainDialog = new RankedAIPlayAgainFragment();

		playAgainDialog.initialize(new RankedAIPlayAgainListener() {

			@Override
			public void playAgain() {
				AbstractLocalStrategy.this.playAgain();
			}

			@Override
			public void cancel() {
				leaveGame();
			}
		}, getMatchInfo().getBlackName(), getMatchInfo().getWhiteName(),
				winner, false);
		playAgainDialog.show(getGameContext().getGameFragment()
				.getChildFragmentManager(), "playAgain");

	}

	protected void playAgain() {
		Game game = getMatchInfo().readGame(getContext());
		boolean firstIsBlack = game.getCurrentPlayer().isBlack();
		startNewGame(firstIsBlack, game.getBlackPlayer().getName(), game
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

	public void startNewGame(final boolean blackFirst, final String blackName,
			final String whiteName, final GameType type, final ScoreCard score) {
		final Player whitePlayer = createWhitePlayer(whiteName);
		final GameMode gameMode = getGameMode();

		sendAnalyticStartGameEvent(type);

		final Player blackPlayer = HumanStrategy.createPlayer(blackName, true);
		final Player firstPlayer = blackFirst ? blackPlayer : whitePlayer;

		startGame(gameMode, type, blackFirst, blackName, whitePlayer,
				whiteName, score, blackPlayer, firstPlayer);
		return;
	}

	protected void startGame(GameMode gameMode, GameType type,
			boolean blackFirst, String blackName, Player whitePlayer,
			String whiteName, final ScoreCard score, Player blackPlayer,
			Player firstPlayer) {
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
		matchInfo = theMatchInfo;
		insertMatch();
	}

	protected void insertMatch() {
		DatabaseHandler db = new DatabaseHandler(getContext());
		db.insertMatch(getMatchInfo(), new OnLocalMatchUpdateListener() {
			@Override
			public void onUpdateSuccess(LocalMatchInfo matchInfo) {
				showAndStartGame();

			}
			@Override
			public void onUpdateFailure() {
				showAlert("Error inserting match into the DB");
			}
		});
	}

	protected void showAndStartGame() {
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

	protected abstract LocalMatchInfo createNewMatchInfo(String blackName,
			String whiteName, final Game game, ScoreCard score);

	protected abstract Player createWhitePlayer(String whiteName);

	@Override
	protected String getMatchId() {
		return getMatchInfo().getId() + "";
	}

}
