package com.oakonell.ticstacktoe.model;

public class Move {
	private final Player player;
	private final Cell from;
	private final Cell to;
	private final Piece previousMarker;
	private final Piece playedMarker;

	public Move(Player player, Piece playedMarker, Cell cell,
			Piece previousMarker) {
		this.player = player;
		from = null;
		this.to = cell;
		this.previousMarker = previousMarker;
		this.playedMarker = playedMarker;
	}

	public Move(Player player, Piece existing, Cell from, Cell to, Piece previousTarget) {
		this.from  = from;
		this.to = to;
		this.player = player;
		this.previousMarker = previousTarget;
		this.playedMarker = existing;		
	}

	public Player getPlayer() {
		return player;
	}

	public Cell getTargetCell() {
		return to;
	}

	public Cell getSourceCell() {
		return from;
	}

	public Piece getPreviousMarker() {
		return previousMarker;
	}

	public Piece getPlayedMarker() {
		return playedMarker;
	}

}
