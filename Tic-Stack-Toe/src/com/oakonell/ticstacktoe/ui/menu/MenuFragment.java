package com.oakonell.ticstacktoe.ui.menu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.InvitationBuffer;
import com.google.android.gms.games.multiplayer.Invitations;
import com.google.android.gms.games.multiplayer.Invitations.LoadInvitationsResult;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.LoadMatchesResponse;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdateReceivedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchBuffer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer.InitiateMatchResult;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer.LoadMatchResult;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer.LoadMatchesResult;
import com.oakonell.ticstacktoe.GameContext;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.Sounds;
import com.oakonell.ticstacktoe.TicStackToe;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler.LocalMatchesBuffer;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler.OnLocalMatchesLoadListener;
import com.oakonell.ticstacktoe.model.rank.RankStorage;
import com.oakonell.ticstacktoe.model.rank.TypeRankStorage;
import com.oakonell.ticstacktoe.rank.RankHelper.OnRankReceived;
import com.oakonell.ticstacktoe.settings.SettingsActivity;
import com.oakonell.ticstacktoe.ui.local.AbstractLocalStrategy;
import com.oakonell.ticstacktoe.ui.local.LocalMatchInfo;
import com.oakonell.ticstacktoe.ui.local.tutorial.TutorialGameStrategy;
import com.oakonell.ticstacktoe.ui.network.realtime.RealtimeGameStrategy;
import com.oakonell.ticstacktoe.ui.network.turn.InviteMatchInfo;
import com.oakonell.ticstacktoe.ui.network.turn.TurnBasedMatchGameStrategy;
import com.oakonell.ticstacktoe.ui.network.turn.TurnBasedMatchInfo;
import com.oakonell.ticstacktoe.utils.DevelopmentUtil.Info;

