package com.oakonell.ticstacktoe.test;

import junit.framework.TestCase;
import android.util.Log;

import com.oakonell.ticstacktoe.model.rank.EloRanker;
import com.oakonell.ticstacktoe.model.rank.GameOutcome;
import com.oakonell.ticstacktoe.model.rank.RankingRater;

public class EloRankTest extends TestCase {
	public void testSimple() {
		RankingRater rater = new EloRanker();
		long a = rater.initialRank();
		long b = rater.initialRank();

		for (int i = 0; i < 100000; i++) {
			long newA = rater.calculateRank(a, b, GameOutcome.WIN);
			long newB = rater.calculateRank(b, a, GameOutcome.LOSE);
			long diffA = newA - a;
			long diffB = newB - b;
			if (Math.abs(diffA - diffB) > 1) {
				displayRankings(a, b, newA, newB);
			}
			a = newA;
			b = newB;
		}
		displayRankings(a, b, a, b);

	}

	private void displayRankings(long a, long b, long newA, long newB) {
		String log = "newA=" + newA + ", newB=" + newB + ", oldA=" + a
				+ ", oldB=" + b;
		System.out.println(log);
		Log.i("ELO Test", log);
	}
}
