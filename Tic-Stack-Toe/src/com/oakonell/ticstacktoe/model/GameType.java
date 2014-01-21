package com.oakonell.ticstacktoe.model;

import java.util.ArrayList;
import java.util.List;

import com.oakonell.ticstacktoe.model.Board.PieceStack;

public class GameType {
	int numStacks; 
	int size;
	boolean strict;

	public final static GameType JUNIOR = new GameType(3, false, 2);
	public final static GameType EASY = new GameType(4, false, 3);
	public final static GameType REGULAR = new GameType(4, true,3);

	public GameType(int size, boolean strict, int numStacks) {
		this.size = size;
		this.strict = strict;
		this.numStacks = numStacks;
	}

	public int getSize() {
		return size;
	}

	public boolean isStrict() {
		return strict;
	}

	public List<PieceStack> createBlackPlayerStacks() {
		ArrayList<PieceStack> blackPlayerPieces = new ArrayList<Board.PieceStack>();
		blackPlayerPieces.add(createBlackPlayerStack());
		blackPlayerPieces.add(createBlackPlayerStack());
		if (numStacks == 3) {
			blackPlayerPieces.add(createBlackPlayerStack());
		}
		return blackPlayerPieces;
	}

	private PieceStack createBlackPlayerStack() {
		PieceStack pieceStack = new PieceStack();
		if (numStacks == 3) {
			pieceStack.add(Piece.BLACK1);
		}
		pieceStack.add(Piece.BLACK2);
		pieceStack.add(Piece.BLACK3);
		pieceStack.add(Piece.BLACK4);
		return pieceStack;
	}

	public ArrayList<PieceStack> createWhitePlayerStacks() {
		ArrayList<PieceStack> list = new ArrayList<Board.PieceStack>();
		list.add(createWhitePlayerStack());
		list.add(createWhitePlayerStack());
		if (numStacks == 3) {
		list.add(createWhitePlayerStack());
		}
		return list;
	}

	private PieceStack createWhitePlayerStack() {
		PieceStack pieceStack = new PieceStack();
		if (numStacks == 3) {
			pieceStack.add(Piece.WHITE1);
		}
		pieceStack.add(Piece.WHITE2);
		pieceStack.add(Piece.WHITE3);
		pieceStack.add(Piece.WHITE4);
		return pieceStack;
	}

	public int getStackSize() {
		return numStacks;
	}

}