public class MenuFragment extends SherlockFragment implements
		OnTurnBasedMatchUpdateReceivedListener, OnInvitationReceivedListener {

	public static final String SAW_TUTORIAL = "saw_tutorial";

	private DatabaseHandler dbHandler;

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

	private GameContext context;

	private View juniorRankView;
	private View normalRankView;
	private View strictRankView;

	private boolean localRefreshed;
	private boolean networkRefreshed;
	private boolean rankRefreshed;

	private MergeAdapter menuAdapter;

	public MenuFragment() {
		// for reference finding
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == GameContext.RC_UNUSED) {
			setActive();
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = (GameContext) activity;
		dbHandler = new DatabaseHandler(getActivity());
	}

	public static MenuFragment createMenuFragment() {
		MenuFragment fragment = new MenuFragment();
		return fragment;
	}

	public DatabaseHandler getDbHandler() {
		return dbHandler;
	}

	public void leaveRoom() {
		if (getView() == null) {
			return;
		}
		Log.d(TAG, "Leaving room.");
		context.setGameStrategy(null);
		registerMatchListeners();
		refreshMatches();
	}

	private void configureDisplayHomeUp() {
		if (getActivity() == null)
			return;
		getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(
				false);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_menu, container,
				false);
		setHasOptionsMenu(true);

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

		signInView = view.findViewById(R.id.sign_in_bar);
		signOutView = view.findViewById(R.id.sign_out_bar);
		SignInButton signInButton = (SignInButton) view
				.findViewById(R.id.sign_in_button);
		signInButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				context.getGameHelper().beginUserInitiatedSignIn();
			}
		});

		Button signOutButton = (Button) view.findViewById(R.id.sign_out_button);
		signOutButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				context.getGameHelper().signOut();
				signOut();
				// show login button
				view.findViewById(R.id.sign_in_bar).setVisibility(View.VISIBLE);
				// Sign-in failed, so show sign-in button on main menu
				view.findViewById(R.id.sign_out_bar).setVisibility(
						View.INVISIBLE);
			}
		});
		waiting = (ProgressBar) view.findViewById(R.id.waiting);

		View main = configureMainView(inflater, view);
		View mainFooter = configureMainViewFooter(inflater, view);

		if (context.getGameHelper().isSignedIn()) {
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
		menuAdapter = new MergeAdapter();

		juniorRankView = inflater.inflate(R.layout.rank_layout, null);
		juniorRankView.setVisibility(View.GONE);
		menuAdapter.addView(juniorRankView);
		menuAdapter.setActive(juniorRankView, false);
		normalRankView = inflater.inflate(R.layout.rank_layout, null);
		normalRankView.setVisibility(View.GONE);
		menuAdapter.addView(normalRankView);
		menuAdapter.setActive(normalRankView, false);
		strictRankView = inflater.inflate(R.layout.rank_layout, null);
		strictRankView.setVisibility(View.GONE);
		menuAdapter.addView(strictRankView);
		menuAdapter.setActive(strictRankView, false);

		menuAdapter.addView(main);

		View myTurnHeader = createMatchListHeader(inflater, view, "Your Turn",
				true);
		View listBottomView = inflater.inflate(
				R.layout.fragment_menu_list_bottom, null);
		menuAdapter.addView(myTurnHeader);
		myTurnsAdapter = new MatchAdapter(getActivity(), this, myTurns,
				menuAdapter, myTurnHeader, listBottomView);
		menuAdapter.addAdapter(myTurnsAdapter);
		menuAdapter.addView(listBottomView);

		View theirTurnHeader = createMatchListHeader(inflater, view,
				"Their turn", false);
		listBottomView = inflater.inflate(R.layout.fragment_menu_list_bottom,
				null);
		menuAdapter.addView(theirTurnHeader);
		theirTurnsAdapter = new MatchAdapter(getActivity(), this, theirTurns,
				menuAdapter, theirTurnHeader, listBottomView);
		menuAdapter.addAdapter(theirTurnsAdapter);
		menuAdapter.addView(listBottomView);

		menuAdapter.addView(mainFooter);

		View completedHeader = createMatchListHeader(inflater, view,
				"Completed", true);
		listBottomView = inflater.inflate(R.layout.fragment_menu_list_bottom,
				null);
		menuAdapter.addView(completedHeader);
		completedMatchesAdapter = new MatchAdapter(getActivity(), this,
				completedMatches, menuAdapter, completedHeader, listBottomView);
		menuAdapter.addAdapter(completedMatchesAdapter);
		menuAdapter.addView(listBottomView);

		listView.setAdapter(menuAdapter);

		return view;
	}

	private View createMatchListHeader(LayoutInflater inflater, View parent,
			String label, boolean left) {
		View view;
		if (left) {
			view = inflater.inflate(R.layout.fragment_menu_list_header_left,
					null);
		} else {
			view = inflater.inflate(R.layout.fragment_menu_list_header_right,
					null);
		}
		TextView text = (TextView) view.findViewById(R.id.label);
		text.setText(label);

		return view;
	}

	private View configureMainView(LayoutInflater inflater, final View parent) {
		final View view = inflater.inflate(R.layout.fragment_menu_main, null);

		View newGame = view.findViewById(R.id.new_game);
		newGame.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				SharedPreferences preferences = PreferenceManager
						.getDefaultSharedPreferences(getActivity());
				boolean sawTutorial = preferences.getBoolean(SAW_TUTORIAL,
						false);
				if (sawTutorial) {
					StartAGameFragment fragment = StartAGameFragment
							.createStartGameFragment();

					FragmentManager manager = getActivity()
							.getSupportFragmentManager();
					FragmentTransaction transaction = manager
							.beginTransaction();
					transaction.replace(R.id.main_frame, fragment,
							GameContext.FRAG_TAG_START_GAME);
					transaction.addToBackStack(null);
					transaction.commit();
					return;
				}
				setInactive();
				TutorialGameStrategy strategy = new TutorialGameStrategy(
						context);
				strategy.startGame();
			}

		});

		return view;
	}

	private View configureMainViewFooter(LayoutInflater inflater,
			final View parent) {
		final View view = inflater.inflate(R.layout.fragment_menu_main_footer,
				null);

		View viewAchievements = view.findViewById(R.id.view_achievements);
		viewAchievements.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (context.getGameHelper().isSignedIn()) {
					setInactive();
					startActivityForResult(Games.Achievements
							.getAchievementsIntent(context.getGameHelper()
									.getApiClient()), GameContext.RC_UNUSED);
				} else {
					// TODO display pending achievements
					showAlert(getString(R.string.achievements_not_available));
				}
			}
		});

		View viewLeaderboards = view.findViewById(R.id.view_leaderboards);
		viewLeaderboards.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (context.getGameHelper().isSignedIn()) {
					setInactive();
					startActivityForResult(Games.Leaderboards
							.getAllLeaderboardsIntent(context.getGameHelper()
									.getApiClient()), GameContext.RC_UNUSED);
				} else {
					// TODO display pending leaderboard
					showAlert(getString(R.string.achievements_not_available));
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

			Info info = null;
			TicStackToe app = (TicStackToe) getActivity().getApplication();
			GameHelper helper = context.getGameHelper();
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
		if (signInView == null)
			return;

		// show login button
		signInView.setVisibility(View.VISIBLE);
		// Sign-in failed, so show sign-in button on main menu
		signOutView.setVisibility(View.INVISIBLE);
	}

	private void showLogout() {
		// disable/hide login button
		if (signInView == null)
			return;

		signInView.setVisibility(View.INVISIBLE);
		// show sign out button
		signOutView.setVisibility(View.VISIBLE);
		TextView signedInAsText = (TextView) getActivity().findViewById(
				R.id.signed_in_as_text);
		if (signedInAsText == null)
			return;
		signedInAsText.setText(getResources().getString(
				R.string.you_are_signed_in_as,
				Games.getCurrentAccountName(context.getGameHelper()
						.getApiClient())));
	}

	public void onSignInSucceeded() {
		if (signInView == null) {
			// if I'm not visible, notihng to do do
			// TODO Hmm... should pull out the intent match launch into the main
			// activity?
			return;
		}
		showLogout();

		registerMatchListeners();
		refreshInvites(true);
		refreshMatches();

		TurnBasedMatch aMatch = context.getGameHelper().getTurnBasedMatch();
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

	protected void updateRankView(View rankView, String typeString,
			TypeRankStorage rank) {
		if (!rank.hasPlayed()) {
			rankView.setVisibility(View.GONE);
			return;
		}
		rankView.setVisibility(View.VISIBLE);
		menuAdapter.setActive(rankView, true);
		TextView label = (TextView) rankView.findViewById(R.id.rank_label);
		label.setText("Your " + typeString + " rank");
		TextView rankText = (TextView) rankView.findViewById(R.id.rank);
		rankText.setText(rank.getRank() + "");
	}

	public void signOut() {
		context.clearRanks();
		juniorRankView.setVisibility(View.GONE);
		menuAdapter.setActive(juniorRankView, false);
		normalRankView.setVisibility(View.GONE);
		menuAdapter.setActive(normalRankView, false);
		strictRankView.setVisibility(View.GONE);
		menuAdapter.setActive(strictRankView, false);

		clearInvites();
		clearNonLocalMatches(myTurns);
		clearNonLocalMatches(theirTurns);
		clearNonLocalMatches(completedMatches);
		// refreshMatches();
	}

	private void refreshInvites(final boolean shouldFlashNumber) {
		Games.Invitations.loadInvitations(
				context.getGameHelper().getApiClient()).setResultCallback(
				new ResultCallback<Invitations.LoadInvitationsResult>() {

					@Override
					public void onResult(LoadInvitationsResult result) {
						int statusCode = result.getStatus().getStatusCode();
						InvitationBuffer buffer = result.getInvitations();
						// remove existing invite matches from myTurn list, and
						// add back these
						clearInvites();
						if (statusCode == GamesClient.STATUS_OK) {
							// update the online invites button with the count
							int count = buffer.getCount();
							if (count != 0) {
								for (int i = 0; i < count; i++) {
									Invitation invite = buffer.get(i);
									InviteMatchInfo matchInfo = new InviteMatchInfo(
											context.getGameHelper(), invite);
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
		RealtimeGameStrategy roomListener = new RealtimeGameStrategy(context,
				null, false, false, false);
		// accept the invitation
		Log.d(TAG, "Accepting invitation: " + invId);
		RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(roomListener);
		roomConfigBuilder.setInvitationIdToAccept(invId)
				.setMessageReceivedListener(roomListener)
				.setRoomStatusUpdateListener(roomListener);
		setActive();
		Games.RealTimeMultiplayer.join(context.getGameHelper().getApiClient(),
				roomConfigBuilder.build());
	}

	// private GamesClient getGamesClient() {
	// return context.getGameHelper().getGamesClient();
	// }

	public void setActive() {
		if (waiting == null)
			return;
		waiting.setVisibility(View.INVISIBLE);
		Log.i(TAG, "Setting active");
	}

	public void setInactive() {
		if (waiting == null)
			return;
		waiting.setVisibility(View.VISIBLE);
		Log.i(TAG, "Setting inactive");
	}

	@Override
	public void onPause() {
		if (context.getGameHelper().isSignedIn()) {
			unregisterMatchListeners();
		}
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (context.getGameHelper().isSignedIn()) {
			registerMatchListeners();
			showLogout();
		} else {
			showLogin();
		}
	}

	private void registerMatchListeners() {
		Games.Invitations.registerInvitationListener(context.getGameHelper()
				.getApiClient(), this);
		Games.TurnBasedMultiplayer.registerMatchUpdateListener(context
				.getGameHelper().getApiClient(), this);
	}

	private void unregisterMatchListeners() {
		// TODO this is being called AFTER the game strategy has been created,
		// and registered itself!
		Log.d(TAG, "called to unregister match listener");
		// Games.Invitations.unregisterInvitationListener(context.getGameHelper()
		// .getApiClient());
		// Games.TurnBasedMultiplayer.unregisterMatchUpdateListener(context
		// .getGameHelper().getApiClient());
	}

	public void acceptTurnBasedInvitation(final String inviteId) {
		setInactive();

		Games.TurnBasedMultiplayer
				.acceptInvitation(context.getGameHelper().getApiClient(),
						inviteId)
				.setResultCallback(
						new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {

							@Override
							public void onResult(InitiateMatchResult result) {
								int status = result.getStatus().getStatusCode();
								if (status != GamesClient.STATUS_OK) {
									showAlert("Error accepting invitation "
											+ inviteId + ": error=" + status);
									return;
								}
								TurnBasedMatchGameStrategy listener = new TurnBasedMatchGameStrategy(
										context, result.getMatch());

								listener.showGame();
							}
						});

	}

	public void showMatch(String matchId) {
		setInactive();

		Games.TurnBasedMultiplayer
				.loadMatch(context.getGameHelper().getApiClient(), matchId)
				.setResultCallback(
						new ResultCallback<TurnBasedMultiplayer.LoadMatchResult>() {

							@Override
							public void onResult(LoadMatchResult result) {
								if (result.getStatus().getStatusCode() != GamesClient.STATUS_OK) {
									showAlert("Error loading match");
									setActive();
									return;
								}
								updateMatch(result.getMatch());

							}
						});
	}

	// This is the main function that gets called when players choose a match
	// from the inbox, or else create a match and want to start it.
	public void updateMatch(TurnBasedMatch match) {
		// if sign in just succeeded, but another game is in progress, don't
		// interrupt
		// / may need to check if gameFragment exists?
		if (context.getGameStrategy() != null)
			return;

		setInactive();
		TurnBasedMatchGameStrategy listener = new TurnBasedMatchGameStrategy(
				context, match);

		listener.showFromMenu();
	}

	public void refreshMatches() {
		if (mPullToRefreshLayout != null) {
			mPullToRefreshLayout.setRefreshing(true);
		}
		localRefreshed = false;
		networkRefreshed = false;
		rankRefreshed = false;
		Log.i(TAG, "about to refresh DB");

		dbHandler.getMatches(new OnLocalMatchesLoadListener() {
			@Override
			public void onLoadSuccess(LocalMatchesBuffer localMatchesBuffer) {
				// remove existing local matches from my,their, completed
				clearLocalMatches(myTurns);
				clearLocalMatches(theirTurns);
				clearLocalMatches(completedMatches);

				myTurns.addAll(localMatchesBuffer.getMyTurn());
				myTurnsAdapter.notifyDataSetChanged();

				theirTurns.addAll(localMatchesBuffer.getTheirTurn());
				theirTurnsAdapter.notifyDataSetChanged();

				completedMatches.addAll(localMatchesBuffer
						.getCompletedMatches());
				completedMatchesAdapter.notifyDataSetChanged();

				localRefreshed = true;
				conditionallyMarkRefreshComplete();
			}

			private void clearLocalMatches(List<MatchInfo> list) {
				for (Iterator<MatchInfo> iter = list.iterator(); iter.hasNext();) {
					MatchInfo each = iter.next();
					if (each instanceof LocalMatchInfo) {
						iter.remove();
					}
				}
			}

			@Override
			public void onLoadFailure() {
				Log.w(TAG, "Error loading local matches");
				localRefreshed = true;
				conditionallyMarkRefreshComplete();
			}

		});
		if (!context.getGameHelper().getApiClient().isConnected()) {
			if (!context.getGameHelper().getApiClient().isConnecting()
					&& mPullToRefreshLayout != null) {
				networkRefreshed = true;
				rankRefreshed = true;
				conditionallyMarkRefreshComplete();
				return;
			}
		}

		context.loadRank(new OnRankReceived() {
			@Override
			public void receivedRank(RankStorage rankStorage) {
				rankRefreshed = true;
				if (rankStorage == null) {
					conditionallyMarkRefreshComplete();
					return;
				}
				if (isVisible()) {
					updateRankView(juniorRankView, "junior",
							rankStorage.getJuniorRank());
					updateRankView(normalRankView, "normal",
							rankStorage.getNormalRank());
					updateRankView(strictRankView, "strict",
							rankStorage.getStrictRank());
				}
				conditionallyMarkRefreshComplete();
			}
		}, false);

		Games.TurnBasedMultiplayer.loadMatchesByStatus(
				context.getGameHelper().getApiClient(),
				TurnBasedMatch.MATCH_TURN_STATUS_INVITED,
				TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN,
				TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN,
				TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE).setResultCallback(
				new ResultCallback<TurnBasedMultiplayer.LoadMatchesResult>() {

					@Override
					public void onResult(LoadMatchesResult result) {
						int status = result.getStatus().getStatusCode();
						if (status != GamesClient.STATUS_OK) {
							networkRefreshed = true;
							conditionallyMarkRefreshComplete();
							// TODO report an error in some way, retry
							return;
						}
						LoadMatchesResponse response = result.getMatches();
						InvitationBuffer invitations = response
								.getInvitations();

						// put invites into my turns
						clearNonLocalMatches(myTurns);
						int max = invitations.getCount();
						for (int i = 0; i < max; i++) {
							Invitation invitation = invitations.get(i);
							myTurns.add(new InviteMatchInfo(context
									.getGameHelper(), invitation));
						}
						invitations.close();

						TurnBasedMatchBuffer myTurnMatches = response
								.getMyTurnMatches();
						populateMatches(myTurnMatches, myTurnsAdapter, myTurns);

						clearNonLocalMatches(theirTurns);
						TurnBasedMatchBuffer theirTurnMatches = response
								.getTheirTurnMatches();
						populateMatches(theirTurnMatches, theirTurnsAdapter,
								theirTurns);

						clearNonLocalMatches(completedMatches);
						TurnBasedMatchBuffer completedMatchesBuffer = response
								.getCompletedMatches();
						populateMatches(completedMatchesBuffer,
								completedMatchesAdapter, completedMatches);

						networkRefreshed = true;
						conditionallyMarkRefreshComplete();
					}

					private void populateMatches(
							TurnBasedMatchBuffer matchesBuffer,
							MatchAdapter adapter, List<MatchInfo> matches) {
						int count = matchesBuffer.getCount();
						for (int i = 0; i < count; i++) {
							TurnBasedMatch match = matchesBuffer.get(i);
							MatchInfo info = new TurnBasedMatchInfo(
									getActivity(), context.getGameHelper(),
									match);
							matches.add(info);
						}
						matchesBuffer.close();
						adapter.notifyDataSetChanged();
					}
				});
	}

	private void conditionallyMarkRefreshComplete() {
		if (localRefreshed && networkRefreshed && rankRefreshed) {
			mPullToRefreshLayout.setRefreshComplete();
		}
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
		context.getSoundManager().playSound(Sounds.INVITE_RECEIVED);
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

	public void showLocalMatch(LocalMatchInfo localMatchInfo) {
		setInactive();

		AbstractLocalStrategy strategy = localMatchInfo.createStrategy(context);
		strategy.showFromMenu();
	}

	public void showAlert(String message) {
		Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
	}

	private void clearNonLocalMatches(List<MatchInfo> list) {
		for (Iterator<MatchInfo> iter = list.iterator(); iter.hasNext();) {
			MatchInfo each = iter.next();
			if (each instanceof LocalMatchInfo)
				continue;
			iter.remove();
		}
	}

	private void clearInvites() {
		for (Iterator<MatchInfo> iter = myTurns.iterator(); iter.hasNext();) {
			MatchInfo each = iter.next();
			if (each instanceof InviteMatchInfo) {
				iter.remove();
			}
		}
	}

	public void gameEnded() {
		setActive();
		refreshMatches();
	}
}
