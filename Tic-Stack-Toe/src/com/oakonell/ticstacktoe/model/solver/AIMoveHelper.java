package com.oakonell.ticstacktoe.model.solver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Board;
import com.oakonell.ticstacktoe.model.Board.PieceStack;
import com.oakonell.ticstacktoe.model.Cell;
import com.oakonell.ticstacktoe.model.ExistingPieceMove;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Piece;
import com.oakonell.ticstacktoe.model.PlaceNewPieceMove;
import com.oakonell.ticstacktoe.model.Player;

/**
 * A utility class to get the list of valid moves for a given game type.
 */
public class AIMoveHelper {
	public static List<AbstractMove> getValidMoves(GameType type,
			List<PieceStack> blackPlayerPieces,
			List<PieceStack> whitePlayerPieces, Board board, Player player) {
		List<AbstractMove> result = new ArrayList<AbstractMove>();
		// add new piece moves from stack
		List<PieceStack> stacks;
		if (player.isBlack()) {
			stacks = blackPlayerPieces;
		} else {
			stacks = whitePlayerPieces;
		}

		addStackMoves(result, type, stacks, board, player);

		// add board to board moves
		addBoardMoves(result, board, player);

		return result;
	}

	private static void addBoardMoves(List<AbstractMove> result, Board board,
			Player currentPlayer) {
		int size = board.getSize();
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				Piece piece = board.getVisiblePiece(x, y);
				if (piece != null && piece.isBlack() == currentPlayer.isBlack()) {
					Cell fromCell = new Cell(x, y);
					Piece nextPiece = board.peekNextPiece(fromCell);
					addMovesFrom(result, fromCell, piece, nextPiece, board,
							currentPlayer);
				}
			}
		}
	}

	private static void addMovesFrom(List<AbstractMove> result, Cell fromCell,
			Piece playedPiece, Piece nextPiece, Board board,
			Player currentPlayer) {
		int size = board.getSize();
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				if (x == fromCell.getX() && y == fromCell.getY()) {
					continue;
				}
				Piece piece = board.getVisiblePiece(x, y);
				if (piece == null || playedPiece.isLargerThan(piece)) {
					result.add(new ExistingPieceMove(currentPlayer,
							playedPiece, nextPiece, fromCell, new Cell(x, y),
							piece));
				}
			}
		}
	}

	private static void addStackMoves(List<AbstractMove> result, GameType type,
			List<PieceStack> stacks, Board board, Player currentPlayer) {
		// look through each top of the stack piece, and see where it can be
		// played
		// optimize to only try new pieces
		Set<Piece> alreadySeen = new HashSet<Piece>();
		for (int i = 0; i < stacks.size(); i++) {
			PieceStack stack = stacks.get(i);
			Piece piece = stack.getTopPiece();
			if (piece == null) {
				continue;
			}
			if (alreadySeen.contains(piece)) {
				continue;
			}
			alreadySeen.add(piece);
			addMovesFromStack(result, type, piece, i, stack, board,
					currentPlayer);
		}
	}

	private static void addMovesFromStack(List<AbstractMove> result,
			GameType type, Piece playedPiece, int stackNum, PieceStack stack,
			Board board, Player currentPlayer) {

		int size = board.getSize();
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				Piece piece = board.getVisiblePiece(x, y);

				if (piece == null) {
					result.add(new PlaceNewPieceMove(currentPlayer,
							playedPiece, stackNum, new Cell(x, y), piece));
					continue;
				}
				Cell target = new Cell(x, y);
				boolean canMoveToOccupied = !type.isStrict()
						|| (board.isPartOfThreeInARow(piece, target) && piece
								.isBlack() != playedPiece.isBlack());
				if (canMoveToOccupied && playedPiece.isLargerThan(piece)) {
					result.add(new PlaceNewPieceMove(currentPlayer,
							playedPiece, stackNum, target, piece));
				}
			}
		}

	}

}
