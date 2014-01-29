package com.oakonell.ticstacktoe.ui.menu;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.InvitationBuffer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.OnInvitationsLoadedListener;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.LoadMatchesResponse;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchesLoadedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchBuffer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.oakonell.ticstacktoe.GameListener;
import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.RoomListener;
import com.oakonell.ticstacktoe.Sounds;
import com.oakonell.ticstacktoe.TicStackToe;
import com.oakonell.ticstacktoe.TurnListener;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.solver.MinMaxAI;
import com.oakonell.ticstacktoe.model.solver.RandomAI;
import com.oakonell.ticstacktoe.settings.SettingsActivity;
import com.oakonell.ticstacktoe.ui.game.GameFragment;
import com.oakonell.ticstacktoe.ui.game.HumanStrategy;
import com.oakonell.ticstacktoe.ui.menu.NewAIGameDialog.LocalAIGameModeListener;
import com.oakonell.ticstacktoe.ui.menu.NewLocalGameDialog.LocalGameModeListener;
import com.oakonell.ticstacktoe.ui.menu.OnlineGameModeDialog.OnlineGameModeListener;
import com.oakonell.ticstacktoe.utils.DevelopmentUtil.Info;
import com.oakonell.utils.StringUtils;

public class MenuFragment extends SherlockFragment {
	public static final int BOARD_SIZE = 3;

	private String TAG = MenuFragment.class.getName();

	private View signInView;
	private View signOutView;
	private ImageView invitesButton;
	private TextView numInvitesTextView;
	private ProgressBar loading_num_invites;

	private ProgressBar waiting;
	Boolean useTurnBased;
	GameType onlineType = null;

	@Override
	public void onActivityResult(int request, int response, Intent data) {
		waiting.setVisibility(View.INVISIBLE);
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
				waiting.setVisibility(View.VISIBLE);
				getMainActivity().getRoomListener().backFromWaitingRoom();
			} else if (response == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
				// player actively indicated that they want to leave the room
				leaveRoom();
			} else if (response == Activity.RESULT_CANCELED) {
				/*
				 * Dialog was cancelled (user pressed back key, for instance).
				 * In our game, this means leaving the room too. In more
				 * elaborate games,this could mean something else (like
				 * minimizing the waiting room UI but continue in the handshake
				 * process).
				 */
				leaveRoom();
			}
			break;
		case MainActivity.RC_INVITATION_INBOX:
			refreshInvites(false);
			if (response != Activity.RESULT_OK) {
				Log.i(TAG, "Returned from invitation- Canceled");
				return;
			}
			Invitation inv = data.getExtras().getParcelable(
					GamesClient.EXTRA_INVITATION);

			int invitationType = inv.getInvitationType();
			if (invitationType == Invitation.INVITATION_TYPE_REAL_TIME) {
				// accept realtime invitation
				acceptInviteToRoom(inv.getInvitationId());

			} else {
				// accept turn based match
				acceptTurnBasedInvitation(inv);
			}
			break;
		case MainActivity.RC_LOOK_AT_MATCHES:
			// Returning from the 'Select Match' dialog

			if (response != Activity.RESULT_OK) {
				// user canceled
				return;
			}

			TurnBasedMatch match = data
					.getParcelableExtra(GamesClient.EXTRA_TURN_BASED_MATCH);

			if (match != null) {
				updateMatch(match);
			}

			Log.d(TAG, "Match = " + match);

