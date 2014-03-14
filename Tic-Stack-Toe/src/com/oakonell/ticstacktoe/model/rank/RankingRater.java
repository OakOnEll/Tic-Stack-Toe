package com.oakonell.ticstacktoe.model.rank;

public interface RankingRater {
	long initialRank();

	long calculateRank(long oldRank, long opponentsRank, GameOutcome outcome);
}
