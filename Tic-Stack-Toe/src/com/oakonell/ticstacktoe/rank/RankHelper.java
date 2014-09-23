package com.oakonell.ticstacktoe.rank;

import java.nio.ByteBuffer;

import android.util.Log;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gms.appstate.AppStateManager;
import com.google.android.gms.appstate.AppStateManager.StateConflictResult;
import com.google.android.gms.appstate.AppStateManager.StateDeletedResult;
import com.google.android.gms.appstate.AppStateManager.StateLoadedResult;
import com.google.android.gms.appstate.AppStateManager.StateResult;
import com.google.android.gms.appstate.AppStateStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.oakonell.ticstacktoe.GameContext;
import com.oakonell.ticstacktoe.Leaderboards;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.TicStackToe;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.RankInfo;
import com.oakonell.ticstacktoe.model.rank.RankStorage;
import com.oakonell.ticstacktoe.model.rank.RankedGame;
import com.oakonell.ticstacktoe.model.rank.RankingRater;
import com.oakonell.ticstacktoe.model.rank.TypeRankStorage;
import com.oakonell.ticstacktoe.utils.ByteBufferDebugger;

/**
 * This class helps handle the save-game state storage of the user's rank
 * 
 */
public class RankHelper {
	private static final String TAG = RankHelper.class.getName();
	private static final int RANK_APP_STATE_KEY = 1;

	public interface OnRankReceived {
		void receivedRank(RankStorage storage);
	}

	public interface OnRankDeleted {
		void rankDeleted(Status status);
	}

	public static void deleteRankStorage(GoogleApiClient client,
			final OnRankDeleted onRankDeleted) {
		AppStateManager.delete(client, RANK_APP_STATE_KEY).setResultCallback(
				new ResultCallback<AppStateManager.StateDeletedResult>() {

					@Override
					public void onResult(StateDeletedResult result) {
						Status status = result.getStatus();
						onRankDeleted.rankDeleted(status);
					}
				});
	}

	public static void loadRankStorage(final GameContext context,
			final OnRankReceived onRankReceived, final boolean initializeIfNone) {

		final long start = System.currentTimeMillis();
		Log.i(TAG, "loading ranks...");
		AppStateManager.load(context.getGameHelper().getApiClient(),
				RANK_APP_STATE_KEY).setResultCallback(
				new ResultCallback<AppStateManager.StateResult>() {

					@Override
					public void onResult(StateResult result) {
						Log.i(TAG,
								"  done loading ranks..."
										+ (System.currentTimeMillis() - start)
										+ " ms");

						if (result.getStatus().getStatusCode() == AppStateStatusCodes.STATUS_STATE_KEY_NOT_FOUND) {
							if (initializeIfNone) {
								Log.i(TAG, "  no ranks, initialing ranks...");
								RankStorage rankStorage = createInitializedRanks();

								saveRanks(context, rankStorage);

								onRankReceived.receivedRank(rankStorage);
								return;
							}
							Log.i(TAG, "  No rank save data yet!");
							onRankReceived.receivedRank(null);
						}
						if (result.getStatus().getStatusCode() == AppStateStatusCodes.STATUS_WRITE_OUT_OF_DATE_VERSION) {
							Log.w(TAG, "  rank write data is out of date");
						} else if (result.getStatus().getStatusCode() == AppStateStatusCodes.STATUS_NETWORK_ERROR_STALE_DATA) {
							Log.w(TAG, "  rank network data is stale");
						} else if (result.getStatus().getStatusCode() != AppStateStatusCodes.STATUS_OK) {
							Log.w(TAG, "Error("
									+ result.getStatus().getStatusCode()
									+ ") loading Rank");
							onRankReceived.receivedRank(null);
							return;
						}
						// AppStateManager.delete(context.getGameHelper().getApiClient(),
						// RANK_APP_STATE_KEY);
						// if (true) return;
						RankStorage rankStorage = null;
						try {
							if (result.getLoadedResult() != null) {
								Log.i(TAG, "  Found rank save data!");
								StateLoadedResult loadedResult = result
										.getLoadedResult();
								byte[] saveData = loadedResult.getLocalData();
								if (saveData != null) {
									ByteBufferDebugger buffer = new ByteBufferDebugger(
											ByteBuffer.wrap(saveData));
									rankStorage = RankStorage.fromBytes(buffer);
								}
							} else {
								Log.i(TAG, "  Found conflicted rank save data!");
								Tracker myTracker = EasyTracker.getTracker();
								String action = "resolve";
								String label = "";
								myTracker.sendEvent(context.getSherlockActivity().getString(R.string.an_conlficted_rank_save), action, label, 0L );
								StateConflictResult conflictResult = result
										.getConflictResult();
								byte[] localData = conflictResult
										.getLocalData();
								byte[] serverData = conflictResult
										.getServerData();

								RankStorage serverStorage = RankStorage
										.fromBytes(new ByteBufferDebugger(
												ByteBuffer.wrap(serverData)));
								RankStorage localStorage = RankStorage
										.fromBytes(new ByteBufferDebugger(
												ByteBuffer.wrap(localData)));
								rankStorage = localStorage
										.resolveConflict(serverStorage);
								String version = conflictResult
										.getResolvedVersion();

								// TODO time the resolution?
								AppStateManager.resolve(context.getGameHelper()
										.getApiClient(), RANK_APP_STATE_KEY,
										version, rankStorage.toBytes());
							}
						} catch (Exception e) {
							Log.e(TAG,
									"Error reading saved ranks, creating initialized ranks",
									e);
							Toast.makeText(context.getContext(),
									"Error loading saved ranks...",
									Toast.LENGTH_SHORT).show();
							rankStorage = createInitializedRanks();
						}
						onRankReceived.receivedRank(rankStorage);
					}

					private RankStorage createInitializedRanks() {
						RankingRater ranker = RankingRater.Factory.getRanker();
						short initialRank = ranker.initialRank();
						TypeRankStorage junior = new TypeRankStorage(
								GameType.JUNIOR, initialRank);
						TypeRankStorage normal = new TypeRankStorage(
								GameType.NORMAL, initialRank);
						TypeRankStorage strict = new TypeRankStorage(
								GameType.STRICT, initialRank);
						RankStorage rankStorage = new RankStorage(junior,
								normal, strict);
						return rankStorage;
					}
				});
	}

