package com.oakonell.ticstacktoe.model.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Board;
import com.oakonell.ticstacktoe.model.Board.PieceStack;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Piece;
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
		// when the memory depth for a particular cell is hit in the search,
		// return the heuristic?
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

		List<AbstractMove> moves = AIMoveHelper.getValidMoves(type,
				blackPlayerPieces, whitePlayerPieces, board, currentPlayer);
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
