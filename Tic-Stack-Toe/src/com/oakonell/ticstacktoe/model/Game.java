package com.oakonell.ticstacktoe.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oakonell.ticstacktoe.model.Board.PieceStack;

public class Game {
	private int moves;
	private final Board board;

	private Player blackPlayer;
	private Player whitePlayer;
	private GameMode mode;

	private List<PieceStack> blackPlayerPieces;
	private List<PieceStack> whitePlayerPieces;

	private Player player;
	private Map<String, Integer> numVisitsPerState = new HashMap<String, Integer>();

	public Game(int size, GameMode mode, Player blackPlayer,
			Player whitePlayer, Player startingPlayer) {
		board = new Board(size);
		this.blackPlayer = blackPlayer;
		this.whitePlayer = whitePlayer;
		blackPlayer.setOpponent(whitePlayer);
		whitePlayer.setOpponent(blackPlayer);
		blackPlayer.setGame(this);
		whitePlayer.setGame(this);

		blackPlayerPieces = new ArrayList<Board.PieceStack>();
		blackPlayerPieces.add(createBlackPlayerStack());
		blackPlayerPieces.add(createBlackPlayerStack());
		blackPlayerPieces.add(createBlackPlayerStack());
		whitePlayerPieces = new ArrayList<Board.PieceStack>();
		whitePlayerPieces.add(createWhitePlayerStack());
		whitePlayerPieces.add(createWhitePlayerStack());
		whitePlayerPieces.add(createWhitePlayerStack());

		// player = startingPlayer;
		player = startingPlayer;
		this.mode = mode;
	}

	private PieceStack createBlackPlayerStack() {
		PieceStack pieceStack = new PieceStack();
		pieceStack.add(Piece.BLACK1);
		pieceStack.add(Piece.BLACK2);
		pieceStack.add(Piece.BLACK3);
		pieceStack.add(Piece.BLACK4);
		return pieceStack;
	}

	private PieceStack createWhitePlayerStack() {
		PieceStack pieceStack = new PieceStack();
		pieceStack.add(Piece.WHITE1);
		pieceStack.add(Piece.WHITE2);
		pieceStack.add(Piece.WHITE3);
		pieceStack.add(Piece.WHITE4);
		return pieceStack;
	}

	public State placePlayerPiece(int stack, Cell cell) {
		List<PieceStack> playerPieces = getCurrentPlayerPieces();
		if (stack < 0 || stack >= playerPieces.size()) {
			throw new IllegalArgumentException("Invalid stack '" + stack
					+ "', should be in [0," + playerPieces.size() + "]");
		}
		// get the piece to try the move
		PieceStack pieceStack = playerPieces.get(stack);
		Piece piece = pieceStack.getTopPiece();

		State outcome = board.placePiece(cell, player, piece, stack);
		// if move was valid, remove the piece, it is now on the board
		pieceStack.removeTopPiece();

		// switch to next player
		player = player.opponent();

		recordVisitToState();
		moves++;

		return outcome;
	}

	public State movePiece(Cell from, Cell to) {
		State outcome = board.moveFrom(player, from, to);

		// switch to next player
		player = player.opponent();

		recordVisitToState();
		moves++;

		return outcome;
	}

	private void recordVisitToState() {
		String state = board.getVisibleBoardStateAsLong();
		Integer number = numVisitsPerState.get(state);
		if (number == null) {
			number = 0;
		}
		numVisitsPerState.put(state, number + 1);
	}

	public Player getCurrentPlayer() {
		return player;
	}

	public int getNumberOfMoves() {
		return moves;
	}

	public int getNumberOfTimesInThisState() {
		String state = board.getVisibleBoardStateAsLong();
		Integer integer = numVisitsPerState.get(state);
		if (integer == null)
			return 0;
		return integer;
	}

	public Board getBoard() {
		return board;
	}

	public GameMode getMode() {
		return mode;
	}

	/**
	 * Return the human player in a game with only one human player, else null
	 * 
	 * @return
	 */
	public Player getLocalPlayer() {
		if (getMode() == GameMode.PASS_N_PLAY) {
			return null;
		}
		if (blackPlayer.getStrategy().isHuman()) {
			return blackPlayer;
		}
		return whitePlayer;
	}

	/**
	 * Return the non-human player in a game with only one human player, else
	 * null
	 * 
	 * @return
	 */
	public Player getNonLocalPlayer() {
		if (getMode() == GameMode.PASS_N_PLAY) {
			return null;
		}
		if (blackPlayer.getStrategy().isHuman()) {
			return whitePlayer;
		}
		return blackPlayer;
	}

	public Player getBlackPlayer() {
		return blackPlayer;
	}

	public Player getWhitePlayer() {
		return whitePlayer;
	}

	public List<PieceStack> getBlackPlayerPieces() {
		return blackPlayerPieces;
	}

	public List<PieceStack> getWhitePlayerPieces() {
		return whitePlayerPieces;
	}

	public List<PieceStack> getCurrentPlayerPieces() {
		if (player == blackPlayer)
			return blackPlayerPieces;
		return whitePlayerPieces;
	}

}
