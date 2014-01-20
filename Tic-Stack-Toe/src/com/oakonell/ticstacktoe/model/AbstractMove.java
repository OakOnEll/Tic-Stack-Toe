package com.oakonell.ticstacktoe.model;

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

}
