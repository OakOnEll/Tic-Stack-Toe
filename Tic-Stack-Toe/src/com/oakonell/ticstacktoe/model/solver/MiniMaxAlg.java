package com.oakonell.ticstacktoe.model.solver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Board;
import com.oakonell.ticstacktoe.model.Board.PieceStack;
import com.oakonell.ticstacktoe.model.Cell;
import com.oakonell.ticstacktoe.model.ExistingPieceMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Piece;
import com.oakonell.ticstacktoe.model.PlaceNewPieceMove;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.State;

/**
 * MiniMax solving algorithm with Alpha/Beta pruning
 */
public class MiniMaxAlg {
	private final Player player;
	private final int depth;

	private final static Random random = new Random();

	public MiniMaxAlg(Player player, int depth) {
		// TODO include a memory depth, separate from the search depth?
		//   when the memory depth for a particular cell is hit in the search, return the heuristic? 
		this.player = player;
		if (depth <= 0)
			throw new RuntimeException("Search-tree depth must be positive");
		this.depth = depth;
	}

	public int getDepth() {
		return depth;
	}

	public AbstractMove solve(Game game) {
		Board copy = game.getBoard().copy();
		List<PieceStack> blackPlayerPieces = new ArrayList<Board.PieceStack>(
				game.getBlackPlayerPieces());
		List<PieceStack> whitePlayerPieces = new ArrayList<Board.PieceStack>(
				game.getWhitePlayerPieces());

		MoveAndScore solve = solve(game.getType(), copy, blackPlayerPieces,
				whitePlayerPieces, depth, player, Integer.MIN_VALUE,
				Integer.MAX_VALUE, true);
		if (solve.move == null) {
			throw new RuntimeException("Move should not be null!");
		}
		return solve.move;
	}

	private MoveAndScore solve(GameType type, Board board,
			List<PieceStack> blackPlayerPieces,
			List<PieceStack> whitePlayerPieces, int depth,
			Player currentPlayer, int theAlpha, int theBeta, boolean isFirst) {
		State state = board.getState();
		if (state.isOver()) {
			// how can moves be empty is state is not over?!
			// game is over
			Player winner = state.getWinner();
			if (winner != null) {
				// someone won, give the heuristic score
				return new MoveAndScore(null, getHeuristicScore(board));
			}
			// game was a draw, score is 0
			return new MoveAndScore(null, 0);
		}
		// reached the search depth
		if (depth == 0) {
			return new MoveAndScore(null, getHeuristicScore(board));
		}

		List<MoveAndScore> bestMoves = null;
		// AbstractMove bestMove = null;
		int alpha = theAlpha;
		int beta = theBeta;

		List<AbstractMove> moves = getValidMoves(type, blackPlayerPieces,
				whitePlayerPieces, board, currentPlayer);
		State originalState = board.getState();
		for (AbstractMove move : moves) {
			move.applyTo(type, board, blackPlayerPieces, whitePlayerPieces);
			int currentScore;
			MoveAndScore childMoveAndScore = solve(type, board,
					blackPlayerPieces, whitePlayerPieces, depth - 1,
					currentPlayer.opponent(), alpha, beta, false);
			// discount deeper scores, prefer strategies with less moves
			currentScore = (int) (childMoveAndScore.score - 1);
			MoveAndScore currentMoveAndScore = new MoveAndScore(move,
					currentScore);
			if (currentPlayer.equals(player)) {
				if (currentScore == alpha) {
					if (bestMoves != null) {
						bestMoves.add(currentMoveAndScore);
					}
				} else if (currentScore > alpha) {
					alpha = currentScore;
					bestMoves = new ArrayList<MoveAndScore>();
					bestMoves.add(currentMoveAndScore);
				}
			} else {
				if (currentScore == beta) {
					if (bestMoves != null) {
						bestMoves.add(currentMoveAndScore);
					}
				} else if (currentScore < beta) {
					beta = currentScore;
					bestMoves = new ArrayList<MoveAndScore>();
					bestMoves.add(currentMoveAndScore);
				}
			}
			move.undo(board, originalState, blackPlayerPieces,
					whitePlayerPieces);
			if (alpha >= beta)
				break;
		}
		int bestScore = currentPlayer.equals(player) ? alpha : beta;
		// pick one of the equal scoring moves at random
		AbstractMove bestMove = null;
		if (bestMoves != null) {
			// if (isFirst) {
			// Log.i("MiniMax", "MiniMax bestMoves " + bestMoves);
			// }
			if (isFirst) {
				int index = random.nextInt(bestMoves.size());
				bestMove = bestMoves.get(index).move;
			} else {
				bestMove = bestMoves.get(0).move;
			}
		}

		return new MoveAndScore(bestMove, bestScore);
	}

