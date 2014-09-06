package com.oakonell.ticstacktoe.ui.network;

import com.google.android.gms.games.multiplayer.Participant;

public interface ParticipantById {
	Participant getParticipant(String id);
}
