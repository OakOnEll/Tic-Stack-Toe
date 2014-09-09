package com.oakonell.ticstacktoe;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer.LoadMatchResult;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.InvalidMoveException;
import com.oakonell.ticstacktoe.model.PlayerStrategy;
import com.oakonell.ticstacktoe.model.RankInfo;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.State;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler.OnLocalMatchReadListener;
import com.oakonell.ticstacktoe.settings.SettingsActivity;
import com.oakonell.ticstacktoe.ui.GameTypeDialogFragment;
import com.oakonell.ticstacktoe.ui.game.GameFragment;
import com.oakonell.ticstacktoe.ui.local.AiGameStrategy;
import com.oakonell.ticstacktoe.ui.local.AiMatchInfo;
import com.oakonell.ticstacktoe.ui.local.LocalMatchInfo;
import com.oakonell.ticstacktoe.ui.local.PassNPlayGameStrategy;
import com.oakonell.ticstacktoe.ui.local.PassNPlayMatchInfo;
import com.oakonell.ticstacktoe.ui.local.tutorial.TutorialGameStrategy;
import com.oakonell.ticstacktoe.ui.menu.MenuFragment;
import com.oakonell.ticstacktoe.ui.network.turn.TurnBasedMatchGameStrategy;
import com.oakonell.ticstacktoe.utils.DevelopmentUtil.Info;

/**
 * The abstract base class for handling player interactions.
 * 
 */
public abstract class GameStrategy {
	private static final String BUNDLE_KEY_GAME_STRATEGY_MATCH_ID = "GAME_STRATEGY_MATCH_ID";
	private static final String BUNDLE_KEY_GAME_STRATEGY_TYPE = "GAME_STRATEGY_TYPE";

	private static final String TAG = "GameStrategy";
	private GameContext gameContext;

	private Game game;
	private ScoreCard score = new ScoreCard(0, 0, 0);

	protected GameStrategy(GameContext gameContext) {
		this.gameContext = gameContext;
		gameContext.setGameStrategy(this);
	}

	protected abstract GameMode getGameMode();

	public abstract void leaveRoom();

	public abstract void sendHumanMove();

	public abstract void backFromWaitingRoom();

	public abstract boolean warnToLeave();

	public abstract void promptToPlayAgain(String winner, String title);

	public abstract void onSignInSuccess(MainActivity activity);

	public void onActivityPause(MainActivity mainActivity2) {
		// TODO Auto-generated method stub

	}

	public abstract void onActivityResume(MainActivity activity);

	public abstract void onSignInFailed(SherlockFragmentActivity mainActivity);

	public void showSettings(Fragment fragment) {
		showFullSettingsPreference(fragment);
	}

	public boolean shouldKeepScreenOn() {
		return false;
	}

	public int playSound(Sounds sound) {
		return gameContext.getSoundManager().playSound(sound);
	}

	public int playSound(Sounds sound, boolean loop) {
		return gameContext.getSoundManager().playSound(sound, loop);
	}

	public void stopSound(int streamId) {
		gameContext.getSoundManager().stopSound(streamId);
	}

	protected Context getContext() {
		return gameContext.getContext();
	}

	protected SherlockFragmentActivity getActivity() {
		return gameContext.getSherlockActivity();
	}

	protected GameFragment getGameFragment() {
		return gameContext.getGameFragment();
	}

	protected MenuFragment getMenuFragment() {
		return gameContext.getMenuFragment();
	}

