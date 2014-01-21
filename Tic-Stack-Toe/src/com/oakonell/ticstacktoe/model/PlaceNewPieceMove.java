package com.oakonell.ticstacktoe.model;

import java.util.List;

import com.oakonell.ticstacktoe.model.Board.PieceStack;

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

	@Override
	public State applyTo(GameType type, Board board,
			List<PieceStack> blackPlayerPieces,
			List<PieceStack> whitePlayerPieces) {
		List<PieceStack> stacks;
		if (getPlayer().isBlack()) {
			stacks = blackPlayerPieces;
		} else {
			stacks = whitePlayerPieces;
		}
		PieceStack stack = stacks.get(stackNum);
		stack.removeTopPiece();
		return board.placePiece(getTargetCell(), getPlayer(), getPlayedPiece(),
				stackNum, type);
	}

	@Override
	public void undo(Board board,State originalState,List<PieceStack> blackPlayerPieces,
			List<PieceStack> whitePlayerPieces) {
		board.undoStackMove(this, originalState, blackPlayerPieces, whitePlayerPieces);
	}
}
