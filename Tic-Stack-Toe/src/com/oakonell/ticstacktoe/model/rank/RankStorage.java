package com.oakonell.ticstacktoe.model.rank;

import java.nio.ByteBuffer;

import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.utils.ByteBufferDebugger;

public class RankStorage {
	private TypeRankStorage juniorRank;
	private TypeRankStorage normalRank;
	private TypeRankStorage strictRank;

	public RankStorage(TypeRankStorage junior, TypeRankStorage normal,
			TypeRankStorage strict) {
		this.juniorRank = junior;
		this.normalRank = normal;
		this.strictRank = strict;
	}

	public TypeRankStorage getJuniorRank() {
		return juniorRank;
	}

	public TypeRankStorage getNormalRank() {
		return normalRank;
	}

	public TypeRankStorage getStrictRank() {
		return strictRank;
	}

	public void appendToBytes(ByteBufferDebugger buffer) {
		juniorRank.appendToBytes(buffer);
		normalRank.appendToBytes(buffer);
		strictRank.appendToBytes(buffer);
	}

	public static RankStorage fromBytes(ByteBufferDebugger buffer) {
		TypeRankStorage junior = TypeRankStorage.fromBytes(buffer);
		TypeRankStorage normal = TypeRankStorage.fromBytes(buffer);
		TypeRankStorage strict = TypeRankStorage.fromBytes(buffer);

		return new RankStorage(junior, normal, strict);
	}

	public byte[] toBytes() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(256 * 1024);
		ByteBufferDebugger buffer = new ByteBufferDebugger(byteBuffer);
		appendToBytes(buffer);
		return byteBuffer.array();
	}

	public RankStorage resolveConflict(RankStorage serverStorage) {
		TypeRankStorage juniorResolve = juniorRank
				.resolveConflict(serverStorage.juniorRank);
		TypeRankStorage normalResolve = normalRank
				.resolveConflict(serverStorage.normalRank);
		TypeRankStorage strictResolve = strictRank
				.resolveConflict(serverStorage.strictRank);
		return new RankStorage(juniorResolve, normalResolve, strictResolve);
	}

	public TypeRankStorage getRank(GameType type) {
		if (type.getVariant() == 1) {
			return getJuniorRank();
		} else if (type.getVariant() == 2) {
			return getNormalRank();
		} else if (type.getVariant() == 3) {
			return getStrictRank();
		} else {
			throw new RuntimeException("Unsupported variant "
					+ type.getVariant());
		}
	}

}
