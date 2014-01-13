package com.oakonell.ticstacktoe.model;

import java.util.ArrayList;
import java.util.List;

import com.oakonell.ticstacktoe.ui.game.WinOverlayView.WinStyle;

public class State {
	public static class Win {
		private final Cell start;
		private final Cell end;
		private final WinStyle winStyle;

		public Win(Cell start, Cell end, WinStyle winStyle) {
			this.start = start;
			this.end = end;
			this.winStyle = winStyle;
		}

		public Cell getStart() {
			return start;
		}

		public Cell getEnd() {
			return end;
		}

		public WinStyle getWinStyle() {
			return winStyle;
		}

	}

	public enum SimpleState {
		WIN, DRAW, OPEN;
	}

	private final SimpleState state;
	private final Player winner;
	private final Move move;

	private List<Win> wins = new ArrayList<State.Win>();

	public static State winner(Move move, List<Win> wins, Player winner) {
		return new State(move, wins, winner, SimpleState.WIN);
	}

	public static State draw(Move move) {
		return new State(move, null, null, SimpleState.DRAW);
	}

	public static State open(Move move) {
		return new State(move, null, null, SimpleState.OPEN);
	}

	private State(Move move, List<Win> wins, Player winner, SimpleState state) {
		this.move = move;
		this.winner = winner;
		this.state = state;

		this.wins = wins;
	}

	public Player getWinner() {
		if (state == SimpleState.WIN) {
			return winner;
		}
		return null;
	}

	public boolean isDraw() {
		return state == SimpleState.DRAW;
	} 

	public List<Win> getWins() {
		return wins;
	}

	public boolean isOver() {
		return state != SimpleState.OPEN;
	}

	public Move getLastMove() {
		return move;
	}

}
