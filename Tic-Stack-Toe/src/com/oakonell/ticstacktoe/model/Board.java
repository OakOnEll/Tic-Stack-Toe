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
		List<Piece> pieces = new ArrayList<Piece>();

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

		public Piece peekNextPiece() {
			if (pieces.size() <= 1)
				return null;
			return pieces.get(1);
		}

		public int getNumber() {
			// TODO Auto-generated method stub
			return 0;
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
		boundsCheck(x, y);
		return board[x][y].getTopPiece();
	}

	public Piece peekNextPiece(Cell cell) {
		boundsCheck(cell.x, cell.y);
		return board[cell.x][cell.y].peekNextPiece();
	}

	private void boundsCheck(int x, int y) {
		if (x < 0 || x >= size) {
			throw new IllegalArgumentException("x value (" + x
					+ " invalid, should be between [0," + size + "]");
		}
		if (y < 0 || y >= size) {
			throw new IllegalArgumentException("y value (" + y
					+ " invalid, should be between [0," + size + "]");
		}
	}

	public State placePiece(Cell cell, Player player, Piece piece, int stackNum) {
		boundsCheck(cell.x, cell.y);
		if (state.isOver()) {
			throw new IllegalArgumentException(
					"Game is already over, unable to make move");
		}
		Piece existing = board[cell.x][cell.y].getTopPiece();
		if (existing != null && !piece.isLargerThan(existing)) {
			throw new InvalidMoveException("Cell " + cell.x + ", " + cell.y
					+ " already has a larger " + existing
					+ ", unable to place a " + piece + " there",
					R.string.invalid_move_space_has_bigger_piece);
		}
		board[cell.x][cell.y].add(piece);
		AbstractMove move = new PlaceNewPieceMove(player, piece, stackNum,
				cell, existing);
		return evaluateAndStore(move);
	}

	public State moveFrom(Player player, Cell from, Cell to) {
		boundsCheck(from.x, from.y);
		boundsCheck(to.x, to.y);
		Piece movedPiece = board[from.x][from.y].getTopPiece();
		if (movedPiece == null) {
			throw new InvalidMoveException("Cell " + from.x + ", " + from.y
					+ " does not have any pieces, unable to move",
					R.string.invalid_move_source_is_empty);
		}
		Piece existingTarget = board[to.x][to.y].getTopPiece();
		if (existingTarget != null && !movedPiece.isLargerThan(existingTarget)) {
			throw new InvalidMoveException("Cell " + to.x + ", " + to.y
					+ " already has a larger " + existingTarget
					+ ", unable to place " + movedPiece + " there",
					R.string.invalid_move_space_has_bigger_piece);
		}

		board[from.x][from.y].removeTopPiece();
		Piece previousTarget = board[to.x][to.y].getTopPiece();
		board[to.x][to.y].add(movedPiece);
		Piece previousSource = board[from.x][from.y].getTopPiece();

		AbstractMove move = new ExistingPieceMove(player, movedPiece,
				previousSource, from, to, previousTarget);
		return evaluateAndStore(move);
	}

	private State evaluateAndStore(AbstractMove move) {
		state = evaluate(move);
		return state;
	}

	private State evaluate(AbstractMove move) {
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
				Piece topPiece = board[x][y].getTopPiece();
				int val = topPiece != null ? topPiece.getVal() : 0;
				if (val >= 0) {
					squareChar = val + "";
				} else {
					squareChar = (10 + val) + "";
				}
				builder.append(squareChar);
			}
		}
		return builder.toString();
	}

}
