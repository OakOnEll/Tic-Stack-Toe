package com.oakonell.ticstacktoe.model;

import java.util.List;

import com.oakonell.ticstacktoe.model.Board.PieceStack;

public abstract class AbstractMove {
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

	public abstract void undo(Board board, State originalState, List<PieceStack> blackPlayerPieces,
			List<PieceStack> whitePlayerPieces);

}
