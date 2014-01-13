package com.oakonell.ticstacktoe.model;

public enum Piece {
	BLACK1(1), BLACK2(2), BLACK3(3), BLACK4(4), WHITE1(-1), WHITE2(-2), WHITE3(
			-3), WHITE4(-4), EMPTY(0);

	private final int val;

	private Piece(int val) {
		this.val = val;
	}

	public int getVal() {
		return val;
	}

	public static Piece fromInt(int i) {
		for (Piece each : values()) {
			if (each.getVal() == i) {
				return each;
			}
		}
		return null;
	}

	public boolean isLargerThan(Piece mark) {
		return Math.abs(getVal()) > Math.abs(mark.getVal());
	}

	public boolean isBlack() {
		return getVal() > 0;
	}

	public boolean isWhite() {
		return getVal() < 0;
	}
}