	protected static void saveRanks(GameContext context, RankStorage ranks) {
		Log.i(TAG, "  Saving ranks: ");
		AppStateManager.update(context.getGameHelper().getApiClient(),
				RANK_APP_STATE_KEY, ranks.toBytes());
		context.updateCachedRank(ranks);
	}

	public interface RankInfoUpdated {
		void onRankInfoUpdated(RankInfo info);
	}

	public static void createRankInfo(GameContext context, final GameType type,
			final boolean iAmBlack, final RankInfoUpdated infoUpdated) {
		context.loadRank(new OnRankReceived() {
			@Override
			public void receivedRank(RankStorage storage) {
				TypeRankStorage typeRank = storage.getRank(type);
				short rank = typeRank.getRank();
				RankInfo info;
				if (iAmBlack) {
					info = new RankInfo(rank, (short) -1);
				} else {
					info = new RankInfo((short) -1, rank);
				}
				infoUpdated.onRankInfoUpdated(info);
			}
		}, true);
	}

	public interface OnMyRankUpdated {
		void onRankUpdated(short oldRank, short newRank);
	}

	public static void updateRank(final GameContext gameContext,
			final GameType type, final RankedGame rankedGame,
			final OnMyRankUpdated onRankUpdated) {

		Log.i(TAG, "Updating rank. First load existing one...");
		loadRankStorage(gameContext, new OnRankReceived() {
			@Override
			public void receivedRank(RankStorage storage) {
				TypeRankStorage typeStorage = storage.getRank(type);
				RankingRater ranker = RankingRater.Factory.getRanker();
				short currentRank = typeStorage.getRank();
				short myNewRank = ranker.calculateRank(currentRank,
						rankedGame.getOpponentRank(), rankedGame.getOutcome());
				Log.i(TAG, "  Calculating new rank: " + currentRank + "->"
						+ myNewRank);

				rankedGame.setMyRank(currentRank);
				typeStorage.setRank(myNewRank);

				TicStackToe application = ((TicStackToe) gameContext
						.getSherlockActivity().getApplication());
				Leaderboards leaderboards = application.getLeaderboards();
				leaderboards.submitRank(gameContext, type, myNewRank);

				typeStorage.add(rankedGame);
				if (onRankUpdated != null) {
					onRankUpdated.onRankUpdated(currentRank, myNewRank);
				}
				saveRanks(gameContext, storage);

				gameContext.updateCachedRank(storage);
			}
		}, true);

	}
}