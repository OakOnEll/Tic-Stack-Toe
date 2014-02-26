package com.oakonell.ticstacktoe.model.solver;

import java.util.List;
import java.util.Random;

import android.net.Uri;

import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Board;
import com.oakonell.ticstacktoe.model.Board.PieceStack;
import com.oakonell.ticstacktoe.model.Cell;
import com.oakonell.ticstacktoe.model.ExistingPieceMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.Piece;
import com.oakonell.ticstacktoe.model.PlaceNewPieceMove;
import com.oakonell.ticstacktoe.model.Player;

public class RandomAI extends AiPlayerStrategy {

	protected static Player createPlayer(String name, boolean isBlack) {
		Player player = new Player(name, getImageUri(), new RandomAI(isBlack));
		return player;
	}

	private RandomAI(boolean isBlack) {
		super(isBlack);
	}

	protected static Uri getImageUri() {
		return Uri.parse("android.resource://com.oakonell.ticstacktoe/"
				+ R.drawable.dice_icon_14730);
	}

	@Override
	public boolean isAI() {
		return true;
	}

	private Random random = new Random();

	public AbstractMove move(Game game) {
		Board board = game.getBoard();
		// Pick a random piece as the source, from board or stacks
		// count number of pieces on board
		// count number of non-empty stacks
		int size = board.getSize();
		int numOnBoard = 0;
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				Piece piece = board.getVisiblePiece(x, y);
				if (piece != null && piece.isBlack() == isBlack()) {
					numOnBoard++;
				}
			}
		}
		Player player = isBlack() ? game.getBlackPlayer() : game
				.getWhitePlayer();
		List<PieceStack> playerPieces = player.getPlayerPieces();
		int numNonEmptyStack = 0;
		for (PieceStack each : playerPieces) {
			if (each.getTopPiece() != null) {
				numNonEmptyStack++;
			}
		}
		int randomNum = random.nextInt(numNonEmptyStack + numOnBoard);
		if (randomNum < numNonEmptyStack) {
			return moveFromPlayerStack(game, player, randomNum);
		}

		int count = randomNum - numNonEmptyStack;
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				Piece piece = board.getVisiblePiece(x, y);
				if (piece != null && piece.isBlack() == isBlack()) {
					if (count == 0) {
						return moveFromBoard(game, player, new Cell(x, y));
					}
					count--;
				}
			}
		}

		throw new RuntimeException("Can't get here");
	}

	private AbstractMove moveFromBoard(Game game, Player player, Cell source) {
		Piece playedPiece = game.getBoard().getVisiblePiece(source);
		Piece exposedSourcePiece = game.getBoard().peekNextPiece(source);

		Cell target = getValidTargetForPiece(game, playedPiece, source);
		Piece existingTargetPiece = game.getBoard().getVisiblePiece(target);

		return new ExistingPieceMove(player, playedPiece, exposedSourcePiece,
				source, target, existingTargetPiece);
	}

	private AbstractMove moveFromPlayerStack(Game game, Player player,
			int stackNum) {
		Piece playedPiece = player.getPlayerPieces().get(stackNum)
				.getTopPiece();
		if (playedPiece == null) {
			throw new RuntimeException("Got no piece from a stack");
		}
		Cell target = getValidTargetForPiece(game, playedPiece, null);
		Piece existingTargetPiece = game.getBoard().getVisiblePiece(target);

		return new PlaceNewPieceMove(player, playedPiece, stackNum, target,
				existingTargetPiece);
	}

	private Cell getValidTargetForPiece(Game game, Piece playedPiece,
			Cell excluded) {
		Board board = game.getBoard();
		int size = board.getSize();
		int numOnBoard = 0;
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				Piece piece = board.getVisiblePiece(x, y);
				if (piece == null || playedPiece.isLargerThan(piece)) {
					if (excluded == null || excluded.getX() != x
							|| excluded.getY() != y) {
						numOnBoard++;
					}
				}
			}
		}
		int randomNum = random.nextInt(numOnBoard);
		int count = randomNum;
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				Piece piece = board.getVisiblePiece(x, y);
				if (piece == null || playedPiece.isLargerThan(piece)) {
					if (excluded == null || excluded.getX() != x
							|| excluded.getY() != y) {
						if (count == 0) {
							return new Cell(x, y);
						}
						count--;
					}
				}
			}
		}
		throw new RuntimeException("Can't get here");
	}
}
