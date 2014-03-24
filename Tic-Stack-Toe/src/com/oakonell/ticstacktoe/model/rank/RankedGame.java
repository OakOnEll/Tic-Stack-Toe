package com.oakonell.ticstacktoe.model.rank;

import com.oakonell.ticstacktoe.utils.ByteBufferDebugger;

public class RankedGame {
	private final short opponentRank;
	private final GameOutcome outcome;

	public RankedGame(short opponentRank, GameOutcome outcome) {
		super();
		this.opponentRank = opponentRank;
		this.outcome = outcome;
	}

	public short getOpponentRank() {
		return opponentRank;
	}

	public GameOutcome getOutcome() {
		return outcome;
	}

	public static RankedGame fromBytes(ByteBufferDebugger byteBuffer) {
		short opponentRank = byteBuffer.getShort("opponentRank");
		byte outcomeByte  =byteBuffer.get("gameOutcome");
		GameOutcome outcome = null;
		if (outcomeByte ==0) {
			outcome = GameOutcome.LOSE;
		} else if (outcomeByte == 1){
			outcome = GameOutcome.DRAW;			
		} else if (outcomeByte == 2) {
			outcome = GameOutcome.WIN;
		} else {
			throw new RuntimeException("Unexpected outcome byte " + outcomeByte);
		}
		return new RankedGame(opponentRank, outcome);
	}
	
	public void appendToBytes(ByteBufferDebugger byteBuffer) {
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