	private List<AbstractMove> getValidMoves(GameType type,
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

	private void addBoardMoves(List<AbstractMove> result, Board board,
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

	private void addMovesFrom(List<AbstractMove> result, Cell fromCell,
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

	private void addStackMoves(List<AbstractMove> result, GameType type,
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

	private void addMovesFromStack(List<AbstractMove> result, GameType type,
			Piece playedPiece, int stackNum, PieceStack stack, Board board,
			Player currentPlayer) {

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

	private int scoreLine(int size, int numMine, int numOpponent) {
		if (numOpponent == 0) {
			return (int) Math.pow(10, numMine + 1);
		}
		if (numMine == 0) {
			return (int) -Math.pow(10, numOpponent + 1);
		}
		return 0;
	}

	private int getHeuristicScore(Board board) {
		int size = board.getSize();
		int score = 0;
		boolean playerIsBlack = player.isBlack();

		// Inspect the columns
		for (int x = 0; x < size; ++x) {
			int numMine = 0;
			int numOpponent = 0;

			for (int y = 0; y < size; ++y) {
				Piece piece = board.getVisiblePiece(x, y);
				if (piece == null)
					continue;
				if (piece.isBlack() == playerIsBlack) {
					numMine++;
				} else {
					numOpponent++;
				}
			}

			score += scoreLine(size, numMine, numOpponent);
		}

		// Inspect the rows
		for (int y = 0; y < size; ++y) {
			int numMine = 0;
			int numOpponent = 0;

			for (int x = 0; x < size; ++x) {
				Piece piece = board.getVisiblePiece(x, y);
				if (piece == null)
					continue;
				if (piece.isBlack() == playerIsBlack) {
					numMine++;
				} else {
					numOpponent++;
				}
			}

			score += scoreLine(size, numMine, numOpponent);
		}

		// Inspect the top-left/bottom-right diagonal
		int numMine = 0;
		int numOpponent = 0;
		for (int x = 0; x < size; ++x) {
			Piece piece = board.getVisiblePiece(x, x);
			if (piece == null)
				continue;
			if (piece.isBlack() == playerIsBlack) {
				numMine++;
			} else {
				numOpponent++;
			}
		}

		score += scoreLine(size, numMine, numOpponent);

		numMine = 0;
		numOpponent = 0;
		// Inspect the bottom-right/top-right
		for (int x = 0; x < size; ++x) {
			Piece piece = board.getVisiblePiece(x, size - x - 1);
			if (piece == null)
				continue;
			if (piece.isBlack() == playerIsBlack) {
				numMine++;
			} else {
				numOpponent++;
			}
		}

		score += scoreLine(size, numMine, numOpponent);

		return score;
	}

	public static class MoveAndScore {
		AbstractMove move;
		int score;

		MoveAndScore(AbstractMove move, int score) {
			this.score = score;
			this.move = move;
		}

		public String toString() {
			StringBuilder builder = new StringBuilder("Move: ");
			builder.append(move.toString());
			builder.append(", Score:");
			builder.append(score);
			return builder.toString();
		}
	}

}
