package com.oakonell.ticstacktoe.model;

import java.nio.ByteBuffer;
import java.util.List;

import com.oakonell.ticstacktoe.model.Board.PieceStack;

public abstract class AbstractMove {
	public static final byte STACK_MOVE = 0;
	public static final byte BOARD_MOVE = 1;

	private final Player player;
	private final Cell target;
	private final Piece playedPiece;
	private final Piece existingTargetPiece;

	protected AbstractMove(Player player, Piece playedPiece, Cell target,
			Piece existingTargetPiece) {
		this.player = player;
		this.target = target;
		this.playedPiece = playedPiece;
		this.existingTargetPiece = existingTargetPiece;
	}

	public Player getPlayer() {
		return player;
	}

	public Cell getTargetCell() {
		return target;
	}

	public Piece getExistingTargetPiece() {
		return existingTargetPiece;
	}

	public Piece getPlayedPiece() {
		return playedPiece;
	}

	abstract public State applyToGame(Game game);

	public abstract State applyTo(GameType type, Board board,
			List<PieceStack> blackPlayerPieces,
			List<PieceStack> whitePlayerPieces);

	public abstract void undo(Board board, State originalState,
			List<PieceStack> blackPlayerPieces,
			List<PieceStack> whitePlayerPieces);

	protected void appendTargetToString(StringBuilder builder) {
		builder.append(" to ");
		builder.append(getTargetCell());
		Piece existingTargetPiece = getExistingTargetPiece();
		if (existingTargetPiece != null) {
			builder.append(" (covering ");
			builder.append(existingTargetPiece);
			builder.append(")");
		}
	}

	public void appendBytesToMessage(ByteBuffer buffer) {
		buffer.put(getMoveType());
		privateAppendBytesToMessage(buffer);
	}

	protected abstract byte getMoveType();

	protected abstract void privateAppendBytesToMessage(ByteBuffer buffer);

	protected void appendCommonToMessage(ByteBuffer buffer) {
		int targetX = getTargetCell().getX();
		int targetY = getTargetCell().getY();

		buffer.put((byte) targetX);
		buffer.put((byte) targetY);
		// checksums
		Piece existingTargetPiece = getExistingTargetPiece();
		buffer.putInt(existingTargetPiece != null ? getExistingTargetPiece()
				.getVal() : 0);
		buffer.putInt(getPlayedPiece().getVal());
		buffer.put((byte) (getPlayer().isBlack() ? 1 : 0));
	}

	static CommonMoveInfo commonFromBytes(ByteBuffer buffer, Game game,
			Piece playedPiece) {
		int targetX = buffer.get();
		int targetY = buffer.get();

		// checksums
		int existingTargetPieceVal = buffer.getInt();
		int playedPieceVal = buffer.getInt();
		boolean playerIsBlack = buffer.get() == 1;

		if (game.getCurrentPlayer().isBlack() != playerIsBlack) {
			throw new RuntimeException(
					"Invalid common move message, wrong player. Received isBlack=" + playerIsBlack);
		}
		Board board = game.getBoard();
		Piece existingTargetPiece = board.getVisiblePiece(targetX, targetY);
		int myExistingTargetPieceVal = existingTargetPiece != null ? existingTargetPiece
				.getVal() : 0;
		if (myExistingTargetPieceVal != existingTargetPieceVal) {
			throw new RuntimeException(
					"Invalid common move message, wrong exisitng target piece value. Received " + existingTargetPieceVal + ", but is " + myExistingTargetPieceVal);
		}
		if (playedPiece.getVal() != playedPieceVal) {
			throw new RuntimeException(
					"Invalid common move message, wrong played piece value. Received " + playedPieceVal + ", but is " + playedPiece.getVal());
		}
		Cell target = new Cell(targetX, targetY);
		CommonMoveInfo commonInfo = new CommonMoveInfo(playedPiece, target,
				existingTargetPiece);
		return commonInfo;
	}

	static class CommonMoveInfo {
		public CommonMoveInfo(Piece playedPiece, Cell target,
				Piece existingTargetPiece) {
			this.playedPiece = playedPiece;
			this.target = target;
			this.existingTargetPiece = existingTargetPiece;
		}

		Piece playedPiece;
		Cell target;
		Piece existingTargetPiece;
	}

	public static AbstractMove fromMessageBytes(ByteBuffer buffer, Game game) {
		if (buffer.get() == STACK_MOVE) {
			return PlaceNewPieceMove.fromBytes(buffer, game);
		}
		return ExistingPieceMove.fromBytes(buffer, game);
	}

}
