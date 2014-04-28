package com.oakonell.ticstacktoe.model.db;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.solver.AILevel;
import com.oakonell.ticstacktoe.ui.local.AiMatchInfo;
import com.oakonell.ticstacktoe.ui.local.LocalMatchInfo;
import com.oakonell.ticstacktoe.ui.local.LocalMatchInfo.LocalMatchVisitor;
import com.oakonell.ticstacktoe.ui.local.PassNPlayMatchInfo;
import com.oakonell.ticstacktoe.ui.local.tutorial.TutorialMatchInfo;
import com.oakonell.utils.StringUtils;

public class DatabaseHandler extends SQLiteOpenHelper {
	private static final String MATCH_FILENAME_PREFIX = "match_";

	private static final String TAG = "DatabaseHandler";

	// Database Version
	private static final int DATABASE_VERSION = 10;

	// Database Name
	private static final String DATABASE_NAME = "localMatches";

	private static class TableLocalMatches {
		// Table name
		private static final String NAME = "local_matches";

		// Table Columns names
		private static final String KEY_ID = "id";
		private static final String KEY_MODE = "mode";
		private static final String KEY_MATCH_STATUS = "match_status";
		private static final String KEY_TURN_STATUS = "turn_status";

		private static final String KEY_BLACK_NAME = "black_name";
		private static final String KEY_WHITE_NAME = "white_name";
		private static final String KEY_WHITE_AI_LEVEL = "white_ai_level";

		private static final String KEY_LAST_UPDATED = "last_updated";

		private static final String KEY_FILENAME = "filename";

		private static final String KEY_REMATCH_ID = "rematch_id";

		private static final String KEY_SCORE_BLACK_WINS = "score_black_wins";
		private static final String KEY_SCORE_WHITE_WINS = "score_white_wins";
		private static final String KEY_SCORE_TOTAL_GAMES = "score_total_games";

		private static final String KEY_WINNER = "winner";
		private static final String KEY_IS_RANKED = "isRanked";

		private static final int BLACK_WON = 1;
		private static final int WHITE_WON = -1;
	}

	private static class TableAIRanks {
		private static final String NAME = "ai_ranks";

		// Table Columns names
		private static final String KEY_AI_ID = "id";
		private static final String KEY_AI_TYPE = "mode";
		private static final String KEY_AI_RANDOM_RANK = "random_rank";
		private static final String KEY_AI_EASY_RANK = "easy_rank";
		private static final String KEY_AI_MEDIUM_RANK = "medium_rank";
		private static final String KEY_AI_HARD_RANK = "hard_rank";
	}

	private static class TableAIRanksUpdated {
		private static final String NAME = "ai_ranks_updated";

		// Table Columns names
		private static final String KEY_AI_UPDATED_ID = "id";
		private static final String KEY_AI_UPDATED_UPDATED = "updated";
	}

	private final Context context;

	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TableLocalMatches.NAME
				+ "(" + TableLocalMatches.KEY_ID + " INTEGER PRIMARY KEY," //
				+ TableLocalMatches.KEY_MODE + " TEXT," //
				+ TableLocalMatches.KEY_MATCH_STATUS + " INTEGER," //
				+ TableLocalMatches.KEY_TURN_STATUS + " INTEGER," //
				+ TableLocalMatches.KEY_BLACK_NAME + " TEXT," //
				+ TableLocalMatches.KEY_WHITE_NAME + " TEXT," //
				+ TableLocalMatches.KEY_WHITE_AI_LEVEL + " INTEGER," //
				+ TableLocalMatches.KEY_LAST_UPDATED + " INTEGER," //
				+ TableLocalMatches.KEY_FILENAME + " TEXT," //

				+ TableLocalMatches.KEY_REMATCH_ID + " INTEGER," //

				+ TableLocalMatches.KEY_SCORE_BLACK_WINS + " INTEGER," //
				+ TableLocalMatches.KEY_SCORE_WHITE_WINS + " INTEGER," //
				+ TableLocalMatches.KEY_SCORE_TOTAL_GAMES + " INTEGER," //

				+ TableLocalMatches.KEY_WINNER + " INTEGER," //
				+ TableLocalMatches.KEY_IS_RANKED + " INTEGER" //

