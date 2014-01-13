package com.oakonell.ticstacktoe.model;

public class ScoreCard {
	private int blackWins;
	private int whiteWins;
	private int draws;

	public ScoreCard(int xwins, int owins, int draws) {
		this.blackWins = xwins;
		this.whiteWins = owins;
		this.draws = draws;
	}

	public int getBlackWins() {
		return blackWins;
	}

	public void setBlackWins(int xWins) {
		this.blackWins = xWins;
	}

	public int getWhiteWins() {
		return whiteWins;
	}

	public void setWhiteWins(int oWins) {
		this.whiteWins = oWins;
	}

	public int getDraws() {
		return draws;
	}

	public void setDraws(int draws) {
		this.draws = draws;
	}

	public void incrementScore(Player winner) {
		if (winner == null) {
			draws++;
		} else if (winner.isBlack()) {
			blackWins++;
		} else {
			whiteWins++;
		}
	}

	public int getTotalGames() {
		return blackWins + whiteWins + draws;
	}
}
