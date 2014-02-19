package com.oakonell.ticstacktoe.model.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.ui.menu.LocalMatchInfo;

public class DatabaseHandler extends SQLiteOpenHelper {

	// All Static variables
	// Database Version
	private static final int DATABASE_VERSION = 4;

	// Database Name
	private static final String DATABASE_NAME = "localMatches";

	// Contacts table name
	private static final String TABLE_LOCAL_MATCHES = "local_matches";

	// Contacts Table Columns names
	private static final String KEY_ID = "id";
	private static final String KEY_MODE = "mode";
	private static final String KEY_MATCH_STATUS = "match_status";
	private static final String KEY_TURN_STATUS = "turn_status";

	private static final String KEY_BLACK_NAME = "black_name";
	private static final String KEY_WHITE_NAME = "white_name";
	private static final String KEY_WHITE_AI_LEVEL = "white_ai_level";

	private static final String KEY_LAST_UPDATED = "last_updated";

	private static final String KEY_FILENAME = "filename";

	private final Context context;

	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_LOCAL_MATCHES
				+ "(" + KEY_ID + " INTEGER PRIMARY KEY," //
				+ KEY_MODE + " TEXT," //
				+ KEY_MATCH_STATUS + " INTEGER," //
				+ KEY_TURN_STATUS + " INTEGER," //
				+ KEY_BLACK_NAME + " TEXT," //
				+ KEY_WHITE_NAME + " TEXT," //
				+ KEY_WHITE_AI_LEVEL + " INTEGER," //
				+ KEY_LAST_UPDATED + " INTEGER," //
				+ KEY_FILENAME + " TEXT" //
				+ ")";
		db.execSQL(CREATE_CONTACTS_TABLE);
	}

	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCAL_MATCHES);

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
					// updating row
					long id = db.delete(TABLE_LOCAL_MATCHES, KEY_ID + "=?",
							new String[] { Long.toString(matchInfo.getId()) });
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
					long id = db.insertOrThrow(TABLE_LOCAL_MATCHES, null,
							values);

					// write file name
					String fileName = "match_" + id;
					matchInfo.setFileName(fileName);
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
					int updated = db.update(TABLE_LOCAL_MATCHES, values, KEY_ID
							+ " = ?",
							new String[] { String.valueOf(matchInfo.getId()) });
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
				String columnsNames[] = new String[] { KEY_ID, KEY_MODE,
						KEY_MATCH_STATUS, KEY_TURN_STATUS, KEY_BLACK_NAME,
						KEY_WHITE_NAME, KEY_WHITE_AI_LEVEL, KEY_LAST_UPDATED,
						KEY_FILENAME };
				Cursor query = db.query(TABLE_LOCAL_MATCHES, columnsNames,
						KEY_ID + "=?", new String[] { Long.toString(id) },
						null, null, null);
				try {
					if (!query.moveToFirst()) {
						return null;
					}
					return readMatch(query);
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

	private ContentValues getContentValues(LocalMatchInfo matchInfo) {
		ContentValues values = new ContentValues();
		values.put(KEY_MODE, matchInfo.getMode() == GameMode.AI ? 1 : 0);
		values.put(KEY_MATCH_STATUS, matchInfo.getMatchStatus());
		values.put(KEY_TURN_STATUS, matchInfo.getTurnStatus());

		values.put(KEY_BLACK_NAME, matchInfo.getBlackName());
		values.put(KEY_WHITE_NAME, matchInfo.getWhiteName());

		values.put(KEY_WHITE_AI_LEVEL, matchInfo.getWhiteAILevel());
		values.put(KEY_LAST_UPDATED, System.currentTimeMillis());

		values.put(KEY_FILENAME, matchInfo.getFilename());
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
				String columnsNames[] = new String[] { KEY_ID, KEY_MODE,
						KEY_MATCH_STATUS, KEY_TURN_STATUS, KEY_BLACK_NAME,
						KEY_WHITE_NAME, KEY_WHITE_AI_LEVEL, KEY_LAST_UPDATED,
						KEY_FILENAME };

				SQLiteDatabase db = getReadableDatabase();
				Cursor cursor = null;
				try {
					Log.i("DB", "getting matches");
					cursor = db.query(TABLE_LOCAL_MATCHES, columnsNames,
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
							} else  if (readMatch.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
								myTurns.add(readMatch);
							} else {
								theirTurns.add(readMatch);
							}
						} while (cursor.moveToNext());
					}
					LocalMatchesBuffer buffer = new LocalMatchesBuffer(myTurns,
							theirTurns, completed);

					// return contact list
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
		long id = query.getLong(query.getColumnIndex(KEY_ID));
		int modeNum = query.getInt(query.getColumnIndex(KEY_MODE));
		int matchStatus = query.getInt(query.getColumnIndex(KEY_MATCH_STATUS));
		int turnStatus = query.getInt(query.getColumnIndex(KEY_TURN_STATUS));
		String blackName = query
				.getString(query.getColumnIndex(KEY_BLACK_NAME));
		String whiteName = query
				.getString(query.getColumnIndex(KEY_WHITE_NAME));
		int aiLevel = query.getInt(query.getColumnIndex(KEY_WHITE_AI_LEVEL));
		long lastupdated = query
				.getLong(query.getColumnIndex(KEY_LAST_UPDATED));
		String fileName = query.getString(query.getColumnIndex(KEY_FILENAME));

		GameMode mode = modeNum == 1 ? GameMode.AI : GameMode.PASS_N_PLAY;
		return new LocalMatchInfo(id, mode, matchStatus, turnStatus, blackName,
				whiteName, aiLevel, lastupdated, fileName);
	}
}
