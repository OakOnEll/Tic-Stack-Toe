package com.oakonell.ticstacktoe;

import android.content.Context;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.model.rank.RankStorage;
import com.oakonell.ticstacktoe.rank.RankHelper.OnRankReceived;
import com.oakonell.ticstacktoe.ui.game.GameFragment;
import com.oakonell.ticstacktoe.ui.game.SoundManager;
import com.oakonell.ticstacktoe.ui.menu.MenuFragment;

public interface GameContext {
	public final static int RC_UNUSED = 1;
	// online play request codes
	public final static int RC_SELECT_PLAYERS = 10000;
	// public final static int RC_INVITATION_INBOX = 10001;
	public final static int RC_WAITING_ROOM = 10002;

	public static final String FRAG_TAG_GAME = "game";
	public static final String FRAG_TAG_MENU = "menu";
	public static final String FRAG_TAG_START_GAME = "startGame";

	
	GameHelper getGameHelper();

	SoundManager getSoundManager();

	GameStrategy getGameStrategy();

	void setGameStrategy(GameStrategy strategy);

	Context getContext();

	GameFragment getGameFragment();

	MenuFragment getMenuFragment();

	SherlockFragmentActivity getSherlockActivity();

	void hideAd();

	void gameEnded();
	
	void loadRank(OnRankReceived onRankLoaded, boolean initializeIfNone);

	void updateCachedRank(RankStorage storage);

	void backFromRealtimeWaitingRoom();
}
