package com.oakonell.ticstacktoe.model;

import java.util.ArrayList;
import java.util.List;

import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.State.Win;
import com.oakonell.ticstacktoe.ui.game.WinOverlayView.WinStyle;

public class Board {
	private final int size;
	private final PieceStack[][] board;
	private State state = State.open(null);

	public static class PieceStack {
		List<Piece> pieces;

		public Piece getTopPiece() {
			if (pieces.isEmpty())
				return null;
			return pieces.get(0);
		}

		public void add(Piece mark) {
			pieces.add(0, mark);
		}

		public Piece removeTopPiece() {
			return pieces.remove(0);
		}
	}

	public Board(int size) {
		this.size = size;
		board = new PieceStack[size][size];
		initializeBoard();
	}

	private void initializeBoard() {
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				board[x][y] = new PieceStack();
			}
		}
	}

	public Piece getVisiblePiece(int x, int y) {
		if (x < 0 || x >= size) {
			throw new IllegalArgumentException("x value (" + x
					+ " invalid, should be between [0," + size + "]");
		}
		if (y < 0 || y >= size) {
			throw new IllegalArgumentException("y value (" + y
					+ " invalid, should be between [0," + size + "]");
		}
		return board[x][y].getTopPiece();
	}

	public State placeMarker(Cell cell, Player player, Piece mark) {
		if (state.isOver()) {
			throw new IllegalArgumentException(
					"Game is already over, unable to make move");
		}
		if (cell.x < 0 || cell.x >= size) {
			throw new IllegalArgumentException("x value (" + cell.x
					+ " invalid, should be between [0," + size + "]");
		}
		if (cell.y < 0 || cell.y >= size) {
			throw new IllegalArgumentException("y value (" + cell.y
					+ " invalid, should be between [0," + size + "]");
		}
		Piece existing = board[cell.x][cell.y].getTopPiece();
		if (existing != null && existing.isLargerThan(mark)) {
			throw new InvalidMoveException("Cell " + cell.x + ", " + cell.y
					+ " already has a larger " + existing
					+ ", unable to place a " + mark + " there",
					R.string.invalid_move_space_has_bigger_piece);
		}
		board[cell.x][cell.y].add(mark);
		Move move = new Move(player, mark, cell, existing);
		return evaluateAndStore(move);
	}

	public State moveFrom(Player player, Cell from, Cell to) {
		if (from.x < 0 || from.x >= size) {
			throw new IllegalArgumentException("from x value (" + from.x
					+ " invalid, should be between [0," + size + "]");
		}
		if (from.y < 0 || from.y >= size) {
			throw new IllegalArgumentException("from y value (" + from.y
					+ " invalid, should be between [0," + size + "]");
		}
		if (to.x < 0 || to.x >= size) {
			throw new IllegalArgumentException("to x value (" + to.x
					+ " invalid, should be between [0," + size + "]");
		}
		if (to.y < 0 || to.y >= size) {
			throw new IllegalArgumentException("to y value (" + to.y
					+ " invalid, should be between [0," + size + "]");
		}
		Piece existing = board[from.x][from.y].getTopPiece();
		if (existing == null) {
			throw new InvalidMoveException("Cell " + from.x + ", " + from.y
					+ " does not have any pieces, unable to move",
					R.string.invalid_move_source_is_empty);
		}
		Piece target = board[to.x][to.y].getTopPiece();
		if (target != null && target.isLargerThan(existing)) {
			throw new InvalidMoveException("Cell " + to.x + ", " + to.y
					+ " already has a larger " + target + ", unable to place "
					+ existing + " there",
					R.string.invalid_move_space_has_bigger_piece);
		}

		board[from.x][from.y].removeTopPiece();
		Piece previousTarget = board[to.x][to.y].getTopPiece();
		board[to.x][to.y].add(existing);

		Move move = new Move(player, existing, from, to, previousTarget);
		return evaluateAndStore(move);
	}

	private State evaluateAndStore(Move move) {
		state = evaluate(move);
		return state;
	}

	private State evaluate(Move move) {
		Player player = move.getPlayer();
		List<Win> wins = new ArrayList<Win>();
		// Inspect the columns
		for (int x = 0; x < size; ++x) {
			int sum = 0;

			for (int y = 0; y < size; ++y) {
				sum += getPieceWinValue(board[x][y].getTopPiece());
			}

			int score = sum;
			if (Math.abs(score) == size) {
				wins.add(new Win(new Cell(x, 0), new Cell(x, size - 1),
						WinStyle.row(x)));
			}
		}

		// Inspect the rows
		for (int y = 0; y < size; ++y) {
			int sum = 0;

			for (int x = 0; x < size; ++x) {
				sum += getPieceWinValue(board[x][y].getTopPiece());
			}

			int score = sum;
			if (Math.abs(score) == size) {
				wins.add(new Win(new Cell(0, y), new Cell(size - 1, y),
						WinStyle.column(y)));
			}
		}

		int sum = 0;

		// Inspect the top-left/bottom-right diagonal
		for (int x = 0; x < size; ++x) {
			sum += getPieceWinValue(board[x][x].getTopPiece());
		}

		int score = sum;
		if (Math.abs(score) == size) {
			wins.add(new Win(new Cell(0, 0), new Cell(size - 1, size - 1),
					WinStyle.TOP_LEFT_DIAG));
		}

		sum = 0;
		// Inspect the bottom-right/top-right
		for (int x = 0; x < size; ++x) {
			sum += getPieceWinValue(board[x][size - x - 1].getTopPiece());
		}

		score = sum;
		if (Math.abs(score) == size) {
			wins.add(new Win(new Cell(0, size - 1), new Cell(size - 1, 0),
					WinStyle.TOP_RIGHT_DIAG));
		}
		if (!wins.isEmpty()) {
			Player winner;
			// TODO deal with an exposed opponent win... black picks up black
			// piece, exposing white underneath for a win that can't be blocked
			winner = player;
			// } else {
			// winner = player.opponent();
			// }
			return State.winner(move, wins, winner);
		}

		return State.open(move);
	}

	private int getPieceWinValue(Piece topPiece) {
		if (topPiece == null) {
			return 0;
		}
		return topPiece.getVal() > 0 ? 1 : -1;
	}

	public int getSize() {
		return size;
	}

	public State getState() {
		return state;
	}

	public String getVisibleBoardStateAsLong() {
		StringBuilder builder = new StringBuilder();
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				String squareChar = "0";
				int val = board[x][y].getTopPiece().getVal();
				if (board[x][y].getTopPiece().getVal() > 0) {
					squareChar = val + "";
				} else {
					squareChar = (10 - val) + "";
				}
				builder.append(squareChar);
			}
		}
		return builder.toString();
	}

}
