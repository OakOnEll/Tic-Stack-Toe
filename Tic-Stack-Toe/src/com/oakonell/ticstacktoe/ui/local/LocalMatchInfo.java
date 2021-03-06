package com.oakonell.ticstacktoe.ui.local;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.google.analytics.tracking.android.Log;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.GameContext;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler.OnLocalMatchDeleteListener;
import com.oakonell.ticstacktoe.model.solver.AILevel;
import com.oakonell.ticstacktoe.ui.DismissHelper;
import com.oakonell.ticstacktoe.ui.game.HumanStrategy;
import com.oakonell.ticstacktoe.ui.local.tutorial.TutorialMatchInfo;
import com.oakonell.ticstacktoe.ui.menu.GameTypeSpinnerHelper;
import com.oakonell.ticstacktoe.ui.menu.MatchAdapter;
import com.oakonell.ticstacktoe.ui.menu.MatchAdapter.ItemExecute;
import com.oakonell.ticstacktoe.ui.menu.MatchAdapter.MatchMenuItem;
import com.oakonell.ticstacktoe.ui.menu.MatchInfo;
import com.oakonell.ticstacktoe.ui.menu.MenuFragment;
import com.oakonell.ticstacktoe.utils.ByteBufferDebugger;

public abstract class LocalMatchInfo implements MatchInfo {

	private long id;

	private int matchStatus;
	private int turnStatus;
	private String blackName;
	private String whiteName;
	private long lastUpdated;
	private String fileName;
	private Game game;

	private long rematchId;

	private ScoreCard scoreCard;
	private int winner;

	private GameType type;

	public interface LocalMatchVisitor {
		void visitPassNPlay(PassNPlayMatchInfo info);

		void visitAi(AiMatchInfo info);

		void visitTutorial(TutorialMatchInfo info);
	}

	public abstract void accept(LocalMatchVisitor visitor);

	protected LocalMatchInfo(long id, GameType type, int matchStatus,
			int turnStatus, String blackName, String whiteName,
			long lastUpdated, String fileName, ScoreCard score, long rematchId,
			int winner) {
		this.id = id;
		this.matchStatus = matchStatus;
		this.turnStatus = turnStatus;
		this.blackName = blackName;
		this.whiteName = whiteName;
		this.lastUpdated = lastUpdated;
		this.fileName = fileName;

		this.rematchId = rematchId;
		this.scoreCard = score;
		this.winner = winner;
		this.type = type;
	}

	protected LocalMatchInfo(int matchStatus, int turnStatus, String blackName,
			String whiteName, Game game, ScoreCard score) {
		this.matchStatus = matchStatus;
		this.turnStatus = turnStatus;
		this.blackName = blackName;
		this.whiteName = whiteName;
		this.lastUpdated = System.currentTimeMillis();
		this.game = game;

		this.scoreCard = score;

		// TODO ?validate that the winner equals the game's state's winner
	}

	@Override
	public CharSequence getText(Context context) {
		if (matchStatus == TurnBasedMatch.MATCH_STATUS_COMPLETE) {
			if (winner == 1) {
				return blackWon(context);
			} else if (winner == -1) {
				return whiteWon(context);
			}
			return context.getString(R.string.draw);
		}
		if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
			return blackName;
		}
		return whiteName;
	}

	abstract protected CharSequence whiteWon(Context context);

	abstract protected CharSequence blackWon(Context context);

	@Override
	public CharSequence getSubtext(Context context) {
		CharSequence typeString = GameTypeSpinnerHelper.getTypeName(context,
				type);
		if (isRanked()) {
			return context.getString(R.string.local_ranked_match_subtext,
					typeString, blackName, whiteName);
		}
		return context.getString(R.string.local_unranked_match_subtext,
				typeString, blackName, whiteName);
	}

	@Override
	public CharSequence getUpdatedText(Context context) {
		CharSequence timeSpanString = MatchUtils.getTimeSince(context,
				lastUpdated);
		String lastPlayed;
		if (matchStatus == TurnBasedMatch.MATCH_STATUS_COMPLETE) {
			lastPlayed = context.getString(R.string.local_completed_match_text,
					timeSpanString);
			// + "("
			// + getScoreCard().getBlackWins() + " / "
			// + getScoreCard().getWhiteWins() + ")";
		} else {
			lastPlayed = context.getString(R.string.local_played_last_text,
					timeSpanString);
		}
		return lastPlayed;
	}

	@Override
	public long getUpdatedTimestamp() {
		return lastUpdated;
	}

	@Override
	public List<MatchMenuItem> getMenuItems(Context context) {
		List<MatchMenuItem> result = new ArrayList<MatchMenuItem>();
		MatchMenuItem dismiss = new MatchMenuItem(
				context.getString(R.string.dismiss), new ItemExecute() {
					@Override
					public void execute(final MenuFragment fragment,
							List<MatchInfo> matches, MatchAdapter adapter) {
						DismissHelper.dismiss(fragment, LocalMatchInfo.this,
								matches, adapter);
					}
				});
		result.add(dismiss);
		MatchInfo.MatchUtils.addDismissThisAndOlder(context, result, this);

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
		Player whitePlayer = createWhitePlayerStrategy();

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

	abstract protected Player createWhitePlayerStrategy();

	public void setTurnStatus(int status) {
		this.turnStatus = status;
	}

	public void setMatchStatus(int matchStatus) {
		this.matchStatus = matchStatus;
	}

	public abstract AbstractLocalStrategy createStrategy(GameContext context);

	public static LocalMatchInfo createLocalMatch(GameMode mode, GameType type,
			long id, int matchStatus, int turnStatus, String blackName,
			String whiteName, AILevel aiLevel, long lastUpdated,
			String fileName, ScoreCard score, long rematchId, int winner,
			boolean isRanked) {
		if (mode == GameMode.AI) {
			return new AiMatchInfo(id, type, matchStatus, turnStatus,
					blackName, whiteName, aiLevel, lastUpdated, fileName,
					score, rematchId, winner, isRanked);
		} else if (mode == GameMode.PASS_N_PLAY) {
			return new PassNPlayMatchInfo(id, type, matchStatus, turnStatus,
					blackName, whiteName, lastUpdated, fileName, score,
					rematchId, winner);
		} else if (mode == GameMode.TUTORIAL) {
			return new TutorialMatchInfo(id, matchStatus, turnStatus,
					blackName, whiteName, aiLevel, lastUpdated, fileName,
					score, rematchId, winner);
		}
		throw new RuntimeException("Unhandled GameMode " + mode);
	}

	public ScoreCard getScoreCard() {
		return scoreCard;
	}

	public long getRematchId() {
		return rematchId;
	}

	public void setWinner(int winner) {
		this.winner = winner;
	}

	public void setScoreCard(ScoreCard score) {
		this.scoreCard = score;
	}

	@Override
	public void dismiss(final MenuFragment fragment) {
		DatabaseHandler dbHandler = fragment.getDbHandler();

		// TODO show progress, or present an undo..
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
	}

	public boolean isRanked() {
		return false;
	}
}
