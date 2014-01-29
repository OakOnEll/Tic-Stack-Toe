package com.oakonell.ticstacktoe.model;

import java.util.ArrayList;
import java.util.List;

import com.oakonell.ticstacktoe.model.Game.ByteBufferDebugger;
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
			// TODO something weird about the row/col x/y numbers.. board is
			// rotated somehow?
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
					|| (winStyle == WinStyle.TOP_RIGHT_DIAG && source.getY()
							+ source.getX() + 1 == size)) {
				return true;
			}
			return false;

		}
	}

	public enum SimpleState {
		WIN, DRAW, OPEN;

		public static SimpleState from(byte simpleStateByte) {
			if (simpleStateByte == 'W') {
				return SimpleState.WIN;
			}
			if (simpleStateByte == 'D') {
				return SimpleState.DRAW;
			}
			if (simpleStateByte == 'O') {
				return SimpleState.OPEN;
			}
			throw new RuntimeException("Invalid byte '" + simpleStateByte + "'");
		}

		public byte toByte() {
			if (this == DRAW)
				return 'D';
			if (this == WIN)
				return 'W';
			if (this == OPEN)
				return 'O';
			throw new RuntimeException("Invalid simple state" + this);

		}
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

	public static State fromBytes(ByteBufferDebugger buffer, Game game,
			Player blackPlayer, Player whitePlayer) {
		byte simpleStateByte = buffer.get("simple state");
		SimpleState simpleState = SimpleState.from(simpleStateByte);
		Player winner = null;
		List<Win> wins = null;
		if (simpleState == SimpleState.WIN) {
			byte winnerIsBlack = buffer.get("Winner is black");
			if (winnerIsBlack != 0) {
				winner = blackPlayer;
			} else {
				winner = whitePlayer;
			}
			wins = new ArrayList<State.Win>();
			// get wins
			int numWins = buffer.get("Num wins");
			for (int i = 0; i < numWins; i++) {
				int startX = buffer.get("win start " + i + " x");
				int startY = buffer.get("win start " + i + " y");
				int endX = buffer.get("win end " + i + " x");
				int endY = buffer.get("win end " + i + " y");
				WinStyle style = WinStyle.fromByte(buffer);
				Win win = new Win(new Cell(startX, startY),
						new Cell(endX, endY), style);
				wins.add(win);
			}
		}
		boolean hasMove = buffer.get("has move") != 0;
		AbstractMove move = null;
		if (hasMove) {
			move = AbstractMove.fromMessageBytes(buffer, game);
		}
		return new State(move, wins, winner, simpleState);
	}

	public void toBytes(ByteBufferDebugger buffer) {
		buffer.put("Simple state", state.toByte());
		if (state == SimpleState.WIN) {
			byte isBlack = (byte) (winner.isBlack() ? 1 : 0);
			buffer.put("winner is black", isBlack);
			// put wins
			buffer.put("Num wins", (byte) wins.size());
			for (Win each : wins) {
				buffer.put("win start x", (byte) each.getStart().getX());
				buffer.put("win start y", (byte) each.getStart().getY());
				buffer.put("win end x", (byte) each.getEnd().getX());
				buffer.put("win end y", (byte) each.getEnd().getY());
				each.getWinStyle().writeBytes(buffer);
			}

		}
		buffer.put("has move" , (byte) (move != null ? 1 : 0));
		if (move != null) {
			move.appendBytesToMessage(buffer);
		}
	}
}
