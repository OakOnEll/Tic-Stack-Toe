package com.oakonell.ticstacktoe;

import java.util.Random;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.actionbarsherlock.app.ActionBar;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesClient;
import com.oakonell.ticstacktoe.googleapi.BaseGameActivity;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.ui.game.GameFragment;
import com.oakonell.ticstacktoe.ui.game.SoundManager;
import com.oakonell.ticstacktoe.ui.menu.MenuFragment;
import com.oakonell.ticstacktoe.ui.menu.StartAGameFragment;
import com.oakonell.utils.Utils;
import com.oakonell.utils.activity.AppLaunchUtils;

public class MainActivity extends BaseGameActivity {
	public static final String FRAG_TAG_GAME = "game";
	public static final String FRAG_TAG_MENU = "menu";
	public static final String FRAG_TAG_START_GAME = "startGame";

	// Request codes for the UIs that we show with startActivityForResult:
	public final static int RC_UNUSED = 1;
	// online play request codes
	public final static int RC_SELECT_PLAYERS = 10000;
	// public final static int RC_INVITATION_INBOX = 10001;
	public final static int RC_WAITING_ROOM = 10002;
	// public final static int RC_LOOK_AT_MATCHES = 10003;

	private GameStrategy gameStrategy;
	private InterstitialAd mInterstitialAd;
	private AdView mAdView;
	private SoundManager soundManager;

	@Override
	protected void onActivityResult(int request, int response, Intent data) {
		super.onActivityResult(request, response, data);
		if (request == RC_WAITING_ROOM) {
			// TODO currently specially launched from (real-time) strategy, with
			// access to activity only
			getMenuFragment().onActivityResult(request, response, data);
		} else if (request == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
			getGameStrategy().leaveRoom();
		}
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

		MenuFragment menuFrag = (MenuFragment) getSupportFragmentManager()
				.findFragmentByTag(FRAG_TAG_MENU);
		if (menuFrag == null) {
			menuFrag = MenuFragment.createMenuFragment(getGameHelper(),
					soundManager);
			FragmentTransaction transaction = getSupportFragmentManager()
					.beginTransaction();
			transaction.add(R.id.main_frame, menuFrag, FRAG_TAG_MENU);
			transaction.commit();
		}

		setSignInMessages(getString(R.string.signing_in),
				getString(R.string.signing_out));
	}

	private void initializeSoundManager() {
		soundManager = new SoundManager(this);
		soundManager.addSound(Sounds.PLAY_X, R.raw.play_x_sounds_882_solemn);
		soundManager.addSound(Sounds.PLAY_O, R.raw.play_o_sounds_913_served);
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
	}

	private void initializeAds() {
		initializeInterstitialAd();

		// initialize banner ad
		mAdView = (AdView) findViewById(R.id.adView);
		mAdView.loadAd(createAdRequest());
	}

	private void initializeInterstitialAd() {
		mInterstitialAd = new InterstitialAd(MainActivity.this);
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

		StartAGameFragment startFragment = getStartFragment();
		if (startFragment != null) {
			startFragment.onSignInSucceeded();
		}

		TicStackToe app = (TicStackToe) getApplication();

		Intent settingsIntent = getGamesClient().getSettingsIntent();
		app.setSettingsIntent(settingsIntent);

		Achievements achievements = app.getAchievements();
		if (achievements.hasPending()) {
			achievements.pushToGoogle(getGameHelper(), this);
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

	}

	@Override
	public GameHelper getGameHelper() {
		return super.getGameHelper();
	}

	@Override
	public GamesClient getGamesClient() {
		return super.getGamesClient();
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
				FRAG_TAG_GAME);
	}

	public void gameEnded() {
		possiblyShowInterstitialAd();
		getMenuFragment().setActive();
		gameStrategy = null;
		mAdView.setVisibility(View.VISIBLE);

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
			}

			// else listener allows just exiting
			if (gameStrategy != null) {
				gameStrategy.leaveRoom();
			} else {
				// store local game...
			}
			getGameFragment().leaveGame();
			return;
		}
		super.onBackPressed();
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
		// mAdView.pause();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// mAdView.resume();
		if (getGameFragment() != null) {
			GameStrategy appListener = getGameStrategy();
			if (appListener != null) {
				appListener.onResume(this);
			}
		}

	}

	@Override
	protected void onDestroy() {
		mAdView.destroy();
		soundManager.release();
		super.onDestroy();
	}

	private void possiblyShowInterstitialAd() {
		// show an ad with some probability (~50%?)
		Random random = new Random();
		if (random.nextInt(10) > 5)
			return;

		if (mInterstitialAd.isLoaded()) {
			mInterstitialAd.show();

			mInterstitialAd.setAdListener(new AdListener() {
				@Override
				public void onAdClosed() {
					super.onAdClosed();
					initializeInterstitialAd();
				}

			});
		}
	}

}
