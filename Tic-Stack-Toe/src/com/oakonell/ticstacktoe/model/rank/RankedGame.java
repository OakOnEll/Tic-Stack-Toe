package com.oakonell.ticstacktoe.model.rank;

import com.oakonell.ticstacktoe.utils.ByteBufferDebugger;

/**
 * A Ranked game result, stored to help resolve conflicts in save games The
 * ranks are the ranks at the start of the game.
 */
public class RankedGame {
	private final long startedTime;
	private short myRank;
	private final short opponentRank;
	private final GameOutcome outcome;

	public RankedGame(long startedTime, short opponentRank, GameOutcome outcome) {
		this.startedTime = startedTime;
		this.opponentRank = opponentRank;
		this.outcome = outcome;
	}

	protected RankedGame(long startedTime, short myRank, short opponentRank,
			GameOutcome outcome) {
		this.startedTime = startedTime;
		this.myRank = myRank;
		this.opponentRank = opponentRank;
		this.outcome = outcome;
	}

	public short getOpponentRank() {
		return opponentRank;
	}

	public short getMyRank() {
		return myRank;
	}

	public void setMyRank(short currentRank) {
		this.myRank = currentRank;
	}

	public GameOutcome getOutcome() {
		return outcome;
	}

	public long getStartedTime() {
		return startedTime;
	}

	public static RankedGame fromBytes(ByteBufferDebugger byteBuffer) {
		long startedTime = byteBuffer.getLong("startedTime");
		short myRank = byteBuffer.getShort("myRank");
		short opponentRank = byteBuffer.getShort("opponentRank");
		byte outcomeByte = byteBuffer.get("gameOutcome");
		GameOutcome outcome = null;
		if (outcomeByte == 0) {
			outcome = GameOutcome.LOSE;
		} else if (outcomeByte == 1) {
			outcome = GameOutcome.DRAW;
		} else if (outcomeByte == 2) {
			outcome = GameOutcome.WIN;
		} else {
			throw new RuntimeException("Unexpected outcome byte " + outcomeByte);
		}
		return new RankedGame(startedTime, myRank, opponentRank, outcome);
	}

	public void appendToBytes(ByteBufferDebugger byteBuffer) {
		byteBuffer.putLong("startedTime", startedTime);
		byteBuffer.putShort("myRank", (short) myRank);
		byteBuffer.putShort("opponentRank", (short) opponentRank);
		byte outcomeByte;
		switch (outcome) {
		case LOSE:
			outcomeByte = 0;
			break;
		case DRAW:
			outcomeByte = 1;
			break;
		case WIN:
			outcomeByte = 2;
			break;
		default:
			throw new RuntimeException("Unexpected outcome " + outcome);
		}
		byteBuffer.put("gameOutcome", outcomeByte);
	}

}
