package com.oakonell.ticstacktoe.ui.menu;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer.InitiateMatchResult;
import com.oakonell.ticstacktoe.GameContext;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.solver.AILevel;
import com.oakonell.ticstacktoe.ui.local.AbstractLocalStrategy;
import com.oakonell.ticstacktoe.ui.local.AiGameStrategy;
import com.oakonell.ticstacktoe.ui.local.NewAIGameDialog;
import com.oakonell.ticstacktoe.ui.local.NewAIGameDialog.LocalAIGameModeListener;
import com.oakonell.ticstacktoe.ui.local.NewLocalGameDialog;
import com.oakonell.ticstacktoe.ui.local.NewLocalGameDialog.LocalGameModeListener;
import com.oakonell.ticstacktoe.ui.local.PassNPlayGameStrategy;

import com.oakonell.ticstacktoe.ui.network.OnlineGameModeDialog;
import com.oakonell.ticstacktoe.ui.network.OnlineGameModeDialog.OnlineGameModeListener;
import com.oakonell.ticstacktoe.ui.network.realtime.RealtimeGameStrategy;
import com.oakonell.ticstacktoe.ui.network.turn.TurnBasedMatchGameStrategy;

public class StartAGameFragment extends SherlockFragment {
	private static final String TAG = "StartAGameFragment";

	private Boolean useTurnBased;
	private GameType onlineType;
	private Boolean isRanked;

	private ProgressBar waiting;

	private GameContext context;

	public StartAGameFragment() {
		// for finding references
	}

	public static StartAGameFragment createStartGameFragment() {
		StartAGameFragment fragment = new StartAGameFragment();
		return fragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = (GameContext) activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_menu_start,
				container, false);
		setHasOptionsMenu(true);
		waiting = (ProgressBar) view.findViewById(R.id.waiting);
		setActive();
		final ActionBar ab = getSherlockActivity().getSupportActionBar();
		ab.setTitle("Start a Game");

		// Listen for changes in the back stack
		getSherlockActivity().getSupportFragmentManager()
				.addOnBackStackChangedListener(
						new OnBackStackChangedListener() {
							@Override
							public void onBackStackChanged() {
								configureDisplayHomeUp();
							}
						});
		// Handle when activity is recreated like on orientation Change
		configureDisplayHomeUp();