	public boolean onOptionsItemSelected(Fragment fragment, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			showSettings(fragment);
			return true;
		case R.id.action_help:
			showGameTypeDialog();
			return true;
		}
		return false;
	}

	protected void showGameTypeDialog() {
		GameTypeDialogFragment dialog = new GameTypeDialogFragment();
		dialog.initialize(getGameContext());
		dialog.show(getActivity().getSupportFragmentManager(), "gamehelp");
	}

	public void onCreateOptionsMenu(Fragment fragment, Menu menu,
			MenuInflater inflater) {
		inflater.inflate(R.menu.menu_game, menu);
		// possibly change the help icon, based on the game type
	}

	public void onPrepareOptionsMenu(Fragment fragment, Menu menu) {
		// do nothing
	}

	private void showFullSettingsPreference(Fragment fragment) {
		// create special intent
		Intent prefIntent = new Intent(fragment.getActivity(),
				SettingsActivity.class);

		GameHelper helper = getHelper();
		Info info = null;
		TicStackToe app = (TicStackToe) fragment.getActivity().getApplication();
		if (helper.isSignedIn()) {
			info = new Info(helper);
		}
		app.setDevelopInfo(info);
		// ugh.. does going to preferences leave the room!?
		fragment.getActivity().startActivityForResult(prefIntent,
				GameContext.RC_UNUSED);
	}

	public void acceptMove() {
		final PlayerStrategy currentStrategy = getGame().getCurrentPlayer()
				.getStrategy();
		if (currentStrategy.isHuman()) {
			acceptHumanMove(currentStrategy);
			return;
		}

		acceptNonHumanPlayerMove(currentStrategy);
	}

	protected void acceptHumanMove(PlayerStrategy currentStrategy) {
		getGameFragment().acceptHumanMove();
	}

	abstract protected void acceptNonHumanPlayerMove(
			final PlayerStrategy currentStrategy);

	public Game getGame() {
		return game;
	}

	protected void setGame(Game game) {
		this.game = game;
	}

	public ScoreCard getScore() {
		return score;
	}

	protected void setScore(ScoreCard score) {
		this.score = score;
	}

	protected GameContext getGameContext() {
		return gameContext;
	}

	protected GameHelper getHelper() {
		return gameContext.getGameHelper();
	}

	public boolean shouldHideAd() {
		return false;
	}

	protected State applyNonHumanMove(AbstractMove move) {
		try {
			return move.applyToGame(getGame());
		} catch (InvalidMoveException e) {
			throw new RuntimeException(
					"Game error while applying non-human move");
		}
	}

	public void attemptHumanMove(AbstractMove move, OnHumanMove onHumanMove) {
		try {
			State state = move.applyToGame(getGame());
			onHumanMove.onSuccess(state);
			sendHumanMove();
		} catch (final InvalidMoveException e) {
			onHumanMove.onInvalid(e);
			return;
		}
	}

	public interface OnHumanMove {
		void onSuccess(State state);

		void onInvalid(InvalidMoveException e);
	}

	public void writeToBundle(Bundle bundle) {
		Log.i("GameStrategy", "Writing strategy to bundle: " + getGameMode()
				+ ", " + getMatchId());
		bundle.putInt(BUNDLE_KEY_GAME_STRATEGY_TYPE, getGameMode().getVal());
		writeDetailsToBundle(bundle);
	}

	protected void writeDetailsToBundle(Bundle bundle) {
		bundle.putString(BUNDLE_KEY_GAME_STRATEGY_MATCH_ID, getMatchId());
	}

	protected abstract String getMatchId();

	public interface OnGameStrategyLoad {
		void onSuccess(GameStrategy strategy);

		void onFailure(String reason);
	}

	public static class StrategyId {
		private final GameMode mode;
		private final String matchId;

		StrategyId(Bundle bundle) {
			int modeVal = bundle.getInt(BUNDLE_KEY_GAME_STRATEGY_TYPE);
			mode = GameMode.fromValue(modeVal);
			matchId = bundle.getString(BUNDLE_KEY_GAME_STRATEGY_MATCH_ID);
		}

		public boolean waitTillSignIn() {
			return mode == GameMode.TURN_BASED;
		}
	}

	public static StrategyId readFromBundle(final GameContext context,
			Bundle bundle) {
		if (!bundle.containsKey(BUNDLE_KEY_GAME_STRATEGY_TYPE))
			return null;
		return new StrategyId(bundle);
	}

	public static void read(final GameContext context, StrategyId id,
			final OnGameStrategyLoad onLoad) {
		if (id == null) {
			onLoad.onSuccess(null);
			return;
		}
		loadStrategy(context, onLoad, id.mode, id.matchId);
	}

	private static void loadStrategy(final GameContext context,
			final OnGameStrategyLoad onLoad, GameMode mode, String matchId) {
		switch (mode) {
		case AI:
			loadAIStrategy(context, onLoad, matchId);
			break;
		case PASS_N_PLAY:
			loadPassNPlayStrategy(context, onLoad, matchId);
			break;
		case TURN_BASED:
			loadTurnBasedStrategy(context, onLoad, matchId);
			break;
		case TUTORIAL:
			loadTutorialStrategy(context, onLoad, matchId);
			break;
		case ONLINE:
			onLoad.onFailure("Realtime game can't be restored...");
			break;
		}
	}

	private static void loadTutorialStrategy(GameContext context,
			OnGameStrategyLoad onLoad, String matchId) {

		onLoad.onSuccess(new TutorialGameStrategy(context, true));
	}

	private static void loadTurnBasedStrategy(final GameContext context,
			final OnGameStrategyLoad onLoad, final String matchId) {
		if (!context.getGameHelper().isSignedIn()) {
			onLoad.onFailure("Not Signed in");
			return;
		}

		Games.TurnBasedMultiplayer
				.loadMatch(context.getGameHelper().getApiClient(), matchId)
				.setResultCallback(
						new ResultCallback<TurnBasedMultiplayer.LoadMatchResult>() {

							@Override
							public void onResult(LoadMatchResult result) {
								Status status = result.getStatus();
								TurnBasedMatch match = result.getMatch();
								if (status.getStatusCode() != GamesClient.STATUS_OK) {
									onLoad.onFailure("Error(" + status
											+ ") loading turn-based game");
									return;
								}
								onLoad.onSuccess(new TurnBasedMatchGameStrategy(
										context, match, false));

							}
						});
	}

	private static void loadPassNPlayStrategy(final GameContext context,
			final OnGameStrategyLoad onLoad, String matchId) {
		DatabaseHandler handler = new DatabaseHandler(context.getContext());
		handler.getMatch(Long.parseLong(matchId),
				new OnLocalMatchReadListener() {
					@Override
					public void onReadSuccess(LocalMatchInfo matchInfo) {
						PassNPlayMatchInfo localMatchInfo = (PassNPlayMatchInfo) matchInfo;
						onLoad.onSuccess(new PassNPlayGameStrategy(context,
								localMatchInfo));
					}

					@Override
					public void onReadFailure() {
						onLoad.onFailure("Could not load local Pass'N'Play match");
					}
				});

	}

	private static void loadAIStrategy(final GameContext context,
			final OnGameStrategyLoad onLoad, String matchId) {
		DatabaseHandler handler = new DatabaseHandler(context.getContext());
		handler.getMatch(Long.parseLong(matchId),
				new OnLocalMatchReadListener() {
					@Override
					public void onReadSuccess(LocalMatchInfo matchInfo) {
						AiMatchInfo aiMatchInfo = (AiMatchInfo) matchInfo;
						onLoad.onSuccess(new AiGameStrategy(context,
								aiMatchInfo.getWhiteAILevel(), aiMatchInfo));
					}

					@Override
					public void onReadFailure() {
						onLoad.onFailure("Could not load local AI match");
					}
				});
	}

	public void showGame() {
		getGameFragment().startGame(false);
	}

	public abstract RankInfo getRankInfo();

	public void viewCreated(GameFragment gameFragment, LayoutInflater inflater,
			ViewGroup container, FrameLayout frame) {

	}

	protected void sendAnalyticStartGameEvent(final GameType type) {
		Tracker myTracker = EasyTracker.getTracker();
		String gameAction = getContext()
				.getString(getAnalyticGameActionResId());
		Log.i(TAG, "Starting game " + gameAction + ", type=" + type + "");
		myTracker.sendEvent(getContext().getString(R.string.an_start_game_cat),
				gameAction, type + "", 0L);
	}

	protected abstract int getAnalyticGameActionResId();

	public void invalidateMenu() {
		if (!ActivityCompat.invalidateOptionsMenu(getActivity())) {
			handleMenu();
		} else {
			honeyCombInvalidateMenu();
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void honeyCombInvalidateMenu() {
		getActivity().invalidateOptionsMenu();
	}

	protected void handleMenu() {
		ActivityCompat.invalidateOptionsMenu(getActivity());
	}

	public void evaluateGameEndAchievements(Achievements achievements,
			GameContext gameContext2, Game game2, State outcome) {
		// do nothing- subclasses override
	}

	public void postMove(Runnable postMove) {
		postMove.run();		
	}

	public boolean rotateBlackLayout() {
		return false;
	}
}
