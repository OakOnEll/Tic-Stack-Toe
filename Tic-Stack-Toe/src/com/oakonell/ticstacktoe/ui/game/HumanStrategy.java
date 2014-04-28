package com.oakonell.ticstacktoe.ui.game;

import android.net.Uri;

import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.PlayerStrategy;

/**
 * Strategy intended to take move input from drag/drop events.
 */
public class HumanStrategy extends PlayerStrategy {

	public static Player createPlayer(String name, boolean isBlack) {
		Player player = new Player(name, getImage(isBlack), new HumanStrategy(
				isBlack));
		return player;
	}

	public static Player createPlayer(String name, boolean isBlack,
			Uri iconImageUri) {
		Player player = new Player(name, iconImageUri, new HumanStrategy(
				isBlack));
		return player;
	}

	private HumanStrategy(boolean isBlack) {
		super(isBlack);
	}

	public static Uri getImage(boolean isBlack) {
		if (isBlack)
			return Uri.parse("android.resource://com.oakonell.ticstacktoe/"
					+ R.drawable.black_piece4);
		return Uri.parse("android.resource://com.oakonell.ticstacktoe/"
				+ R.drawable.white_piece4);
	}

	@Override
	public boolean isHuman() {
		return true;
	}

}
