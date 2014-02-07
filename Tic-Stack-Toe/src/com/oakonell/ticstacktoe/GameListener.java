package com.oakonell.ticstacktoe;

import com.google.android.gms.games.multiplayer.Participant;
import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.ui.game.GameFragment;

public interface GameListener {

	void leaveRoom();

	void sendMove(Game game, AbstractMove lastMove, ScoreCard score);

	Participant getMe();

	String getOpponentName();

	void backFromWaitingRoom();

	boolean warnToLeave();

	void promptToPlayAgain(String title);

	void sendInChat(boolean b);

	void sendMessage(String string);

	void reassociate(MainActivity activity);

	void onResume(MainActivity activity);

	void onSignInFailed(MainActivity mainActivity);

}
