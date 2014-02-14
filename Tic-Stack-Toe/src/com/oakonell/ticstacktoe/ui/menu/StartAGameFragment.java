package com.oakonell.ticstacktoe.ui.menu;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchInitiatedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.RoomListener;
import com.oakonell.ticstacktoe.TurnListener;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.solver.MinMaxAI;
import com.oakonell.ticstacktoe.model.solver.RandomAI;
import com.oakonell.ticstacktoe.ui.game.GameFragment;
import com.oakonell.ticstacktoe.ui.game.HumanStrategy;
import com.oakonell.ticstacktoe.ui.menu.NewAIGameDialog.LocalAIGameModeListener;
import com.oakonell.ticstacktoe.ui.menu.NewLocalGameDialog.LocalGameModeListener;
import com.oakonell.ticstacktoe.ui.menu.OnlineGameModeDialog.OnlineGameModeListener;

public class StartAGameFragment extends SherlockFragment {
	private static final String TAG = "StartAGameFragment";

	Boolean useTurnBased;
	GameType onlineType = null;

	private ProgressBar waiting;
	GamesClient client;

	public void initialize(GamesClient client) {
		this.client = client;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_menu_start,
				container, false);
		setHasOptionsMenu(true);
		waiting = (ProgressBar) view.findViewById(R.id.waiting);
		setActive();
		final ActionBar ab = getMainActivity().getSupportActionBar();
		ab.setTitle("Start a Game");

		// Listen for changes in the back stack
		getMainActivity().getSupportFragmentManager()
				.addOnBackStackChangedListener(
						new OnBackStackChangedListener() {
							@Override
							public void onBackStackChanged() {
								configureDisplayHomeUp();
							}
						});
		// Handle when activity is recreated like on orientation Change
		configureDisplayHomeUp();

