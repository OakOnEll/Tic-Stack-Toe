package com.oakonell.ticstacktoe;

import java.util.Map;
import java.util.Random;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.oakonell.ticstacktoe.GameStrategy.OnGameStrategyLoad;
import com.oakonell.ticstacktoe.GameStrategy.StrategyId;
import com.oakonell.ticstacktoe.googleapi.BaseGameActivity;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler;
import com.oakonell.ticstacktoe.model.rank.RankStorage;
import com.oakonell.ticstacktoe.model.solver.AILevel;
import com.oakonell.ticstacktoe.rank.AIRankHelper;
import com.oakonell.ticstacktoe.rank.AIRankHelper.OnRanksRetrieved;
import com.oakonell.ticstacktoe.rank.RankHelper;
import com.oakonell.ticstacktoe.rank.RankHelper.OnRankReceived;
import com.oakonell.ticstacktoe.ui.game.GameFragment;
import com.oakonell.ticstacktoe.ui.game.SoundManager;
import com.oakonell.ticstacktoe.ui.menu.MenuFragment;
import com.oakonell.ticstacktoe.ui.menu.StartAGameFragment;
import com.oakonell.utils.Utils;
import com.oakonell.utils.activity.AppLaunchUtils;

public class MainActivity extends BaseGameActivity implements GameContext {
	private GameStrategy gameStrategy;
	private InterstitialAd mInterstitialAd;
	private AdView mAdView;
	private SoundManager soundManager;

	private StrategyId strategyToLoadOnSignIn;
	private RankRequest rankRequest;

	private class RankRequest {
		private RankStorage rankStorage;
		private OnRankReceived onRankLoaded;
		private boolean rankLoaded = false;
	}

	public MainActivity() {
		super(CLIENT_GAMES | CLIENT_APPSTATE);
	}

	@Override
	protected void onActivityResult(int request, int response, Intent data) {
		super.onActivityResult(request, response, data);
		if (request == GameContext.RC_WAITING_ROOM) {
			if (getStartFragment() != null && getStartFragment().isVisible()) {
				getStartFragment().onActivityResult(request, response, data);
			} else {
				backFromRealtimeWaitingRoom();
			}
		} else if (request == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
			getGameStrategy().leaveRoom();
		}
	}

	@Override
	public void backFromRealtimeWaitingRoom() {
		if (getGameStrategy().shouldHideAd()) {
			hideAd();
		}
		getGameStrategy().backFromWaitingRoom();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.enableStrictMode();
		setContentView(R.layout.main_activity);

		initializeAds();

		initializeSoundManager();

		AppLaunchUtils.appLaunched(this, null);

		final ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(false);
		ab.setDisplayUseLogoEnabled(true);
		ab.setDisplayShowTitleEnabled(true);

		if (savedInstanceState != null) {
			StrategyId strategyId = GameStrategy.readFromBundle(this,
					savedInstanceState);
			if (strategyId != null && !strategyId.waitTillSignIn()
					&& isSignedIn()) {
				loadStrategy(strategyId);
			} else {
				strategyToLoadOnSignIn = strategyId;
			}
		}

		MenuFragment menuFrag = (MenuFragment) getSupportFragmentManager()
				.findFragmentByTag(FRAG_TAG_MENU);
		if (menuFrag == null) {
			menuFrag = MenuFragment.createMenuFragment();
			FragmentTransaction transaction = getSupportFragmentManager()
					.beginTransaction();
			transaction.add(R.id.main_frame, menuFrag, FRAG_TAG_MENU);
			transaction.commit();
		}

		// TODO no longer used signing in/out resource
		// setSignInMessages(getString(R.string.signing_in),
		// getString(R.string.signing_out));
	}

	private void loadStrategy(StrategyId strategyId) {
		GameStrategy.read(this, strategyId, new OnGameStrategyLoad() {

			@Override
			public void onSuccess(GameStrategy strategy) {
				// TODO show the game?
				// gameStrategy.
				if (strategy == null) {
					return;
				}
				setGameStrategy(strategy);
				strategy.showGame();
			}

			@Override
			public void onFailure(String reason) {
				showAlert(reason);
			}

		});
	}

