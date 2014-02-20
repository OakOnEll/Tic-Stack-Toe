package com.oakonell.ticstacktoe.ui.network;

import android.content.Context;
import android.text.format.DateUtils;

import com.google.android.gms.games.multiplayer.Participant;

public class ChatMessage {
	private final Participant player;
	private final boolean isLocal;
	private final String message;
	private final long timestamp;

	public ChatMessage(Participant player, String string, boolean isLocal,
			long timestamp) {
		this.player = player;
		this.message = string;
		this.isLocal = isLocal;
		this.timestamp = timestamp;
	}

	public CharSequence getMessage() {
		return message;
	}

	public Participant getParticipant() {
		return player;
	}

	public boolean isLocal() {
		return isLocal;
	}

	public CharSequence getTimestamp(Context context) {
		return DateUtils.formatSameDayTime(timestamp,
				System.currentTimeMillis(), java.text.DateFormat.SHORT,
				java.text.DateFormat.SHORT);
	}
}
