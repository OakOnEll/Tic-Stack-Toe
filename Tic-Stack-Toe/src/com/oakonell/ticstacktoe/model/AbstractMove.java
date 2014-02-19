package com.oakonell.ticstacktoe.model;

import java.util.List;

import com.oakonell.ticstacktoe.model.Board.PieceStack;
import com.oakonell.ticstacktoe.utils.ByteBufferDebugger;

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

	public abstract State applyToGame(Game game);

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

	public void appendBytesToMessage(ByteBufferDebugger buffer) {
		buffer.put("Move type", getMoveType());
		privateAppendBytesToMessage(buffer);
	}

	protected abstract byte getMoveType();

	protected abstract void privateAppendBytesToMessage(
			ByteBufferDebugger buffer);

	protected void appendCommonToMessage(ByteBufferDebugger buffer) {
		int targetX = getTargetCell().getX();
		int targetY = getTargetCell().getY();

		buffer.put("target x", (byte) targetX);
		buffer.put("target y", (byte) targetY);
		// checksums
		Piece existingTargetPiece = getExistingTargetPiece();
		buffer.putInt("existing target piece",
				existingTargetPiece != null ? getExistingTargetPiece().getVal()
						: 0);
		buffer.putInt("played piece", getPlayedPiece().getVal());
		buffer.put("player is black", (byte) (getPlayer().isBlack() ? 1 : 0));
	}

	static CommonMoveInfo commonFromBytes(ByteBufferDebugger buffer, Game game) {
		int targetX = buffer.get("target x");
		int targetY = buffer.get("targey y");

		// checksums
		int existingTargetPieceVal = buffer.getInt("existing target piece");
		int playedPieceVal = buffer.getInt("player piece");
		Piece playedPiece = Piece.fromInt(playedPieceVal);
		boolean playerIsBlack = buffer.get("player is black") == 1;
		Player player;
		if (playerIsBlack) {
			player = game.getBlackPlayer();
		} else {
			player = game.getWhitePlayer();
		}
		Piece existingTargetPiece = null;
		if (existingTargetPieceVal != 0) {
			existingTargetPiece = Piece.fromInt(existingTargetPieceVal);
		}
		Cell target = new Cell(targetX, targetY);
		CommonMoveInfo commonInfo = new CommonMoveInfo(player, playedPiece,
				target, existingTargetPiece);
		return commonInfo;
	}

	static class CommonMoveInfo {
		public CommonMoveInfo(Player player, Piece playedPiece, Cell target,
				Piece existingTargetPiece) {
			this.player = player;
			this.playedPiece = playedPiece;
			this.target = target;
			this.existingTargetPiece = existingTargetPiece;
		}

		final Piece playedPiece;
		final Cell target;
		final Piece existingTargetPiece;
		final Player player;
	}

	public static AbstractMove fromMessageBytes(ByteBufferDebugger buffer,
			Game game) {
		if (buffer.get("move type") == STACK_MOVE) {
			return PlaceNewPieceMove.fromBytes(buffer, game);
		}
		return ExistingPieceMove.fromBytes(buffer, game);
	}

}
