package com.oakonell.ticstacktoe.model;

public class InvalidMoveException extends RuntimeException {
	int resId;

	public InvalidMoveException(String string, int resId) {
		super(string);
		this.resId = resId;
	}

	public int getErrorResourceId() {
		return resId;
	}

	
}
