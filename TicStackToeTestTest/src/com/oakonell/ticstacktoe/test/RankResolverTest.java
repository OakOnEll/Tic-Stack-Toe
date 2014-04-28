package com.oakonell.ticstacktoe.test;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.rank.GameOutcome;
import com.oakonell.ticstacktoe.model.rank.RankStorage;
import com.oakonell.ticstacktoe.model.rank.RankedGame;
import com.oakonell.ticstacktoe.model.rank.RankingRater;
import com.oakonell.ticstacktoe.model.rank.TypeRankStorage;

public class RankResolverTest extends TestCase {
	public void testNoResolve() {
		TypeRankStorage strict = new TypeRankStorage(GameType.STRICT,
				(short) 1200);
		TypeRankStorage normal = new TypeRankStorage(GameType.STRICT,
				(short) 1200);
		TypeRankStorage junior = new TypeRankStorage(GameType.STRICT,
				(short) 1200);
		RankStorage storage = new RankStorage(junior, normal, strict);

		TypeRankStorage strict2 = new TypeRankStorage(GameType.STRICT,
				(short) 1200);
		TypeRankStorage normal2 = new TypeRankStorage(GameType.STRICT,
				(short) 1200);
		TypeRankStorage junior2 = new TypeRankStorage(GameType.STRICT,
				(short) 1200);
		RankStorage storage2 = new RankStorage(junior2, normal2, strict2);

		RankStorage resolved = storage.resolveConflict(storage2);
		assertEquals(1200, resolved.getJuniorRank().getRank());
		assertEquals(1200, resolved.getNormalRank().getRank());
		assertEquals(1200, resolved.getStrictRank().getRank());
	}

	public void testGamesInChronologicalOrder() {
		TypeRankStorage storage = new TypeRankStorage(GameType.STRICT,
				(short) 1200);

		long twoDaysAgo = System.currentTimeMillis()
				- TimeUnit.DAYS.toMillis(2);
		RankedGame game1 = new RankedGame(twoDaysAgo, (short) 1400,
				GameOutcome.WIN);
		storage.add(game1);

		long threeDaysAgo = System.currentTimeMillis()
				- TimeUnit.DAYS.toMillis(3);
		RankedGame game3 = new RankedGame(threeDaysAgo, (short) 1400,
				GameOutcome.WIN);
		storage.add(game3);

		long yesterday = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
		RankedGame game2 = new RankedGame(yesterday, (short) 1400,
				GameOutcome.WIN);
		storage.add(game2);

		assertEquals(game3, storage.getGames().iterator().next());
	}

	public void testResolve() {
		TypeRankStorage strict = new TypeRankStorage(GameType.STRICT,
				(short) 1200);

		RankingRater ranker = RankingRater.Factory.getRanker();
		short startOfSecondRank = ranker.calculateRank((short) 1200,
				(short) 1400, GameOutcome.WIN);
		short startOfThirdRank = ranker.calculateRank(
				(short) startOfSecondRank, (short) 1400, GameOutcome.WIN);
		short endThirdRank = ranker.calculateRank((short) startOfThirdRank,
				(short) 1500, GameOutcome.WIN);

		long twoDaysAgo = System.currentTimeMillis()
				- TimeUnit.DAYS.toMillis(2);
		RankedGame game1 = new RankedGame(twoDaysAgo, (short) 1400,
				GameOutcome.WIN);
		game1.setMyRank((short) 1200);
		strict.add(game1);

		long yesterday = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
		RankedGame game2 = new RankedGame(yesterday, (short) 1400,
				GameOutcome.WIN);
		game2.setMyRank(startOfSecondRank);
		strict.add(game2);

		assertEquals(game1, strict.getGames().iterator().next());

		TypeRankStorage strict2 = new TypeRankStorage(GameType.STRICT,
				(short) 1200);
		strict2.add(game1);

		long now = System.currentTimeMillis();
		RankedGame game3 = new RankedGame(now, (short) 1500, GameOutcome.WIN);
		game3.setMyRank(startOfSecondRank);
		strict2.add(game3);

		TypeRankStorage resolveConflict = strict2.resolveConflict(strict);

		assertEquals(3, resolveConflict.getGames().size());
		Iterator<RankedGame> iter = resolveConflict.getGames().iterator();
		assertEquals(game1, iter.next());
		assertEquals(game2, iter.next());
		assertEquals(game3, iter.next());
		assertEquals(endThirdRank, resolveConflict.getRank());

		// test reversed
		resolveConflict = strict.resolveConflict(strict2);

		assertEquals(3, resolveConflict.getGames().size());
		iter = resolveConflict.getGames().iterator();
		assertEquals(game1, iter.next());
		assertEquals(game2, iter.next());
		assertEquals(game3, iter.next());
		assertEquals(endThirdRank, resolveConflict.getRank());

	}

	public void testBothDiffResolve() {
		RankingRater ranker = RankingRater.Factory.getRanker();
		short startOfSecondRank = ranker.calculateRank((short) 1200,
				(short) 1400, GameOutcome.WIN);
		short startOfThirdRank = ranker.calculateRank(
				(short) startOfSecondRank, (short) 1400, GameOutcome.WIN);
		short startFourthRank = ranker.calculateRank((short) startOfThirdRank,
				(short) 1400, GameOutcome.WIN);
		short endOfFourthRank = ranker.calculateRank((short) startFourthRank,
				(short) 1500, GameOutcome.WIN);

		TypeRankStorage strict = new TypeRankStorage(GameType.STRICT,
				(short) 1200);

		long twoDaysAgo = System.currentTimeMillis()
				- TimeUnit.DAYS.toMillis(2);
		RankedGame game1 = new RankedGame(twoDaysAgo, (short) 1400,
				GameOutcome.WIN);
		game1.setMyRank((short) 1200);
		strict.add(game1);

		long yesterday = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
		RankedGame game2 = new RankedGame(yesterday, (short) 1400,
				GameOutcome.WIN);
		game2.setMyRank((short) 1300);
		strict.add(game2);

		assertEquals(game1, strict.getGames().iterator().next());

		TypeRankStorage strict2 = new TypeRankStorage(GameType.STRICT,
				(short) 1200);
		long twoDaysAgo2 = System.currentTimeMillis()
				- TimeUnit.DAYS.toMillis(2) - 100;
		RankedGame game1a = new RankedGame(twoDaysAgo2, (short) 1400,
				GameOutcome.WIN);
		game1a.setMyRank((short) 1200);
		strict2.add(game1a);

		long now = System.currentTimeMillis();
		RankedGame game3 = new RankedGame(now, (short) 1500, GameOutcome.WIN);
		game3.setMyRank((short) 1300);
		strict2.add(game3);

		TypeRankStorage resolveConflict = strict2.resolveConflict(strict);

		assertEquals(4, resolveConflict.getGames().size());
		Iterator<RankedGame> iter = resolveConflict.getGames().iterator();
		assertEquals(game1a, iter.next());
		assertEquals(game1, iter.next());
		assertEquals(game2, iter.next());
		assertEquals(game3, iter.next());
		assertEquals(endOfFourthRank, resolveConflict.getRank());

		// Test reversed
		resolveConflict = strict.resolveConflict(strict2);

		assertEquals(4, resolveConflict.getGames().size());
		iter = resolveConflict.getGames().iterator();
		assertEquals(game1a, iter.next());
		assertEquals(game1, iter.next());
		assertEquals(game2, iter.next());
		assertEquals(game3, iter.next());
		assertEquals(endOfFourthRank, resolveConflict.getRank());
	}

}
