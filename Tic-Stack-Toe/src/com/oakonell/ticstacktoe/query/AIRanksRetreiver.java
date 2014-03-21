package com.oakonell.ticstacktoe.query;

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
import com.oakonell.ticstacktoe.model.solver.AILevel;
import com.oakonell.ticstacktoe.model.solver.AiPlayerStrategy;

public class AIRanksRetreiver {
	private static final long RANKS_CACHE_TIME_MS = TimeUnit.DAYS.toMillis(1);
	private final static String URL_STRING = "http://ticstacktoe.appspot.com/airanks";
	private static final String TAG = AIRanksRetreiver.class.getName();

	public interface OnRanksRetrieved {
		void onSuccess(Map<AILevel, Integer> ranks);
	}

	public static void retrieveRanks(DatabaseHandler db, GameType type,
			OnRanksRetrieved onRetrieved) {
		AIRanksRetreiver newMe = new AIRanksRetreiver();
		newMe.privateRetrieveRanks(db, type, onRetrieved);
	}

	private void privateRetrieveRanks(final DatabaseHandler db,
			final GameType type, final OnRanksRetrieved onRetrieved) {
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
		task.execute(URL_STRING);
	}

	private Map<AILevel, Integer> returnCachedRanks(DatabaseHandler db,
			GameType type) {
		return db.getRanks(type);
	}

	private Map<AILevel, Integer> storeRanksAndReturn(DatabaseHandler db,
			String result, GameType type) {

		Map<GameType, Map<AILevel, Integer>> aiRanksByGameType = new HashMap<GameType, Map<AILevel, Integer>>();
		try {
			JSONObject object = new JSONObject(result);
			JSONArray random = object.getJSONArray(AiPlayerStrategy.RANDOM_AI
					+ "");
			JSONArray easy = object.getJSONArray(AiPlayerStrategy.EASY_AI + "");
			JSONArray medium = object.getJSONArray(AiPlayerStrategy.MEDIUM_AI
					+ "");
			JSONArray hard = object.getJSONArray(AiPlayerStrategy.HARD_AI + "");

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

			db.updateRanks(aiRanksByGameType);
		} catch (Exception e) {
			Log.e(TAG, "Error reading JSON result from '" + result + "'", e);
		}

		return db.getRanks(type);
	}
}
