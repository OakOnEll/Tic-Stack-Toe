package com.oakonell.ticstacktoe.model.rank;

import java.util.ArrayList;
import java.util.List;

import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.utils.ByteBufferDebugger;

public class TypeRankStorage {
	final private GameType type;
	private short rank;
	final private List<RankedGame> games;

	public TypeRankStorage(GameType type, short rank) {
		this.type = type;
		this.rank = rank;
		this.games = new ArrayList<RankedGame>();
	}

	public TypeRankStorage(GameType type, short rank, List<RankedGame> games) {
		this.type = type;
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
		buffer.putShort("variant", (short) type.getVariant());
		buffer.putShort("rank", rank);
		buffer.putShort("num games", (short) games.size());
		for (RankedGame each : games) {
			each.appendToBytes(buffer);
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

	public TypeRankStorage resolveConflict(TypeRankStorage serverStorage) {
		// apply the local games outcomes to the server's storage
		short newRank = serverStorage.getRank();
		RankingRater ranker = RankingRater.Factory.getRanker();
		for (RankedGame each : getGames()) {
			newRank = ranker.calculateRank(newRank, each.getOpponentRank(),
					each.getOutcome());
		}
		return new TypeRankStorage(serverStorage.getType(), newRank, games);
	}

	public void setRank(short myNewRank) {
		this.rank = myNewRank;
	}

}
