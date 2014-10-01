package com.oakonell.ticstacktoe.utils;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.appstate.AppStateManager;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.games.Games;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.TicStackToe;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.googleapi.inappbill.IabHelper;
import com.oakonell.ticstacktoe.googleapi.inappbill.IabHelper.OnConsumeFinishedListener;
import com.oakonell.ticstacktoe.googleapi.inappbill.IabResult;
import com.oakonell.ticstacktoe.googleapi.inappbill.Inventory;
import com.oakonell.ticstacktoe.googleapi.inappbill.Purchase;
import com.oakonell.ticstacktoe.rank.RankHelper;
import com.oakonell.ticstacktoe.rank.RankHelper.OnRankDeleted;

public class DevelopmentUtil {
	private static final String LogTag = DevelopmentUtil.class.getName();

	public static class Info {
		public Info(GameHelper helper) {
			accountName = Games.getCurrentAccountName(helper.getApiClient());
			scopes = Scopes.GAMES;
			achievementIntent = Games.Achievements.getAchievementsIntent(helper
					.getApiClient());
			allLeaderboardsIntent = Games.Leaderboards
					.getAllLeaderboardsIntent(helper.getApiClient());
		}

		public String scopes;
		public String accountName;
		public Intent achievementIntent;
		public Intent allLeaderboardsIntent;
	}

	public static void resetAchievements(Activity context, Info helper) {
		// as seen on
		// http://stackoverflow.com/questions/17658732/reset-achievements-leaderboard-from-my-android-application
		ProgressDialog dialog = ProgressDialog.show(context,
				"Resetting Achievements", "Please Wait...");
		new AchievementsResetterTask(context, helper, dialog)
				.execute((Void) null);
	}

	public static void resetLeaderboards(Activity context, Info helper) {
		ProgressDialog dialog = ProgressDialog.show(context,
				"Resetting Leaderboards", "Please Wait...");
		new LeaderboardResetterTask(context, helper, dialog)
				.execute((Void) null);
	}

