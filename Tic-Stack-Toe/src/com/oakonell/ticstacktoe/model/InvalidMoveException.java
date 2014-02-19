package com.oakonell.ticstacktoe.model;

public class InvalidMoveException extends RuntimeException {
	private static final long serialVersionUID = 6228711269185435150L;
	private final int resId;

	public InvalidMoveException(String string, int resId) {
		super(string);
		this.resId = resId;
	}

	public int getErrorResourceId() {
		return resId;
	}

}
