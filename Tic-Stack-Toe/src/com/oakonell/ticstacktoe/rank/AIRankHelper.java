package com.oakonell.ticstacktoe.rank;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler;
import com.oakonell.ticstacktoe.model.rank.GameOutcome;
import com.oakonell.ticstacktoe.model.solver.AILevel;

public class AIRankHelper {
	private static final long RANKS_CACHE_TIME_MS = TimeUnit.DAYS.toMillis(1);
	private final static String QUERY_URL_STRING = "http://ticstacktoe.appspot.com/airanks";
	private final static String UPDATE_URL_STRING = "http://ticstacktoe.appspot.com/aiadjust";
	private static final String TAG = AIRankHelper.class.getName();

	public interface OnRanksRetrieved {
		void onSuccess(Map<AILevel, Integer> ranks);
	}

	public static void updateRank(final DatabaseHandler db,
			final GameType type, final AILevel aiLevel,
			final GameOutcome outcome, final short humanRank) {
		Log.i(TAG, "Updating AI rank");
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... arg0) {
				HttpClient httpclient = new DefaultHttpClient();
				HttpResponse response;
				String responseString = null;
				try {
					String url = UPDATE_URL_STRING;
					url += "?";
					url += "aiKey=" + aiLevel.getValue();
					url += "&";
					url += "aiType=" + type.getVariant();
					url += "&";
					url += "outcome=" + outcome.getId();
					url += "&";
					url += "opponentRank=" + humanRank;
					HttpGet httpGet = new HttpGet(url);
					// HttpParams params = httpGet.getParams();
					// params.setIntParameter("aiKey", aiLevel.getValue());
					// params.setIntParameter("aiType", type.getVariant());
					// params.setIntParameter("outcome", outcome.getId());
					// params.setIntParameter("opponentRank", humanRank);
					Log.i(TAG,
							"  connecting to URL " + httpGet.getRequestLine());
					response = httpclient.execute(httpGet);
					StatusLine statusLine = response.getStatusLine();
					Log.i(TAG, "  received response " + response);
					if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						response.getEntity().writeTo(out);
						out.close();
						responseString = out.toString();
					} else {
						// Closes the connection.
						response.getEntity().getContent().close();
						Log.w(TAG,
								"Couldn't update AI rank on server, because '"
										+ statusLine.getReasonPhrase() + "'");
					}
				} catch (Exception e) {
					Log.w(TAG,
							"Couldn't update AI rank on server, caught an exception "
									+ e.getMessage());
					return null;
				}

				storeRanksAndReturn(db, responseString, type);
				return null;
			}

		};
		task.execute((Void) null);
	}

	public static void retrieveRanks(final DatabaseHandler db,
			final GameType type, final OnRanksRetrieved onRetrieved) {
		Log.i(TAG, "Retrieving AI ranks...");
		AsyncTask<String, String, Map<AILevel, Integer>> task = new AsyncTask<String, String, Map<AILevel, Integer>>() {
			@Override
			protected Map<AILevel, Integer> doInBackground(String... uri) {
				// check if cache is relatively new, skip retrieve from the
				// server
				long lastUpdated = db.ranksLastUpdated();
				if (System.currentTimeMillis() - lastUpdated > RANKS_CACHE_TIME_MS) {
					return queryForRanks(uri);
				}
				return returnCachedRanks(db, type);
			}

			private Map<AILevel, Integer> queryForRanks(String... uri) {
				Log.i(TAG, "  Querying for AI ranks...");
				HttpClient httpclient = new DefaultHttpClient();
				HttpResponse response;
				String responseString = null;
				try {
					response = httpclient.execute(new HttpGet(uri[0]));
					StatusLine statusLine = response.getStatusLine();
					if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						response.getEntity().writeTo(out);
						out.close();
						responseString = out.toString();
					} else {
						// Closes the connection.
						response.getEntity().getContent().close();
						Log.w(TAG,
								"Couldn't retrieve ranks from server, because '"
										+ statusLine.getReasonPhrase() + "'");
					}
				} catch (Exception e) {
					Log.w(TAG,
							"Couldn't retrieve ranks from server, caught an exception "
									+ e.getMessage());
				}

				if (responseString == null) {
					return returnCachedRanks(db, type);
				}
				return storeRanksAndReturn(db, responseString, type);
			}

			@Override
			protected void onPostExecute(Map<AILevel, Integer> ranks) {
				onRetrieved.onSuccess(ranks);
			}

		};
		task.execute(QUERY_URL_STRING);
	}

	private static Map<AILevel, Integer> returnCachedRanks(DatabaseHandler db,
			GameType type) {
		Log.i(TAG, "  Retrieving cached AI ranks...");
		return db.getRanks(type);
	}

	private static Map<AILevel, Integer> storeRanksAndReturn(
			DatabaseHandler db, String result, GameType type) {
		Log.i(TAG, "  interpretting AI rank query results");

		Map<GameType, Map<AILevel, Integer>> aiRanksByGameType = new HashMap<GameType, Map<AILevel, Integer>>();
		try {
			JSONObject object = new JSONObject(result);
			JSONArray random = object.getJSONArray(AILevel.RANDOM_AI.getValue()
					+ "");
			JSONArray easy = object.getJSONArray(AILevel.EASY_AI.getValue()
					+ "");
			JSONArray medium = object.getJSONArray(AILevel.MEDIUM_AI.getValue()
					+ "");
			JSONArray hard = object.getJSONArray(AILevel.HARD_AI.getValue()
					+ "");

			HashMap<AILevel, Integer> junior = new HashMap<AILevel, Integer>();
			junior.put(AILevel.RANDOM_AI, random.getInt(0));
			junior.put(AILevel.EASY_AI, easy.getInt(0));
			junior.put(AILevel.MEDIUM_AI, medium.getInt(0));
			junior.put(AILevel.HARD_AI, hard.getInt(0));
			aiRanksByGameType.put(GameType.JUNIOR, junior);

			HashMap<AILevel, Integer> normal = new HashMap<AILevel, Integer>();
			normal.put(AILevel.RANDOM_AI, random.getInt(1));
			normal.put(AILevel.EASY_AI, easy.getInt(1));
			normal.put(AILevel.MEDIUM_AI, medium.getInt(1));
			normal.put(AILevel.HARD_AI, hard.getInt(1));
			aiRanksByGameType.put(GameType.NORMAL, normal);

			HashMap<AILevel, Integer> strict = new HashMap<AILevel, Integer>();
			strict.put(AILevel.RANDOM_AI, random.getInt(2));
			strict.put(AILevel.EASY_AI, easy.getInt(2));
			strict.put(AILevel.MEDIUM_AI, medium.getInt(2));
			strict.put(AILevel.HARD_AI, hard.getInt(2));
			aiRanksByGameType.put(GameType.STRICT, strict);

			Log.i(TAG, "  updating DB stored AI ranks");
			db.updateRanks(aiRanksByGameType);
		} catch (Exception e) {
			Log.e(TAG, "Error reading JSON result from '" + result + "'", e);
		}

		Log.i(TAG, "  querying DB for AI rank");
		return db.getRanks(type);
	}
}
