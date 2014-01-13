package com.oakonell.ticstacktoe.model.solver;

import com.oakonell.ticstacktoe.model.Player;

/**
 * MiniMax solving algorithm with Alpha/Beta pruning
 */
public class MiniMaxAlg {
	private final Player player;
	private final int depth;

	public MiniMaxAlg(Player player, int depth) {
		this.player = player;
		if (depth < 0)
			throw new RuntimeException("Search-tree depth cannot be negative");
		this.depth = depth;
	}

	public int getDepth() {
		return depth;
	}

	/*
	 * public Cell solve(Board board, Piece toPlay) { Board copy = board.copy();
	 * MoveAndScore solve = solve(copy, depth, player, toPlay,
	 * Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY); if (solve.move ==
	 * null) { throw new RuntimeException("Move should not be null!"); } return
	 * solve.move; }
	 * 
	 * private MoveAndScore solve(Board board, int depth, Player currentPlayer,
	 * Piece toPlay, double theAlpha, double theBeta) { State state =
	 * board.getState(); if (state.isOver()) { // how can moves be empty is
	 * state is not over?! // game is over Player winner = state.getWinner(); if
	 * (winner != null) { // someone won, give the heuristic score return new
	 * MoveAndScore(null, getHeuristicScore(board)); } // game was a draw, score
	 * is 0 return new MoveAndScore(null, 0); } // reached the search depth if
	 * (depth == 0) { return new MoveAndScore(null, getHeuristicScore(board)); }
	 * 
	 * Cell bestMove = null; double alpha = theAlpha; double beta = theBeta;
	 * 
	 * List<MoveAndWeight> moves = getValidMoves(board, currentPlayer, toPlay);
	 * for (MoveAndWeight move : moves) { Piece original = null; if (move.marker
	 * == Piece.EMPTY) { original = board.getCell(move.move.getX(),
	 * move.move.getY()); board.removeMarker(move.move, player); } else {
	 * board.placeMarker(move.move, player, move.marker); } double currentScore;
	 * if (currentPlayer == player) { currentScore = solve(board, depth - 1,
	 * currentPlayer.opponent(), null, alpha, beta).score move.weight; if
	 * (currentScore > alpha) { alpha = currentScore; bestMove = move.move; } }
	 * else { currentScore = solve(board, depth - 1, currentPlayer.opponent(),
	 * null, alpha, beta).score move.weight; if (currentScore < beta) { beta =
	 * currentScore; bestMove = move.move; } } if (move.marker == Piece.EMPTY) {
	 * board.placeMarker(move.move, player, original); } else {
	 * board.clearMarker(move.move, player); } if (alpha >= beta) break; }
	 * double bestScore = currentPlayer == player ? alpha : beta; return new
	 * MoveAndScore(bestMove, bestScore); }
	 * 
	 * private List<MoveAndWeight> getValidMoves(Board board, Player player,
	 * Piece toPlay) { List<MoveAndWeight> result = new
	 * ArrayList<MoveAndWeight>(); if (toPlay != null) { addMoves(result, board,
	 * toPlay, 1); return result; } if (chance.getMyMarker() != 0) {
	 * addMoves(result, board, player.getMarker(),
	 * chance.getMyMarkerPercentage()); } if (chance.getOpponentMarker() != 0) {
	 * addMoves(result, board, player.opponent().getMarker(),
	 * chance.getOpponentMarkerPercentage()); } if (chance.getRemoveMarker() !=
	 * 0) { addMoves(result, board, Piece.EMPTY,
	 * chance.getRemoveMarkerPercentage()); } return result; }
	 * 
	 * private void addMoves(List<MoveAndWeight> result, Board board, Piece
	 * marker, double weight) { int size = board.getSize(); for (int x = 0; x <
	 * size; ++x) { for (int y = 0; y < size; ++y) { Piece boardMarker =
	 * board.getCell(x, y); Cell cell = new Cell(x, y); MoveAndWeight move = new
	 * MoveAndWeight(cell, marker, weight); if (boardMarker == Piece.EMPTY &&
	 * marker != Piece.EMPTY) { result.add(move); } else if (boardMarker !=
	 * Piece.EMPTY && marker == Piece.EMPTY) { result.add(move); } } } }
	 * 
	 * private int scoreLine(int size, int numMine, int numOpponent) { if
	 * (numOpponent == 0) { return (int) Math.pow(10, numMine); } if (numMine ==
	 * 0) { return (int) -Math.pow(10, numOpponent); } // if (numMine == 3) { //
	 * return 1000; // } // if (numOpponent == 3) { // return -1000; // } // //
	 * if (numMine == 2 && numOpponent == 0) // return 100; // if (numMine == 1
	 * && numOpponent == 0) // return 10; // // if (numMine == 0 && numOpponent
	 * == 2) // return -100; // if (numMine == 0 && numOpponent == 1) // return
	 * -10;
	 * 
	 * return 0; }
	 * 
	 * private int getHeuristicScore(Board board) { int size = board.getSize();
	 * int score = 0; Piece opponent = player.opponent().getMarker();
	 * 
	 * // Inspect the columns for (int x = 0; x < size; ++x) { int numMine = 0;
	 * int numOpponent = 0;
	 * 
	 * for (int y = 0; y < size; ++y) { Piece cell = board.getCell(x, y); if
	 * (cell == player.getMarker()) { numMine++; } if (cell == opponent) {
	 * numOpponent++; } }
	 * 
	 * score += scoreLine(size, numMine, numOpponent); }
	 * 
	 * // Inspect the rows for (int y = 0; y < size; ++y) { int numMine = 0; int
	 * numOpponent = 0;
	 * 
	 * for (int x = 0; x < size; ++x) { Piece cell = board.getCell(x, y); if
	 * (cell == player.getMarker()) { numMine++; } if (cell == opponent) {
	 * numOpponent++; } }
	 * 
	 * score += scoreLine(size, numMine, numOpponent); }
	 * 
	 * // Inspect the top-left/bottom-right diagonal int numMine = 0; int
	 * numOpponent = 0; for (int x = 0; x < size; ++x) { Piece cell =
	 * board.getCell(x, x); if (cell == player.getMarker()) { numMine++; } if
	 * (cell == opponent) { numOpponent++; } }
	 * 
	 * score += scoreLine(size, numMine, numOpponent);
	 * 
	 * numMine = 0; numOpponent = 0; // Inspect the bottom-right/top-right for
	 * (int x = 0; x < size; ++x) { Piece cell = board.getCell(x, size - x - 1);
	 * if (cell == player.getMarker()) { numMine++; } if (cell == opponent) {
	 * numOpponent++; } }
	 * 
	 * score += scoreLine(size, numMine, numOpponent);
	 * 
	 * return score; }
	 * 
	 * public static class MoveAndWeight { Cell move; Piece marker; double
	 * weight;
	 * 
	 * public MoveAndWeight(Cell move, Piece marker, double weight) { this.move
	 * = move; this.marker = marker; this.weight = weight; } }
	 * 
	 * public static class MoveAndScore { Cell move; double score;
	 * 
	 * MoveAndScore(Cell move, double score) { this.score = score; this.move =
	 * move; } }
	 */
}