			break;

		}
		// super.onActivityResult(request, response, data);
	}

	void leaveRoom() {
		Log.d(TAG, "Leaving room.");
		GameListener roomListener = getMainActivity().getRoomListener();
		if (roomListener != null) {
			roomListener.leaveRoom();
			getMainActivity().setRoomListener(null);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_menu, container,
				false);
		setHasOptionsMenu(true);

		signInView = view.findViewById(R.id.sign_in_bar);
		signOutView = view.findViewById(R.id.sign_out_bar);
		invitesButton = (ImageView) view.findViewById(R.id.invites);
		numInvitesTextView = (TextView) view.findViewById(R.id.num_invites);
		loading_num_invites = (ProgressBar) view
				.findViewById(R.id.loading_num_invites);
		waiting = (ProgressBar) view.findViewById(R.id.waiting);

		ImageView newGameOnSameDevice = (ImageView) view
				.findViewById(R.id.new_game_same_device);
		newGameOnSameDevice.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				selectLocalGame();
			}

		});

		ImageView viewAchievements = (ImageView) view
				.findViewById(R.id.view_achievements);
		viewAchievements.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getMainActivity().isSignedIn()) {
					waiting.setVisibility(View.VISIBLE);
					startActivityForResult(getMainActivity().getGamesClient()
							.getAchievementsIntent(), MainActivity.RC_UNUSED);
				} else {
					// TODO display pending achievements
					getMainActivity().showAlert(
							getString(R.string.achievements_not_available));
				}
			}
		});

		ImageView viewLeaderboards = (ImageView) view
				.findViewById(R.id.view_leaderboards);
		viewLeaderboards.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getMainActivity().isSignedIn()) {
					waiting.setVisibility(View.VISIBLE);
					startActivityForResult(getMainActivity().getGamesClient()
							.getAllLeaderboardsIntent(), MainActivity.RC_UNUSED);
				} else {
					// TODO display pending leaderboard
					getMainActivity().showAlert(
							getString(R.string.achievements_not_available));
				}
			}
		});

		SignInButton signInButton = (SignInButton) view
				.findViewById(R.id.sign_in_button);
		signInButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getMainActivity().beginUserInitiatedSignIn();
			}
		});

		Button signOutButton = (Button) view.findViewById(R.id.sign_out_button);
		signOutButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getMainActivity().signOut();

				// show login button
				view.findViewById(R.id.sign_in_bar).setVisibility(View.VISIBLE);
				// Sign-in failed, so show sign-in button on main menu
				view.findViewById(R.id.sign_out_bar).setVisibility(
						View.INVISIBLE);
			}
		});

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

		invitesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getMainActivity().isSignedIn()) {
					Intent intent = getMainActivity().getGamesClient()
							.getInvitationInboxIntent();
					waiting.setVisibility(View.VISIBLE);
					startActivityForResult(intent,
							MainActivity.RC_INVITATION_INBOX);
				} else {
					getMainActivity().showAlert(
							getResources().getString(
									R.string.sign_in_to_view_invites));
				}
			}
		});

		ImageView ai = (ImageView) view.findViewById(R.id.new_game_vs_ai);
		ai.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				selectAIGame();
			}
		});

		view.findViewById(R.id.pending_games).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (getMainActivity().getGamesClient().isConnected()) {
							Intent intent = getMainActivity().getGamesClient()
									.getMatchInboxIntent();
							startActivityForResult(intent,
									MainActivity.RC_LOOK_AT_MATCHES);
						} else if (getMainActivity().getGamesClient()
								.isConnecting()) {
							getMainActivity().showAlert(
									"Connecting... please try again.");
						} else {
							getMainActivity().showAlert(
									"Please login to see pending matches");
						}

					}
				});

		if (getMainActivity().isSignedIn()) {
			showLogout();
		} else {
			showLogin();
		}

		ListView myTurns = (ListView) view.findViewById(R.id.my_turn_games);
		ListView theirTurns = (ListView) view
				.findViewById(R.id.their_turn_games);
		ListView completeMatches = (ListView) view
				.findViewById(R.id.completed_games);

		return view;
	}

	private void selectAIGame() {
		// prompt for AI level and game mode
		// then start the game activity
		NewAIGameDialog dialog = new NewAIGameDialog();
		dialog.initialize(new LocalAIGameModeListener() {
			@Override
			public void chosenMode(GameType type, String aiName, int level) {
				startAIGame(type, aiName, level);
			}
		});
		dialog.show(getFragmentManager(), "aidialog");
	}

	private void selectLocalGame() {
		// prompt for player names, and game type
		// then start the game activity
		NewLocalGameDialog dialog = new NewLocalGameDialog();
		dialog.initialize(new LocalGameModeListener() {
			@Override
			public void chosenMode(GameType type, String xName, String oName) {
				startLocalTwoPlayerGame(type, xName, oName);
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
		int variant = type.getVariant();

		TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
				.setAutoMatchCriteria(autoMatchCriteria).build();

		// TODO
		TurnListener listener = new TurnListener(getMainActivity(),
				getMainActivity().getGameHelper(), type, true, true);
		getMainActivity().setRoomListener(listener);

		// Kick the match off
		getMainActivity().getGamesClient().createTurnBasedMatch(listener, tbmc);

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
		waiting.setVisibility(View.VISIBLE);
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
				MenuFragment.this.onlineType = type;
				MenuFragment.this.useTurnBased = useTurnBased;
				Intent intent = getMainActivity().getGamesClient()
						.getSelectPlayersIntent(1, 1);
				waiting.setVisibility(View.VISIBLE);
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
			createTurnBasedMatch(invitees, type, autoMatchCriteria, initiated);
			return;
		}

		createRealtimeBasedMatch(invitees, type, autoMatchCriteria, initiated);
	}

	private void createTurnBasedMatch(ArrayList<String> invitees,
			GameType type, Bundle autoMatchCriteria, boolean initiated) {

		TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
				.addInvitedPlayers(invitees)
				.setAutoMatchCriteria(autoMatchCriteria).build();

		// TODO
		TurnListener listener = new TurnListener(getMainActivity(),
				getMainActivity().getGameHelper(), type, false, initiated);
		getMainActivity().setRoomListener(listener);

		// Kick the match off
		getMainActivity().getGamesClient().createTurnBasedMatch(listener, tbmc);

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
		waiting.setVisibility(View.VISIBLE);
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
		gameFragment.startGame(game, score);

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

		gameFragment.startGame(game, score);

		FragmentManager manager = getActivity().getSupportFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		transaction.replace(R.id.main_frame, gameFragment,
				MainActivity.FRAG_TAG_GAME);
		transaction.addToBackStack(null);
		transaction.commit();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			// create special intent
			Intent prefIntent = new Intent(getActivity(),
					SettingsActivity.class);

			GameHelper helper = getMainActivity().getGameHelper();
			Info info = null;
			TicStackToe app = (TicStackToe) getActivity().getApplication();
			if (helper.isSignedIn()) {
				info = new Info(helper);
			}
			app.setDevelopInfo(info);

			getActivity().startActivity(prefIntent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void onSignInFailed() {
		// notify user with dialog?
		showLogin();

		// mMainMenuFragment.setGreeting(getString(R.string.signed_out_greeting));
		// mMainMenuFragment.setShowSignInButton(true);
		// mWinFragment.setShowSignInButton(true);
	}

	private void showLogin() {
		// show login button
		signInView.setVisibility(View.VISIBLE);
		// Sign-in failed, so show sign-in button on main menu
		signOutView.setVisibility(View.INVISIBLE);
	}

	public void onSignInSucceeded() {
		showLogout();

		// install invitation listener so we get notified if we receive an
		// invitation to play
		// a game.
		registerInviteListener();

		refreshInvites(true);

		// TODO load matches and display on menu screen
		getMainActivity().getGamesClient().loadTurnBasedMatches(
				new OnTurnBasedMatchesLoadedListener() {

					@Override
					public void onTurnBasedMatchesLoaded(int status,
							LoadMatchesResponse response) {
						if (status != GamesClient.STATUS_OK) {
							// TODO report an error in some way, retry
							return;
						}
						InvitationBuffer invitations = response
								.getInvitations();
						TurnBasedMatchBuffer myTurnMatches = response
								.getMyTurnMatches();

						TurnBasedMatchBuffer theirTurnMatches = response
								.getTheirTurnMatches();
						TurnBasedMatchBuffer completedMatches = response
								.getCompletedMatches();

						int count = myTurnMatches.getCount();
						for (int i = 0; i < count; i++) {
							TurnBasedMatch match = myTurnMatches.get(i);
							match.getLastUpdatedTimestamp();
							match.getParticipantIds();
						}
					}
				},
				//
				TurnBasedMatch.MATCH_TURN_STATUS_INVITED,
				TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN,
				TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN,
				TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE);

		TurnBasedMatch aMatch = getMainActivity().getGameHelper()
				.getTurnBasedMatch();
		if (aMatch != null) {
			// GameHelper will cache any connection hint it gets. In this case,
			// it can cache a TurnBasedMatch that it got from choosing a
			// turn-based
			// game notification. If that's the case, you should go straight
			// into
			// the game.
			updateMatch(aMatch);
			return;
		}
	}

	private void registerInviteListener() {
		getMainActivity().getGamesClient().registerInvitationListener(
				new OnInvitationReceivedListener() {
					@Override
					public void onInvitationReceived(Invitation invite) {
						getMainActivity().playSound(Sounds.INVITE_RECEIVED);
						refreshInvites(true);
						Toast.makeText(
								getActivity(),
								getResources().getString(
										R.string.received_invite_from,
										invite.getParticipants().get(0)
												.getDisplayName()),
								Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onInvitationRemoved(String invitationId) {
						refreshInvites(true);
						// TODO display invite rescinded?
					}
				});
	}

	public void signOut() {

	}

	private void showLogout() {
		// disable/hide login button
		signInView.setVisibility(View.INVISIBLE);
		// show sign out button
		signOutView.setVisibility(View.VISIBLE);
		TextView signedInAsText = (TextView) getActivity().findViewById(
				R.id.signed_in_as_text);
		if (signedInAsText == null)
			return;
		signedInAsText.setText(getResources().getString(
				R.string.you_are_signed_in_as,
				getMainActivity().getGamesClient().getCurrentAccountName()));
	}

	private void refreshInvites(final boolean shouldFlashNumber) {
		loading_num_invites.setVisibility(View.VISIBLE);
		getMainActivity().getGamesClient().loadInvitations(
				new OnInvitationsLoadedListener() {
					@Override
					public void onInvitationsLoaded(int statusCode,
							InvitationBuffer buffer) {
						loading_num_invites.setVisibility(View.INVISIBLE);
						if (statusCode == GamesClient.STATUS_OK) {
							// update the online invites button with the count
							int count = buffer.getCount();
							if (count == 0) {
								invitesButton
										.setImageResource(R.drawable.no_invites_icon_15776);
								numInvitesTextView.setText("");
							} else {
								invitesButton
										.setImageResource(R.drawable.invites_icon_15777);
								numInvitesTextView.setText("" + count);
								if (shouldFlashNumber) {
									StringUtils
											.applyFlashEnlargeAnimation(numInvitesTextView);
								}
							}
						} else if (statusCode == GamesClient.STATUS_NETWORK_ERROR_STALE_DATA) {

						} else if (statusCode == GamesClient.STATUS_CLIENT_RECONNECT_REQUIRED) {

						} else if (statusCode == GamesClient.STATUS_INTERNAL_ERROR) {

						}
					}
				});
	}

	// Accept the given invitation.
	public void acceptInviteToRoom(String invId) {
		RoomListener roomListener = new RoomListener(getMainActivity(),
				getMainActivity().getGameHelper(), null, false, false);
		getMainActivity().setRoomListener(roomListener);
		// accept the invitation
		Log.d(TAG, "Accepting invitation: " + invId);
		RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(roomListener);
		roomConfigBuilder.setInvitationIdToAccept(invId)
				.setMessageReceivedListener(roomListener)
				.setRoomStatusUpdateListener(roomListener);
		waiting.setVisibility(View.VISIBLE);
		getMainActivity().getGamesClient().joinRoom(roomConfigBuilder.build());
	}

	public MainActivity getMainActivity() {
		return (MainActivity) super.getActivity();
	}

	public void setActive() {
		waiting.setVisibility(View.INVISIBLE);
	}

	@Override
	public void onPause() {
		if (getMainActivity().isSignedIn()) {
			getMainActivity().getGamesClient().registerInvitationListener(null);
		}
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (getMainActivity().isSignedIn()) {
			registerInviteListener();
			showLogout();
		} else {
			showLogin();
		}
	}

	private void acceptTurnBasedInvitation(Invitation inv) {
		TurnListener listener = new TurnListener(getMainActivity(),
				getMainActivity().getGameHelper(), null, false, false);

		getMainActivity().getGamesClient().acceptTurnBasedInvitation(listener,
				inv.getInvitationId());
		getMainActivity().setRoomListener(listener);

		listener.showGame();

	}

	// This is the main function that gets called when players choose a match
	// from the inbox, or else create a match and want to start it.
	public void updateMatch(TurnBasedMatch match) {
		// if sign in just succeeded, but another game is in progress, don't
		// interupt
		// / may need to check if gameFragment exists?
		if (getMainActivity().getRoomListener() != null)
			return;

		int status = match.getStatus();
		int turnStatus = match.getTurnStatus();

		switch (status) {
		case TurnBasedMatch.MATCH_STATUS_CANCELED:
			getMainActivity().getGameHelper().showAlert("Canceled!",
					"This game was canceled!");
			return;
		case TurnBasedMatch.MATCH_STATUS_EXPIRED:
			getMainActivity().getGameHelper().showAlert("Expired!",
					"This game is expired.  So sad!");
			return;
		case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
			getMainActivity().getGameHelper().showAlert(
					"Waiting for auto-match...",
					"We're still waiting for an automatch partner.");
			return;
		case TurnBasedMatch.MATCH_STATUS_COMPLETE:
			if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE) {
				getMainActivity()
						.getGameHelper()
						.showAlert(
								"Complete!",
								"This game is over; someone finished it, and so did you!  There is nothing to be done.");
				break;
			}

			// Note that in this state, you must still call "Finish" yourself,
			// so we allow this to continue.
			getMainActivity()
					.getGameHelper()
					.showAlert("Complete!",
							"This game is over; someone finished it!  You can only finish it now.");
		}

		// OK, it's active. Check on turn status.
		switch (turnStatus) {
		case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN: {
			// launch game
			TurnListener listener = new TurnListener(getMainActivity(),
					getMainActivity().getGameHelper(), match);
			getMainActivity().setRoomListener(listener);

			listener.showGame();

			return;
		}
		case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN: {
			// Should return results.
			TurnListener listener = new TurnListener(getMainActivity(),
					getMainActivity().getGameHelper(), match);
			getMainActivity().setRoomListener(listener);

			listener.showGame();
			break;
		}
		case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
			getMainActivity().getGameHelper().showAlert("Good inititative!",
					"Still waiting for invitations.\n\nBe patient!");
		}

	}

}
