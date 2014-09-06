package com.oakonell.ticstacktoe.ui.network;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import android.content.Context;
import android.text.format.DateUtils;

import com.google.android.gms.games.multiplayer.Participant;
import com.oakonell.ticstacktoe.utils.ByteBufferDebugger;

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

	public static ChatMessage fromBytes(Context context,
			Map<String, Participant> participantsById, ByteBufferDebugger buffer) {
		boolean isLocal = buffer.getInt("isLocal") != 0;
		int msgSize = buffer.getInt("message size");
		byte[] msgBytes = new byte[msgSize];
		buffer.get("message Bytes", msgBytes);
		long timestamp = buffer.getLong("timestamp");

		String message;
		try {
			message = new String(msgBytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Can't convert string to UTF-8 bytes", e);
		}

		int idByteSize = buffer.getInt("participant id size");
		byte[] idBytes = new byte[idByteSize];
		buffer.get("participant Bytes", idBytes);

		String id;
		try {
			id = new String(idBytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Can't convert string to UTF-8 bytes", e);
		}
		Participant player = participantsById.get(id);
		if (player == null) {
			throw new RuntimeException("Could not find participant with id '"
					+ id + "'");
		}

		return new ChatMessage(player, message, isLocal, timestamp);
	}

	public void writeToBytes(Context context, ByteBufferDebugger theBuffer) {
		CharSequence message = getMessage();

		theBuffer.putInt("isLocal", isLocal() ? 1 : 0);
		byte[] msgBytes;
		try {
			msgBytes = message.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Can't convert string to UTF-8 bytes", e);
		}
		theBuffer.putInt("message size", msgBytes.length);
		theBuffer.put("message Bytes", msgBytes);
		theBuffer.putLong("timestamp", this.timestamp);
		byte[] idBytes;
		try {
			idBytes = player.getParticipantId().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Can't convert string to UTF-8 bytes", e);
		}
		theBuffer.putInt("participant id size", idBytes.length);
		theBuffer.put("participant id", idBytes);

	}
}
