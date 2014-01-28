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

		public String toString() {
			return winStyle.name() + "-" + start + " to " + end;
		}

		public boolean contains(Cell source, int size) {
			// TODO something weird about the row/col x/y numbers.. board is rotated somehow?
			if ((winStyle == WinStyle.COL1 && source.getY() == 0)
					|| (winStyle == WinStyle.COL2 && source.getY() == 1)
					|| (winStyle == WinStyle.COL3 && source.getY() == 2)
					|| (winStyle == WinStyle.COL4 && source.getY() == 3)
					|| (winStyle == WinStyle.COL5 && source.getY() == 4)
					|| (winStyle == WinStyle.ROW1 && source.getX() == 0)
					|| (winStyle == WinStyle.ROW2 && source.getX() == 1)
					|| (winStyle == WinStyle.ROW3 && source.getX() == 2)
					|| (winStyle == WinStyle.ROW4 && source.getX() == 3)
					|| (winStyle == WinStyle.ROW5 && source.getX() == 4)
					|| (winStyle == WinStyle.TOP_LEFT_DIAG && source.getY() == source
							.getX())
					|| (winStyle == WinStyle.TOP_RIGHT_DIAG && source.getY()  + source.getX() + 1 == size)) {
				return true;
			}
			return false;

		}
	}

	public enum SimpleState {
		WIN, DRAW, OPEN;
	}

	private final SimpleState state;
	private final Player winner;
	private final AbstractMove move;

	private List<Win> wins = new ArrayList<State.Win>();

	public static State winner(AbstractMove move, List<Win> wins, Player winner) {
		return new State(move, wins, winner, SimpleState.WIN);
	}

	public static State draw(AbstractMove move) {
		return new State(move, null, null, SimpleState.DRAW);
	}

	public static State open(AbstractMove move) {
		return new State(move, null, null, SimpleState.OPEN);
	}

	private State(AbstractMove move, List<Win> wins, Player winner,
			SimpleState state) {
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

	public AbstractMove getLastMove() {
		return move;
	}

	public String toString() {
		if (state == SimpleState.OPEN) {
			return "Open";
		}
		if (state == SimpleState.DRAW) {
			return "Draw";
		}
		return winner + " won: " + wins;
	}

}
