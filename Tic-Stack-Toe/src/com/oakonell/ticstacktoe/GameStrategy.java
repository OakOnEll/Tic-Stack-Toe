package com.oakonell.ticstacktoe;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.PlayerStrategy;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.settings.SettingsActivity;
import com.oakonell.ticstacktoe.ui.game.GameFragment;
import com.oakonell.ticstacktoe.ui.game.SoundManager;
import com.oakonell.ticstacktoe.ui.menu.MenuFragment;
import com.oakonell.ticstacktoe.utils.DevelopmentUtil.Info;

public abstract class GameStrategy {
	private final SoundManager soundManager;
	private MainActivity mainActivity;

	private Game game;
	private ScoreCard score = new ScoreCard(0, 0, 0);

	protected GameStrategy(MainActivity mainActivity, GameHelper helper,
			SoundManager soundManager) {
		this.mainActivity = mainActivity;
		this.soundManager = soundManager;
		this.helper = helper;
	}

	public abstract void leaveRoom();

	public abstract void sendMove(Game game, AbstractMove lastMove,
			ScoreCard score);

	public abstract void backFromWaitingRoom();

	public abstract boolean warnToLeave();

	public abstract void promptToPlayAgain(String winner, String title);

	public abstract void onSignInSuccess(MainActivity activity);

	public abstract void onResume(MainActivity activity);

	public abstract void onSignInFailed(SherlockFragmentActivity mainActivity);

	public abstract void onFragmentResume();

	public void showSettings(Fragment fragment) {
		showFullSettingsPreference(fragment);
	}

	public boolean shouldKeepScreenOn() {
		return false;
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

	protected Context getContext() {
		return mainActivity;
	}

	protected SherlockFragmentActivity getActivity() {
		return mainActivity;
	}

	protected MainActivity getMainActivity() {
		return mainActivity;
	}

	protected GameFragment getGameFragment() {
		return getMainActivity().getGameFragment();
	}

	protected MenuFragment getMenuFragment() {
		return getMainActivity().getMenuFragment();
	}

	protected void setMainActivity(MainActivity theActivity) {
		mainActivity = theActivity;
	}

	public boolean onOptionsItemSelected(Fragment fragment, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			showSettings(fragment);
			return true;
		}
		return false;
	}

	public void onCreateOptionsMenu(Fragment fragment, Menu menu,
			MenuInflater inflater) {
		inflater.inflate(R.menu.menu, menu);
	}

	public void onPrepareOptionsMenu(Fragment fragment, Menu menu) {
		// do nothing
	}

	private void showFullSettingsPreference(Fragment fragment) {
		// create special intent
		Intent prefIntent = new Intent(fragment.getActivity(),
				SettingsActivity.class);

		GameHelper helper = getHelper();
		Info info = null;
		TicStackToe app = (TicStackToe) fragment.getActivity().getApplication();
		if (helper.isSignedIn()) {
			info = new Info(helper);
		}
		app.setDevelopInfo(info);
		// ugh.. does going to preferences leave the room!?
		fragment.getActivity().startActivityForResult(prefIntent,
				MainActivity.RC_UNUSED);
	}

	public void acceptMove() {
		final PlayerStrategy currentStrategy = getGame().getCurrentPlayer()
				.getStrategy();
		if (currentStrategy.isHuman()) {
			getGameFragment().acceptHumanMove();
			return;
		}

		// show the waiting text
		getGameFragment().configureNonLocalProgresses();
		acceptCurrentPlayerMove(currentStrategy);
	}

	abstract protected void acceptCurrentPlayerMove(
			final PlayerStrategy currentStrategy);

	public Game getGame() {
		return game;
	}

	protected void setGame(Game game) {
		this.game = game;
	}

	public ScoreCard getScore() {
		return score;
	}

	protected void setScore(ScoreCard score) {
		this.score = score;
	}

	private GameHelper helper;

	protected GameHelper getHelper() {
		return helper;
	}

	protected void setHelper(GameHelper helper) {
		this.helper = helper;
	}

}