	private void initializeSoundManager() {
		soundManager = new SoundManager(this);
		soundManager.addSound(Sounds.INVALID_MOVE,
				R.raw.invalid_move_metal_gong_dianakc_109711828);
		soundManager.addSound(Sounds.CHAT_RECIEVED,
				R.raw.chat_received_sounds_954_all_eyes_on_me);
		soundManager.addSound(Sounds.INVITE_RECEIVED,
				R.raw.invite_received_sounds_1044_inquisitiveness);
		soundManager.addSound(Sounds.GAME_LOST,
				R.raw.game_lost_sad_trombone_joe_lamb_665429450);
		soundManager.addSound(Sounds.GAME_WON,
				R.raw.game_won_small_crowd_applause_yannick_lemieux_1268806408);
		soundManager.addSound(Sounds.GAME_DRAW, R.raw.game_draw_clong_1);

		soundManager.addSound(Sounds.PUT_ON, R.raw.put_on);
		soundManager.addSound(Sounds.TAKE_OFF, R.raw.take_off);
		soundManager.addSound(Sounds.SLIDE_ON, R.raw.slide_on);
		soundManager.addSound(Sounds.SLIDE_OFF, R.raw.slide_off);

	}

	private void initializeAds() {
		initializeInterstitialAd();

		// initialize banner ad
		mAdView = (AdView) findViewById(R.id.adView);
		mAdView.loadAd(createAdRequest());
	}

	private void initializeInterstitialAd() {
		mInterstitialAd = new InterstitialAd(this);
		mInterstitialAd
				.setAdUnitId(getResources().getString(R.string.admob_id));
		mInterstitialAd.loadAd(createAdRequest());
	}

	private AdRequest createAdRequest() {
		return new com.google.android.gms.ads.AdRequest.Builder().build();
	}

	@Override
	public void onSignInFailed() {
		if (getMenuFragment() != null) {
			getMenuFragment().onSignInFailed();
		}
		if (getGameStrategy() != null) {
			getGameStrategy().onSignInFailed(this);
		}

	}

	@Override
	public void signOut() {
		getMenuFragment().signOut();
		super.signOut();
	}

	@Override
	public void onSignInSucceeded() {
		getMenuFragment().onSignInSucceeded();

		if (strategyToLoadOnSignIn != null) {
			loadStrategy(strategyToLoadOnSignIn);
			strategyToLoadOnSignIn = null;
		}

		StartAGameFragment startFragment = getStartFragment();
		if (startFragment != null) {
			startFragment.onSignInSucceeded();
		}

		TicStackToe app = (TicStackToe) getApplication();

		Intent settingsIntent = Games.getSettingsIntent(getApiClient());
		app.setSettingsIntent(settingsIntent);

		Achievements achievements = app.getAchievements();
		if (achievements.hasPending()) {
			achievements.pushToGoogle(this);
		}

		if (getGameStrategy() != null) {
			getGameStrategy().onSignInSuccess(this);
		}

		// if we received an invite via notification, accept it; otherwise, go
		// to main screen
		String invitationId = getInvitationId();
		if (invitationId != null) {
			getMenuFragment().acceptInviteToRoom(invitationId);
			return;
		}

		loadRank(null, false);

		// precache the AI ranks from the server, if needed
		DatabaseHandler dbHandler = new DatabaseHandler(this);
		AIRankHelper.retrieveRanks(dbHandler, GameType.NORMAL,
				new OnRanksRetrieved() {
					@Override
					public void onSuccess(Map<AILevel, Integer> ranks) {
						// do nothing
					}
				});

	}

	public void clearRanks() {
		rankRequest = null;
	}

	public void loadRank(final OnRankReceived onRankLoaded,
			boolean initializeIfNone) {
		if (rankRequest == null) {
			rankRequest = new RankRequest();
			rankRequest.onRankLoaded = onRankLoaded;
			RankHelper.loadRankStorage(this, new OnRankReceived() {
				@Override
				public void receivedRank(RankStorage rankStorage) {
					// mark in some way that a null was loaded?
					rankRequest.rankLoaded = true;
					rankRequest.rankStorage = rankStorage;
					if (rankRequest.onRankLoaded != null) {
						rankRequest.onRankLoaded.receivedRank(rankStorage);
					}
					rankRequest.onRankLoaded = null;
				}
			}, false);
		}

		if (!rankRequest.rankLoaded) {
			if (rankRequest.onRankLoaded != null) {
				if (onRankLoaded == null)
					return;
				final OnRankReceived existing = rankRequest.onRankLoaded;
				rankRequest.onRankLoaded = new OnRankReceived() {
					@Override
					public void receivedRank(RankStorage storage) {
						existing.receivedRank(storage);
						onRankLoaded.receivedRank(storage);
					}
				};
			}
			rankRequest.onRankLoaded = onRankLoaded;
			return;
		}
		if (onRankLoaded != null) {
			onRankLoaded.receivedRank(rankRequest.rankStorage);
		}
	}

