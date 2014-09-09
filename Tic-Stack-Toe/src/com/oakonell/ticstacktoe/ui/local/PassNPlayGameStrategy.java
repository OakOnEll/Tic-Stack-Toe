package com.oakonell.ticstacktoe.ui.local;

import android.support.v4.app.Fragment;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.GameContext;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.PlayerStrategy;
import com.oakonell.ticstacktoe.model.RankInfo;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.ui.game.HumanStrategy;
import com.oakonell.utils.Utils;

public class PassNPlayGameStrategy extends AbstractLocalStrategy {

	public PassNPlayGameStrategy(GameContext context) {
		super(context);
	}

	public PassNPlayGameStrategy(GameContext context,
			PassNPlayMatchInfo localMatchInfo) {
		super(context, localMatchInfo);
	}

	@Override
	protected LocalMatchInfo createNewMatchInfo(String blackName,
			String whiteName, Game game, ScoreCard score) {
		return new PassNPlayMatchInfo(TurnBasedMatch.MATCH_STATUS_ACTIVE,
				TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN, blackName, whiteName,
				game, score);
	}

	protected int getAnalyticGameActionResId() {
		return R.string.an_start_pass_n_play_game_action;
	}

	@Override
	protected GameMode getGameMode() {
		return GameMode.PASS_N_PLAY;
	}

	@Override
	protected Player createWhitePlayer(String whiteName) {
		return HumanStrategy.createPlayer(whiteName, false);
	}

	@Override
	protected void acceptNonHumanPlayerMove(PlayerStrategy currentStrategy) {
		throw new RuntimeException(
				"Shouldn't get here on a human vs human, local match");
	}

	@Override
	public RankInfo getRankInfo() {
		return null;
	}

	private boolean rotateBlackLayout = false;

	public boolean rotateBlackLayout() {
		return rotateBlackLayout;
	}

	public void onCreateOptionsMenu(Fragment fragment, Menu menu,
			MenuInflater inflater) {
		inflater.inflate(R.menu.pass_n_play_menu, menu);
		// possibly change the help icon, based on the game type
		if (!Utils.hasHoneycomb()) {
			MenuItem headToHead = menu.findItem(R.id.action_head_to_head);
			headToHead.setVisible(false);
		}
	}

	@Override
	public boolean onOptionsItemSelected(Fragment fragment, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_head_to_head:
			rotateBlackLayout = !rotateBlackLayout;
			getGameFragment().invertBlackHeader();
			return true;
		}
		return super.onOptionsItemSelected(fragment, item);
	}
}
