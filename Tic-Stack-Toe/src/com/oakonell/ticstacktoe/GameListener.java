package com.oakonell.ticstacktoe;

import android.support.v4.app.Fragment;

import com.google.android.gms.games.multiplayer.Participant;
import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.ui.game.AbstractGameFragment;
import com.oakonell.ticstacktoe.ui.game.GameFragment;

public interface GameListener {

	void leaveRoom();

	void sendMove(Game game, AbstractMove lastMove, ScoreCard score);


	void backFromWaitingRoom();

	boolean warnToLeave();

	void promptToPlayAgain(String winner, String title);



	void reassociate(MainActivity activity);

	void onResume(MainActivity activity);

	void onSignInFailed(MainActivity mainActivity);

	void onFragmentResume();

	void showSettings(AbstractGameFragment fragment);


	boolean shouldKeepScreenOn();

	ChatHelper getChatHelper();

}
