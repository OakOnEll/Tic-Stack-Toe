package com.oakonell.ticstacktoe.model;

import java.util.List;

import com.oakonell.ticstacktoe.model.Board.PieceStack;

public class ExistingPieceMove extends AbstractMove {
	private final Piece exposedSourcePiece;
	private final Cell source;

	public ExistingPieceMove(Player player, Piece playedPiece,
			Piece exposedSourcePiece, Cell from, Cell target,
			Piece existingTargetPiece) {
		super(player, playedPiece, target, existingTargetPiece);
		this.source = from;
		this.exposedSourcePiece = exposedSourcePiece;
	}

	public Piece getExposedSourcePiece() {
		return exposedSourcePiece;
	}

	public Cell getSource() {
		return source;
	}

	@Override
	public State applyToGame(Game game) {
		return game.movePiece(source, getTargetCell());
	}

	@Override
	public State applyTo(GameType type, Board board,
			List<PieceStack> blackPlayerPieces,
			List<PieceStack> whitePlayerPieces) {
		return board.moveFrom(getPlayer(), getSource(), getTargetCell());
	}

	@Override
	public void undo(Board board, State originalState,
			List<PieceStack> blackPlayerPieces,
			List<PieceStack> whitePlayerPieces) {
		board.undoBoardMove(this, originalState);
	}

	public String toString() {
		StringBuilder builder = new StringBuilder("Board Move ");
		builder.append(getPlayedPiece());
		builder.append(" from ");
		builder.append(getSource());
		if (exposedSourcePiece != null) {
			builder.append(" (exposing ");
			builder.append(exposedSourcePiece);
			builder.append(")");
		}
		appendTargetToString(builder);
		return builder.toString();
	}

	public static AbstractMove fromBytes(ByteBufferDebugger buffer, Game game) {
		int sourceX = buffer.get("source x");
		int sourceY = buffer.get("source y");
		Cell source = new Cell(sourceX, sourceY);
		int exposedSourceVal = buffer.getInt("exposed source piece");

		Piece exposedSourcePiece = null;
		if (exposedSourceVal != 0) {
			exposedSourcePiece = Piece.fromInt(exposedSourceVal);
		}

		CommonMoveInfo commonInfo = commonFromBytes(buffer, game);

		return new ExistingPieceMove(game.getCurrentPlayer(),
				commonInfo.playedPiece, exposedSourcePiece, source,
				commonInfo.target, commonInfo.existingTargetPiece);
	}

	@Override
	protected void privateAppendBytesToMessage(ByteBufferDebugger buffer) {
		int x = source.getX();
		int y = source.getY();

		buffer.put("Source x", (byte) x);
		buffer.put("Source y", (byte) y);

		// checksum
		int sourceVal = exposedSourcePiece != null ? exposedSourcePiece
				.getVal() : 0;
		buffer.putInt("Exposed source piece", sourceVal);

		appendCommonToMessage(buffer);
	}

	@Override
	protected byte getMoveType() {
		return BOARD_MOVE;
	}

}
