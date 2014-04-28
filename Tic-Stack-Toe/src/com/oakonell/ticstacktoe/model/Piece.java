package com.oakonell.ticstacktoe.model;

import com.oakonell.ticstacktoe.R;

/**
 * An enumeration of all the differently sized pieces that exist in the game
 */
public enum Piece {
	// Black pieces
	BLACK1(1, R.drawable.black_piece1), //
	BLACK2(2, R.drawable.black_piece2), //
	BLACK3(3, R.drawable.black_piece3), //
	BLACK4(4, R.drawable.black_piece4),

	// White pieces
	WHITE1(-1, R.drawable.white_piece1), //
	WHITE2(-2, R.drawable.white_piece2), //
	WHITE3(-3, R.drawable.white_piece3), //
	WHITE4(-4, R.drawable.white_piece4);//

	private final int val;
	private final int resId;

	private Piece(int val, int resourceId) {
		this.val = val;
		this.resId = resourceId;
	}

	public int getVal() {
		return val;
	}

	public int getImageResourceId() {
		return resId;
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

	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (isBlack()) {
			builder.append("black_");
		} else {
			builder.append("white_");
		}
		builder.append(Math.abs(val));
		return builder.toString();
	}
}
