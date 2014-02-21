package com.oakonell.ticstacktoe.ui.local;

import java.util.ArrayList;
import java.util.List;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.solver.AiPlayerStrategy;
import com.oakonell.ticstacktoe.ui.menu.GameTypeSpinnerHelper;
import com.oakonell.ticstacktoe.ui.menu.TypeDropDownItem;

public class NewAIGameDialog extends SherlockDialogFragment {

	public interface LocalAIGameModeListener {
		void chosenMode(GameType type, String aiName, int level);

		void cancel();
	}

	private LocalAIGameModeListener listener;

	public void initialize(LocalAIGameModeListener listener) {
		this.listener = listener;
	}

	public static class AiDropDownItem {
		private final String text;
		private final int level;

		public AiDropDownItem(String string, int level) {
			this.level = level;
			this.text = string;
		}

		@Override
		public String toString() {
			return text;
		}
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater
				.inflate(R.layout.dialog_local_ai, container, false);

		getDialog().setTitle(R.string.choose_ai_mode_title);

		List<AiDropDownItem> aiLevels = new ArrayList<NewAIGameDialog.AiDropDownItem>();
		aiLevels.add(new AiDropDownItem(getResources().getString(
				R.string.ai_random), AiPlayerStrategy.RANDOM_AI));
		aiLevels.add(new AiDropDownItem(getResources().getString(
				R.string.ai_easy), AiPlayerStrategy.EASY_AI));
		aiLevels.add(new AiDropDownItem(getResources().getString(
				R.string.ai_medium), AiPlayerStrategy.MEDIUM_AI));
		aiLevels.add(new AiDropDownItem(getResources().getString(
				R.string.ai_hard), AiPlayerStrategy.HARD_AI));

		final Spinner aiLevelSpinner = (Spinner) view
				.findViewById(R.id.ai_level);
		ArrayAdapter<AiDropDownItem> aiLevelAdapter = new ArrayAdapter<AiDropDownItem>(
				getActivity(), android.R.layout.simple_spinner_dropdown_item,
				aiLevels);
		aiLevelSpinner.setAdapter(aiLevelAdapter);
		aiLevelSpinner.setSelection(1);

		final Spinner typeSpinner = (Spinner) view.findViewById(R.id.game_type);
		GameTypeSpinnerHelper.populateSpinner(getActivity(), typeSpinner);

		Button start = (Button) view.findViewById(R.id.start);
		start.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AiDropDownItem selectedItem = (AiDropDownItem) aiLevelSpinner
						.getSelectedItem();

				TypeDropDownItem typeItem = (TypeDropDownItem) typeSpinner
						.getSelectedItem();

				String whiteName = selectedItem.text + " AI";

				dismiss();
				listener.chosenMode(typeItem.type, whiteName,
						selectedItem.level);
			}
		});
		return view;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		listener.cancel();
	}

}
