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

}
