package com.oakonell.ticstacktoe.ui.menu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
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
import com.commonsware.cwac.merge.MergeAdapter;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.InvitationBuffer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.OnInvitationsLoadedListener;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.LoadMatchesResponse;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchInitiatedListener;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchLoadedListener;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdateReceivedListener;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchesLoadedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchBuffer;
import com.oakonell.ticstacktoe.GameListener;
import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.RoomListener;
import com.oakonell.ticstacktoe.Sounds;
import com.oakonell.ticstacktoe.TicStackToe;
import com.oakonell.ticstacktoe.TurnListener;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.settings.SettingsActivity;
import com.oakonell.ticstacktoe.utils.DevelopmentUtil.Info;

public class MenuFragment extends SherlockFragment implements MatchShower,
		OnTurnBasedMatchUpdateReceivedListener, OnInvitationReceivedListener {

	private String TAG = MenuFragment.class.getName();

	private View signInView;
	private View signOutView;

	private ProgressBar waiting;

	private MatchAdapter completedMatchesAdapter;
	private MatchAdapter theirTurnsAdapter;
	private MatchAdapter myTurnsAdapter;

	private List<MatchInfo> myTurns = new ArrayList<MatchInfo>();
	private List<MatchInfo> theirTurns = new ArrayList<MatchInfo>();
	private List<MatchInfo> completedMatches = new ArrayList<MatchInfo>();

	private PullToRefreshLayout mPullToRefreshLayout;

	@Override
	public void onActivityResult(int request, int response, Intent data) {
		setActive();
		switch (request) {

		case MainActivity.RC_INVITATION_INBOX: {
			Log.i(TAG, "RC_INVITATION_INBOX start");
			refreshInvites(false);
			if (response != Activity.RESULT_OK) {
				setActive();
				// getMainActivity().getGameHelper().showAlert(
				// "Bad response on return from accept invite (request="
				// + request + " ): " + response + ", intent: "
				// + data);
				Log.i(TAG, "Returned from invitation- Canceled/Error "
						+ response);
				return;
			}
			Log.i(TAG, "RC_INVITATION_INBOX got here1");
			Invitation inv = data.getExtras().getParcelable(
					GamesClient.EXTRA_INVITATION);
			if (inv == null) {
				Log.i(TAG,
						"RC_INVITATION_INBOX no invite, assume a turn based!");
				TurnBasedMatch match = data.getExtras().getParcelable(
						GamesClient.EXTRA_TURN_BASED_MATCH);
				if (match == null) {
					getMainActivity()
							.getGameHelper()
							.showAlert(
									"No invite NOR match. What kind of invite was accepted?");
					return;
				}
				updateMatch(match);
				break;
			}
			Log.i(TAG, "RC_INVITATION_INBOX got here2");
			int invitationType = inv.getInvitationType();
			if (invitationType == Invitation.INVITATION_TYPE_REAL_TIME) {
				Log.i(TAG, "RC_INVITATION_INBOX got here3");
				// accept realtime invitation
				acceptInviteToRoom(inv.getInvitationId());

			} else {
				Log.i(TAG, "RC_INVITATION_INBOX got here4");
				// getMainActivity().getGameHelper().showAlert("No invite NOR match. What kind of invite was accepted?");
				// accept turn based match
				acceptTurnBasedInvitation(inv.getInvitationId());
			}
			Log.i(TAG, "RC_INVITATION_INBOX got here5");
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
			registerMatchListeners();
		}
		refreshMatches();
	}

	private void configureDisplayHomeUp() {
		if (getMainActivity() == null)
			return;
		getMainActivity().getSupportActionBar()
				.setDisplayHomeAsUpEnabled(false);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_menu, container,
				false);
		setHasOptionsMenu(true);

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

		signInView = view.findViewById(R.id.sign_in_bar);
		signOutView = view.findViewById(R.id.sign_out_bar);
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
		waiting = (ProgressBar) view.findViewById(R.id.waiting);

		View main = configureMainView(inflater, view);

		if (getMainActivity().isSignedIn()) {
			showLogout();
		} else {
			showLogin();
		}

		mPullToRefreshLayout = (PullToRefreshLayout) view
				.findViewById(R.id.ptr_layout);

		// Now setup the PullToRefreshLayout
		ActionBarPullToRefresh.from(getActivity())
		// Mark All Children as pullable
				.allChildrenArePullable()
				// Set the OnRefreshListener
				.listener(new OnRefreshListener() {
					@Override
					public void onRefreshStarted(View view) {
						refreshMatches();
					}
				})
				// Finally commit the setup to our PullToRefreshLayout
				.setup(mPullToRefreshLayout);

		ListView listView = (ListView) view.findViewById(R.id.list);
		MergeAdapter adapter = new MergeAdapter();

		adapter.addView(main);

		View myTurnHeader = createMatchListHeader(inflater, view, "Your Turn");
		adapter.addView(myTurnHeader);
		myTurnHeader.setVisibility(View.GONE);
		myTurnsAdapter = new MatchAdapter(getActivity(), this, myTurns,
				myTurnHeader);
		adapter.addAdapter(myTurnsAdapter);

		View theirTurnHeader = createMatchListHeader(inflater, view,
				"Their turn");
		adapter.addView(theirTurnHeader);
		theirTurnHeader.setVisibility(View.GONE);
		theirTurnsAdapter = new MatchAdapter(getActivity(), this, theirTurns,
				theirTurnHeader);
		adapter.addAdapter(theirTurnsAdapter);

		View completedHeader = createMatchListHeader(inflater, view,
				"Completed");
		adapter.addView(completedHeader);
		completedHeader.setVisibility(View.GONE);
		completedMatchesAdapter = new MatchAdapter(getActivity(), this,
				completedMatches, completedHeader);
		adapter.addAdapter(completedMatchesAdapter);

		listView.setAdapter(adapter);

		return view;
	}

	private View createMatchListHeader(LayoutInflater inflater, View parent,
			String label) {
		final View view = inflater.inflate(R.layout.fragment_menu_list_header,
				null);

		TextView text = (TextView) view.findViewById(R.id.label);
		text.setText(label);

		return view;
	}

	private View configureMainView(LayoutInflater inflater, final View parent) {
		final View view = inflater.inflate(R.layout.fragment_menu_main, null);

		ImageView newGame = (ImageView) view.findViewById(R.id.new_game);
		newGame.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO start a menu fragment(?) to choose which style of game

				StartAGameFragment fragment = new StartAGameFragment();
				fragment.initialize(getMainActivity().getGamesClient());

				FragmentManager manager = getActivity()
						.getSupportFragmentManager();
				FragmentTransaction transaction = manager.beginTransaction();
				transaction.replace(R.id.main_frame, fragment,
						MainActivity.FRAG_TAG_START_GAME);
				transaction.addToBackStack(null);
				transaction.commit();
			}

		});

		ImageView viewAchievements = (ImageView) view
				.findViewById(R.id.view_achievements);
		viewAchievements.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getMainActivity().isSignedIn()) {
					setInactive();
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
					setInactive();
					startActivityForResult(getMainActivity().getGamesClient()
							.getAllLeaderboardsIntent(), MainActivity.RC_UNUSED);
				} else {
					// TODO display pending leaderboard
					getMainActivity().showAlert(
							getString(R.string.achievements_not_available));
				}
			}
		});

		return view;
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

		registerMatchListeners();

		refreshInvites(true);

		refreshMatches();

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
		getMainActivity().getGamesClient().loadInvitations(
				new OnInvitationsLoadedListener() {
					@Override
					public void onInvitationsLoaded(int statusCode,
							InvitationBuffer buffer) {
						// remove existing invite matches from myTurn list, and
						// add back these
						for (Iterator<MatchInfo> iter = myTurns.iterator(); iter
								.hasNext();) {
							MatchInfo each = iter.next();
							if (each instanceof InviteMatchInfo) {
								iter.remove();
							}
						}
						if (statusCode == GamesClient.STATUS_OK) {
							// update the online invites button with the count
							int count = buffer.getCount();
							if (count != 0) {
								for (int i = 0; i < count; i++) {
									Invitation invite = buffer.get(i);
									InviteMatchInfo matchInfo = new InviteMatchInfo(
											getMainActivity().getGamesClient(),
											invite);
									myTurns.add(matchInfo);
								}
							}
							myTurnsAdapter.notifyDataSetChanged();
							buffer.close();
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
		setActive();
		getMainActivity().getGamesClient().joinRoom(roomConfigBuilder.build());
	}

	public MainActivity getMainActivity() {
		return (MainActivity) super.getActivity();
	}

	public void setActive() {
		waiting.setVisibility(View.INVISIBLE);
		Log.i(TAG, "Setting active");
	}

	public void setInactive() {
		waiting.setVisibility(View.VISIBLE);
		Log.i(TAG, "Setting inactive");
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
			registerMatchListeners();
			showLogout();
		} else {
			showLogin();
		}
	}

	private void registerMatchListeners() {
		getMainActivity().getGamesClient().registerInvitationListener(this);
		getMainActivity().getGamesClient().registerMatchUpdateListener(this);
	}

	public void acceptTurnBasedInvitation(final String inviteId) {
		setInactive();

		getMainActivity().getGamesClient().acceptTurnBasedInvitation(
				new OnTurnBasedMatchInitiatedListener() {
					@Override
					public void onTurnBasedMatchInitiated(int status,
							TurnBasedMatch match) {
						if (status != GamesClient.STATUS_OK) {
							getMainActivity().getGameHelper().showAlert(
									"Error accepting invitation " + inviteId
											+ ": error=" + status);
							return;
						}
						TurnListener listener = new TurnListener(
								getMainActivity(), getMainActivity()
										.getGameHelper(), match);
						getMainActivity().setRoomListener(listener);

						listener.showGame();
					}
				},

				inviteId);

	}

	public void showMatch(String matchId) {
		setInactive();

		getMainActivity().getGamesClient().getTurnBasedMatch(
				new OnTurnBasedMatchLoadedListener() {
					@Override
					public void onTurnBasedMatchLoaded(int status,
							TurnBasedMatch match) {
						if (status != GamesClient.STATUS_OK) {
							getMainActivity().getGameHelper().showAlert(
									"Error loading match");
							setActive();
							return;
						}
						updateMatch(match);
					}
				}, matchId);
	}

	// This is the main function that gets called when players choose a match
	// from the inbox, or else create a match and want to start it.
	public void updateMatch(TurnBasedMatch match) {
		// if sign in just succeeded, but another game is in progress, don't
		// interupt
		// / may need to check if gameFragment exists?
		if (getMainActivity().getRoomListener() != null)
			return;

		setInactive();
		TurnListener listener = new TurnListener(getMainActivity(),
				getMainActivity().getGameHelper(), match);
		getMainActivity().setRoomListener(listener);

		listener.showFromMenu();
	}

	public void refreshMatches() {
		if (mPullToRefreshLayout != null) {
			mPullToRefreshLayout.setRefreshing(true);
		}
		if (!getMainActivity().isSignedIn()) {
			if (!getMainActivity().getGamesClient().isConnecting()
					&& mPullToRefreshLayout != null) {
				mPullToRefreshLayout.setRefreshComplete();
			}
			return;
		}
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
						// put invites into my turns?
						myTurns.clear();
						int max = invitations.getCount();
						for (int i = 0; i < max; i++) {
							Invitation invitation = invitations.get(i);
							myTurns.add(new InviteMatchInfo(getMainActivity()
									.getGamesClient(), invitation));
						}
						invitations.close();

						TurnBasedMatchBuffer myTurnMatches = response
								.getMyTurnMatches();
						populateMatches(myTurnMatches, myTurnsAdapter, myTurns);

						theirTurns.clear();
						TurnBasedMatchBuffer theirTurnMatches = response
								.getTheirTurnMatches();
						populateMatches(theirTurnMatches, theirTurnsAdapter,
								theirTurns);

						completedMatches.clear();
						TurnBasedMatchBuffer completedMatchesBuffer = response
								.getCompletedMatches();
						populateMatches(completedMatchesBuffer,
								completedMatchesAdapter, completedMatches);

						if (mPullToRefreshLayout != null) {
							mPullToRefreshLayout.setRefreshComplete();
						}
					}

					private void populateMatches(
							TurnBasedMatchBuffer matchesBuffer,
							MatchAdapter adapter, List<MatchInfo> matches) {
						int count = matchesBuffer.getCount();
						for (int i = 0; i < count; i++) {
							TurnBasedMatch match = matchesBuffer.get(i);
							MatchInfo info = new TurnBasedMatchInfo(
									getMainActivity(), getMainActivity()
											.getGamesClient(), match);
							matches.add(info);
						}
						matchesBuffer.close();
						adapter.notifyDataSetChanged();
					}
				},
				//
				TurnBasedMatch.MATCH_TURN_STATUS_INVITED,
				TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN,
				TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN,
				TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE);
	}

	@Override
	public void onTurnBasedMatchReceived(TurnBasedMatch match) {
		refreshMatches();
	}

	@Override
	public void onTurnBasedMatchRemoved(String matchId) {
		refreshMatches();
	}

	@Override
	public void onInvitationReceived(Invitation invite) {
		getMainActivity().playSound(Sounds.INVITE_RECEIVED);
		refreshInvites(true);
		Toast.makeText(
				getActivity(),
				getResources().getString(R.string.received_invite_from,
						invite.getParticipants().get(0).getDisplayName()),
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onInvitationRemoved(String invitationId) {
		refreshInvites(true);
	}

}
