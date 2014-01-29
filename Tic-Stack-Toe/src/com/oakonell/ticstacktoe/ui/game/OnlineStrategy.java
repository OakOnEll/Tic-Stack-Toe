package com.oakonell.ticstacktoe.ui.game;

import android.net.Uri;

import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.PlayerStrategy;

public class OnlineStrategy extends PlayerStrategy {

	public static Player createPlayer(String name, boolean isBlack,
			Uri iconImageUri, String id) {
		Player player = new Player(name, iconImageUri, new OnlineStrategy(
				isBlack), id);
		return player;
	}

	public OnlineStrategy(boolean isBlack) {
		super(isBlack);
	}

}
