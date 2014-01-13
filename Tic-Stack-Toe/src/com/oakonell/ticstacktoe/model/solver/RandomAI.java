package com.oakonell.ticstacktoe.model.solver;

import android.net.Uri;

import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.Board;
import com.oakonell.ticstacktoe.model.Cell;
import com.oakonell.ticstacktoe.model.Piece;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.PlayerStrategy;

public class RandomAI extends PlayerStrategy {

	public static Player createPlayer(String name, boolean isBlack) {
		Player player = new Player(name, getImageUri(), new RandomAI(isBlack));
		return player;
	}

	private RandomAI(boolean isBlack) {
		super(isBlack);
	}

	private static Uri getImageUri() {
		return Uri.parse("android.resource://com.oakonell.ticstacktoe/"
				+ R.drawable.dice_icon_14730);
	}

	@Override
	public boolean isAI() {
		return true;
	}

	public Cell move(Board board, Piece toPlay) {
		/*
		 * if (toPlay == Piece.EMPTY) { return pickMarkerToRemove(board); } int
		 * numEmpty = 0; int size = board.getSize(); for (int x = 0; x < size;
		 * x++) { for (int y = 0; y < size; y++) { Piece cell = board.getCell(x,
		 * y); if (cell == Piece.EMPTY) { numEmpty++; } } } int cellNum =
		 * MarkerChance.random.nextInt(numEmpty); int count = cellNum; for (int
		 * x = 0; x < size; x++) { for (int y = 0; y < size; y++) { Piece cell =
		 * board.getCell(x, y); if (cell == Piece.EMPTY) { if (count == 0) {
		 * return new Cell(x, y); } count--; } } }
		 */
		throw new RuntimeException("Can't get here");
	}

}