	private static class AchievementsResetterTask extends
			AsyncTask<Void, Void, Void> {
		private Activity mContext;
		private Info helper;
		private ProgressDialog dialog;

		public AchievementsResetterTask(Activity con, Info helper,
				ProgressDialog dialog) {
			mContext = con;
			this.helper = helper;
			this.dialog = dialog;
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				String scope =  "oauth2:" +
				 "https://www.googleapis.com/auth/games";
				String accesstoken = GoogleAuthUtil.getToken(mContext,
						helper.accountName, scope);

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

				HttpResponse response = client.execute(post);
				int callResponseCode = response.getStatusLine().getStatusCode();
				String stringResponse = EntityUtils.toString(response
						.getEntity());
				Log.i(LogTag, "Reset achievements done: " + callResponseCode
						+ "-- " + stringResponse);
			} catch (Exception e) {
				Toast.makeText(mContext,
						"Error resetting achievements: " + e.getMessage(),
						Toast.LENGTH_SHORT).show();
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

		public LeaderboardResetterTask(Activity con, Info helper,
				ProgressDialog dialog) {
			mContext = con;
			this.helper = helper;
			this.dialog = dialog;
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				String scope = "oauth2:https://www.googleapis.com/auth/games";

				String accesstoken = GoogleAuthUtil.getToken(mContext,
						helper.accountName, scope);

				TicStackToe app = (TicStackToe) mContext.getApplication();
				for (String leaderboardid : app.getLeaderboards()
						.getLeaderboardIds(mContext)) {
					HttpClient client = new DefaultHttpClient();
					// Reset leader board:
					HttpPost post = new HttpPost("https://www.googleapis.com"
							+ "/games/v1management" + "/leaderboards/"
							+ leaderboardid + "/scores/reset?access_token="
							+ accesstoken);

					HttpResponse response = client.execute(post);
					int callResponseCode = response.getStatusLine().getStatusCode();
					String stringResponse = EntityUtils.toString(response
							.getEntity());
					Log.i(LogTag, "Reset leaderboard done: " + callResponseCode
							+ "-- " + stringResponse);
				}
				Log.i(LogTag, "Reset leaderboards done.");
			} catch (Exception e) {
				Toast.makeText(mContext,
						"Error resetting leaderboards: " + e.getMessage(),
						Toast.LENGTH_SHORT).show();
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

	public static void deleteRanks(final Activity activity, Info info) {
		final GoogleApiClient[] client = new GoogleApiClient[1];

		final ProgressDialog progress = ProgressDialog.show(activity,
				"Deleting Rank Save Data", "Please wait...");
		progress.setCancelable(false);

		ConnectionCallbacks connectionCallbacks = new ConnectionCallbacks() {
			@Override
			public void onConnectionSuspended(int arg0) {
				progress.dismiss();
				progress.setMessage("Connection suspended.");
				progress.setCancelable(true);
			}

			@Override
			public void onConnected(Bundle arg0) {
				RankHelper.deleteRankStorage(client[0], new OnRankDeleted() {
					@Override
					public void rankDeleted(Status status) {
						progress.dismiss();
						Toast.makeText(activity, "Ranks deleted",
								Toast.LENGTH_SHORT).show();
					}
				});

			}
		};
		OnConnectionFailedListener onConnectionFailedListener = new OnConnectionFailedListener() {

			@Override
			public void onConnectionFailed(ConnectionResult arg0) {
				progress.dismiss();
				progress.setMessage("Connection failed.");
				progress.setCancelable(true);
			}
		};
		GoogleApiClient.Builder builder = new GoogleApiClient.Builder(activity,
				connectionCallbacks, onConnectionFailedListener);
		builder.addApi(AppStateManager.API);
		builder.addScope(AppStateManager.SCOPE_APP_STATE);
		client[0] = builder.build();
		client[0].connect();

	}

	public static void downgradeFromPremium(final Activity activity,
			final OnConsumeFinishedListener listener) {
		try {
			// complain(activity, "Starting downgrade");
			String base64EncodedPublicKey = BillingHelper.getPublicKey();
			// Create the helper, passing it our context and the public key to
			// verify signatures with
			Log.d(LogTag, "Creating IAB helper.");
			final IabHelper mHelper = new IabHelper(activity,
					base64EncodedPublicKey);

			// enable debug logging (for a production application, you should
			// set
			// this to false).
			mHelper.enableDebugLogging(true);
			Log.d(LogTag, "Starting setup.");
			final IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {

				public void onQueryInventoryFinished(IabResult result,
						Inventory inventory) {
					// complain(activity, "query finished");

					Log.d(LogTag, "Query inventory finished.");

					// Have we been disposed of in the meantime? If so, quit.
					// if (mHelper == null)
					// return;

					// Is it a failure?
					if (result.isFailure()) {
						complain(activity, "Failed to query inventory: "
								+ result);
						mHelper.dispose();
						return;
					}

					Log.d(LogTag, "Query inventory was successful.");

					/*
					 * Check for items we own. Notice that for each purchase, we
					 * check the developer payload to see if it's correct! See
					 * verifyDeveloperPayload().
					 */

					// Do we have the premium upgrade?
					Purchase premiumPurchase = inventory
							.getPurchase(BillingHelper.SKU_PREMIUM);
					if (premiumPurchase != null) {
						mHelper.consumeAsync(premiumPurchase,
								new OnConsumeFinishedListener() {
									@Override
									public void onConsumeFinished(
											Purchase purchase, IabResult result) {
										if (result.isSuccess()) {
											SharedPreferences sharedPrefs = PreferenceManager
													.getDefaultSharedPreferences(activity);
											Editor edit = sharedPrefs.edit();
											edit.putBoolean(
													activity.getString(R.string.pref_premium_key),
													false);
											edit.apply();

											Toast.makeText(activity,
													"Downgraded",
													Toast.LENGTH_LONG).show();
										} else {
											complain(
													activity,
													"Unable to downgrade: "
															+ result.getMessage());
										}
										listener.onConsumeFinished(purchase,
												result);
										mHelper.dispose();
									}
								});
					} else {
						Toast.makeText(activity,
								"Not currently a premium upgrade",
								Toast.LENGTH_LONG).show();
					}
				}
			};

			mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
				public void onIabSetupFinished(IabResult result) {
					// complain(activity, "Setup finished");
					Log.d(LogTag, "Setup finished.");

					if (!result.isSuccess()) {
						// Oh noes, there was a problem.
						String message = "Problem setting up in-app billing: "
								+ result;
						complain(activity, message);
						mHelper.dispose();

						return;
					}

					// Have we been disposed of in the meantime? If so, quit.
					// if (mHelper == null) {
					// return;
					// }

					// IAB is fully set up. Now, let's get an inventory of stuff
					// we
					// own.
					Log.d(LogTag,
							"Billing Setup successful. Querying inventory.");
					mHelper.queryInventoryAsync(mGotInventoryListener);
				}

			});
		} catch (Exception e) {
			complain(activity,
					"Got an exception trying to downgrade..." + e.getMessage());
		}
	}

	private static void complain(final Activity activity, String message) {
		AlertDialog.Builder bld = new AlertDialog.Builder(activity);
		bld.setMessage(message);
		bld.setNeutralButton("OK", null);
		Log.d(LogTag, "Showing alert dialog: " + message);
		bld.show();
	}

}
