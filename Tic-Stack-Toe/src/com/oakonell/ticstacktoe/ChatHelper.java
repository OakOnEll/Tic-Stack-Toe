package com.oakonell.ticstacktoe;

import com.google.android.gms.games.multiplayer.Participant;

public interface ChatHelper {
	void sendInChat(boolean b);

	Participant getMeForChat();

	void sendMessage(String string);

	String getOpponentName();

}
