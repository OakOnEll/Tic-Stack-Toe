package com.oakonell.ticstacktoe;

import com.google.android.gms.games.multiplayer.Participant;
import com.oakonell.ticstacktoe.RoomListener.PlayAgainState;
import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.ScoreCard;

public interface GameListener {

	void leaveRoom();

	void sendMove(Game game, AbstractMove lastMove, ScoreCard score);


	void sendNotPlayAgain(Runnable success, Runnable error);

	PlayAgainState getOpponentPlayAgainState();

	void sendPlayAgain(Runnable success, Runnable error);


	void sendMessage(String string);

	void sendInChat(boolean b);

	Participant getMe();

	String getOpponentName();

	void restartGame();

	void backFromWaitingRoom();

	boolean warnToLeave();

}
