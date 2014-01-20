package com.oakonell.ticstacktoe.model;

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

}
