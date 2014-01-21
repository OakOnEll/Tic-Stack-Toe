package com.oakonell.ticstacktoe.model.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
		this.player = player;
		if (depth < 0)
			throw new RuntimeException("Search-tree depth cannot be negative");
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
				whitePlayerPieces, depth, player, Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY);
		if (solve.move == null) {
			throw new RuntimeException("Move should not be null!");
		}
		return solve.move;
	}

	private MoveAndScore solve(GameType type, Board board,
			List<PieceStack> blackPlayerPieces,
			List<PieceStack> whitePlayerPieces, int depth,
			Player currentPlayer, double theAlpha, double theBeta) {
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

		AbstractMove bestMove = null;
		double alpha = theAlpha;
		double beta = theBeta;

		List<AbstractMove> moves = getValidMoves(type, blackPlayerPieces,
				whitePlayerPieces, board, currentPlayer);
		State originalState = board.getState();
		for (AbstractMove move : moves) {
			move.applyTo(type, board, blackPlayerPieces, whitePlayerPieces);
			double currentScore;
			if (currentPlayer == player) {
				currentScore = solve(type, board, blackPlayerPieces,
						whitePlayerPieces, depth - 1, currentPlayer.opponent(),
						alpha, beta).score;
				if (currentScore > alpha) {
					alpha = currentScore;
					bestMove = move;
				}
				if (currentScore == alpha) {
					// accept the new move with some probability, to not always be deterministic
					if (random.nextInt(2)==0) {
						alpha = currentScore;
						bestMove = move;						
					}
				}
			} else {
				currentScore = solve(type, board, blackPlayerPieces,
						whitePlayerPieces, depth - 1, currentPlayer.opponent(),
						alpha, beta).score;
				if (currentScore < beta) {
					beta = currentScore;
					bestMove = move;
				}
				if (currentScore == beta) {
					// accept the new move with some probability, to not always be deterministic
					if (random.nextInt(2)==0) {
						beta = currentScore;
						bestMove = move;
					}
				}
			}
			move.undo(board, originalState, blackPlayerPieces,
					whitePlayerPieces);
			if (alpha >= beta)
				break;
		}
		double bestScore = currentPlayer.equals(player) ? alpha : beta;
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
		for (int i = 0; i < stacks.size(); i++) {
			PieceStack stack = stacks.get(i);
			Piece piece = stack.getTopPiece();
			if (piece == null)
				continue;
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
						|| board.isPartOfThreeInARow(piece, target);
				if (canMoveToOccupied && playedPiece.isLargerThan(piece)) {
					result.add(new PlaceNewPieceMove(currentPlayer,
							playedPiece, stackNum, target, piece));
				}
			}
		}

	}

	private int scoreLine(int size, int numMine, int numOpponent) {
		if (numOpponent == 0) {
			return (int) Math.pow(10, numMine);
		}
		if (numMine == 0) {
			return (int) -Math.pow(10, numOpponent);
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
		double score;

		MoveAndScore(AbstractMove move, double score) {
			this.score = score;
			this.move = move;
		}
	}

}
