package com.oakonell.ticstacktoe.ui.local;

import java.util.Map;

import android.os.AsyncTask;

import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.GameContext;
import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.PlayerStrategy;
import com.oakonell.ticstacktoe.model.RankInfo;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.State;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler;
import com.oakonell.ticstacktoe.model.rank.GameOutcome;
import com.oakonell.ticstacktoe.model.rank.RankedGame;
import com.oakonell.ticstacktoe.model.solver.AILevel;
import com.oakonell.ticstacktoe.model.solver.AiPlayerStrategy;
import com.oakonell.ticstacktoe.rank.AIRankHelper;
import com.oakonell.ticstacktoe.rank.AIRankHelper.OnRanksRetrieved;
import com.oakonell.ticstacktoe.rank.RankHelper;
import com.oakonell.ticstacktoe.rank.RankHelper.OnMyRankUpdated;
import com.oakonell.ticstacktoe.ui.local.RankedAIPlayAgainFragment.RankedAIPlayAgainListener;

public class AiGameStrategy extends AbstractLocalStrategy {

	private final AILevel aiDepth;

	public AiGameStrategy(GameContext context, AILevel aiDepth) {
		super(context);
		this.aiDepth = aiDepth;
	}

	public AiGameStrategy(GameContext context, AILevel aiDepth,
			AiMatchInfo matchInfo) {
		super(context, matchInfo);
		this.aiDepth = aiDepth;
	}

	protected AiMatchInfo createNewMatchInfo(String blackName,
			String whiteName, final Game game, ScoreCard score) {
		return new AiMatchInfo(TurnBasedMatch.MATCH_STATUS_ACTIVE,
				TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN, blackName, whiteName,
				aiDepth, System.currentTimeMillis(), game, score);
	}

	protected GameMode getGameMode() {
		return GameMode.AI;
	}

	@Override
	protected Player createWhitePlayer(String whiteName) {
		return AiPlayerStrategy.createThePlayer(whiteName, false, aiDepth);
	}

	protected void acceptNonHumanPlayerMove(final PlayerStrategy currentStrategy) {
		getGameFragment().showStatusText(
				getMatchInfo().getWhiteName() + " is thinking...");
		AsyncTask<Void, Void, State> aiMove = new AsyncTask<Void, Void, State>() {
			@Override
			protected State doInBackground(Void... params) {
				AbstractMove move = currentStrategy.move(getGame());
				State state = applyNonHumanMove(move);
				updateGame();
				return state;
			}

			@Override
			protected void onPostExecute(final State state) {
				getGameFragment().animateMove(state.getLastMove(), state);
			}
		};
		aiMove.execute((Void) null);
	}

	// @Override
	// protected void gameFinished() {
	// if (getGame().getRankInfo() == null) {
	// return;
	// }
	// updateRanks(null);
	// }

	@Override
	public void promptToPlayAgain(String winner, String title) {
		if (getGame().getRankInfo() == null) {
			super.promptToPlayAgain(winner, title);
			return;
		}
		// TODO display play again prompt, with rank change info... with a
		// progress
		final RankedAIPlayAgainFragment playAgainDialog = new RankedAIPlayAgainFragment();

		updateRanks(new PostRankUpdate() {
			@Override
			public void ranksUpdated(short oldRank, short newRank,
					short oldAiRank, short newAiRank) {
				playAgainDialog.updateRanks(oldRank, newRank, oldAiRank,
						newAiRank);
			}
		});

		playAgainDialog
				.initialize(new RankedAIPlayAgainListener() {

					@Override
					public void playAgain() {
						AiGameStrategy.this.playAgain();
					}

					@Override
					public void cancel() {
						leaveGame();
					}
				}, getMatchInfo().getBlackName(),
						getMatchInfo().getWhiteName(), winner);
		playAgainDialog.show(getGameContext().getGameFragment()
				.getChildFragmentManager(), "playAgain");

	}

	protected void startGame(final GameMode gameMode, final GameType type,
			final boolean blackFirst, final String blackName,
			final Player whitePlayer, final String whiteName,
			final ScoreCard score, final Player blackPlayer,
			final Player firstPlayer, final RankInfo rankInfo) {
		if (rankInfo == null) {
			super.startGame(gameMode, type, blackFirst, blackName, whitePlayer,
					whiteName, score, blackPlayer, firstPlayer, rankInfo);
			return;
		}
		AIRankHelper.retrieveRanks(new DatabaseHandler(getContext()), type,
				new OnRanksRetrieved() {
					@Override
					public void onSuccess(Map<AILevel, Integer> ranks) {
						int aiRank = ranks.get(aiDepth);
						rankInfo.setWhiteRank((short) aiRank);
						AiGameStrategy.super.startGame(gameMode, type,
								blackFirst, blackName, whitePlayer, whiteName,
								score, blackPlayer, firstPlayer, rankInfo);
					}
				});
	}

	public interface PostRankUpdate {
		void ranksUpdated(short oldBlackRank, short newBlackRank,
				short oldWhiteRank, short newWhiteRank);
	}

	private void updateRanks(final PostRankUpdate postUpdate) {
		State state = getGame().getBoard().getState();
		final GameOutcome outcome;
		Player winner = state.getWinner();
		if (winner != null) {
			outcome = winner.isBlack() ? GameOutcome.WIN : GameOutcome.LOSE;
		} else {
			outcome = GameOutcome.DRAW;
		}

		final DatabaseHandler dbHandler = new DatabaseHandler(getContext());
		AIRankHelper.retrieveRanks(dbHandler, getGame().getType(),
				new OnRanksRetrieved() {
					@Override
					public void onSuccess(Map<AILevel, Integer> ranks) {
						final int aiRank = ranks.get(aiDepth);
						RankHelper.updateRank(getGameContext(), getGame()
								.getType(), new RankedGame((short) aiRank,
								outcome), new OnMyRankUpdated() {
							@Override
							public void onRankUpdated(final short oldRank,
									final short newRank) {
								AIRankHelper.updateRank(dbHandler, getGame()
										.getType(), aiDepth,
										outcome.opposite(), oldRank);
								if (postUpdate != null) {
									AIRankHelper.retrieveRanks(dbHandler,
											getGame().getType(),
											new OnRanksRetrieved() {
												@Override
												public void onSuccess(
														Map<AILevel, Integer> ranks) {
													int newAiRank = ranks
															.get(aiDepth);
													postUpdate.ranksUpdated(
															oldRank, newRank,
															(short) aiRank,
															(short) newAiRank);

												}
											});
								}
							}
						});

					}
				});

	}

}
