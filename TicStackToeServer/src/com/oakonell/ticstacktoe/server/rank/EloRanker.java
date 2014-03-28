package com.oakonell.ticstacktoe.server.rank;

public class EloRanker implements RankingRater {

	@Override
	public short calculateRank(short oldRank, short opponentsRank,
			GameOutcome outcome) {
		double expected = calculateExpected(oldRank, opponentsRank);

		double actual = outcome.getMultiplier();

		double newRank = oldRank + getKFactor(oldRank) * (actual - expected);
		return (short) Math.round(newRank);
	}

	private double getKFactor(short oldRank) {
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

	private double calculateExpected(short oldRank, short opponentsRank) {
		double denom = 1 + Math.pow(10, (opponentsRank - oldRank) / 400.0);
		return 1.0 / denom;
	}

	@Override
	public short initialRank() {
		return 1200;
	}

}
