package com.oakonell.ticstacktoe.ui.network;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.ui.menu.GameTypeSpinnerHelper;
import com.oakonell.ticstacktoe.ui.menu.TypeDropDownItem;

public class OnlineGameModeDialog extends SherlockDialogFragment {
	public static final String SELECT_PLAYER_INTENT_KEY = "select_player";

	public interface OnlineGameModeListener {
		void chosenMode(GameType type, boolean useTurnBased);
	}

	private OnlineGameModeListener listener;
	private boolean isQuick;

	public void initialize(boolean isQuick, OnlineGameModeListener listener) {
		this.listener = listener;
		this.isQuick = isQuick;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.dialog_online_type, container,
				false);

		if (isQuick) {
			getDialog().setTitle(R.string.choose_quick_game_mode_title);
		} else {
			getDialog().setTitle(R.string.choose_online_game_mode_title);
		}

		Button start = (Button) view.findViewById(R.id.start);
		if (isQuick) {
			start.setText(R.string.choose_online_opponent);
		}

		final Spinner typeSpinner = (Spinner) view.findViewById(R.id.game_type);
		GameTypeSpinnerHelper.populateSpinner(getActivity(), typeSpinner);

		final CheckBox realtime = (CheckBox) view
				.findViewById(R.id.realtime);

		start.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dismiss();

				TypeDropDownItem typeItem = (TypeDropDownItem) typeSpinner
						.getSelectedItem();
				boolean isTurnBased = !realtime.isChecked();

				listener.chosenMode(typeItem.type, isTurnBased);
			}
		});
		return view;

	}

}
