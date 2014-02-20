package com.oakonell.ticstacktoe;

import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.ui.game.AbstractGameFragment;
import com.oakonell.ticstacktoe.ui.game.SoundManager;

public abstract class GameStrategy {
	private final SoundManager soundManager;
	private MainActivity mainActivity;

	protected GameStrategy(MainActivity mainActivity, SoundManager soundManager) {
		this.mainActivity = mainActivity;
		this.soundManager = soundManager;
	}

	public abstract void leaveRoom();

	public abstract void sendMove(Game game, AbstractMove lastMove,
			ScoreCard score);

	public abstract void backFromWaitingRoom();

	public abstract boolean warnToLeave();

	public abstract void promptToPlayAgain(String winner, String title);

	public abstract void onSignInSuccess(MainActivity activity);

	public abstract void onResume(MainActivity activity);

	public abstract void onSignInFailed(MainActivity mainActivity);

	public abstract void onFragmentResume();

	public abstract void showSettings(AbstractGameFragment fragment);

	public boolean shouldKeepScreenOn() {
		return false;
	}

	public ChatHelper getChatHelper() {
		return null;
	}

	public int playSound(Sounds sound) {
		return soundManager.playSound(sound);
	}

	public int playSound(Sounds sound, boolean loop) {
		return soundManager.playSound(sound, loop);
	}

	public void stopSound(int streamId) {
		soundManager.stopSound(streamId);
	}

	protected MainActivity getMainActivity() {
		return mainActivity;
	}

	protected void setMainActivity(MainActivity theActivity) {
		mainActivity = theActivity;
	}

}