				+ ")";
		db.execSQL(CREATE_CONTACTS_TABLE);

		String CREATE_AI_RANKS_TABLE = "CREATE TABLE " + TableAIRanks.NAME
				+ "(" + TableAIRanks.KEY_AI_ID + " INTEGER PRIMARY KEY," //
				+ TableAIRanks.KEY_AI_TYPE + " TEXT," //
				+ TableAIRanks.KEY_AI_RANDOM_RANK + " INTEGER," //
				+ TableAIRanks.KEY_AI_EASY_RANK + " INTEGER," //
				+ TableAIRanks.KEY_AI_MEDIUM_RANK + " INTEGER," //
				+ TableAIRanks.KEY_AI_HARD_RANK + " INTEGER" //
				+ ")";
		db.execSQL(CREATE_AI_RANKS_TABLE);

		String CREATE_AI_RANKS_UPDATED_TABLE = "CREATE TABLE "
				+ TableAIRanksUpdated.NAME + "("
				+ TableAIRanksUpdated.KEY_AI_UPDATED_ID
				+ " INTEGER PRIMARY KEY," //
				+ TableAIRanksUpdated.KEY_AI_UPDATED_UPDATED + " INTEGER" //
				+ ")";
		db.execSQL(CREATE_AI_RANKS_UPDATED_TABLE);

	}

	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO do a better upgrade
		db.execSQL("DROP TABLE IF EXISTS " + TableAIRanks.NAME);

		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TableLocalMatches.NAME);

		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TableAIRanksUpdated.NAME);

		// delete the existing match files as well
		File dir = context.getFilesDir();
		String[] matchFiles = dir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return filename.startsWith(MATCH_FILENAME_PREFIX);
			}
		});
		for (String each : matchFiles) {
			context.deleteFile(each);
		}

		// Create tables again
		onCreate(db);
	}

	public interface OnLocalMatchDeleteListener {
		void onDeleteSuccess();

		void onDeleteFailure();
	}

	public void deleteMatch(final LocalMatchInfo matchInfo,
			final OnLocalMatchDeleteListener updateListener) {

		AsyncTask<Void, Void, Long> task = new AsyncTask<Void, Void, Long>() {
			@Override
			protected Long doInBackground(Void... params) {
				SQLiteDatabase db = getWritableDatabase();
				try {
					// deleting the row
					long id = db.delete(TableLocalMatches.NAME,
							TableLocalMatches.KEY_ID + "=?",
							new String[] { Long.toString(matchInfo.getId()) });

					// also delete the corresponding match file
					String filename = matchInfo.getFilename();
					if (!StringUtils.isEmpty(filename)) {
						boolean deleted = context.deleteFile(filename);
						if (!deleted) {
							File file = new File(context.getFilesDir(),
									filename);
							if (file.exists()) {
								Log.w(TAG, "File '" + filename
										+ "' could not be deleted");
							}
						}
					}
					return id;
				} finally {
					db.close();
				}
			}

			@Override
			protected void onPostExecute(Long id) {
				if (id < 0) {
					updateListener.onDeleteFailure();
					return;
				}
				matchInfo.setId(id);
				updateListener.onDeleteSuccess();
			}

		};
		task.execute((Void) null);

	}

	public interface OnLocalMatchUpdateListener {
		void onUpdateSuccess(LocalMatchInfo matchInfo);

		void onUpdateFailure();
	}

	public void insertMatch(final LocalMatchInfo matchInfo,
			final OnLocalMatchUpdateListener updateListener) {

		AsyncTask<Void, Void, Long> task = new AsyncTask<Void, Void, Long>() {
			@Override
			protected Long doInBackground(Void... params) {
				ContentValues values = getContentValues(matchInfo);
				SQLiteDatabase db = getWritableDatabase();
				try {
					// updating row
					long id = db.insertOrThrow(TableLocalMatches.NAME, null,
							values);

					// set and write the game bytes file
					String fileName = MATCH_FILENAME_PREFIX + id;
					matchInfo.setFileName(fileName);

					ContentValues fileNameValue = new ContentValues();
					fileNameValue.put(TableLocalMatches.KEY_FILENAME, fileName);
					int updated = db.update(TableLocalMatches.NAME,
							fileNameValue, TableLocalMatches.KEY_ID + "=?",
							new String[] { Long.toString(id) });
					if (updated != 1) {
						throw new RuntimeException(
								"Error updating match record");
					}

					// Write the game bytes file
					matchInfo.writeGame(context);
					return id;
				} finally {
					db.close();
				}
			}

			@Override
			protected void onPostExecute(Long id) {
				if (id < 0) {
					updateListener.onUpdateFailure();
					return;
				}
				matchInfo.setId(id);
				updateListener.onUpdateSuccess(matchInfo);
			}

		};
		task.execute((Void) null);
	}

	public void updateMatch(final LocalMatchInfo matchInfo,
			final OnLocalMatchUpdateListener updateListener) {
		AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... params) {
				ContentValues values = getContentValues(matchInfo);

				SQLiteDatabase db = getWritableDatabase();
				try {
					// updating row
					int updated = db.update(TableLocalMatches.NAME, values,
							TableLocalMatches.KEY_ID + " = ?",
							new String[] { String.valueOf(matchInfo.getId()) });

					// update the game bytes file
					matchInfo.writeGame(context);
					return updated;
				} finally {
					db.close();
				}
			}

			@Override
			protected void onPostExecute(Integer updated) {
				if (updated < 0) {
					updateListener.onUpdateFailure();
					return;
				}

				updateListener.onUpdateSuccess(matchInfo);
			}

		};
		task.execute((Void) null);
	}

	public interface OnLocalMatchReadListener {
		void onReadSuccess(LocalMatchInfo matchInfo);

		void onReadFailure();
	}

	public void getMatch(final long id, final OnLocalMatchReadListener listener) {
		AsyncTask<Void, Void, LocalMatchInfo> task = new AsyncTask<Void, Void, LocalMatchInfo>() {
			@Override
			protected LocalMatchInfo doInBackground(Void... params) {
				SQLiteDatabase db = getReadableDatabase();
				String[] columnsNames = getMatchColumnNames();
				Cursor query = db.query(TableLocalMatches.NAME, columnsNames,
						TableLocalMatches.KEY_ID + "=?",
						new String[] { Long.toString(id) }, null, null, null);
				try {
					if (!query.moveToFirst()) {
						return null;
					}
					LocalMatchInfo match = readMatch(query);
					match.readGame(context);
					return match;
				} finally {
					if (query != null) {
						query.close();
					}
					db.close();
				}

			}

			@Override
			protected void onPostExecute(LocalMatchInfo updated) {
				if (updated == null) {
					listener.onReadFailure();
					return;
				}
				listener.onReadSuccess(updated);
			}

		};
		task.execute((Void) null);

	}

	private String[] getMatchColumnNames() {
		String columnsNames[] = new String[] { TableLocalMatches.KEY_ID,
				TableLocalMatches.KEY_MODE, TableLocalMatches.KEY_MATCH_STATUS,
				TableLocalMatches.KEY_TURN_STATUS,
				TableLocalMatches.KEY_BLACK_NAME,
				TableLocalMatches.KEY_WHITE_NAME,
				TableLocalMatches.KEY_WHITE_AI_LEVEL,
				TableLocalMatches.KEY_LAST_UPDATED,
				TableLocalMatches.KEY_FILENAME,

				TableLocalMatches.KEY_REMATCH_ID,

				TableLocalMatches.KEY_SCORE_BLACK_WINS,
				TableLocalMatches.KEY_SCORE_WHITE_WINS,
				TableLocalMatches.KEY_SCORE_TOTAL_GAMES,
				TableLocalMatches.KEY_IS_RANKED,

				TableLocalMatches.KEY_WINNER };
		return columnsNames;
	}

	private ContentValues getContentValues(LocalMatchInfo matchInfo) {
		final ContentValues values = new ContentValues();
		values.put(TableLocalMatches.KEY_MATCH_STATUS,
				matchInfo.getMatchStatus());
		values.put(TableLocalMatches.KEY_TURN_STATUS, matchInfo.getTurnStatus());

		values.put(TableLocalMatches.KEY_BLACK_NAME, matchInfo.getBlackName());
		values.put(TableLocalMatches.KEY_WHITE_NAME, matchInfo.getWhiteName());

		values.put(TableLocalMatches.KEY_LAST_UPDATED,
				System.currentTimeMillis());

		values.put(TableLocalMatches.KEY_FILENAME, matchInfo.getFilename());

		values.put(TableLocalMatches.KEY_REMATCH_ID, matchInfo.getRematchId());

		ScoreCard score = matchInfo.getScoreCard();

		values.put(TableLocalMatches.KEY_SCORE_BLACK_WINS, score.getBlackWins());
		values.put(TableLocalMatches.KEY_SCORE_WHITE_WINS, score.getWhiteWins());
		values.put(TableLocalMatches.KEY_SCORE_TOTAL_GAMES,
				score.getTotalGames());

		values.put(TableLocalMatches.KEY_IS_RANKED, matchInfo.isRanked() ? 1
				: 0);
		values.put(TableLocalMatches.KEY_FILENAME, matchInfo.getFilename());

		Game game = matchInfo.readGame(context);
		int winner = 0;
		Player winnerPlayer = game.getBoard().getState().getWinner();
		if (winnerPlayer != null) {
			if (winnerPlayer.isBlack()) {
				winner = TableLocalMatches.BLACK_WON;
			} else {
				winner = TableLocalMatches.WHITE_WON;
			}
		}
		values.put(TableLocalMatches.KEY_WINNER, winner);

		LocalMatchVisitor visitor = new LocalMatchVisitor() {

			@Override
			public void visitPassNPlay(PassNPlayMatchInfo info) {
				values.put(TableLocalMatches.KEY_MODE,
						GameMode.PASS_N_PLAY.getVal());
			}

			@Override
			public void visitAi(AiMatchInfo info) {
				values.put(TableLocalMatches.KEY_MODE, GameMode.AI.getVal());
				values.put(TableLocalMatches.KEY_WHITE_AI_LEVEL, info
						.getWhiteAILevel().getValue());

			}

			@Override
			public void visitTutorial(TutorialMatchInfo info) {
				values.put(TableLocalMatches.KEY_MODE,
						GameMode.TUTORIAL.getVal());
			}

		};
		matchInfo.accept(visitor);

		return values;
	}

	public interface OnLocalMatchesLoadListener {
		void onLoadSuccess(LocalMatchesBuffer localMatchesBuffer);

		void onLoadFailure();
	}

	public static class LocalMatchesBuffer {
		private List<LocalMatchInfo> myTurn;
		private List<LocalMatchInfo> theirTurn;
		private List<LocalMatchInfo> completedMatches;

		public LocalMatchesBuffer(List<LocalMatchInfo> myTurn,
				List<LocalMatchInfo> theirTurn,
				List<LocalMatchInfo> completedMatches) {
			this.myTurn = myTurn;
			this.theirTurn = theirTurn;
			this.completedMatches = completedMatches;
		}

		public List<LocalMatchInfo> getMyTurn() {
			return myTurn;
		}

		public List<LocalMatchInfo> getTheirTurn() {
			return theirTurn;
		}

		public List<LocalMatchInfo> getCompletedMatches() {
			return completedMatches;
		}

	}

	public void getMatches(final OnLocalMatchesLoadListener listener) {
		AsyncTask<Void, Void, LocalMatchesBuffer> task = new AsyncTask<Void, Void, LocalMatchesBuffer>() {
			@Override
			protected LocalMatchesBuffer doInBackground(Void... params) {
				String[] columnsNames = getMatchColumnNames();

				SQLiteDatabase db = getReadableDatabase();
				Cursor cursor = null;
				try {
					Log.i("DB", "getting matches");
					cursor = db.query(TableLocalMatches.NAME, columnsNames,
							null, null, null, null, null);

					List<LocalMatchInfo> myTurns = new ArrayList<LocalMatchInfo>();
					List<LocalMatchInfo> theirTurns = new ArrayList<LocalMatchInfo>();
					List<LocalMatchInfo> completed = new ArrayList<LocalMatchInfo>();

					// looping through all rows and adding to list
					if (cursor.moveToFirst()) {
						do {
							LocalMatchInfo readMatch = readMatch(cursor);
							if (readMatch.getMatchStatus() == TurnBasedMatch.MATCH_STATUS_COMPLETE) {
								completed.add(readMatch);
							} else if (readMatch.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
								myTurns.add(readMatch);
							} else {
								theirTurns.add(readMatch);
							}
						} while (cursor.moveToNext());
					}
					LocalMatchesBuffer buffer = new LocalMatchesBuffer(myTurns,
							theirTurns, completed);
					return buffer;
				} finally {
					if (cursor != null) {
						cursor.close();
					}
					db.close();
				}

			}

			@Override
			protected void onPostExecute(LocalMatchesBuffer buffer) {
				if (buffer == null) {
					listener.onLoadFailure();
					return;
				}
				Log.i("DB", "success");
				listener.onLoadSuccess(buffer);
			}

		};
		task.execute((Void) null);

	}

	private LocalMatchInfo readMatch(Cursor query) {
		long id = query.getLong(query.getColumnIndex(TableLocalMatches.KEY_ID));
		int modeNum = query.getInt(query
				.getColumnIndex(TableLocalMatches.KEY_MODE));
		int matchStatus = query.getInt(query
				.getColumnIndex(TableLocalMatches.KEY_MATCH_STATUS));
		int turnStatus = query.getInt(query
				.getColumnIndex(TableLocalMatches.KEY_TURN_STATUS));
		String blackName = query.getString(query
				.getColumnIndex(TableLocalMatches.KEY_BLACK_NAME));
		String whiteName = query.getString(query
				.getColumnIndex(TableLocalMatches.KEY_WHITE_NAME));
		int aiLevelInt = query.getInt(query
				.getColumnIndex(TableLocalMatches.KEY_WHITE_AI_LEVEL));
		AILevel aiLevel = null;
		if (aiLevelInt != 0) {
			aiLevel = AILevel.fromValue(aiLevelInt);
		}
		long lastUpdated = query.getLong(query
				.getColumnIndex(TableLocalMatches.KEY_LAST_UPDATED));
		String fileName = query.getString(query
				.getColumnIndex(TableLocalMatches.KEY_FILENAME));

		if (fileName == null) {
			Log.e(TAG, "Got a null file name?");
		}

		long rematchId = query.getLong(query
				.getColumnIndex(TableLocalMatches.KEY_REMATCH_ID));
		int blackWins = query.getInt(query
				.getColumnIndex(TableLocalMatches.KEY_SCORE_BLACK_WINS));
		int whiteWins = query.getInt(query
				.getColumnIndex(TableLocalMatches.KEY_SCORE_WHITE_WINS));
		int totalGames = query.getInt(query
				.getColumnIndex(TableLocalMatches.KEY_SCORE_TOTAL_GAMES));
		boolean isRanked = query.getInt(query
				.getColumnIndex(TableLocalMatches.KEY_IS_RANKED)) != 0;

		ScoreCard score = new ScoreCard(blackWins, whiteWins, totalGames
				- (blackWins + whiteWins));

		int winner = query.getInt(query
				.getColumnIndex(TableLocalMatches.KEY_WINNER));

		GameMode mode = GameMode.fromValue(modeNum);

		return LocalMatchInfo.createLocalMatch(mode, id, matchStatus,
				turnStatus, blackName, whiteName, aiLevel, lastUpdated,
				fileName, score, rematchId, winner, isRanked);
	}

	public Map<AILevel, Integer> getCachedAiRanks(GameType type) {
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = null;
		try {
			Log.i("DB", "getting locally cached AI ranks");
			String[] columnsNames = new String[] { TableAIRanks.KEY_AI_TYPE,
					TableAIRanks.KEY_AI_RANDOM_RANK,
					TableAIRanks.KEY_AI_EASY_RANK,
					TableAIRanks.KEY_AI_MEDIUM_RANK,
					TableAIRanks.KEY_AI_HARD_RANK };
			cursor = db.query(TableAIRanks.NAME, columnsNames,
					TableAIRanks.KEY_AI_TYPE + " = ?",
					new String[] { type.getVariant() + "" }, null, null, null);

			if (cursor.moveToFirst()) {
				int randomRank = cursor.getInt(cursor
						.getColumnIndex(TableAIRanks.KEY_AI_RANDOM_RANK));
				int easyRank = cursor.getInt(cursor
						.getColumnIndex(TableAIRanks.KEY_AI_EASY_RANK));
				int mediumRank = cursor.getInt(cursor
						.getColumnIndex(TableAIRanks.KEY_AI_MEDIUM_RANK));
				int hardRank = cursor.getInt(cursor
						.getColumnIndex(TableAIRanks.KEY_AI_HARD_RANK));

				Map<AILevel, Integer> ranks = new HashMap<AILevel, Integer>();
				ranks.put(AILevel.RANDOM_AI, randomRank);
				ranks.put(AILevel.EASY_AI, easyRank);
				ranks.put(AILevel.MEDIUM_AI, mediumRank);
				ranks.put(AILevel.HARD_AI, hardRank);
				return ranks;
			} else {
				Log.e("DB", "There were no ranks in the DB");
				Map<AILevel, Integer> ranks = new HashMap<AILevel, Integer>();
				ranks.put(AILevel.RANDOM_AI, 600);
				ranks.put(AILevel.EASY_AI, 1400);
				ranks.put(AILevel.MEDIUM_AI, 1800);
				ranks.put(AILevel.HARD_AI, 2000);
				return ranks;
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
			db.close();
		}

	}

	public void updateCachedAiRanks(
			Map<GameType, Map<AILevel, Integer>> aiRanksByGameType) {

		SQLiteDatabase db = getWritableDatabase();
		try {
			Log.i("DB", "Updating locally cached AI ranks");
			for (Entry<GameType, Map<AILevel, Integer>> entry : aiRanksByGameType
					.entrySet()) {
				GameType type = entry.getKey();
				Map<AILevel, Integer> ranks = entry.getValue();

				// updating row
				ContentValues values = new ContentValues();
				values.put(TableAIRanks.KEY_AI_TYPE, type.getVariant());
				values.put(TableAIRanks.KEY_AI_RANDOM_RANK,
						ranks.get(AILevel.RANDOM_AI));
				values.put(TableAIRanks.KEY_AI_EASY_RANK,
						ranks.get(AILevel.EASY_AI));
				values.put(TableAIRanks.KEY_AI_MEDIUM_RANK,
						ranks.get(AILevel.MEDIUM_AI));
				values.put(TableAIRanks.KEY_AI_HARD_RANK,
						ranks.get(AILevel.HARD_AI));
				// try to update an existing row
				int updated = db.update(TableAIRanks.NAME, values,
						TableAIRanks.KEY_AI_TYPE + " = ?",
						new String[] { String.valueOf(type.getVariant()) });
				if (updated != 1) {
					// if it didn't exist, insert a new one
					long id = db.insert(TableAIRanks.NAME, null, values);
					if (id < 0) {
						// TODO throw/report an error
						Log.e(TAG, "Error updating/inserting AI rank");
					}
				}
			}

			ContentValues values = new ContentValues();
			values.put(TableAIRanksUpdated.KEY_AI_UPDATED_UPDATED,
					System.currentTimeMillis());
			// just update 'all' rows- there should be only one
			int updated = db.update(TableAIRanksUpdated.NAME, values, null,
					null);
			if (updated < 1) {
				// if one didn't exist to update, insert it now
				long id = db.insert(TableAIRanksUpdated.NAME, null, values);
				if (id < 0) {
					// TODO throw/report an error
					Log.e(TAG,
							"Error updating/inserting the AI rank updated record");
				}
			}

		} finally {
			db.close();
		}

	}

	public long aiRanksLastUpdated() {
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = null;
		try {
			Log.i("DB", "getting when ranks were updated");
			String[] columnsNames = new String[] { TableAIRanksUpdated.KEY_AI_UPDATED_UPDATED };
			cursor = db.query(TableAIRanksUpdated.NAME, columnsNames, null,
					null, null, null, null);

			if (cursor.moveToFirst()) {
				long lastUpdated = cursor
						.getLong(cursor
								.getColumnIndex(TableAIRanksUpdated.KEY_AI_UPDATED_UPDATED));
				return lastUpdated;
			} else {
				Log.e("DB", "There was no last updated in the DB");
				return 0;
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
			db.close();
		}
	}

}
