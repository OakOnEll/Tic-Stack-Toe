package com.oakonell.ticstacktoe.model.rank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.utils.ByteBufferDebugger;

public class TypeRankStorage {
	private GameType type;
	private short rank;
	private List<RankedGame> games;

	public TypeRankStorage(short rank) {
		this.rank = rank;
		this.games = Collections.emptyList();
	}

	public TypeRankStorage(short rank, List<RankedGame> games) {
		this.rank = rank;
		this.games = games;
	}

	public GameType getType() {
		return type;
	}

	public short getRank() {
		return rank;
	}

	public List<RankedGame> getGames() {
		return games;
	}

	public void appendToBytes(ByteBufferDebugger buffer) {
		buffer.putShort("rank", rank);
		buffer.putShort("num games", (short) games.size());
		for (RankedGame each : games) {
			each.appendToBytes(buffer);
		}
	}

	public static TypeRankStorage fromBytes(ByteBufferDebugger buffer) {
		short rank = buffer.getShort("rank");
		short numGames = buffer.getShort("num games");
		List<RankedGame> games = new ArrayList<RankedGame>(numGames);
		for (int i = 0; i < numGames; i++) {
			games.add(RankedGame.fromBytes(buffer));
		}
		return new TypeRankStorage(rank, games);
	}

	public TypeRankStorage resolveConflict(TypeRankStorage serverStorage) {
		// apply the local games outcomes to the server's storage
		short newRank = serverStorage.getRank();
		RankingRater ranker = RankingRater.Factory.getRanker();
		for (RankedGame each : getGames()) {
			newRank =  ranker.calculateRank(newRank,
					each.getOpponentRank(), each.getOutcome());
		}
		return new TypeRankStorage(newRank, games);
	}

	public void setRank(short myNewRank) {
		this.rank = myNewRank;
	}

}
