package com.oakonell.ticstacktoe.ui.network.turn;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.ui.network.ChatMessage;

public class SendMoveFragment extends SherlockDialogFragment {
	private TurnBasedMatchGameStrategy turnStrategy;

	public void initialize(TurnBasedMatchGameStrategy turnStrategy) {
		this.turnStrategy = turnStrategy;
	}

	@Override
	public final View onCreateView(LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState) {
		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getDialog().getWindow().setBackgroundDrawable(
				new ColorDrawable(Color.TRANSPARENT));
		final View view = inflater.inflate(R.layout.turn_send_move_message,
				container, false);
		getDialog().setCancelable(false);

		setTitle(view, getString(R.string.send_move_message));

		final TextView message = (TextView) view.findViewById(R.id.message);

		Button button = (Button) view.findViewById(R.id.positive);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				CharSequence text = message.getText();
				if (!TextUtils.isEmpty(text)) {
					turnStrategy.getChatMessages().add(
							new ChatMessage(turnStrategy.getMeForChat(), text
									.toString(), true, System
									.currentTimeMillis()));
					turnStrategy.setNumNewMessages(1);
				}
				turnStrategy.takeTurn();
				dismiss();
			}
		});

		return view;
	}

	protected void setTitle(final View view, String title) {
		((TextView) view.findViewById(R.id.title)).setText(title);
		getDialog().setTitle(title);
	}
}