		ImageView newGameOnSameDevice = (ImageView) view
				.findViewById(R.id.new_game_same_device);
		newGameOnSameDevice.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				selectLocalGame();
			}
		});

		ImageView ai = (ImageView) view.findViewById(R.id.new_game_vs_ai);
		ai.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				selectAIGame();
			}
		});

		// network games

		ImageView quick = (ImageView) view.findViewById(R.id.new_quick_play);
		quick.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getMainActivity().isSignedIn()) {
					selectQuickMode();
				} else {
					getMainActivity().showAlert(
							getResources().getString(
									R.string.sign_in_to_play_network_game));
				}
			}
		});

		ImageView inviteFriend = (ImageView) view
				.findViewById(R.id.new_game_live);
		inviteFriend.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getMainActivity().isSignedIn()) {
					selectOnlineGameMode();
				} else {
					getMainActivity().showAlert(
							getResources().getString(
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
		if (getMainActivity() == null)
			return;
		getMainActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	public void setActive() {
		getMainActivity().getMenuFragment().setActive();
		waiting.setVisibility(View.INVISIBLE);
		Log.i(TAG, "Setting active");
	}

	public void setInactive() {
		getMainActivity().getMenuFragment().setInactive();
		waiting.setVisibility(View.VISIBLE);
		Log.i(TAG, "Setting inactive");
	}

	@Override
	public void onActivityResult(int request, int response, Intent data) {
		setActive();
		switch (request) {
		case MainActivity.RC_SELECT_PLAYERS: {
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

				createOnlineRoom(invitees, type, turnBased, autoMatchCriteria,
						true);
			} else {
				Log.i(TAG, "Select players canceled");
			}
		}
			break;

		case MainActivity.RC_WAITING_ROOM:
			// ignore result if we dismissed the waiting room from code:
			// if (mWaitRoomDismissedFromCode)
			// break;

			// we got the result from the "waiting room" UI.
			if (response == Activity.RESULT_OK) {
				setInactive();
				getMainActivity().getRoomListener().backFromWaitingRoom();
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
		// case MainActivity.RC_INVITATION_INBOX: {
		// Log.i(TAG, "RC_INVITATION_INBOX start");
		// refreshInvites(false);
		// if (response != Activity.RESULT_OK) {
		// setActive();
		// // getMainActivity().getGameHelper().showAlert(
		// // "Bad response on return from accept invite (request="
		// // + request + " ): " + response + ", intent: "
		// // + data);
		// Log.i(TAG, "Returned from invitation- Canceled/Error "
		// + response);
		// return;
		// }
		// Log.i(TAG, "RC_INVITATION_INBOX got here1");
		// Invitation inv = data.getExtras().getParcelable(
		// GamesClient.EXTRA_INVITATION);
		// if (inv == null) {
		// Log.i(TAG,
		// "RC_INVITATION_INBOX no invite, assume a turn based!");
		// TurnBasedMatch match = data.getExtras().getParcelable(
		// GamesClient.EXTRA_TURN_BASED_MATCH);
		// if (match == null) {
		// getMainActivity()
		// .getGameHelper()
		// .showAlert(
		// "No invite NOR match. What kind of invite was accepted?");
		// return;
		// }
		// updateMatch(match);
		// break;
		// }
		// Log.i(TAG, "RC_INVITATION_INBOX got here2");
		// int invitationType = inv.getInvitationType();
		// if (invitationType == Invitation.INVITATION_TYPE_REAL_TIME) {
		// Log.i(TAG, "RC_INVITATION_INBOX got here3");
		// // accept realtime invitation
		// acceptInviteToRoom(inv.getInvitationId());
		//
		// } else {
		// Log.i(TAG, "RC_INVITATION_INBOX got here4");
		// //
		// getMainActivity().getGameHelper().showAlert("No invite NOR match. What kind of invite was accepted?");
		// // accept turn based match
		// acceptTurnBasedInvitation(inv.getInvitationId());
		// }
		// Log.i(TAG, "RC_INVITATION_INBOX got here5");
		// }
		// break;

		// case MainActivity.RC_LOOK_AT_MATCHES:
		// // Returning from the 'Select Match' dialog
		//
		// if (response != Activity.RESULT_OK) {
		// // user canceled
		// return;
		// }
		//
		// TurnBasedMatch match = data
		// .getParcelableExtra(GamesClient.EXTRA_TURN_BASED_MATCH);
		//
		// if (match != null) {
		// updateMatch(match);
		// }
		//
		// Log.d(TAG, "Match = " + match);
		//
		// break;

		}
		// super.onActivityResult(request, response, data);
	}

	private void exitStartMenu() {
		getActivity().getSupportFragmentManager().popBackStack();
		final ActionBar ab = getMainActivity().getSupportActionBar();
		ab.setTitle("Tic-Stack-Toe");
	}

	private void selectAIGame() {
		// prompt for AI level and game mode
		// then start the game activity
		setInactive();
		NewAIGameDialog dialog = new NewAIGameDialog();
		dialog.initialize(new LocalAIGameModeListener() {
			@Override
			public void chosenMode(GameType type, String aiName, int level) {
				startAIGame(type, aiName, level);
			}

			@Override
			public void cancel() {
				setActive();
			}
		});
		dialog.show(getFragmentManager(), "aidialog");
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
			public void chosenMode(GameType type, boolean useTurnBased) {
				final int MIN_OPPONENTS = 1, MAX_OPPONENTS = 1;
				Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
						MIN_OPPONENTS, MAX_OPPONENTS, 0);

				if (useTurnBased) {
					createTurnBasedQuickMatch(type, autoMatchCriteria);
					return;
				}

				createRealtimeBasedQuickMatch(type, autoMatchCriteria);

			}
		});
		dialog.show(getSherlockActivity().getSupportFragmentManager(),
				"gameMode");

	}

	protected void createTurnBasedQuickMatch(GameType type,
			Bundle autoMatchCriteria) {
		setInactive();

		TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
				.setAutoMatchCriteria(autoMatchCriteria).build();

		// TODO
		final TurnListener listener = new TurnListener(getMainActivity(),
				getMainActivity().getGameHelper(), type, true);
		getMainActivity().setRoomListener(listener);

		// Kick the match off
		getMainActivity().getGamesClient().createTurnBasedMatch(
				new OnTurnBasedMatchInitiatedListener() {
					@Override
					public void onTurnBasedMatchInitiated(int status,
							TurnBasedMatch match) {
						if (status != GamesClient.STATUS_OK) {
							getMainActivity().getGameHelper().showAlert(
									"Error starting match: " + status);
							exitStartMenu();
							return;
						}
						exitStartMenu();
						listener.onTurnBasedMatchInitiated(status, match);
					}
				}, tbmc);

	}

	protected void createRealtimeBasedQuickMatch(GameType type,
			Bundle autoMatchCriteria) {
		int variant = type.getVariant();

		RoomListener roomListener = new RoomListener(getMainActivity(),
				getMainActivity().getGameHelper(), type, true, true);
		getMainActivity().setRoomListener(roomListener);
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
				getMainActivity().getGamesClient().createRoom(
						rtmConfigBuilder.build());
			}
		}, 0);

	}

	private void selectOnlineGameMode() {
		// first choose Game mode
		OnlineGameModeDialog dialog = new OnlineGameModeDialog();
		dialog.initialize(false, new OnlineGameModeListener() {
			@Override
			public void chosenMode(GameType type, boolean useTurnBased) {
				StartAGameFragment.this.onlineType = type;
				StartAGameFragment.this.useTurnBased = useTurnBased;
				Intent intent = getMainActivity().getGamesClient()
						.getSelectPlayersIntent(1, 1);
				setInactive();
				startActivityForResult(intent, MainActivity.RC_SELECT_PLAYERS);
			}
		});
		dialog.show(getSherlockActivity().getSupportFragmentManager(),
				"gameMode");
	}

	private void createOnlineRoom(final ArrayList<String> invitees,
			GameType type, boolean turnBased, Bundle autoMatchCriteria,
			boolean initiated) {
		Log.d(TAG, "Invitee count: " + invitees.size());

		StringBuilder stringBuilder = new StringBuilder();
		for (String each : invitees) {
			stringBuilder.append(each);
			stringBuilder.append(",\n");
		}

		// new AlertDialog.Builder(this).setTitle("Invited to play...")
		// .setMessage(stringBuilder.toString()).show();

		if (turnBased) {
			createTurnBasedMatch(invitees, type, autoMatchCriteria);
			return;
		}

		createRealtimeBasedMatch(invitees, type, autoMatchCriteria, initiated);
	}

	private void createTurnBasedMatch(ArrayList<String> invitees,
			GameType type, Bundle autoMatchCriteria) {

		setInactive();

		TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
				.addInvitedPlayers(invitees)
				.setAutoMatchCriteria(autoMatchCriteria).build();

		final TurnListener listener = new TurnListener(getMainActivity(),
				getMainActivity().getGameHelper(), type, false);
		getMainActivity().setRoomListener(listener);

		// Kick the match off
		getMainActivity().getGamesClient().createTurnBasedMatch(
				new OnTurnBasedMatchInitiatedListener() {
					@Override
					public void onTurnBasedMatchInitiated(int status,
							TurnBasedMatch match) {
						if (status == GamesClient.STATUS_OK) {
							getMainActivity().getGameHelper().showAlert(
									"Error starting a match: " + status);
							exitStartMenu();
							return;
						}
						exitStartMenu();
						listener.onTurnBasedMatchInitiated(status, match);
					}
				}, tbmc);

	}

	private void createRealtimeBasedMatch(final ArrayList<String> invitees,
			GameType type, Bundle autoMatchCriteria, boolean initiated) {

		RoomListener roomListener = new RoomListener(getMainActivity(),
				getMainActivity().getGameHelper(), type, false, initiated);
		getMainActivity().setRoomListener(roomListener);
		// create the room
		Log.d(TAG, "Creating room...");
		final RoomConfig.Builder rtmConfigBuilder = RoomConfig
				.builder(roomListener);
		rtmConfigBuilder.addPlayersToInvite(invitees);
		rtmConfigBuilder.setMessageReceivedListener(roomListener);
		rtmConfigBuilder.setRoomStatusUpdateListener(roomListener);
		if (autoMatchCriteria != null) {
			rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
		}
		setInactive();
		// post delayed so that the progess is shown
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				getMainActivity().getGamesClient().createRoom(
						rtmConfigBuilder.build());
			}
		}, 0);

		Log.d(TAG, "Room created, waiting for it to be ready...");
	}

	private void startLocalTwoPlayerGame(GameType type, String blackName,
			String whiteName) {
		GameFragment gameFragment = new GameFragment();

		Player blackPlayer = HumanStrategy.createPlayer(blackName, true);
		Player whitePlayer = HumanStrategy.createPlayer(whiteName, false);

		Tracker myTracker = EasyTracker.getTracker();
		myTracker.sendEvent(getString(R.string.an_start_game_cat),
				getString(R.string.an_start_pass_n_play_game_action),
				type + "", 0L);

		Game game = new Game(type, GameMode.PASS_N_PLAY, blackPlayer,
				whitePlayer, blackPlayer);
		ScoreCard score = new ScoreCard(0, 0, 0);
		gameFragment.startGame(game, score, null, true);

		exitStartMenu();
		FragmentManager manager = getActivity().getSupportFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		transaction.replace(R.id.main_frame, gameFragment,
				MainActivity.FRAG_TAG_GAME);
		transaction.addToBackStack(null);
		transaction.commit();
	}

	private void startAIGame(GameType type, String whiteName, int aiDepth) {
		GameFragment gameFragment = new GameFragment();

		ScoreCard score = new ScoreCard(0, 0, 0);
		String blackName = getResources().getString(R.string.local_player_name);

		Player whitePlayer;
		if (aiDepth < 0) {
			whitePlayer = RandomAI.createPlayer(whiteName, false);
		} else {
			whitePlayer = MinMaxAI.createPlayer(whiteName, false, aiDepth);
		}

		Player blackPlayer = HumanStrategy.createPlayer(blackName, true);

		Tracker myTracker = EasyTracker.getTracker();
		myTracker.sendEvent(getString(R.string.an_start_game_cat),
				getString(R.string.an_start_ai_game_action), type + "", 0L);
		Game game = new Game(type, GameMode.AI, blackPlayer, whitePlayer,
				blackPlayer);

		gameFragment.startGame(game, score, null, true);

		exitStartMenu();
		FragmentManager manager = getActivity().getSupportFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		transaction.replace(R.id.main_frame, gameFragment,
				MainActivity.FRAG_TAG_GAME);
		transaction.addToBackStack(null);
		transaction.commit();
	}

	public MainActivity getMainActivity() {
		return (MainActivity) super.getActivity();
	}

	public void onSignInSucceeded() {

	}

}
