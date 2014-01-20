package com.oakonell.ticstacktoe.model;

public class PlaceNewPieceMove extends AbstractMove {
	private final int stackNum;

	public PlaceNewPieceMove(Player player, Piece playedPiece, int stack,
			Cell target, Piece existingTargetPiece) {
		super(player, playedPiece, target, existingTargetPiece);
		this.stackNum = stack;
	}

	public int getStackNum() {
		return stackNum;
	}

	@Override
	public State applyToGame(Game game) {
		return game.placePlayerPiece(stackNum, getTargetCell());
	}

}
