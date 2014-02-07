package com.oakonell.ticstacktoe.model;

import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oakonell.ticstacktoe.model.Board.PieceStack;

public class Game {
	private final Board board;
	private final GameType gameType;

	private final Player blackPlayer;
	private final Player whitePlayer;
	private final GameMode mode;

	private final List<PieceStack> blackPlayerPieces;
	private final List<PieceStack> whitePlayerPieces;
	private final Map<String, Integer> numVisitsPerState = new HashMap<String, Integer>();

	private int moves;
	private Player player;
	private Cell firstPickedCell;

	public Game(GameType gameType, GameMode mode, Player blackPlayer,
			Player whitePlayer, Player startingPlayer) {
		this(gameType, mode, new Board(gameType.size), 0, blackPlayer, gameType
				.createBlackPlayerStacks(), whitePlayer, gameType
				.createWhitePlayerStacks(), null, startingPlayer);
	}

	private Game(GameType gameType, GameMode mode, Board board, int moves,
			Player blackPlayer, List<PieceStack> blackStacks,
			Player whitePlayer, List<PieceStack> whiteStacks,
			Cell firstPickedCell, Player startingPlayer) {
		this.board = board;
		this.moves = moves;
		this.gameType = gameType;
		this.blackPlayer = blackPlayer;
		this.whitePlayer = whitePlayer;
		blackPlayer.setOpponent(whitePlayer);
		whitePlayer.setOpponent(blackPlayer);
		blackPlayer.setGame(this);
		whitePlayer.setGame(this);

		blackPlayerPieces = blackStacks;
		whitePlayerPieces = whiteStacks;
		this.firstPickedCell = firstPickedCell;

		// player = startingPlayer;
		player = startingPlayer;
		this.mode = mode;
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

		State outcome = board.placePiece(cell, player, piece, stack, gameType);
		// if move was valid, remove the piece, it is now on the board
		pieceStack.removeTopPiece();

		// switch to next player
		switchPlayer();

		recordVisitToState();
		moves++;

		return outcome;
	}

	public State movePiece(Cell from, Cell to) {
		State outcome = board.moveFrom(player, from, to);

		switchPlayer();

		recordVisitToState();
		moves++;

		return outcome;
	}

	public void switchPlayer() {
		player = player.opponent();		
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

	public GameType getType() {
		return gameType;
	}

	public void writeBytes(String blackPlayerId, ByteBufferDebugger buffer) {
		buffer.putInt("Num Moves", moves);
		buffer.putInt("Variant", getType().getVariant());

		int boardSize = gameType.getSize();
		for (int i = 0; i < boardSize; i++) {
			for (int j = 0; j < boardSize; j++) {
				PieceStack stack = board.getStackAt(i, j);
				writeStack(buffer, "Board " + i + "," + j, stack);
			}
		}


		int i = 0;
		for (PieceStack each : getBlackPlayerPieces()) {
			writeStack(buffer, "Black stack " + i, each);
			i++;
		}
		i = 0;
		for (PieceStack each : getWhitePlayerPieces()) {
			writeStack(buffer, "White stack " + i, each);
			i++;
		}

		if (firstPickedCell != null) {
			buffer.put("has first picked cell", (byte) 1);
			buffer.put("first picked cell X", (byte) firstPickedCell.x);
			buffer.put("first picked cell Y", (byte) firstPickedCell.y);
		} else {
			buffer.put("has first picked cell", (byte) 0);
		}

		
		getBoard().getState().toBytes(buffer);
	}

	private void writeStack(ByteBufferDebugger buffer, String comment,
			PieceStack stack) {
		int num = stack.pieces.size();
		buffer.put("Num pieces " + comment, (byte) num);
		for (Piece piece : stack.pieces) {
			buffer.putInt("Piece " + comment, piece.getVal());
		}
	}

	public static Game fromBytes(Player blackPlayer, Player whitePlayer,
			Player currentPlayer, ByteBufferDebugger buffer) {
		int moves = buffer.getInt("Num Moves");
		int variant = buffer.getInt("Variant");
		GameType type = GameType.fromVariant(variant);

		int boardSize = type.getSize();
		PieceStack[][] board = new PieceStack[boardSize][boardSize];
		for (int i = 0; i < boardSize; i++) {
			for (int j = 0; j < boardSize; j++) {
				board[i][j] = readStack("Board " + i + "," + j, buffer);
			}
		}
		Board theBoard = new Board(board);

		int numStacks = type.getNumberOfStacks();
		List<PieceStack> blackStacks = new ArrayList<Board.PieceStack>();
		List<PieceStack> whiteStacks = new ArrayList<Board.PieceStack>();
		int num = 0;
		for (int i = 0; i < numStacks; i++) {
			blackStacks.add(readStack("Black stack " + num, buffer));
			num++;
		}
		num = 0;
		for (int i = 0; i < numStacks; i++) {
			whiteStacks.add(readStack("White stack " + num, buffer));
			num++;
		}

		boolean hasFirstPickedCell = buffer.get("has first picked cell") != 0;
		Cell firstPickedCell = null; 
		if (hasFirstPickedCell) {
			byte x = buffer.get("first picked cell X");
			byte y = buffer.get("first picked cell Y");
			firstPickedCell = new Cell(x, y);
		} 

		
		Game game = new Game(type, GameMode.TURN_BASED, theBoard, moves,
				blackPlayer, blackStacks, whitePlayer, whiteStacks,firstPickedCell,
				currentPlayer);

		State state = State.fromBytes(buffer, game, blackPlayer, whitePlayer);
		theBoard.setState(state);

		return game;
	}

	private static PieceStack readStack(String string, ByteBufferDebugger buffer) {
		int num = buffer.get("Number pieces " + string);
		PieceStack stack = new PieceStack();
		for (int i = 0; i < num; i++) {
			stack.pieces.add(Piece.fromInt(buffer.getInt("Piece " + string)));
		}
		return stack;
	}

	public Cell getFirstPickedCell() {
		return firstPickedCell;
	}

	public void setFirstPickedCell(Cell firstPickCell) {
		this.firstPickedCell = firstPickCell;
	}

}
