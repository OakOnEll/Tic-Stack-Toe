package com.oakonell.ticstacktoe.model;

import java.util.ArrayList;
import java.util.List;

import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.State.Win;
import com.oakonell.ticstacktoe.ui.game.WinOverlayView.WinStyle;

/**
 * This class represents the game board, an nxn board where each tile is
 * represented by a PieceStack or null.
 */
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

		public PieceStack copy() {
			PieceStack copy = new PieceStack();
			for (Piece piece : pieces) {
				copy.pieces.add(piece);
			}
			return copy;
		}
	}

	public Board(int size) {
		this.size = size;
		board = new PieceStack[size][size];
		initializeBoard();
	}

	public Board(PieceStack[][] board) {
		this.size = board.length;
		this.board = board;
	}

	private void initializeBoard() {
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				board[x][y] = new PieceStack();
			}
		}
	}

	public Piece getVisiblePiece(Cell cell) {
		return getVisiblePiece(cell.x, cell.y);
	}

	public Piece getVisiblePiece(int x, int y) {
		boundsCheck(x, y);
		return board[x][y].getTopPiece();
	}

	public PieceStack getStackAt(int x, int y) {
		boundsCheck(x, y);
		return board[x][y];
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

	public State placePiece(Cell cell, Player player, Piece piece,
			int stackNum, GameType gameType) {
		boundsCheck(cell.x, cell.y);
		if (state.isOver()) {
			throw new IllegalArgumentException(
					"Game is already over, unable to make move");
		}
		Piece existing = board[cell.x][cell.y].getTopPiece();
		if (existing != null) {
			if (!piece.isLargerThan(existing)) {
				throw new InvalidMoveException("Cell " + cell.x + ", " + cell.y
						+ " already has a larger " + existing
						+ ", unable to place a " + piece + " there",
						R.string.invalid_move_space_has_bigger_piece);
			}
			if (gameType.isStrict()) {
				// you can only place a new piece over an existing one
				// if it is part of a three-in-a-row opponent piece
				if (existing.isBlack() == piece.isBlack()) {
					throw new InvalidMoveException("Cell " + cell.x + ", "
							+ cell.y + " already has a piece " + existing
							+ ", unable to place a new " + piece
							+ " there in 'strict' game type",
							R.string.invalid_stack_move_in_strict);
				}
				// make sure that the current piece participates in a 3 in a row
				if (!isPartOfThreeInARow(existing, cell)) {
					throw new InvalidMoveException("Cell " + cell.x + ", "
							+ cell.y + " already has a piece " + existing
							+ ", unable to place a new " + piece
							+ " there in 'strict' game type",
							R.string.invalid_stack_move_in_strict);
				}
			}
		}

		board[cell.x][cell.y].add(piece);
		AbstractMove move = new PlaceNewPieceMove(player, piece, stackNum,
				cell, existing);
		return evaluateAndStore(move);
	}

	public boolean isPartOfThreeInARow(Piece existing, Cell cell) {
		boolean isBlack = existing.isBlack();
		// check row
		int count = 0;
		for (int x = 0; x < size; x++) {
			Piece topPiece = board[x][cell.getY()].getTopPiece();
			if (topPiece != null && topPiece.isBlack() == isBlack) {
				count++;
			}
		}
		if (count > 2)
			return true;

		// check col
		count = 0;
		for (int y = 0; y < size; y++) {
			Piece topPiece = board[cell.getX()][y].getTopPiece();
			if (topPiece != null && topPiece.isBlack() == isBlack) {
				count++;
			}
		}
		if (count > 2)
			return true;

		// check diagonals
		if (cell.getX() == cell.getY()) {
			return isPartOfTopLeftDiagonalThreeInARow(existing, cell);
		} else if ((cell.getX() + cell.getY() + 1) % size == 0) {
			return isPartOfTopRightDiagonalThreeInARow(existing, cell);
		}

		return false;
	}

	private boolean isPartOfTopLeftDiagonalThreeInARow(Piece existing, Cell cell) {
		boolean isBlack = existing.isBlack();
		int count = 0;
		for (int x = 0; x < size; x++) {
			Piece topPiece = board[x][x].getTopPiece();
			if (topPiece != null && topPiece.isBlack() == isBlack) {
				count++;
			}
		}
		return count > 2;
	}

	private boolean isPartOfTopRightDiagonalThreeInARow(Piece existing,
			Cell cell) {
		boolean isBlack = existing.isBlack();
		int count = 0;
		for (int x = 0; x < size; x++) {
			Piece topPiece = board[x][size - x - 1].getTopPiece();
			if (topPiece != null && topPiece.isBlack() == isBlack) {
				count++;
			}
		}
		return count > 2;
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
		setState(evaluate(move));
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
			// prefer a win including the opponent's exposed piece at the source
			// cell
			if (move instanceof ExistingPieceMove) {
				ExistingPieceMove boardMove = (ExistingPieceMove) move;
				Cell source = boardMove.getSource();
				ArrayList<Win> uncoveredOpponentWins = new ArrayList<Win>();
				for (Win each : wins) {
					if (each.contains(source, size)) {
						Piece topPiece = board[source.getX()][source.getY()]
								.getTopPiece();
						if (topPiece.isBlack() != player.isBlack()) {
							uncoveredOpponentWins.add(each);
						}
					}
				}
				if (!uncoveredOpponentWins.isEmpty()) {
					return State.winner(move, uncoveredOpponentWins,
							player.opponent());
				}
			}
			winner = player;
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

	public Board copy() {
		Board copy = new Board(size);
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				PieceStack stack = board[x][y];
				copy.board[x][y] = stack.copy();
			}
		}
		return copy;
	}

	public void setState(State originalState) {
		state = originalState;
	}

	public void undoBoardMove(ExistingPieceMove existingPieceMove,
			State originalState) {
		setState(originalState);

		Cell targetCell = existingPieceMove.getTargetCell();
		Cell sourceCell = existingPieceMove.getSource();

		Piece playedPiece = board[targetCell.x][targetCell.y].removeTopPiece();
		board[sourceCell.x][sourceCell.y].add(playedPiece);
	}

	public void undoStackMove(PlaceNewPieceMove placeNewPieceMove,
			State originalState, List<PieceStack> blackPlayerPieces,
			List<PieceStack> whitePlayerPieces) {
		setState(originalState);

		Cell targetCell = placeNewPieceMove.getTargetCell();

		Piece playedPiece = board[targetCell.x][targetCell.y].removeTopPiece();

		List<PieceStack> stacks;
		if (placeNewPieceMove.getPlayer().isBlack()) {
			stacks = blackPlayerPieces;
		} else {
			stacks = whitePlayerPieces;
		}
		int stackNum = placeNewPieceMove.getStackNum();
		PieceStack stack = stacks.get(stackNum);
		stack.add(playedPiece);
	}

}
