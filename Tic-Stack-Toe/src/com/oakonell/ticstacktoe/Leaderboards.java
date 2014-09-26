package com.oakonell.ticstacktoe;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.Leaderboards.SubmitScoreResult;
import com.google.android.gms.games.leaderboard.ScoreSubmissionData.Result;
import com.oakonell.ticstacktoe.model.GameType;

public class Leaderboards {

	public List<String> getLeaderboardIds(Context context) {
		List<String> result = new ArrayList<String>();
		result.add(context.getString(R.string.leaderboard_junior_rank));
		result.add(context.getString(R.string.leaderboard_normal_rank));
		result.add(context.getString(R.string.leaderboard_strict_rank));
		return result;
	}

	public void submitRank(GameContext gameHelper, GameType type, short rank) {
		if (type.equals(GameType.JUNIOR)) {
			submitJuniorRank(gameHelper, rank);
		} else if (type.equals(GameType.NORMAL)) {
			submitNormalRank(gameHelper, rank);
		} else if (type.equals(GameType.STRICT)) {
			submitStrictRank(gameHelper, rank);
		} else {
			throw new RuntimeException("Unexpected game type " + type);
		}
	}

	private void submitNormalRank(GameContext gameHelper, short rank) {
		int idRes = R.string.leaderboard_normal_rank;
		final int all_time_res = R.string.beat_all_time_normal_rank;
		final int weekly_res = R.string.beat_weekly_normal_rank;
		final int daily_res = R.string.beat_daily_normal_rank;

		String id = gameHelper.getContext().getString(idRes);

		submitRank(gameHelper, id, rank, all_time_res, weekly_res, daily_res);
	}

	private void submitStrictRank(GameContext gameHelper, short rank) {
		int idRes = R.string.leaderboard_strict_rank;
		final int all_time_res = R.string.beat_all_time_strict_rank;
		final int weekly_res = R.string.beat_weekly_strict_rank;
		final int daily_res = R.string.beat_daily_strict_rank;

		String id = gameHelper.getContext().getString(idRes);

		submitRank(gameHelper, id, rank, all_time_res, weekly_res, daily_res);
	}

	private void submitJuniorRank(final GameContext gameHelper, final short rank) {
		int idRes = R.string.leaderboard_junior_rank;
		final int all_time_res = R.string.beat_all_time_junior_rank;
		final int weekly_res = R.string.beat_weekly_junior_rank;
		final int daily_res = R.string.beat_daily_junior_rank;

		String id = gameHelper.getContext().getString(idRes);

		submitRank(gameHelper, id, rank, all_time_res, weekly_res, daily_res);
	}

	private void submitRank(final GameContext gameHelper, String id,
			final short rank, final int all_time_res, final int weekly_res,
			final int daily_res) {
		final Tracker myTracker = EasyTracker.getTracker();

		Games.Leaderboards.submitScoreImmediate(
				gameHelper.getGameHelper().getApiClient(), id, rank)
				.setResultCallback(new ResultCallback<SubmitScoreResult>() {

					@Override
					public void onResult(SubmitScoreResult result) {
						Result allTimeResult = result.getScoreData()
								.getScoreResult(
										LeaderboardVariant.TIME_SPAN_ALL_TIME);
						Context context = gameHelper.getContext();
						if (allTimeResult != null && allTimeResult.newBest) {
							myTracker.sendEvent(
									gameHelper.getContext().getString(
											R.string.an_leaderboard_beat),
									"all_time_best", allTimeResult.formattedScore, 0L);
							Toast.makeText(
									context,
									context.getResources().getString(
											all_time_res,
											allTimeResult.formattedScore),
									Toast.LENGTH_SHORT).show();
							return;
						}

						Result weeklyResult = result.getScoreData()
								.getScoreResult(
										LeaderboardVariant.TIME_SPAN_WEEKLY);
						if (weeklyResult != null && weeklyResult.newBest) {
							myTracker.sendEvent(
									gameHelper.getContext().getString(
											R.string.an_leaderboard_beat),
									"weekly_best", weeklyResult.formattedScore, 0L);
							Toast.makeText(
									context,
									context.getResources().getString(
											weekly_res,
											weeklyResult.formattedScore),
									Toast.LENGTH_SHORT).show();
							return;
						}

						Result dailyResult = result.getScoreData()
								.getScoreResult(
										LeaderboardVariant.TIME_SPAN_DAILY);
						if (dailyResult != null && dailyResult.newBest) {
							myTracker.sendEvent(
									gameHelper.getContext().getString(
											R.string.an_leaderboard_beat),
									"daily_best", dailyResult.formattedScore, 0L);
							Toast.makeText(
									context,
									context.getResources().getString(daily_res,
											dailyResult.formattedScore),
									Toast.LENGTH_SHORT).show();
						}
					}
				});
	}

}
