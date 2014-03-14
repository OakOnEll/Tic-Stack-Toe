package com.oakonell.ticstacktoe.model.solver;

import java.util.List;
import java.util.Random;

import android.net.Uri;

import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.Player;

public class RandomAI extends AiPlayerStrategy {

	protected static Player createPlayer(String name, boolean isBlack) {
		Player player = new Player(name, getImageUri(), new RandomAI(isBlack));
		return player;
	}

	protected RandomAI(boolean isBlack) {
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
		// Pick a random piece as the source, from board or stacks

		List<AbstractMove> validMoves = AIMoveHelper.getValidMoves(
				game.getType(), game.getBlackPlayerPieces(),
				game.getWhitePlayerPieces(), game.getBoard(),
				game.getCurrentPlayer());
		int randomNum = random.nextInt(validMoves.size());
		return validMoves.get(randomNum);
	}
}