		View newGameOnSameDevice = view.findViewById(R.id.new_game_same_device);
		newGameOnSameDevice.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				selectLocalGame();
			}
		});

		View ai = view.findViewById(R.id.new_game_vs_ai);
		ai.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				selectAIGame();
			}
		});

		// network games

		View quick = view.findViewById(R.id.new_quick_play);
		quick.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (context.getGameHelper().isSignedIn()) {
					selectQuickMode();
				} else {
					showAlert(getResources().getString(
							R.string.sign_in_to_play_network_game));
				}
			}

		});

		View inviteFriend = view.findViewById(R.id.new_game_live);
		inviteFriend.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (context.getGameHelper().isSignedIn()) {
					selectOnlineGameMode();
				} else {
					showAlert(getResources().getString(
							R.string.sign_in_to_play_network_game));
				}
			}
		});

		return view;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// if (useUpNavigation()) {
			// NavUtils.navigateUpFromSameTask(this);
			// } else {
			setActive();
			exitStartMenu();
			// }
			return true;
		}
		return false;
	}

	private void configureDisplayHomeUp() {
		if (getActivity() == null)
			return;
		getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(
				true);
	}

	public void setActive() {
		context.getMenuFragment().setActive();
		waiting.setVisibility(View.INVISIBLE);
		Log.i(TAG, "Setting active");
	}

	public void setInactive() {
		context.getMenuFragment().setInactive();
		waiting.setVisibility(View.VISIBLE);
		Log.i(TAG, "Setting inactive");
	}

	@Override
	public void onActivityResult(int request, int response, Intent data) {
		setActive();
		switch (request) {
		case GameContext.RC_SELECT_PLAYERS: {
			if (response == Activity.RESULT_OK) {
				final ArrayList<String> invitees = data
						.getStringArrayListExtra(GamesClient.EXTRA_PLAYERS);
				int minAutoMatchPlayers = data.getIntExtra(
						GamesClient.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
				int maxAutoMatchPlayers = data.getIntExtra(
						GamesClient.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

				// get the automatch criteria
				Bundle autoMatchCriteria = null;
				if (minAutoMatchPlayers > 0 || maxAutoMatchPlayers > 0) {
					autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
							minAutoMatchPlayers, maxAutoMatchPlayers, 0);
					Log.d(TAG, "Automatch criteria: " + autoMatchCriteria);
				}

				GameType type = onlineType;
				onlineType = null;
				boolean turnBased = useTurnBased;
				useTurnBased = null;
				boolean ranked = isRanked;
				isRanked = null;

				createOnlineRoom(invitees, type, turnBased, autoMatchCriteria,
						true, ranked);
			} else {
				Log.i(TAG, "Select players canceled");
			}
		}
			break;

		case GameContext.RC_WAITING_ROOM:
			// ignore result if we dismissed the waiting room from code:
			// if (mWaitRoomDismissedFromCode)
			// break;

			// we got the result from the "waiting room" UI.
			if (response == Activity.RESULT_OK) {
				setInactive();
				context.backFromRealtimeWaitingRoom();
			} else if (response == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
				// player actively indicated that they want to leave the room

				exitStartMenu();
			} else if (response == Activity.RESULT_CANCELED) {
				/*
				 * Dialog was cancelled (user pressed back key, for instance).
				 * In our game, this means leaving the room too. In more
				 * elaborate games,this could mean something else (like
				 * minimizing the waiting room UI but continue in the handshake
				 * process).
				 */
				exitStartMenu();
			}
			break;
		}
	}

	private void exitStartMenu() {
		getActivity().getSupportFragmentManager().popBackStack();
		final ActionBar ab = getSherlockActivity().getSupportActionBar();
		ab.setTitle("Tic-Stack-Toe");
	}

	private void selectAIGame() {
		// prompt for AI level and game mode
		// then start the game activity
		setInactive();
		NewAIGameDialog dialog = new NewAIGameDialog();
		dialog.initialize(new LocalAIGameModeListener() {
			@Override
			public void chosenMode(GameType type, String aiName, AILevel level,
					boolean isRanked) {
				startAIGame(type, aiName, level, isRanked);
			}

			@Override
			public void cancel() {
				setActive();
			}
		});
		dialog.show(getFragmentManager(), "aidialog");

		// TestDialogFrag frag = new TestDialogFrag();
		// frag.show(getFragmentManager(), "aidialog");

	}

	private void selectLocalGame() {
		// prompt for player names, and game type
		// then start the game activity
		setInactive();
		NewLocalGameDialog dialog = new NewLocalGameDialog();
		dialog.initialize(new LocalGameModeListener() {
			@Override
			public void chosenMode(GameType type, String xName, String oName) {
				startLocalTwoPlayerGame(type, xName, oName);
			}

			@Override
			public void cancel() {
				setActive();
			}
		});
		dialog.show(getFragmentManager(), "localgame");
	}

	private void selectQuickMode() {
		OnlineGameModeDialog dialog = new OnlineGameModeDialog();
		dialog.initialize(true, new OnlineGameModeListener() {
			@Override
			public void chosenMode(GameType type, boolean useTurnBased,
					boolean isRanked) {
				final int MIN_OPPONENTS = 1, MAX_OPPONENTS = 1;
				Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
						MIN_OPPONENTS, MAX_OPPONENTS, 0);

				if (useTurnBased) {
					createTurnBasedQuickMatch(type, autoMatchCriteria, isRanked);
					return;
				}

				createRealtimeBasedQuickMatch(type, autoMatchCriteria, isRanked);

			}
		});
		dialog.show(getSherlockActivity().getSupportFragmentManager(),
				"gameMode");

	}

	protected void createTurnBasedQuickMatch(GameType type,
			Bundle autoMatchCriteria, boolean isRanked) {
		setInactive();

		int variant = type.getVariant();
		if (isRanked)
			variant += 100;

		TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
				.setAutoMatchCriteria(autoMatchCriteria).setVariant(variant)
				.build();

		// TODO
		final TurnBasedMatchGameStrategy listener = new TurnBasedMatchGameStrategy(
				context, type, true, isRanked);

		// Kick the match off
		Games.TurnBasedMultiplayer
				.createMatch(context.getGameHelper().getApiClient(), tbmc)
				.setResultCallback(
						new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
							@Override
							public void onResult(InitiateMatchResult result) {
								Status status = result.getStatus();
								TurnBasedMatch match = result.getMatch();
								if (status.getStatusCode() != GamesClient.STATUS_OK) {
									Toast.makeText(context.getContext(),
											"Error starting match: " + status,
											Toast.LENGTH_LONG).show();
									exitStartMenu();
									return;
								}
								exitStartMenu();
								listener.onTurnBasedMatchInitiated(
										status.getStatusCode(), match);
							}
						});

	}

	protected void createRealtimeBasedQuickMatch(GameType type,
			Bundle autoMatchCriteria, boolean isRanked) {
		int variant = type.getVariant();
		if (isRanked)
			variant += 100;

		RealtimeGameStrategy roomListener = new RealtimeGameStrategy(context,
				type, true, true, isRanked);

		final RoomConfig.Builder rtmConfigBuilder = RoomConfig
				.builder(roomListener);
		rtmConfigBuilder.setVariant(variant);
		rtmConfigBuilder.setMessageReceivedListener(roomListener);
		rtmConfigBuilder.setRoomStatusUpdateListener(roomListener);
		rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
		setInactive();
		// post delayed so that the progess is shown
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				Games.RealTimeMultiplayer.create(context.getGameHelper()
						.getApiClient(), rtmConfigBuilder.build());
			}
		}, 0);

	}

	private void selectOnlineGameMode() {
		// first choose Game mode
		OnlineGameModeDialog dialog = new OnlineGameModeDialog();
		dialog.initialize(false, new OnlineGameModeListener() {
			@Override
			public void chosenMode(GameType type, boolean useTurnBased,
					boolean isRanked) {
				StartAGameFragment.this.onlineType = type;
				StartAGameFragment.this.useTurnBased = useTurnBased;
				StartAGameFragment.this.isRanked = isRanked;
				Intent intent = Games.RealTimeMultiplayer
						.getSelectOpponentsIntent(context.getGameHelper()
								.getApiClient(), 1, 1);
				setInactive();
				startActivityForResult(intent, GameContext.RC_SELECT_PLAYERS);
			}
		});
		dialog.show(getSherlockActivity().getSupportFragmentManager(),
				"gameMode");
	}

	private void createOnlineRoom(final ArrayList<String> invitees,
			GameType type, boolean turnBased, Bundle autoMatchCriteria,
			boolean initiated, boolean isRanked) {
		Log.d(TAG, "Invitee count: " + invitees.size());

		StringBuilder stringBuilder = new StringBuilder();
		for (String each : invitees) {
			stringBuilder.append(each);
			stringBuilder.append(",\n");
		}

		// new AlertDialog.Builder(this).setTitle("Invited to play...")
		// .setMessage(stringBuilder.toString()).show();

		if (turnBased) {
			createTurnBasedMatch(invitees, type, autoMatchCriteria, isRanked);
			return;
		}

		createRealtimeBasedMatch(invitees, type, autoMatchCriteria, initiated,
				isRanked);
	}

	private void createTurnBasedMatch(ArrayList<String> invitees,
			GameType type, Bundle autoMatchCriteria, boolean isRanked) {

		setInactive();

		TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
				.addInvitedPlayers(invitees)
				.setAutoMatchCriteria(autoMatchCriteria).build();

		final TurnBasedMatchGameStrategy strategy = new TurnBasedMatchGameStrategy(
				context, type, false, isRanked);

		// Kick the match off
		Games.TurnBasedMultiplayer
				.createMatch(context.getGameHelper().getApiClient(), tbmc)
				.setResultCallback(
						new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {

							@Override
							public void onResult(InitiateMatchResult result) {
								Status status = result.getStatus();
								TurnBasedMatch match = result.getMatch();

								if (status.getStatusCode() != GamesClient.STATUS_OK) {
									Toast.makeText(
											getActivity(),
											"Error starting a match: "
													+ status.getStatusCode(),
											Toast.LENGTH_LONG).show();
									;
									exitStartMenu();
									return;
								}
								exitStartMenu();
								strategy.onTurnBasedMatchInitiated(
										status.getStatusCode(), match);

							}
						});

	}

	private void createRealtimeBasedMatch(final ArrayList<String> invitees,
			GameType type, Bundle autoMatchCriteria, boolean initiated,
			boolean isRanked) {

		RealtimeGameStrategy strategy = new RealtimeGameStrategy(context, type,
				false, initiated, isRanked);

		// create the room
		Log.d(TAG, "Creating room...");
		final RoomConfig.Builder rtmConfigBuilder = RoomConfig
				.builder(strategy);
		rtmConfigBuilder.addPlayersToInvite(invitees);
		rtmConfigBuilder.setMessageReceivedListener(strategy);
		rtmConfigBuilder.setRoomStatusUpdateListener(strategy);
		if (autoMatchCriteria != null) {
			int variant = type.getVariant();
			if (isRanked)
				variant += 1000;
			rtmConfigBuilder.setVariant(variant);
			rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
		}
		setInactive();
		// post delayed so that the progress is shown
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				Games.RealTimeMultiplayer.create(context.getGameHelper()
						.getApiClient(), rtmConfigBuilder.build());
			}

		}, 0);

		Log.d(TAG, "Room created, waiting for it to be ready...");
	}

	// private GamesClient getGamesClient() {
	// return context.getGameHelper().getGamesClient();
	// }

	private void startLocalTwoPlayerGame(GameType type, String blackName,
			String whiteName) {
		PassNPlayGameStrategy strategy = new PassNPlayGameStrategy(context);

		startLocalGame(strategy, type, whiteName, blackName);
	}

	private void startAIGame(GameType type, String whiteName, AILevel aiLevel,
			boolean isRanked) {
		String blackName = getResources().getString(R.string.local_player_name);
		AiGameStrategy strategy = new AiGameStrategy(context, aiLevel, isRanked);

		startLocalGame(strategy, type, whiteName, blackName);
	}

	private void startLocalGame(AbstractLocalStrategy listener, GameType type,
			String whiteName, String blackName) {
		setInactive();
		exitStartMenu();

		listener.startGame(true, blackName, whiteName, type, new ScoreCard(0,
				0, 0));
	}

	public void onSignInSucceeded() {

	}

	private void showAlert(String string) {
		Toast.makeText(getActivity(), string, Toast.LENGTH_LONG).show();
	}
}
