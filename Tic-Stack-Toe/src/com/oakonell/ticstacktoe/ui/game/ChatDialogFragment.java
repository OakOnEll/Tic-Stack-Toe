package com.oakonell.ticstacktoe.ui.game;

import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.multiplayer.Participant;
import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.R;
import com.oakonell.utils.StringUtils;

public class ChatDialogFragment extends SherlockDialogFragment {
	private List<ChatMessage> messages;
	private MessagesAdapter adapter;
	private Participant me;
	private AbstractGameFragment parent;
	private String friendName;

	private static class MessagesAdapter extends ArrayAdapter<ChatMessage> {
		private LayoutInflater inflater;
		private ImageManager imageManager;

		private static class MessageHolder {
			TextView message;
			ImageView picView;
			TextView time;

			String participantId;
		}

		public MessagesAdapter(MainActivity context, int textViewResourceId,
				List<ChatMessage> objects) {
			super(context, textViewResourceId, objects);
			inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			imageManager = ImageManager.create(context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ChatMessage item = getItem(position);
			String id = item.getParticipant().getParticipantId();

			MessageHolder holder;
			View rowView;
			if (convertView == null
					|| !((MessageHolder) convertView.getTag()).participantId
							.equals(id)) {
				if (item.isLocal()) {
					rowView = inflater.inflate(R.layout.my_message_item,
							parent, false);
				} else {
					rowView = inflater.inflate(R.layout.friend_message_item,
							parent, false);
				}
				holder = new MessageHolder();
				holder.message = (TextView) rowView.findViewById(R.id.message);
				holder.time = (TextView) rowView.findViewById(R.id.timestamp);
				holder.picView = (ImageView) rowView
						.findViewById(R.id.player_pic);
				rowView.setTag(holder);
			} else {
				rowView = convertView;
				holder = (MessageHolder) convertView.getTag();
			}

			holder.message.setText(item.getMessage());
			holder.time.setText(item.getTimestamp(getContext()));
			Uri imageUri = item.getParticipant().getIconImageUri();
			if (imageUri == null) {
				holder.picView
						.setImageResource(R.drawable.silhouette_icon_4520);
			} else {
				imageManager.loadImage(holder.picView, item.getParticipant()
						.getIconImageUri());
			}
			holder.participantId = item.getParticipant().getParticipantId();
			return rowView;
		}
	}

	public void initialize(AbstractGameFragment parent, List<ChatMessage> messages,
			Participant me, String friendName) {
		this.parent = parent;
		this.messages = messages;
		this.me = me;
		this.friendName = friendName;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.chat_dialog, container, false);
		getDialog().setTitle(
				getResources().getString(R.string.chat_title, friendName));

		ListView messagesView = (ListView) view.findViewById(R.id.messages);
		final TextView messageView = (TextView) view.findViewById(R.id.message);

		adapter = new MessagesAdapter(parent.getMainActivity(), R.id.messages,
				messages);
		messagesView.setAdapter(adapter);

		Button sendButton = (Button) view.findViewById(R.id.send);
		sendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String string = messageView.getText().toString();
				if (StringUtils.isEmpty(string))
					return;
				sendMessage(string);
				adapter.notifyDataSetChanged();
				messageView.setText("");
			}
		});

		ImageView closeView = (ImageView) view.findViewById(R.id.close);
		closeView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
				parent.chatClosed();
			}
		});

		return view;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		parent.chatClosed();
	}

	protected void sendMessage(String string) {
		((MainActivity) getActivity()).getRoomListener().sendMessage(string);

		messages.add(new ChatMessage(me, string, true, System
				.currentTimeMillis()));
	}

	public void newMessage() {
		adapter.notifyDataSetChanged();
	}
}
