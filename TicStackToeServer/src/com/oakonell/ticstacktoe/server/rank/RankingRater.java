package com.oakonell.ticstacktoe.server.rank;

public interface RankingRater {
	long initialRank();

	long calculateRank(long oldRank, long opponentsRank, boolean won);
}
