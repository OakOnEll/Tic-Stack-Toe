package com.oakonell.ticstacktoe.model.rank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.utils.ByteBufferDebugger;

/**
 * This class stores the rank info for a given GameType, to be used to help
 * resolve conflicts in the game save state that is used to store the user's
 * rank.
 */
public class TypeRankStorage {
	private static final int NUM_MATCHES_TO_KEEP = 20;
	private static final long DAYS_TO_KEEP_MATCHES = 10;
	final private GameType type;
	private short rank;
	final private List<RankedGame> games;

	public TypeRankStorage(GameType type, short rank) {
		this(type, rank, Collections.<RankedGame> emptyList());
	}

	public TypeRankStorage(GameType type, short rank, List<RankedGame> games) {
		this.type = type;
		this.rank = rank;
		this.games = new ArrayList<RankedGame>();
		this.games.addAll(games);
		sortGames();
		if (games.size() != this.games.size()) {
			throw new RuntimeException("What happened?");
		}
	}

	private void sortGames() {
		Collections.sort(this.games, new Comparator<RankedGame>() {
			@Override
			public int compare(RankedGame lhs, RankedGame rhs) {
				return (int) (lhs.getStartedTime() - rhs.getStartedTime());
			}
		});
	}

	public GameType getType() {
		return type;
	}

	public short getRank() {
		return rank;
	}

	public Collection<RankedGame> getGames() {
		return games;
	}

	public void appendToBytes(ByteBufferDebugger buffer) {
		buffer.putShort("variant", (short) type.getVariant());
		buffer.putShort("rank", rank);
		buffer.putShort("num games", (short) games.size());
		// trim to the last week's or 20 games worth of rank data, to help
		// in resolving conflicts
		int count = 0;
		long now = System.currentTimeMillis();
		for (RankedGame each : games) {
			each.appendToBytes(buffer);
			count++;
			if (count > NUM_MATCHES_TO_KEEP) {
				break;
			}
			if (now - each.getStartedTime() > TimeUnit.DAYS
					.toMillis(DAYS_TO_KEEP_MATCHES)) {
				break;
			}
		}
	}

	public static TypeRankStorage fromBytes(ByteBufferDebugger buffer) {
		short variant = buffer.getShort("type");
		short rank = buffer.getShort("rank");
		short numGames = buffer.getShort("num games");
		List<RankedGame> games = new ArrayList<RankedGame>(numGames);
		for (int i = 0; i < numGames; i++) {
			games.add(RankedGame.fromBytes(buffer));
		}
		return new TypeRankStorage(GameType.fromVariant(variant), rank, games);
	}

	private static class MergedGamesWithStart {
		List<RankedGame> merged;
		int index;
	}

	public TypeRankStorage resolveConflict(TypeRankStorage serverStorage) {
		// apply the local games outcomes to the server's storage

		// find the latest matching game, and adjust rank by playing the merged
		// games forward
		MergedGamesWithStart merged = mergeRankedGames(serverStorage);
		// there should be games if there is any conflict in rank?
		// Well, otherwise the average of the server and my rank should be the
		// rank starting point, since both should be?
		short newRank = (short) ((serverStorage.getRank() + getRank()) / 2.0);
		if (merged.index >= 0) {
			RankedGame first = merged.merged.get(merged.index);
			newRank = first.getMyRank();
			RankingRater ranker = RankingRater.Factory.getRanker();
			for (RankedGame each : merged.merged.subList(merged.index,
					merged.merged.size())) {
				each.setMyRank(newRank);
				newRank = ranker.calculateRank(newRank, each.getOpponentRank(),
						each.getOutcome());
			}
		}
		return new TypeRankStorage(serverStorage.getType(), newRank,
				merged.merged);
	}

	private MergedGamesWithStart mergeRankedGames(TypeRankStorage serverStorage) {
		MergedGamesWithStart result = new MergedGamesWithStart();
		// check for special cases of one or the other empty
		if (serverStorage.getGames().isEmpty()) {
			if (games.isEmpty()) {
				result.index = -1;
				result.merged = Collections.emptyList();
				return result;
			}
			result.index = -1;
			result.merged = new ArrayList<RankedGame>(games);
			return result;
		}
		if (games.isEmpty()) {
			result.index = -1;
			result.merged = new ArrayList<RankedGame>(serverStorage.getGames());
			return result;
		}

		List<RankedGame> localList = new ArrayList<RankedGame>(games);
		Collections.reverse(localList);
		List<RankedGame> serverList = new ArrayList<RankedGame>(
				serverStorage.games);
		Collections.reverse(serverList);

		Iterator<RankedGame> localIter = localList.iterator();
		Iterator<RankedGame> serverIter = serverList.iterator();
		RankedGame local = localIter.next();
		RankedGame server = serverIter.next();
		List<RankedGame> list = new ArrayList<RankedGame>();
		int indexFromEnd = -1;
		while (true) {
			if (local.getStartedTime() == server.getStartedTime()
					&& local.getMyRank() == server.getMyRank()
					&& local.getOpponentRank() == server.getOpponentRank()) {
				// found the branching match, add one of them, and its
				// preceeding matches
				indexFromEnd = list.size() - 1;
				list.add(local);
				while (localIter.hasNext()) {
					list.add(localIter.next());
					indexFromEnd++;
				}
				break;
			} else if (local.getStartedTime() > server.getStartedTime()) {
				list.add(local);
				if (localIter.hasNext()) {
					local = localIter.next();
				} else {
					list.add(server);
					while (serverIter.hasNext()) {
						list.add(serverIter.next());
					}
					break;
				}
			} else {
				list.add(server);
				if (serverIter.hasNext()) {
					server = serverIter.next();
				} else {
					list.add(local);
					while (localIter.hasNext()) {
						list.add(localIter.next());
					}
					break;
				}
			}
		}

		Collections.reverse(list);
		int index = 0;
		if (indexFromEnd > 0) {
			index = list.size() - indexFromEnd - 1;
		}

		result.index = index;
		result.merged = list;
		return result;
	}

	public void setRank(short myNewRank) {
		this.rank = myNewRank;
	}

	public boolean hasPlayed() {
		return !games.isEmpty();
	}

	public void add(RankedGame rankedGame) {
		games.add(rankedGame);
		sortGames();
	}

}
