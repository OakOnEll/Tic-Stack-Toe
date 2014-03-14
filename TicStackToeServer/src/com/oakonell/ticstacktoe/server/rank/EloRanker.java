package com.oakonell.ticstacktoe.server.rank;

public class EloRanker implements RankingRater {

	@Override
	public long calculateRank(long oldRank, long opponentsRank, boolean won) {
		double expected = calculateExpected(oldRank, opponentsRank);

		double actual = won ? 1 : 0;
		double newRank = oldRank + getKFactor(oldRank) * (actual - expected);
		return (long) Math.round(newRank);
	}

	private double getKFactor(long oldRank) {
		// https://en.wikipedia.org/wiki/Elo_rating_system
		// Using USCF logistic distribution
		// Players below 2100: K-factor of 32 used
		// Players between 2100 and 2400: K-factor of 24 used
		// Players above 2400: K-factor of 16 used.
		if (oldRank < 2100)
			return 32;
		if (oldRank < 2400)
			return 24;
		return 16;
	}

	private double calculateExpected(long oldRank, long opponentsRank) {
		double denom = 1 + Math.pow(10, (opponentsRank - oldRank) / 400.0);
		return 1.0 / denom;
	}

	@Override
	public long initialRank() {
		return 1200;
	}

}
