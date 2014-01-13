package com.oakonell.ticstacktoe.utils;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.oakonell.ticstacktoe.TicStackToe;
import com.oakonell.ticstacktoe.googleapi.GameHelper;

public class DevelopmentUtil {
	private static final String LogTag = DevelopmentUtil.class.getName();

	public static class Info {
		public Info(GameHelper helper) {
			accountName = helper.getGamesClient().getCurrentAccountName();
			scopes = helper.getScopes();
			achievementIntent = helper.getGamesClient().getAchievementsIntent();
			allLeaderboardsIntent = helper.getGamesClient()
					.getAllLeaderboardsIntent();
		}

		public String scopes;
		public String accountName;
		public Intent achievementIntent;
		public Intent allLeaderboardsIntent;
	}

	public static void resetAchievements(Activity context, Info helper) {
		// as seen on
		// http://stackoverflow.com/questions/17658732/reset-achievements-leaderboard-from-my-android-application
		ProgressDialog dialog = ProgressDialog.show(context, "Resetting Achievements", "Please Wait...");
		new AchievementsResetterTask(context, helper, dialog).execute((Void) null);
	}

	public static void resetLeaderboards(Activity context, Info helper) {
		ProgressDialog dialog = ProgressDialog.show(context, "Resetting Leaderboards", "Please Wait...");
		new LeaderboardResetterTask(context, helper, dialog).execute((Void) null);
	}

	private static class AchievementsResetterTask extends
			AsyncTask<Void, Void, Void> {
		private Activity mContext;
		private Info helper;
		private ProgressDialog dialog;

		public AchievementsResetterTask(Activity con, Info helper, ProgressDialog dialog) {
			mContext = con;
			this.helper = helper;
			this.dialog = dialog;
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				String accesstoken = GoogleAuthUtil.getToken(mContext,
						helper.accountName, helper.scopes);

				HttpClient client = new DefaultHttpClient();
				// Reset a single achievement like this:
				/*
				 * String acheivementid = "acheivementid"; HttpPost post = new
				 * HttpPost ( "https://www.googleapis.com"+
				 * "/games/v1management"+ "/achievements/"+ acheivementid+
				 * "/reset?access_token="+accesstoken );
				 */

				// This resets all achievements:
				HttpPost post = new HttpPost("https://www.googleapis.com"
						+ "/games/v1management" + "/achievements"
						+ "/reset?access_token=" + accesstoken);

				client.execute(post);
				Log.i(LogTag, "Reset achievements done.");
			} catch (Exception e) {
				Log.e(LogTag, "Failed to reset: " + e.getMessage(), e);
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			dialog.dismiss();
			// Launch activity to refresh data on client.
			// NOTE: Incremental achievements will look like they are not reset.
			// However, next time you and some steps it will start from 0 and
			// gui will look ok.
			mContext.startActivityForResult(helper.achievementIntent, 0);
		}
	}

	private static class LeaderboardResetterTask extends
			AsyncTask<Void, Void, Void> {
		private Activity mContext;
		private Info helper;
		private ProgressDialog dialog;

		public LeaderboardResetterTask(Activity con, Info helper, ProgressDialog dialog) {
			mContext = con;
			this.helper = helper;
			this.dialog = dialog;
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				String accesstoken = GoogleAuthUtil.getToken(mContext,
						helper.accountName, helper.scopes);

				TicStackToe app = (TicStackToe) mContext.getApplication();
				for (String leaderboardid : app.getLeaderboards()
						.getLeaderboardIds(mContext)) {
					HttpClient client = new DefaultHttpClient();
					// Reset leader board:
					HttpPost post = new HttpPost("https://www.googleapis.com"
							+ "/games/v1management" + "/leaderboards/"
							+ leaderboardid + "/scores/reset?access_token="
							+ accesstoken);

					client.execute(post);
				}
				Log.i(LogTag, "Reset leaderboards done.");
			} catch (Exception e) {
				Log.e(LogTag,
						"Failed to reset leaderboards: " + e.getMessage(), e);
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			dialog.dismiss();
			// Launch activity to refresh data on client.
			mContext.startActivityForResult(helper.allLeaderboardsIntent, 0);
		}
	}

}
