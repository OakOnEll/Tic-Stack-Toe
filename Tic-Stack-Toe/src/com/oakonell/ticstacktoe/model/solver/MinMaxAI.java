package com.oakonell.ticstacktoe.model.solver;

import android.net.Uri;

import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.Board;
import com.oakonell.ticstacktoe.model.Cell;
import com.oakonell.ticstacktoe.model.Piece;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.PlayerStrategy;

public class MinMaxAI extends PlayerStrategy {
	private MiniMaxAlg minmax;

	public static Player createPlayer(String whiteName, boolean isBlack,
			int aiDepth) {
		MinMaxAI strategy = new MinMaxAI(isBlack);
		Player player = new Player(whiteName, getImageUri(aiDepth), strategy);
		strategy.setAlg(new MiniMaxAlg(player, aiDepth));

		return player;
	}

	private MinMaxAI(boolean isBlack) {
		super(isBlack);
	}

	private void setAlg(MiniMaxAlg alg) {
		minmax = alg;
	}

	private static Uri getImageUri(int depth) {
		if (depth <= 1) {
			return Uri.parse("android.resource://com.oakonell.ticstacktoe/"
					+ R.drawable.dim_bulb_icon_122);
		} else if (depth == 2) {
			return Uri.parse("android.resource://com.oakonell.ticstacktoe/"
					+ R.drawable.light_bulb_icon_23392);
		} else
			return Uri.parse("android.resource://com.oakonell.ticstacktoe/"
					+ R.drawable.einstein_icon_16422);
	}

	@Override
	public boolean isAI() {
		return true;
	}

	public Cell move(Board board, Piece toPlay) {
		// TODO
		// return minmax.solve(board, toPlay);
		return null;
	}

}
