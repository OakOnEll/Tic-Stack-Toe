package com.oakonell.ticstacktoe.ui;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.oakonell.ticstacktoe.GameContext;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.ui.menu.GameTypeSpinnerHelper;

public class GameTypeDialogFragment extends SherlockDialogFragment {
	private GameContext context;

	public void initialize(GameContext context) {
		this.context = context;
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getDialog().getWindow().setBackgroundDrawable(
				new ColorDrawable(Color.TRANSPARENT));

		View view = inflater.inflate(R.layout.complex_game_mode_help_dialog,
				container, true);
		int howToPlayRes = 0;

		GameType type = context.getGameStrategy().getGame().getType();
		if (type.isJunior()) {
			howToPlayRes = R.string.type_junior_short_descr;
		} else if (type.isNormal()) {
			howToPlayRes = R.string.type_normal_short_descr;
		} else if (type.isStrict()) {
			howToPlayRes = R.string.type_strict_short_descr;
		}

		CharSequence title = GameTypeSpinnerHelper.getTypeName(getActivity(),
				context.getGameStrategy().getGame().getType());
		getDialog().setTitle(title);
		TextView titleView = (TextView) view.findViewById(R.id.title);
		titleView.setText(title);

		TextView descriptionView = (TextView) view
				.findViewById(R.id.how_to_play_text);
		descriptionView.setText(howToPlayRes);

		Button okButton = (Button) view.findViewById(R.id.ok_button);
		okButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		return view;
	}
}
