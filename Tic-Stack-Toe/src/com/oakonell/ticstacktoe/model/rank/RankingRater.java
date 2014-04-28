package com.oakonell.ticstacktoe.model.rank;

/**
 * An interface to abstract a game play/ability rating system.
 *
 */
public interface RankingRater {
	public static class Factory {
		public static RankingRater getRanker() {
			return new EloRanker();
		}
	}

	short initialRank();

	short calculateRank(short oldRank, short opponentsRank, GameOutcome outcome);
}
