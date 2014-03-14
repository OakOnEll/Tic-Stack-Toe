package com.oakonell.ticstacktoe.model.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import android.util.Log;

import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.State;
import com.oakonell.ticstacktoe.model.rank.EloRanker;
import com.oakonell.ticstacktoe.model.rank.GameOutcome;
import com.oakonell.ticstacktoe.model.rank.RankingRater;

public class RankAITest extends TestCase {

	private static class RankedAIPlayer {
		int aiLevel;
		long rank;
		private List<Long> ranks = new ArrayList<Long>();

		public RankedAIPlayer(int aiLevel, long rank) {
			this.aiLevel = aiLevel;
			this.rank = rank;
		}

		Player createPlayer(boolean isBlack) {
			return AiPlayerStrategy.createThePlayer("Level " + aiLevel,
					isBlack, aiLevel);
		}

		public void setNewRank(long calculateRank) {
			rank = calculateRank;
			ranks.add(calculateRank);
		}

	}

	List<RankedAIPlayer> getAIPlayers(RankingRater rater) {
		ArrayList<RankedAIPlayer> result = new ArrayList<RankedAIPlayer>();

		result.add(new RankedAIPlayer(AiPlayerStrategy.RANDOM_AI, 620));
		result.add(new RankedAIPlayer(AiPlayerStrategy.EASY_AI, 1570));
		result.add(new RankedAIPlayer(AiPlayerStrategy.MEDIUM_AI, 1850));
		result.add(new RankedAIPlayer(AiPlayerStrategy.HARD_AI, 1920));

		return result;

	}

	public void testRankAIs() {
		RankingRater rater = new EloRanker();
		List<RankedAIPlayer> aiPlayers = getAIPlayers(rater);
		Map<RankedAIPlayer, Long> currentRanks = new HashMap<RankAITest.RankedAIPlayer, Long>();
		for (RankedAIPlayer each : aiPlayers) {
			currentRanks.put(each, each.rank);
		}

		for (int i = 0; i < 500; i++) {
			tournament(aiPlayers, rater);
			displayRanks(i, aiPlayers, currentRanks);
			for (RankedAIPlayer each : aiPlayers) {
				currentRanks.put(each, each.rank);
			}
		}
		displayFinalRanks(500, aiPlayers);
	}

	private void displayRanks(int iterationNum, List<RankedAIPlayer> aiPlayers,
			Map<RankedAIPlayer, Long> currentRanks) {
		StringBuilder builder = new StringBuilder("AI Ranks after "
				+ iterationNum + ":\n");
		for (RankedAIPlayer each : aiPlayers) {
			builder.append(each.aiLevel);
			builder.append(" : ");
			builder.append(each.rank);
			Long originalRank = currentRanks.get(each);
			builder.append("(" + (originalRank - each.rank) + ")");
			builder.append("\n");
		}
		Log.i("RankAITest", builder.toString());
	}

	private void displayFinalRanks(int iterationNum,
			List<RankedAIPlayer> aiPlayers) {
		StringBuilder builder = new StringBuilder("AI Ranks after "
				+ iterationNum + ":\n");
		for (RankedAIPlayer each : aiPlayers) {
			builder.append(each.aiLevel);
			builder.append(" : ");
			builder.append(each.rank);
			builder.append(": mean=");

			Stats stats = new Stats();
			double mean = stats.mean(each.ranks);
			double median = stats.median(each.ranks);
			double sd = stats.sd(each.ranks);
			builder.append(mean);
			builder.append(", median=");
			builder.append(median);
			builder.append(", sd=");
			builder.append(sd);
			builder.append("\n");
		}
		Log.i("RankAITest", builder.toString());
	}

	private void tournament(List<RankedAIPlayer> aiPlayers, RankingRater rater) {
		int size = aiPlayers.size();
		for (int i = 0; i < size; i++) {
			RankedAIPlayer playBlack = aiPlayers.get(i);
			for (int j = 0; j < size - 1; j++) {
				RankedAIPlayer playWhite = aiPlayers.get(getOtherPlayerIndex(i,
						j));

				try {
					State state = playGame(playBlack.createPlayer(true),
							playWhite.createPlayer(false));
					GameOutcome blackOutcome = null;
					GameOutcome whiteOutcome = null;
					if (state.isDraw()) {
						blackOutcome = GameOutcome.DRAW;
						whiteOutcome = GameOutcome.DRAW;
					} else {
						boolean blackWon = state.getWinner().isBlack();
						blackOutcome = blackWon ? GameOutcome.WIN
								: GameOutcome.LOSE;
						whiteOutcome = blackWon ? GameOutcome.LOSE
								: GameOutcome.WIN;
					}
					playBlack.setNewRank(rater.calculateRank(playBlack.rank,
							playWhite.rank, blackOutcome));
					playWhite.setNewRank(rater.calculateRank(playWhite.rank,
							playBlack.rank, whiteOutcome));
				} catch (Exception e) {
					e.printStackTrace();
					// continue
					// ignore games with bad moves... random seemed at fault
				}
			}
		}
	}

	private State playGame(Player blackPlayer, Player whitePlayer) {
		Game game = new Game(GameType.NORMAL, GameMode.AI, blackPlayer,
				whitePlayer, blackPlayer);
		int numMoves = 0;
		while (!game.getBoard().getState().isOver()) {
			AbstractMove move = blackPlayer.getStrategy().move(game);
			move.applyToGame(game);
			if (game.getBoard().getState().isOver()) {
				break;
			}
			move = whitePlayer.getStrategy().move(game);
			move.applyToGame(game);
			numMoves++;
			if (numMoves > 1000) {
				return State.draw(move);
			}
		}
		return game.getBoard().getState();
	}

	private int getOtherPlayerIndex(int playerIndex, int otherNumber) {
		if (otherNumber < playerIndex)
			return otherNumber;
		return otherNumber + 1;
	}

	private static class Stats {
		public long sum(List<Long> a) {
			if (a.size() > 0) {
				int sum = 0;

				for (Long i : a) {
					sum += i;
				}
				return sum;
			}
			return 0;
		}

		public double mean(List<Long> a) {
			long sum = sum(a);
			double mean = 0;
			mean = sum / (a.size() * 1.0);
			return mean;
		}

		public double median(List<Long> a) {
			int middle = a.size() / 2;

			if (a.size() % 2 == 1) {
				return a.get(middle);
			} else {
				return (a.get(middle - 1) + a.get(middle)) / 2.0;
			}
		}

		public double sd(List<Long> a) {
			long sum = 0;
			double mean = mean(a);

			for (Long i : a)
				sum += Math.pow((i - mean), 2);
			return Math.sqrt(sum / (a.size() - 1)); // sample
		}
	}

}