	@Override
	public void updateCachedRank(RankStorage storage) {
		if (rankRequest == null) {
			rankRequest = new RankRequest();
			rankRequest.rankStorage = storage;
			rankRequest.rankLoaded = true;
			return;
		}
		if (rankRequest.rankLoaded) {
			rankRequest.rankStorage = storage;
			return;
		}
		// hmm... do we just let the current request continue?

	}

	@Override
	public GameHelper getGameHelper() {
		return super.getGameHelper();
	}

	public SoundManager getSoundManager() {
		return soundManager;
	}

	public MenuFragment getMenuFragment() {
		return (MenuFragment) getSupportFragmentManager().findFragmentByTag(
				FRAG_TAG_MENU);
	}

	public StartAGameFragment getStartFragment() {
		return (StartAGameFragment) getSupportFragmentManager()
				.findFragmentByTag(FRAG_TAG_START_GAME);
	}

	public GameFragment getGameFragment() {
		return (GameFragment) getSupportFragmentManager().findFragmentByTag(
				GameContext.FRAG_TAG_GAME);
	}

	public void gameEnded() {
		possiblyShowInterstitialAd();
		gameStrategy = null;
		mAdView.setVisibility(View.VISIBLE);
		getMenuFragment().gameEnded();

		getSupportActionBar().setDisplayHomeAsUpEnabled(false);
	}

	public void hideAd() {
		mAdView.setVisibility(View.GONE);
	}

	public GameStrategy getGameStrategy() {
		return gameStrategy;
	}

	public void setGameStrategy(GameStrategy gameStrategy) {
		this.gameStrategy = gameStrategy;
		if (getGameFragment() != null) {
			getGameFragment().updatedGameStrategy();
		}
	}

	@Override
	public void onBackPressed() {
		if (getGameFragment() != null && getGameFragment().isVisible()) {
			// TODO only realtime game should ask to leave
			// local games can be stored and continued
			// turn-based games also would be written
			if (gameStrategy != null) {
				if (gameStrategy.warnToLeave()) {
					promptAndLeave();
					return;
				}
				gameStrategy.leaveRoom();
			}
			getGameFragment().leaveGame();
			return;
		}
		super.onBackPressed();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (getGameStrategy() != null) {
			getGameStrategy().writeToBundle(outState);
		}
	}

	private void promptAndLeave() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.leave_game_title);
		builder.setMessage(R.string.leave_game_message);
		builder.setPositiveButton(R.string.yes, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();

				if (gameStrategy != null) {
					gameStrategy.leaveRoom();
				}
				getGameFragment().leaveGame();
				// MainActivity.super.onBackPressed();

			}
		});
		builder.setNegativeButton(R.string.no, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.show();
	}

	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		GameStrategy strategy = getGameStrategy();
		if (strategy != null) {
			strategy.onActivityPause(this);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		GameStrategy strategy = getGameStrategy();
		if (strategy != null) {
			strategy.onActivityResume(this);
		}
	}

	@Override
	protected void onDestroy() {
		mAdView.destroy();
		soundManager.release();
		super.onDestroy();
	}

	private void possiblyShowInterstitialAd() {
		// show an ad with some probability
		Random random = new Random();
		if (random.nextInt(100) > 25) {
			return;
		}

		if (!mInterstitialAd.isLoaded()) {
			return;
		}
		mInterstitialAd.show();

		mInterstitialAd.setAdListener(new AdListener() {
			@Override
			public void onAdClosed() {
				super.onAdClosed();
				initializeInterstitialAd();
			}

		});
	}

	@Override
	public Context getContext() {
		return this;
	}

	@Override
	public SherlockFragmentActivity getSherlockActivity() {
		return this;
	}

}
