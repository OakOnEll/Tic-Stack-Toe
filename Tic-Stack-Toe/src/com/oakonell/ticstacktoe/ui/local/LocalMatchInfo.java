package com.oakonell.ticstacktoe.ui.local;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.text.format.DateUtils;

import com.google.analytics.tracking.android.Log;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler.OnLocalMatchDeleteListener;
import com.oakonell.ticstacktoe.ui.game.HumanStrategy;
import com.oakonell.ticstacktoe.ui.menu.MatchAdapter.ItemExecute;
import com.oakonell.ticstacktoe.ui.menu.MatchAdapter.MatchMenuItem;
import com.oakonell.ticstacktoe.ui.menu.MatchInfo;
import com.oakonell.ticstacktoe.ui.menu.MenuFragment;
import com.oakonell.ticstacktoe.utils.ByteBufferDebugger;

public class LocalMatchInfo implements MatchInfo {

	private long id;

	private GameMode mode;
	private int matchStatus;
	private int turnStatus;
	private String blackName;
	private String whiteName;
	private int aiLevel;
	private long lastUpdated;
	private String fileName;
	private Game game;

	public LocalMatchInfo(long id, GameMode mode, int matchStatus,
			int turnStatus, String blackName, String whiteName, int aiLevel,
			long lastUpdated, String fileName) {
		this.id = id;
		this.mode = mode;
		this.matchStatus = matchStatus;
		this.turnStatus = turnStatus;
		this.blackName = blackName;
		this.whiteName = whiteName;
		this.aiLevel = aiLevel;
		this.lastUpdated = lastUpdated;
		this.fileName = fileName;
	}

	public LocalMatchInfo(int matchStatus, int turnStatus, String blackName,
			String whiteName, int aiLevel, long lastUpdated, Game game) {
		this.mode = game.getMode();
		this.matchStatus = matchStatus;
		this.turnStatus = turnStatus;
		this.blackName = blackName;
		this.whiteName = whiteName;
		this.aiLevel = aiLevel;
		this.lastUpdated = lastUpdated;
		this.game = game;
	}

	@Override
	public CharSequence getText(Context context) {
		if (matchStatus == TurnBasedMatch.MATCH_STATUS_COMPLETE) {
			// TODO display who won!
			return blackName + " vs. " + whiteName;
		}
		if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
			return blackName;
		}
		return whiteName;
	}

	@Override
	public CharSequence getSubtext(Context context) {
		CharSequence timeSpanString = DateUtils.getRelativeDateTimeString(
				context, lastUpdated, DateUtils.MINUTE_IN_MILLIS,
				DateUtils.WEEK_IN_MILLIS, 0);

		return "Local " + blackName + " vs. " + whiteName + " last played "
				+ timeSpanString;
	}

	@Override
	public long getUpdatedTimestamp() {
		return lastUpdated;
	}

	@Override
	public Uri getIconImageUri() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<MatchMenuItem> getMenuItems() {
		List<MatchMenuItem> result = new ArrayList<MatchMenuItem>();
		MatchMenuItem dismiss = new MatchMenuItem("Dismiss", new ItemExecute() {
			@Override
			public void execute(final MenuFragment fragment,
					List<MatchInfo> matches) {
				DatabaseHandler dbHandler = fragment.getDbHandler();

				dbHandler.deleteMatch(LocalMatchInfo.this,
						new OnLocalMatchDeleteListener() {
							@Override
							public void onDeleteSuccess() {
							}

							@Override
							public void onDeleteFailure() {
								fragment.showAlert("Error deleting match");
							}
						});

				matches.remove(LocalMatchInfo.this);
			}
		});
		result.add(dismiss);

		// if (canRematch) {
		// MatchMenuItem rematch = new MatchMenuItem();
		// rematch.text = "Rematch";
		// rematch.execute = new ItemExecute() {
		// @Override
		// public void execute(final MenuFragment fragment,
		// List<MatchInfo> matches) {
		// fragment.setInactive();
		// GamesClient gamesClient = fragment.getMainActivity()
		// .getGamesClient();
		// gamesClient.rematchTurnBasedMatch(
		// new OnTurnBasedMatchInitiatedListener() {
		// @Override
		// public void onTurnBasedMatchInitiated(
		// int status, TurnBasedMatch match) {
		// if (status != GamesClient.STATUS_OK) {
		// fragment.getMainActivity()
		// .getGameHelper()
		// .showAlert(
		// "Error starting rematch");
		// fragment.refreshMatches();
		// fragment.setActive();
		// return;
		// }
		// fragment.showMatch(match.getMatchId());
		// }
		// }, matchId);
		// }
		// };
		// result.add(rematch);
		// }

		return result;
	}

	@Override
	public void onClick(MenuFragment fragment) {
		fragment.showLocalMatch(this);
	}

	public GameMode getMode() {
		return mode;
	}

	public int getMatchStatus() {
		return matchStatus;
	}

	public int getTurnStatus() {
		return turnStatus;
	}

	public String getBlackName() {
		return blackName;
	}

	public String getWhiteName() {
		return whiteName;
	}

	public int getWhiteAILevel() {
		return aiLevel;
	}

	public String getFilename() {
		return fileName;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void writeGame(Context context) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 1024);
		ByteBufferDebugger buffer = new ByteBufferDebugger(byteBuffer);
		game.writeBytes("", buffer);

		FileOutputStream out = null;
		try {
			out = context.openFileOutput(fileName, 0);
			out.write(byteBuffer.array());
		} catch (IOException e) {
			throw new RuntimeException("Can't write local match file", e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					Log.e("Error closing file '" + fileName + "':"
							+ e.getMessage());
				}
			}
		}
	}

	public Game readGame(Context context) {
		if (game != null) {
			return game;
		}
		ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 1024);
		ByteBufferDebugger buffer = new ByteBufferDebugger(byteBuffer);

		Player blackPlayer = HumanStrategy.createPlayer(blackName, true);
		Player whitePlayer = HumanStrategy.createPlayer(whiteName, false);

		FileInputStream in = null;
		try {
			in = context.openFileInput(fileName);
			in.read(byteBuffer.array());

			Player currentPlayer;
			if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
				currentPlayer = blackPlayer;
			} else {
				currentPlayer = whitePlayer;
			}

			game = Game.fromBytes(blackPlayer, whitePlayer, currentPlayer,
					buffer);
			return game;
		} catch (IOException e) {
			throw new RuntimeException("Can't write local match file", e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					Log.e("Error closing file '" + fileName + "': "
							+ e.getMessage());
				}
			}
		}
	}

	public void setTurnStatus(int status) {
		this.turnStatus = status;
	}

	public void setMatchStatus(int matchStatus) {
		this.matchStatus = matchStatus;
	}
}
