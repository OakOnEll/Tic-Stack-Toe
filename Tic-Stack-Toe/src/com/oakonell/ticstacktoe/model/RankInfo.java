package com.oakonell.ticstacktoe.model;

public class RankInfo {
	private short blackRank;
	private short whiteRank;

	public RankInfo(short black, short white) {
		this.blackRank = black;
		this.whiteRank = white;
	}

	public short blackRank() {
		return blackRank;
	}

	public short whiteRank() {
		return whiteRank;
	}

	public void setBlackRank(short myRank) {
		blackRank = myRank;
	}

	public void setWhiteRank(short myRank) {
		whiteRank = myRank;
	}
}
