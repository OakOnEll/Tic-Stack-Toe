package com.oakonell.ticstacktoe.ui.network;

import com.oakonell.ticstacktoe.ChatHelper;
import com.oakonell.ticstacktoe.GameStrategy;
import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.ui.game.SoundManager;

public abstract class AbstractNetworkedGameStrategy extends GameStrategy
		implements ChatHelper {

	private GameHelper helper;

	protected AbstractNetworkedGameStrategy(MainActivity mainActivity,
			SoundManager soundManager, GameHelper helper) {
		super(mainActivity, soundManager);
		this.helper = helper;
	}

	protected GameHelper getHelper() {
		return helper;
	}

	protected void setHelper(GameHelper helper) {
		this.helper = helper;
	}

	@Override
	public ChatHelper getChatHelper() {
		return this;
	}
}
