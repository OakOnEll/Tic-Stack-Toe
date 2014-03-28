package com.oakonell.ticstacktoe.server.rank;

public interface RankingRater {
	public static class Factory {
		public static RankingRater getRanker() {
			return new EloRanker();
		}
	}

	short initialRank();

	short calculateRank(short oldRank, short opponentsRank, GameOutcome outcome);
}
